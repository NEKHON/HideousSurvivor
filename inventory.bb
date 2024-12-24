Type dropped_item
	Field entity%
	Field idata$
End Type
Global dropitems_gravity# = -6
Global rhitname$="???"
Global lhitname$="???"
Global itemtip$

Global sfx_inventory_priority = LoadSound("sounds/interactions/inventory_rearange.ogg")

; item data: :HANDLER/FLAGS/CONDITION/STACKSIZE/[LOCALINVENTORY];

; TODO: it should be inventory_collumn, instead of row, and inventory_scroll should be inventory_row, but im lazy.
Function inventory_gui(sorting$="")
	For char.character = Each character
		If char\master_player = localid Then 
			cam_pitch = cam_pitch + 0.25 ; move camera down
			; ------------- HEADERS
			TEXT3D vb20,0,100,inventory_scroll+" | "+inventory_row
			text3d vb20,-155,65,"Wear: "
			Text3d vb20,25,65," Pockets: "
			; ---------------------------- CURRENT ROW
			Select inventory_row
				Case 0
					text3d vb20,-155,100,"\/"
				Case 1
					text3d vb20,45,100,"\/"
				Case 2
					text3d vb20,270,100,"\/"
			End Select
			; --------------------- DISPLAY HANDS
			If inventory_scroll=0 And inventory_row=2 Then text3d vb20s,270,65,"> Right Hand: "+char\stronghand Else text3d vb20,270,65,"Right Hand: "+char\stronghand
			If inventory_scroll=1 And inventory_row=2 Then text3d vb20s,270,45,"> Left Hand: "+char\weakhand Else text3d vb20,270,45,"Left Hand: "+char\weakhand
			; --------------------------------------------------------
			amount_ofclothes=ar_len(char\wear)
			amount_ofitems=0
			For i=0 To amount_ofclothes
				g$=ar_poke(char\wear,i) ; get  this item
				s$=Mid(g,1,Instr(g,"/",1)-1) ; getits handler
				; --------- find this item handler, and then itemtype
				off1=Instr(handlerslist,s,1) 
				off2=Instr(handlerslist,"/",off1)
				handler = Mid(handlerslist,off2+1,2)
				clothtype.item_type = Object.item_type(handler)
				; --------------------------------------------------------------
				; - display cloth in wear
				text3d vb20,-155,50-(i*15),clothtype\name
				;------------------------ LIST POCKETS
				If inventory_scroll=i+amount_ofitems And inventory_row = 1 Then ; if cloth select
					If MouseHit(1) And Len(char\stronghand)>0 Then d$=Left(g,Len(g)-1) char\wear = ar_replace(char\wear,i,d+char\stronghand+"$1%]") char\stronghand=""
					text3d vb20s,15,50-i*30-amount_ofitems*15,"> "+clothtype\name+", 0L/5L"
				Else
					text3d vb20,15,50-i*30-amount_ofitems*15,(i+1)+". "+clothtype\name+", 0L/5L"
				End If
				; ------------------- LIST ITEMS IN POCKETS
				cloth_inventory$ = Mid(g,Instr(g,"[",1)+1,Instr(g,"[",1)-Instr(g,"]",1)-1)
				If Len(cloth_inventory)>1 Then 
					; --------------------- PARSE
					itl_oldof1=1 ; itemlist_oldoffset
					a=-1 ; temp variable to offset items in list
					prevamount_ofitems=amount_ofitems
					Repeat 
						itl_of1=Instr(cloth_inventory,"$1%",itl_oldof1+1)
						If itl_of1=0 Then Exit ; this was the last item, nothing more
						If a>-1 Then b=3 Else b=0 ; if first item, it dont have $1% behind, so dont count it
						itl_item$=Mid(cloth_inventory,itl_oldof1+b,itl_of1-itl_oldof1-b)
						itl_oldof1=itl_of1
						amount_ofitems=amount_ofitems+1
						a=a+1 ; items list continues
						; if item selected or not
						If inventory_scroll = i+amount_ofitems And inventory_row=1 Then
							text3d vb20s,35,35-(i*30)-(prevamount_ofitems*15)-(a*15),"> "+itl_item
						Else
							text3d vb20,35,35-(i*30)-(prevamount_ofitems*15)-(a*15),"- "+itl_item
						End If
					Forever
					;  --------------------------------------
				Else  
					; -------------------------------- NOTHING TO PARSE
					text3d vb20,35,35-i*30-amount_ofitems*15,"Empty"
				EndIf
			Next ; END OF INVENTORY DISPLAY
			; -------- INVENTORY LIMITS
			Select inventory_row ; 
				Case 0 ; CLOTHES
					
				Case 1 ; ---- INV
					If inventory_scroll>amount_ofclothes+amount_ofitems Then inventory_scroll = amount_ofclothes +amount_ofitems 
					If inventory_Scroll<0 Then inventory_scroll = 0
				Case 2 ; HANDS
					If inventory_scroll>1 Then inventory_scroll=1
					If inventory_scroll<0 Then inventory_scroll=0
			End Select
			If inventory_row<0 Then inventory_row=0
			If inventory_row>2 Then inventory_row=2
			; ------------------------------------------
		End If
	Next
