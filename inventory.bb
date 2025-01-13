Type dropped_item
	Field entity%
	Field idata$
End Type
Global dropitems_gravity# = -6
Global rhitname$="???"
Global lhitname$="???"
Global itemtip$

Global network_client_pendingitempickup%
Global client_inventory$
Global client_wear$
Global client_stronghand$
Global client_weakhand$
Global selected_cloth ; global so it wont reset when no clothes selected
Global pocketinventory_row
Global wearinventory_row

Global inventory_amountofseenitems=5

Global inventory_ui_selecteditem_scale#=2
Global inventory_ui_item_scale#=1.3
Global inventory_iconsize# = 64 * GraphicsWidth()/GraphicsHeight()

Global sfx_inventory_priority = LoadSound("sounds/interactions/inventory_rearange.ogg")

; item data: :HANDLER/FLAGS/CONDITION/STACKSIZE/[LOCALINVENTORY];

; TODO: it should be inventory_collumn, instead of row, and inventory_row should be inventory_collumn, but im lazy.
Function inventory_interface(sorting$="")
	.update_inventory
	cam_pitch = cam_pitch + 0.25 ; move camera down
	Local amount_ofitems=0
	Local amount_ofdisplitems=0
	Local amount_ofclothes=0
	Local amount_ofbelongitems=0
	Local pocket_items$=""
	DrawImage3d(inventory_background,0,0, 0,0,3.5 * ui_scaling,0)
	inventory_iconsize# = (64 * ui_scaling) 
	Local scale#
	
	; -------- hands
	
	If inventory_collumn=2 And inventory_row=1 Then text3d vb20s,inventory_iconsize*3.2,inventory_iconsize,"> Left Hand "+client_weakhand Else text3d vb20,inventory_iconsize*3.2,inventory_iconsize,"Left Hand "+client_weakhand
	If inventory_collumn=2 And inventory_row=0 Then text3d vb20s,inventory_iconsize*3.2,0,"> Right Hand "+client_stronghand Else text3d vb20,inventory_iconsize*3.2,0,"Right Hand "+client_stronghand
	
	old_off1=0
	Repeat ; wear loop
		; ----- item info
		off1 = Instr(client_wear,"<",old_off1+1)
		If off1=0 Then Exit Else old_off1=off1 amount_ofclothes=amount_ofclothes+1
		off2 = Instr(client_wear,">",off1)
		item$ = Mid(client_wear,off1+1,off2-off1-1)
		id% = Mid(item,1,Instr(item,"(",1)-1) 
		itype.item_type = Object.item_type(id)
		; -----------
		If inventory_collumn=0 Then wearinventory_row = inventory_row
		If amount_ofclothes=(wearinventory_row+1) And inventory_collumn=0 Then selected=1 Else scale#=Abs(inventory_ui_item_scale*ui_scaling) selected=0 
		If selected_cloth = amount_ofclothes Then scale#=Abs(inventory_ui_selecteditem_scale+(Sin(MilliSecs()*0.25)*0.05)*ui_scaling) 
		If selected=1 Then 
			selected_cloth=amount_ofclothes
			If MouseHit(2) And (client_weakhand="" Or client_stronghand="") ; take off cloth
				old_off2=1
				s$=item
				Repeat ; find  items that belong to this pocket, put them in cloth local inv
					off12 = Instr(client_inventory,"<",old_off2)
					If off12=0 Then Exit Else old_off2=off12
					off22 = Instr(client_inventory,">",off12)
					item2$ = Mid(client_inventory,off12+1,off22-off12-1)
					belongsto% = Mid(item2,Instr(item2,"]",1)+1,1)
					If belongsto = selected_cloth Then s$=Left(s,Len(s)-1) + Left(item2,Len(item2)-1)+ "$1%]" client_inventory=Left(client_inventory,off12-1)+Right(client_inventory,Len(client_inventory)-off22) Else old_off2=old_off2+1
				Forever
				client_wear = Left(client_wear,off1-1)+Right(client_wear,Len(client_wear)-off2)
				If Len(client_stronghand)=0 Then client_stronghand=s Else client_weakhand=s
				clog("I taked off "+itype\name+".")
				Goto update_inventory
			End If	
			; -----------
			If MouseHit(1) ; put in item pockets
				If Len(client_stronghand)>0 Then
					client_inventory = client_inventory + "<"+client_stronghand+amount_ofclothes+">"
					client_stronghand=""
				ElseIf Len(client_weakhand)>0 Then
					client_inventory = client_inventory + "<"+client_weakhand+amount_ofclothes+">"
					client_weakhand=""
				End If
			End If
		End If
		DrawImage3d(debug_item,-inventory_iconsize*4,0+((inventory_iconsize+(25*ui_scaling))*amount_ofclothes)-((inventory_iconsize+(10*ui_scaling))*4), 0,0,scale,0)
		; todo, changing scale if too much clothes
	Forever
	old_off1=0
	; ----- DISPALY POCKET
	pocket_items$ = get_clothinventory$(client_inventory,selected_cloth)
	old_off1=0
	Repeat ; display items in pocket
		If Len(pocket_items)=0 Then Exit ; no items in pocket, skip
		;		RuntimeError(pocket_items)
		off1 = Instr(pocket_items$, "<",old_off1+1)
		If off1=0 Then Exit Else old_off1=off1 amount_ofitems=amount_ofitems+1
		off2 = Instr(pocket_items$ ,">",off1)
		item$ = Mid(pocket_items$ ,off1+1,off2-off1-1)
		id% = Mid(item,1,Instr(item,"(",1)-1)
		flags_start% = Instr(item,"(",1)
		flags_end% = Instr(item,")",flags_start)
		condition = Mid(item,flags_end+1,Instr(item,"/",flags_end)-flags_end-1)
		condition_name$ = LOC_CONDITIONS$(clamp((condition/10)-1,0,9))
		;		text3d vb20,0,0,id
		itype.item_type = Object.item_type(id)
		; -----------
		If inventory_collumn=1 Then pocketinventory_row = inventory_row
		If amount_ofitems=(pocketinventory_row+1) And inventory_collumn=1 Then scale#=Abs(inventory_ui_selecteditem_scale+(Sin(MilliSecs()*0.25)*0.05)*ui_scaling) selected=1 Else scale#=Abs(inventory_ui_item_scale*ui_scaling) selected=0 
		If (amount_ofitems>=(pocketinventory_row+1)-((inventory_amountofseenitems-1)/2) And amount_ofitems<=(pocketinventory_row+1)+((inventory_amountofseenitems-1)/2)) Then ; display seen items
			amount_ofdisplitems=amount_ofdisplitems+1
			DrawImage3d(debug_item,0,0+((inventory_iconsize+(25*ui_scaling))*amount_ofdisplitems)-((inventory_iconsize+(10*ui_scaling))*3), 0,0,scale,0)
			If selected=1 Then currentfont=vb20s Else currentfont=vb20 ; font
			If selected=1 Then 
				; INTERACTIONS
				If MouseHit(1) Then ; wield item
					If Len(client_stronghand)=0 Then ; right hand is empty, put item in it
						client_stronghand=Left(item,Len(item)-1)
						off12 = Instr(client_inventory,"<"+item,1)
						client_inventory = Left(client_inventory,off12-1)+Right(client_inventory,Len(client_inventory)-Instr(client_inventory,">",off12))
						Goto update_inventory
					Else If Len(client_weakhand)=0 Then ; left hand is empty, put item in it
						client_weakhand=Left(item,Len(item)-1)
						off12 = Instr(client_inventory,"<"+item,1)
						client_inventory = Left(client_inventory,off12)+Right(client_inventory,Len(client_inventory)-Instr(client_inventory,">",off12))
						Goto update_inventory
					Else
						clog("My hands is full.")
					End If
				End If 
				; -----
				; display item condition
				text3d currentfont,-StringWidth3d(currentfont,condition_name)/2,0+(inventory_iconsize+(25*ui_scaling))*amount_ofdisplitems-((inventory_iconsize+(10*ui_scaling))*3)-inventory_iconsize/1.5,condition_name
			End If
			text3d currentfont,-StringWidth3d(currentfont,itype\name)/2,0+(inventory_iconsize+(25*ui_scaling))*amount_ofdisplitems-((inventory_iconsize+(10*ui_scaling))*3)-inventory_iconsize/2,itype\name
		End If
	Forever
	If amount_ofitems>0 Then
		;show that there is more items that is cant be seen
		b = pocketinventory_row ; amsssount of items before pocket_inventory_row
		a = amount_ofitems - pocketinventory_row - 1 ; amount of items after pocketinventory_row
		;Text 10,200,"before: "+b
		;Text 10,250,"after: "+a
		If b > ((inventory_amountofseenitems-1)/2) Then DrawImage3d(inventory_cantseeitem, 0,-inventory_iconsize*3, 0,0,-0.4,0)
		If a > ((inventory_amountofseenitems-1)/2) And a=<amount_ofitems Then DrawImage3d(inventory_cantseeitem,0,inventory_iconsize*(amount_ofdisplitems-1), 0,0,0.4,0)
	End If
	; ---------------------
	; inventory limits
	Select inventory_collumn
		Case 0 ; wear
			If inventory_row<0 Then inventory_row = 0
			If inventory_row>amount_ofclothes-1 Then inventory_row= amount_ofclothes-1
		Case 1 ; items
			If inventory_row<0 Then inventory_row = 0
			If inventory_row>amount_ofitems-1 Then inventory_row=amount_ofitems-1
		Case 2 ; hands
			If inventory_row<0 Then inventory_row=0
			If inventory_row>1 Then inventory_row=1
	End Select
	; 3 collumns
	If inventory_collumn<0 Then inventory_collumn=0
	If inventory_collumn>2 Then inventory_collumn=2
	
