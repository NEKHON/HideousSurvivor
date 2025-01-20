;-----------------------------------------------------------------------------------------------------------------------------------;
; Blitz3D UDP Network library 1.12 Author : Flanker (hide_email('geadupr@gmail.com')), jan 2016 ;
;-----------------------------------------------------------------------------------------------------------------------------------;

; Documentation here : http://www.blitzbasic.com/logs/userlog.php?user=15074&log=1916
; -----------------------------------------------------------------------------------

; Here you can change the timeout variable (milliseconds) :
Global net_timeOut = 10000

; Here you can set the server to act as a router or not :
Global net_autoRouteMessages = True

; here enable or disable auto switch host :
Global net_autoSwitchHost = True

; Also you can set a default port (thanks to Rick Nasher for the idea) :
; (NOTE : if you want to be server, you'll have to make a port forwarding in your router/NAT for this port, or the server port you enter in the console)
Global net_defaultPort = 32512

; From here I can't say better than Stayne on BB forums :
; "Going down the confusing rabbit hole of trying to decipher someone else's network code is a dark one. Gooooooooooood luck." :)
; Feel free to contact me by e-mail if you need help.

;-----------------------------------------------------------------------------------------------------------------------------------;
; Oh, and BTW, this library is licensed under the NoLicenseRequired agreement, meaning that you can do whatever you want with it... ;
;-----------------------------------------------------------------------------------------------------------------------------------;

Type net_client
	Field ID
	Field name$
	Field IP
	Field port
	Field ping
	Field lastPong
	Field masteredcharacter
	Field sincelastpos
End Type

Global net_version$ = "v1.hs"
Global net_mode
Global net_stream
Global net_backupStream
Global net_port
Global net_serverIP
Global net_serverPort
Global net_ID
Global net_countID
Global net_pingTime = MilliSecs()
Global net_lastUpdate = MilliSecs()
Global net_msgCount

Global net_dataBank = CreateBank(512)
Global net_dataOffset ; writing/reading bank offset

; these are not functions but they have a "Net_" prefix as they will be used by library user
Global Net_MsgType
Global Net_MsgFrom
Global Net_MsgTo
Global Net_MsgString$
Global Net_MsgFromIP

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_StartInput()
	
;;; basic start input
	.net_reset
	
	Cls:Locate 0,0
	Print("Hideous Survivor "+core_version+". Debug Main Menu. ")
	Print(CurrentTime())
	Print "[J]oin [H]ost"
	WaitKey()
	If KeyDown(36) Then net_mode=1 Else net_mode=2
	
	
	If net_mode = 1
		Cls:Locate 0,0
		ip$ = Input("Server IP (x.x.x.x) ? ")
		If ip <> ""
			net_serverPort = Input("Server port (0-65535) ? ")
			net_port = Input("Local port (0-65535) ? ")
		Else
			ip = "127.0.0.1"
			net_serverPort = net_defaultPort
			net_port = Rand(1024,65535)
		EndIf
		Cls:Locate 0,0
		Return Net_JoinServer(ip,net_serverPort)
	ElseIf net_mode = 2
		Cls:Locate 0,0
		net_port = Input("Local port (0-65535) ? ")
		If net_port = 0 Then net_port = net_defaultPort
		Cls:Locate 0,0
		Return Net_HostServer(net_port)
	Else
		Goto net_reset
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_HostServer(port)
	
	net_port = port
	
	net_stream = CreateUDPStream(net_port)
	
	If net_stream And Net_StreamBackup()
		net_mode = 2
		net_port = UDPStreamPort(net_stream)
		Return 2
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_JoinServer(ip$,port)
	
	net_serverIP = Net_DotToInt(ip)
	net_serverPort = port
	
	net_stream = CreateUDPStream(net_port)
	
	If net_stream
		
		net_port = UDPStreamPort(net_stream)
		
		net_temp = MilliSecs()
		
; while we didn't reach timeout
		While MilliSecs()-net_temp < net_timeOut
			