End Function

Function wield_interaction()
	For char.character = Each character
		If char\master_player = localid Then
			wield_name$=""
			If Len(char\stronghand)>0 Then wield_name$ = Mid(char\stronghand,1,Instr(char\stronghand,"/",1)-1)
			If Len(wield_name)>0 Then
				; find that item type
				off1=Instr(handlerslist,wield_name,1)
				If off1 = 0 Then RuntimeError(wield_name+" IS NOT PRESENTED IN HANDLERSLIST")
				off2=Instr(handlerslist,"/",off1)
				If off2=0 Then RuntimeError("HANDLER OF " + wield_name + " IS Not PRESENTED IN HANDLERSLIST")
				handler = Mid(handlerslist,off2+1,off2-Instr(handlerslist,";",off2)-1)
				itype.item_type = Object.item_type(handler)
				Select itype\globaltype
					Case "clothes"
						itemtip="Press [O] To take on "+itype\name
						If signal_wear Then
							char\wear=ar_add(char\wear,char\stronghand)
							char\stronghand=""
							clog(" I weared "+itype\name)
						End If
				End Select
			End If
			Exit
		End If
	Next
End Function

Function update_droppeditems(dt#)
	For di.dropped_item = Each dropped_item
		EntityColor di\entity,255,255,255
		If EntityY(di\entity)>-1.2 Then MoveEntity di\entity,0,dropitems_gravity*dt,0 EntityColor di\entity,255,0,0
		If EntityY(di\entity)<-1.2 Then PositionEntity di\entity,EntityX(di\entity),-1.4,EntityZ(di\entity)
	Next
End Function

Function create_droppeditem(x#=0,y#=2,z#=0,idata$="",handler$="dummy",flags$="",condition%=-1,stack%=1,localinv$="")
	di.dropped_item = New dropped_item
	di\entity = CreateCube()
	PositionEntity di\entity,x,y,z,1
	ScaleMesh di\entity,0.2,0.2,0.2
	EntityPickMode di\entity,1
	EntityRadius di\entity,0.2,0.2
	NameEntity di\entity,"I"+Handle(di)
	;
	If condition=-1 Then condition=Rand(2,98)
	If Len(idata)=0 Then di\idata = handler+"/("+flags+")/"+condition+"/"+stack+"/["+localinv+"]" Else di\idata=idata
	map_items$=ar_add(map_items$,di\idata)
	If localmode = 2 ; if host, send evewyone info about item
		net_sendmessage(140,x+";"+y+";"+z+"|"+di\idata,1)
	End If 
End Function

Function inv_increaserecursion$(s$)
	tmp% = 1
	Repeat
		offset% = Instr(s,"$"+tmp+"%")
		If offset>0 Then 
			Replace(s,"$"+tmp+"%","$"+(tmp+1)+"%")
		Else
			If Instr(s,"$"+(tmp+1)+"%")>0 Then tmp=tmp+1 Else Exit
		End If
	Forever
	Return s
End Function

; ------ DROPPED ITEMS
Function droppeditem_pickup(handler)
	di.dropped_item = Object.dropped_item(handler)
	If localmode=1 Then
		For char.character = Each character
			If char\master_player = localid Then
				If Len(char\stronghand)=0 Or Len(char\weakhand)=0 Then
					net_sendmessage(141,di\idata,localid,1)
					Exit
				End If
			End If
		Next
	Else
		For char.character = Each character
			If char\master_player = 1 Then 
				If char\stronghand="" Or char\weakhand="" Then
					If Len(char\stronghand)=0 Then
						char\stronghand = di\idata
					Else
						char\weakhand = di\idata
					End If
					net_sendmessage(142,di\idata,1,0)
					rhitname$=char\stronghand
					lhitname$=char\weakhand
					Exit
				Else
					clog("My hands is full")
				End If
			End If
		Next
	End If
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D