;	If inventory_collumn=1 And amount_ofitems=0 Then ; if items selected, but no items then switch to next collumn todo that dont works
;		If old_collumn=0 Then inventory_collumn=2
;		If old_collumn=2 Then inventory_collumn=0
;	End If
;	If inventory_collumn=0 And amount_ofclothes=0 Then inventory_collumn=2 ; if clothes collumn selected but no clothes weared then select hands
	If inventory_collumn=2 And (Len(client_weakhand)=0 And Len(client_stronghand)=0) Then inventory_collumn=1
	old_collumn = inventory_collumn
End Function

Function wield_interaction()
	itemtip=""
	rhitname=""
	lhitname=""
	If Len(client_stronghand)>0 Then 
		id% = Mid(client_stronghand,1,Instr(client_stronghand,"/",1)-1)
		itype.item_type = Object.item_type(id)
		rhitname=itype\name
		; ----
		If itype\name = "Military Jacket" ; DEBUG HARD CODE, FIX ASAP
			itemtip = "[O] To wear "+itype\name
			If signal_wear Then ; check if can wear cloth
				If Len(client_weakhand)>0 Then clog("My both hands is full, cant take on that cloth") signal_wear=0
			End If
			If signal_wear Then ; wear cloth
				If Instr(client_stronghand,"$1%",1)>0 Then ; something in local inventory, put that in pockets
					old_off1=0
					amount_ofclothes=0
					Repeat ; get amount of clothes
						off1 = Instr(client_wear,"<",old_off1+1)
						If off1=0 Then Exit Else old_off1=off1 amount_ofclothes=amount_ofclothes+1
					Forever
					s$ = Mid (client_stronghand,Instr(client_stronghand,"[",1)+1,Len(client_stronghand)-Instr(client_stronghand,"[",1)-1) ; cloth inventory
					Repeat ; parse pocket local inv, remove items from cloth local inv and add them to inventory
						off1 = Instr(s,"$1%",1)
						If off1=0 Then Exit
						item$ = Mid(s,1,off1-1)
						s$ = Right(s,Len(s)-(off1+2))
						client_inventory = client_inventory + "<" + item + (amount_ofclothes+1) + ">"
					Forever
					; put on cloth
					client_wear = client_wear + "<"+Mid(client_stronghand,1,Instr(client_stronghand,"[",1))+"]>"
				Else ; nothing in pockets, just put on cloth
					client_wear = client_wear + "<"+client_stronghand+">"
				End If
				client_stronghand=""
				clog("I took on "+itype\name)
				signal_wear=0
			End If
		End If
	End If 
	
	If Len(client_weakhand)>0 Then 
		id% = Mid(client_weakhand,1,Instr(client_weakhand,"/",1)-1)
		itype.item_type = Object.item_type(id)
		lhitname=itype\name
	EndIf
	
	; ------------------------------------------------------
	; drop item
	If signal_drop<>0 Then
		If Len(client_stronghand)<>0
			create_droppeditem(EntityX(player_camera,1),1.5,EntityZ(player_camera,1),client_stronghand)
			client_stronghand = ""
			rhitname=""
		ElseIf Len(client_weakhand)<>0
			create_droppeditem(EntityX(player_camera,1),1.5,EntityZ(player_camera,1),client_weakhand)
			client_weakhand = ""
			lhitname=""
		End If
	End If
	
	; ----- switch hands
	If signal_switchhands<>0 Then
		If Len(client_weakhand)>0 And Len(client_stronghand)>0 Then ; both hands full, swithc items
			s$ = client_stronghand
			client_stronghand = client_weakhand
			client_weakhand = s
		ElseIf Len(client_weakhand)>0 And Len(client_stronghand)=0 ; weakhand full, stronghand empty, put item in stronghand
			client_stronghand = client_weakhand
			client_weakhand=""
		Else ; stronghand full, weakhand empty, put item in weakhand
			client_weakhand= client_stronghand
			client_stronghand=""
		End If
	End If