; we will send the message every second until timeout or until we are approved
			If MilliSecs()-net_temp2 > 1000
				WriteByte net_stream,201 ; I want to check the library version
				WriteString net_stream,net_version
				SendUDPMsg(net_stream,net_serverIP,net_serverPort)
				net_temp2 = MilliSecs()
			EndIf
			
; check for answer
			If RecvUDPMsg(net_stream)
				If UDPMsgIP(net_stream) = net_serverIP And UDPMsgPort(net_stream) = net_serverPort
					If ReadByte(net_stream) = 202 Then Return 1
				EndIf
			EndIf
			
			Delay 1
		Wend
		
		CloseUDPStream(net_stream):net_stream = 0
		
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_StopNetwork()
	
	If net_stream
		If net_mode = 2
			
; send disconnection message of the server
			n.net_client = First net_client
			Net_SendMessage(101,n\name,n\ID)
			Delete n
			
			If net_autoSwitchHost ; switching host
				
; count players
				For n.net_client = Each net_client
					net_temp = net_temp + 1
				Next
				
				If net_temp > 0
					
; choose the new host, it will be the most ancient client
					n.net_client = First net_client
					
					net_newHostID = n\ID
					net_newHostName$ = n\name
					net_newHostIP = n\IP
					net_newHostPort = n\port
					
; then send new informations to players
					For n.net_client = Each net_client
						
						WriteByte net_stream,206
						WriteShort net_stream,net_newHostID
						
						If n\ID = net_newHostID ; send that only to the new host
							
							WriteShort net_stream,net_temp ; players count
							
							For c.net_client = Each net_client
								WriteShort net_stream,c\ID
								WriteString net_stream,c\name
								WriteInt net_stream,c\IP
								WriteShort net_stream,c\port
							Next
							
						Else ; send new host infos to other players
							
							WriteInt net_stream,net_newHostIP
							WriteInt net_stream,net_newHostPort
							
						EndIf
						
						SendUDPMsg(net_stream,n\IP,n\port)
						
					Next
					
				EndIf
				
			EndIf
			
			If net_backupStream Then CloseUDPStream(net_backupStream):net_backupStream = 0
		Else
			WriteByte net_stream,205
			SendUDPMsg(net_stream,net_serverIP,net_serverPort)
		EndIf
		CloseUDPStream(net_stream):net_stream = 0
	EndIf
	
	For n.net_client = Each net_client
		Delete n
	Next
	
	net_mode = 0
	
	net_stream = 0
	net_port = 0
	
	net_serverIP = 0
	net_serverPort = 0
	
	net_ID = 0
	
	Net_MsgType = 0
	Net_MsgString = ""
	Net_MsgFrom = 0
	Net_MsgTo = 0
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_CreatePlayer(name$)
	
	If net_stream
		
		If net_mode = 2
			
			n.net_client = New net_client
			net_ID = 1
			n\ID = net_ID
			n\name = name
			Return n\ID
			
		Else
			
			net_temp = MilliSecs()
			
; while we didn't reach timeout
			While MilliSecs()-net_temp < net_timeOut
				
; we will send the message every second until timeout or until we are approved
				If MilliSecs()-net_temp2 > 1000
					WriteByte net_stream,203 ; I want to check the library version
					WriteString net_stream,name
					SendUDPMsg(net_stream,net_serverIP,net_serverPort)
					net_temp2 = MilliSecs()
				EndIf
				
; check for answer
				If RecvUDPMsg(net_stream)
					If UDPMsgIP(net_stream) = net_serverIP And UDPMsgPort(net_stream) = net_serverPort
						If ReadByte(net_stream) = 204
							net_ID = ReadShort(net_stream)
							Return net_ID
						EndIf
					EndIf
				EndIf
				
				Delay 1
			Wend
			
			CloseUDPStream(net_stream):net_stream = 0
			
		EndIf
		
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_RequestPlayer(id)
	
	If net_mode <> 2
		WriteByte net_stream,207
		WriteShort net_stream,id
		SendUDPMsg(net_stream,net_serverIP,net_serverPort)
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_KickPlayer(id,reason$)
	
	If net_mode = 2
		
		For n.net_client = Each net_client
			
			If n\id = id
				Net_SendMessage(101,n\name,n\ID)
				Net_SendMessage(119,reason,1,n\id)
				Delete n
			EndIf
			
		Next
		
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_CheckMessage()
	
	If net_stream
		
		If RecvUDPMsg(net_stream)
			
			If net_mode <> 2 ; if we are client
