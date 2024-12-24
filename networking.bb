; globals 
Global LocalMode ; host, client or connection fail
Global LocalName$ ;  player nickname 
Global localID

Const host_positionrate = 20
Const client_mcpositionrate = 20 
Const mp_positionupdatemin# = 0.1 ; minimal distance that need to be passed to send new position info
Const mp_serverocclusion = 1 
Const mp_serverdistcull = 128 

Global my_oldx#
Global my_oldy#
Global my_oldz#
Global my_olddir%
Global my_ping

Global host_beforenextpositionsend
Global client_beforenextpositionsend

Global anticheat_log_file = WriteFile("Logs/anticheat_log_"+CurrentDate()+","+MilliSecs())

Type local_client ; client on clients
	Field ID%
	Field name$
	Field masteredcharacter%
	Field ping
End Type

Function network_funcs()
	
	If LocalMode=2 Then
		; host gives players info about characters
		If MilliSecs()>host_beforenextpositionsend+host_positionrate 
			For nc.net_client = Each net_client
				s$=""
				If nc\id>1 Then
					For char.character = Each character
						dist# = flatdist(EntityX(char\mesh),char\oldx,EntityZ(char\mesh),char\oldz)
						If char\master_player<>nc\id Then
							success=0
						; ----------- SERVER OCCLUSION (TODO)
							If mp_serverocclusion=1 Or mp_serverdistcull>0 Then
								For char2.character = Each character
									If char2\master_player=nc\id Then
										If mp_serverdistcull>0 Then If EntityDistance(char\mesh,char2\mesh)<mp_serverdistcull Then success=1
										;If mp_serverocclusion>0 Then If EntityVisible(char2\mesh,char\mesh)<>0 Then success=2
										;If mp_serverdistcull>0 And mp_serverocclusion>0 Then If success=1 Then success=0
									End If
								Next
							End If
						; ---------
							If success>0 Or mp_serverocclusion=0 Then
								tmp_s$ = ":"+char\id+"/"+EntityX(char\mesh)+"/"+EntityZ(char\mesh)+"/"+Int(char\directiony)+";"
								s$=s$+tmp_s
							End If
						End If
					Next
					net_sendmessage(131,s,1,nc\id)
				End If
			Next
		End If
	End If
	; -------------
	
	While Net_CheckMessage() ; will check for a new message and fill Net_MsgType, Net_MsgString$, Net_MsgFrom and Net_MsgTo variables so we can read it
		; -- signal from unknown player, asking host for info about him
		success=0
		If LocalMode=1 And net_msgtype>103 Then
			For lc.local_client = Each local_client
				If lc\id = net_msgfrom Then success=1 Exit
			Next
			If success=0 Then Net_RequestPlayer(Net_MsgFrom)
		End If
		; ----------
		Select Net_MsgType ; check the type of message
			Case 90 ; asked for map info
				update_characterdata()
				If LocalMode = 2 Then
					If Len(net_msgstring)>0 Then
						If net_msgstring = characterslist Then 
							net_sendmessage(125,"$") 
							DebugLog("Character list accepted, sending accept")
						Else 
							net_sendmessage(125,characterslist)
							DebugLog("Wrong characterlist, ponging it back")
						End If
					Else
						net_sendmessage(125,characterslist)
						DebugLog("No characterlist, ponging it")
						DebugLog(characterslist)
					End If
				End If
			Case 100 ; new player connected, OR the server tells us who is already connected so we can create players when joining the game
				If LocalMode=1 Then
					For lc.local_client=Each local_client
						If lc\id=net_msgfrom Then Return
					Next ; check if that player already exists
					clog(net_msgstring+" Joined")
					;anticheat_log(Net_msgstring+" Joined")
					lc.local_client=New local_client
					DebugLog("new lc")
					lc\id=net_msgfrom
					lc\name=net_msgstring
					For char.character = Each character
						If char\master_player = lc\id Then lc\masteredcharacter=char\id
					Next
				End If
			Case 101 ; a player has quit
				For char.character = Each character ; remove info about player from character
					If char\master_player = Net_msgfrom Then char\master_player = 0 Exit
				Next
				For lc.local_client = Each local_client ; delete client instance
					If lc\id = Net_MsgFrom Then Delete lc Exit
				Next
				clog(Net_msgstring+" Leaved.")
				;anticheat_log(Net_msgstring+" Leaved.")
			Case 103 ; ping
				If LocalMode=1 Then
					For lc.local_client = Each local_client
						lc\ping = net_msgstring
					Next
				End If
			Case 115 ; guy asks for char
				procced=1
				For nc.net_client = Each net_client ; check if that guy have character
					If nc\id = net_msgfrom And nc\masteredcharacter<>0 Then procced=0 Exit
				Next
				If LocalMode=2 And procced=1 Then
					success=1
					For char.character = Each character
						If char\master_player = 0 Then
							For nc.net_client = Each net_client
								If nc\id = net_Msgfrom And nc\masteredcharacter=0 Then
									char\master_player = net_msgfrom
									nc\masteredcharacter = char\id
									net_sendmessage(116,char\id+"/"+net_msgfrom)
									DebugLog("sent player a character")
									success=1
									Exit
								End If
							Next
							Exit
						End If
					Next
					If success=0 Then RuntimeError("asdfpko")
				End If
			Case 116 ;player takes  character
				DebugLog("got 116")
				If LocalMode=1 Then
					success=0
					clog("got a char")
					For char.character = Each character
						If char\master_player = Right(net_msgstring,Len(net_msgstring)-Instr(net_msgstring,"/",1)) Then success=1 Exit ; already tooken it, meow
						If char\id=Left(net_msgstring,Instr(net_msgstring,"/",1)-1) Then
							char\master_player = Right(net_msgstring,Len(net_msgstring)-Instr(net_msgstring,"/",1))
							For lc.local_client = Each local_client
								If lc\id=char\master_player Then lc\masteredcharacter=char\id success=1 Exit
							Next
							Exit
						End If
					Next
					If success=0 Then RuntimeError("Networking error, Signal 166 (Give client a character). Couldnt found character with ID "+Left(net_msgstring,Instr(net_msgstring,"/",1)-1)+ " For player "+Right(net_msgstring,Len(net_msgstring)-Instr(net_msgstring,"/",1)))
				End If
			Case 119 ; KICKED BY HOST
				If net_msgfrom=1 And localID>1 Then RuntimeError("Kicked from server, Reason: "+net_msgstring)
			; -----
			Case 130 ; HOST recieved player position update
				For nc.net_client = Each net_client
					If MilliSecs()-nc\sincelastpos>=client_mcpositionrate And nc\id=net_msgfrom Then ; if player sending too much packages, then just ignore them, no cheats!
						For char.character = Each character
							If char\master_player = nc\id Then ; this is character of guy who wants for us to allow his input
								tmp_s$ = net_msgstring 
								offset1 = Instr(tmp_s,"/",1)
								offset2 = Instr(tmp_s,"/",offset1+1)
								If offset1=0 Or offset2=0 Then clog("Signal 130 Error 1.") Exit ; if somehow data got corrupted
								;------------------
								x# = Left(tmp_s,offset1-1)
								z# = Mid(tmp_s,offset1+1,offset2-offset1-1)
								dir# = Right(tmp_s,Len(tmp_s)-offset2)
								PositionEntity char\mesh,x,0,z,1
								char\directiony = dir
								Exit
							End If
						Next
						Exit
					End If
				Next
			Case 131 ; recieved characters positions package
				If LocalMode=1 And Len(net_msgstring)>0 ; if client and if data exists
					For char.character = Each character
						offset1 = Instr(net_msgstring,":"+char\id,1) ; search for data bout that character
						If offset1<>0 Then ; if have data bout that character
							offset2 = Instr(net_msgstring,";",offset1)
							; cut things that we dont need (data about other characters)
							tmp_s$ = Mid(net_msgstring,offset1,offset2-offset1)
							tmp_s$ = Mid(tmp_s,Instr(tmp_s,"/",1)+1,Len(tmp_s)-Instr(tmp_s,"/",1))
							; ---------------
							offset1=Instr(tmp_s,"/",1)
							offset2=Instr(tmp_s,"/",offset1+1)
							x#=Left(tmp_s,offset1-1) ; his X pos
							z#=Mid(tmp_s,offset1+1,offset2-offset1-1) ; his Z pos
							dir#=Right(tmp_s,Len(tmp_s)-offset2) ; his direction
							char\directiony=dir
							PositionEntity char\mesh,x,0,z
							ShowEntity char\mesh
						Else ; host dont wants for us to see that character
							HideEntity char\mesh
						End If
					Next
				End If
			Case 140 ; recieved create dropped item signal
				If LocalMode=1 And net_msgfrom=1 Then ; only host can send create dropped item signal, and only client should accept it
					s$ = Right(net_msgstring,Len(net_msgstring)-Instr(net_msgstring,"|",1)) ; item data
					d$ = Left(net_msgstring,Instr(net_msgstring,"|",1)) ; position 
					of1 = Instr(d,";",1)
					of2 = Instr(d,";",of1+1)
					create_droppeditem(Left(d,of1-1),Mid(d,of1+1,of2-of1-1),Right(d,Len(d)-of2),s)
				End If
			Case 141 ; player wants to pickup smth
				If LocalMode=2 Then
					For nc.net_client = Each net_client
						If nc\id = net_msgfrom Then
							For di.dropped_item = Each dropped_item
								If di\idata = net_msgstring Then 
									For char.character = Each character
										If char\master_player = net_msgfrom Then
											If Len(char\stronghand)=0 Then
												net_sendmessage(143,"s"+di\idata,1,net_msgfrom) 
												Exit
											ElseIf Len(char\weakhand)=0 Then 
												net_sendmessage(143,"w"+di\idata,1,net_msgfrom) 
												Exit
											End If
											clog("Networking -> Signal 141, Failed to make player pick up item cuz both hands is buzy, desync or cheats.")
											Exit
										End If
									Next
									net_sendmessage(142,di\idata,1,0)
									Exit
								End If 
							Next
							Exit
						End If
					Next
				End If
			Case 142 ; hosts removes item
				success=0
				For di.dropped_item = Each dropped_item
					If di\idata = net_msgstring Then
						FreeEntity di\entity
						Delete di
						success=1
						Exit
					End If
				Next
				If success=0 Then RuntimeError("host remove physitem fail "+net_msgstring)
			Case 143 ; host adds item
				success=0
				If LocalMode=1 Then
					For char.character = Each character
						If char\master_player = localID Then
							item$ = Right(net_msgstring,Len(net_msgstring)-1)
							addto$=Lower(Mid(net_msgstring,1,1))
							Select addto
								Case "w"
									char\weakhand = item
								Case "s"
									char\stronghand = item
							End Select
							clog("host given me "+net_msgstring)
							If Len(char\weakhand)>0 Or Len(char\stronghand)>0 Then success=1
							rhitname$=char\stronghand
							lhitname$=char\weakhand
							Exit
						End If
					Next
				End If
				If success=0 Then RuntimeError("Fail to give "+net_msgstring+" to mastered character of "+localID)
			Case 144 ; client does something in inv
				;Select net_msgstring
					;Case 0 ; wield
						
					;Case 
				;End Select
		End Select
	Wend
End Function

Function anticheat_log(id%,info$)
	For nc.net_client = Each net_client
		If nc\id = id Then name$ = nc\name Exit
	Next
	s$ = "ANTICHEAT: "+CurrentTime() +", TRIGGERED BY "+name$+"("+id+"), CASE: "+info
	WriteLine(anticheat_log_file,s)
End Function

Function idtoname$(id%)
	Select LocalMode
		Case 1
			For lc.local_client = Each local_client
				If lc\id=id Then Return lc\name
			Next
		Default
			For nc.net_client = Each net_client
				If nc\id=id Then Return nc\name
			Next
	End Select
	Return("UNKNOWN")
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D