End Function

Function get_clothinventory$(inv$,queue%)
	old_off1=0
	Repeat ; separate items from current pocket from other items
		off1 = Instr(inv$,"<",old_off1+1)
		If off1=0 Then Exit Else old_off1=off1
		off2 = Instr(inv$,">",off1)
		item$ = Mid(inv$,off1+1,off2-off1-1)
		belongsto% = Mid(item,Instr(item,"]",1)+1,1)
		If belongsto = queue Then s$ = s$ + "<"+item+">"
	Forever
	Return s$
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
	If Len(idata)=0 Then di\idata = handler+"("+flags+")"+condition+"/"+stack+"["+localinv+"]" Else di\idata=idata
	If Len(idata)=0 Then di\idata=handler+"("+flags+")"+condition+"/"+stack+"["+localinv+"]" Else di\idata=idata
	
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
;		For char.character = Each character
;			If char\master_player = localid Then
;				If Len(client_stronghand)=0 Or Len(client_weakhand)=0 Then
;					net_sendmessage(141,di\idata,localid,1)
;					Exit
;				End If
;			End If
;		Next
	Else
;		For char.character = Each character
;			If char\master_player = 1 Then 
;				If client_stronghand="" Or client_weakhand="" Then
;					If Len(client_stronghand)=0 Then
;						client_stronghand = di\idata
;					Else
;						client_weakhand = di\idata
;					End If
;					net_sendmessage(142,di\idata,1,0)
;					rhitname$=client_stronghand
;					lhitname$=client_weakhand
;					Exit
;				Else
;					clog("My hands is full")
;				End If
;			End If
;		Next
		If client_stronghand="" Then client_stronghand=di\idata ElseIf client_weakhand="" Then client_weakhand=di\idata
		FreeEntity di\entity
		Delete di
	End If
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D