; we will ignore messages that are not sent from the server
				If UDPMsgIP(net_stream) <> net_serverIP Or UDPMsgPort(net_stream) <> net_serverPort Then Return
				net_lastUpdate = MilliSecs() ; client side timeout check
			EndIf
			
			net_temp = ReadByte(net_stream)
			
			If net_temp > 0 And net_temp < 200
				
				Net_MsgType = net_temp
				Net_MsgFrom = ReadShort(net_stream)
				Net_MsgTo = ReadShort(net_stream)
				Net_MsgString = ReadString(net_stream)
				Net_MsgFromIP = UDPMsgIP(net_stream)
				
				If ReadAvail(net_stream) ; binary data
					net_dataOffset = ReadAvail(net_stream)
					ReadBytes net_dataBank,net_stream,0,net_dataOffset
				EndIf
				
				If net_mode = 2 And net_autoRouteMessages And Net_MsgFrom <> net_ID And Net_MsgTo <> net_ID And Net_MsgType < 100
					Net_RouteMessage(Net_MsgType,Net_MsgFrom,Net_MsgTo,Net_MsgString)
				Else
					net_dataOffset = 0
				EndIf
				
				Return 1
			Else
				
				Select net_temp
						
					Case 0 ; ping-pong
						
						If net_mode = 2
							For n.net_client = Each net_client
								If UDPMsgIP(net_stream) = n\IP And UDPMsgPort(net_stream) = n\port
									n\lastPong = MilliSecs()
									n\ping = n\lastPong-net_pingTime
									If n\ping > 999 Then n\ping = 999
									Net_SendMessage(103,n\ping,n\ID)
									Net_SendMessage(103,n\ping,n\ID,n\ID)
								EndIf
							Next
						ElseIf UDPMsgIP(net_stream) = net_serverIP And UDPMsgPort(net_stream) = net_serverPort
							WriteByte net_stream,0
							SendUDPMsg(net_stream,net_serverIP,net_serverPort)
						EndIf
						
					Case 201 ; a client wants to check his library version
						
						If net_mode = 2
							If ReadString(net_stream) = net_version
								WriteByte net_stream,202 ; client library version approved
								SendUDPMsg(net_stream,UDPMsgIP(net_stream),UDPMsgPort(net_stream))
							EndIf
						EndIf
						
					Case 203 ; a client wants to connect
						
						If net_mode = 2
							
; verify if the pair ip/port already exists
							For n.net_client = Each net_client
; if so, we resend him the approval, ID, and the list of clients
								If UDPMsgIP(net_stream) = n\IP And UDPMsgPort(net_stream) = n\port
									WriteByte net_stream,204 ; connection approved
									WriteShort net_stream,n\ID
									SendUDPMsg(net_stream,n\IP,n\port)
									
									For c.net_client = Each net_client
										If c\ID <> n\ID Then Net_SendMessage(100,c\name,c\ID,n\ID)
									Next
									
									Return
								EndIf
							Next
							
; everything is fine create the client
							n.net_client = New net_client
							n\name = ReadString(net_stream)
							For n2.net_client = Each net_client
								If n2\id<>n\id And n2\name=n\name Then n\name = get_randomname(0)+Int(iasc( get_randomname$(1)))
							Next
							n\ID = clamp(iasc(n\name),2,intmax())
							n\IP = UDPMsgIP(net_stream)
							n\port = UDPMsgPort(net_stream)
							n\lastPong = MilliSecs()
							
; inform the player that we approved
							WriteByte net_stream,204 ; connection approved
							WriteShort net_stream,n\ID
							SendUDPMsg(net_stream,n\IP,n\port)
							
; and we tell him who is already connected
							For c.net_client = Each net_client
								If c\ID <> n\ID Then Net_SendMessage(100,c\name,c\ID,n\ID)
							Next
							
; finally, inform all players that a New player has joined
							Net_SendMessage(100,n\name,n\ID)
							
						EndIf
						
					Case 205 ; a client is leaving
						
						If net_mode = 2
							
; check wich client is leaving
							For n.net_client = Each net_client
								If UDPMsgIP(net_stream) = n\IP And UDPMsgPort(net_stream) = n\port
									Net_SendMessage(101,n\name,n\ID) ; tell other clients who leaved
									Delete n
									Return
								EndIf
							Next
							
						EndIf
						
					Case 206 ; the host is leaving, new host informations
						
						If net_mode = 1
							net_temp = ReadShort(net_stream) ; new host ID
							
							If net_temp = net_ID ; we are the new host
								If Net_StreamBackup()
									net_mode = 2
									net_temp = ReadShort(net_stream) ; number of players to create
									
									For i = 1 To net_temp
										n.net_client = New net_client
										n\ID = ReadShort(net_stream)
										n\name = ReadString(net_stream)
										n\IP = ReadInt(net_stream)
										n\port = ReadShort(net_stream)
										n\lastPong = MilliSecs()
									Next
									
									Net_SendMessage(102,"",net_ID)
									
								Else
									Net_StopNetwork()
									RuntimeError("Failed to become the new host.")
								EndIf
							Else
								net_serverIP = ReadInt(net_stream)
								net_serverPort = ReadInt(net_stream)
							EndIf
						EndIf
						
					Case 207 ; a player needs information about one specific client
						
						If net_mode = 2
							
; wich client needs infos
							For n.net_client = Each net_client
								
								If UDPMsgIP(net_stream) = n\IP And UDPMsgPort(net_stream) = n\port
									
; about wich client
									net_temp = ReadShort(net_stream)
									
; retrieve infos
									For c.net_client = Each net_client
										If c\ID <> n\ID And c\ID = net_temp Then Net_SendMessage(100,c\name,c\ID,n\ID):Return
									Next
									
								EndIf
							Next
							
						EndIf
						
				End Select
				
			EndIf
			
		EndIf
		
		If net_mode = 2
			Net_PingClients()
		Else
			If MilliSecs()-net_lastUpdate > net_timeOut
				Net_StopNetwork()
				RuntimeError("Connection lost with server.")
			EndIf
		EndIf
		
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_SendMessage(typ,message$="",sender=0,recipient=0)
	
	If sender = 0 Then sender = net_ID
	
	If net_mode = 2
		
		For n.net_client = Each net_client
			
			If recipient = n\ID Or recipient = 0
				
				If n\ID <> net_ID
					
					If n\ID <> sender Or n\ID = recipient
						
						WriteByte net_stream,typ
						WriteShort net_stream,sender
						WriteShort net_stream,recipient
						WriteString net_stream,message
						
						If net_dataOffset > 0 Then WriteBytes net_dataBank,net_stream,0,net_dataOffset
						
						SendUDPMsg(net_stream,n\IP,n\port)
					EndIf
					
				ElseIf typ > 99
					
					WriteByte net_backupStream,typ
					WriteShort net_backupStream,sender
					WriteShort net_backupStream,recipient
					WriteString net_backupStream,message
					SendUDPMsg(net_backupStream,2130706433,net_port) ; integer IP "127.0.0.1"
					
				EndIf
				
			EndIf
		Next
		
	Else
		
		WriteByte net_stream,typ
		WriteShort net_stream,sender
		WriteShort net_stream,recipient
		WriteString net_stream,message
		
		If net_dataOffset > 0 Then WriteBytes net_dataBank,net_stream,0,net_dataOffset
		
		SendUDPMsg(net_stream,net_serverIP,net_serverPort)
		
	EndIf
	
	net_dataOffset = 0
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_RouteMessage(typ,sender,recipient,message$)
	
; we check if the message comes from one of the client (protection from outside)
; and if the sender ID matches the IP and port and finally if the client doesn't try to send a message to himself (protection from inside)
	For n.net_client = Each net_client
		If UDPMsgIP(net_stream) = n\IP And UDPMsgPort(net_stream) = n\port
			If sender = n\ID And recipient <> n\ID Then approved = 1
		EndIf
	Next
	
	If approved
		
		For n.net_client = Each net_client
			
			If recipient = n\ID Or recipient = 0
				
				If n\ID <> net_ID
					
					If n\ID <> sender
						WriteByte net_stream,typ
						WriteShort net_stream,sender
						WriteShort net_stream,recipient
						WriteString net_stream,message
						
						If net_dataOffset > 0 Then WriteBytes net_dataBank,net_stream,0,net_dataOffset
						
						SendUDPMsg(net_stream,n\IP,n\port)
					EndIf
					
				EndIf
				
			EndIf
		Next
		
	EndIf
	
	net_dataOffset = 0
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_WriteByte(value)
	PokeByte net_dataBank,net_dataOffset,value
	net_dataOffset = net_dataOffset + 1
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_WriteShort(value)
	PokeShort net_dataBank,net_dataOffset,value
	net_dataOffset = net_dataOffset + 2
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_WriteInt(value)
	PokeInt net_dataBank,net_dataOffset,value
	net_dataOffset = net_dataOffset + 4
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_WriteFloat(value#)
	PokeFloat net_dataBank,net_dataOffset,value
	net_dataOffset = net_dataOffset + 4
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_WriteString(value$)
	PokeInt net_dataBank,net_dataOffset,Len(value)
	net_dataOffset = net_dataOffset + 4
	For i = 0 To Len(value)-1
		PokeByte net_dataBank,net_dataOffset+i,Asc(Mid(value,i+1,1))
	Next
	net_dataOffset = net_dataOffset + Len(value)
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_ReadByte()
	net_dataOffset = net_dataOffset + 1
	Return PeekByte(net_dataBank,net_dataOffset-1)
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_ReadShort()
	net_dataOffset = net_dataOffset + 2
	Return PeekShort(net_dataBank,net_dataOffset-2)
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_ReadInt()
	net_dataOffset = net_dataOffset + 4
	Return PeekInt(net_dataBank,net_dataOffset-4)
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_ReadFloat#()
	net_dataOffset = net_dataOffset + 4
	Return PeekFloat(net_dataBank,net_dataOffset-4)
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_ReadString$()
	
	lenght = PeekInt(net_dataBank,net_dataOffset)
	net_dataOffset = net_dataOffset + 4
	For i = 0 To lenght-1
		chars$ = chars$ + Chr(PeekByte(net_dataBank,net_dataOffset+i))
	Next
	net_dataOffset = net_dataOffset + lenght
	Return chars
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_StreamBackup()
	
	net_backupStream = CreateUDPStream()
	Return net_backupStream
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_PingClients()
	
	If MilliSecs() - net_pingTime > 1000
		
		For n.net_client = Each net_client
			
			If n\ID <> net_ID
				WriteByte net_stream,0
				SendUDPMsg(net_stream,n\IP,n\port)
				
				If MilliSecs() - n\lastPong > net_timeOut
					Net_SendMessage(101,n\name,n\ID) ; tell other clients who leaved
					Delete n
				EndIf
			EndIf
		Next
		
		net_pingTime = MilliSecs()
		
	EndIf
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_GenerateID()
	
	While id = 0
		
		id = Rand(1,255)
		
		For n.net_client = Each net_client
			If n\ID = id Then id = 0
		Next
		
	Wend
	
	Return id
	
End Function

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
Function Net_DotToInt%(ip$)
	
; thanks to Chroma on b3d forums for the code !
	
	off1=Instr(ip$,".") :ip1=Left$(ip$,off1-1)
	off2=Instr(ip$,".",off1+1):ip2=Mid$(ip$,off1+1,off2-off1-1)
	off3=Instr(ip$,".",off2+1):ip3=Mid$(ip$,off2+1,off3-off2-1)
	off4=Instr(ip$," ",off3+1):ip4=Mid$(ip$,off3+1,off4-off3-1)
	Return ip1 Shl 24 + ip2 Shl 16 + ip3 Shl 8 + ip4
	
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D