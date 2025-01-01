Type dropped_item
	Field entity%
	Field idata$
End Type
Global dropitems_gravity# = -6
Global rhitname$="???"
Global lhitname$="???"
Global itemtip$

Global client_inventory$
Global client_wear$
Global client_stronghand$
Global client_weakhand$

Global inventory_amountofseenitems=5

Global inventory_ui_selecteditem_scale#=2
Global inventory_ui_item_scale#=1.3
Global inventory_iconsize# = 64 * GraphicsWidth()/GraphicsHeight()

Global sfx_inventory_priority = LoadSound("sounds/interactions/inventory_rearange.ogg")

; item data: :HANDLER/FLAGS/CONDITION/STACKSIZE/[LOCALINVENTORY];

; TODO: it should be inventory_collumn, instead of row, and inventory_row should be inventory_collumn, but im lazy.
Function inventory_interface(sorting$="")
	cam_pitch = cam_pitch + 0.25 ; move camera down
	Local amount_ofitems=0
	Local amount_ofdisplitems=0
	Local amount_ofclothes=0
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
		If amount_ofclothes=(wearinventory_row+1) And inventory_collumn=0 Then scale#=Abs(inventory_ui_selecteditem_scale+(Sin(MilliSecs()*0.25)*0.05)*ui_scaling) selected=1 Else scale#=Abs(inventory_ui_item_scale*ui_scaling) selected=0 
		If selected=1 And MouseHit(1) ; add item
			If Len(client_stronghand)>0 Then
				client_inventory = client_inventory + "<"+client_stronghand+">"
				client_stronghand=""
			ElseIf Len(client_weakhand)>0 Then
				client_inventory = client_inventory + "<"+client_weakhand+">"
				client_weakhand=""
			End If
		End If
		DrawImage3d(debug_item,-inventory_iconsize*4,0+((inventory_iconsize+(25*ui_scaling))*amount_ofclothes)-((inventory_iconsize+(10*ui_scaling))*4), 0,0,scale,0)
		; todo, changing scale if too much clothes
	Forever
	old_off1=0
	Repeat ; inventory loop
		; ----- item info
		off1 = Instr(client_inventory,"<",old_off1+1)
		If off1=0 Then Exit Else old_off1=off1 amount_ofitems=amount_ofitems+1
		off2 = Instr(client_inventory,">",off1)
		item$ = Mid(client_inventory,off1+1,off2-off1-1)
		id% = Mid(item,1,Instr(item,"(",1)-1)
		text3d vb20,0,0,id
		itype.item_type = Object.item_type(id)
		; -----------
		If inventory_collumn=1 Then pocketinventory_row = inventory_row
		If amount_ofitems=(pocketinventory_row+1) And inventory_collumn=1 Then scale#=Abs(inventory_ui_selecteditem_scale+(Sin(MilliSecs()*0.25)*0.05)*ui_scaling) selected=1 Else scale#=Abs(inventory_ui_item_scale*ui_scaling) selected=0 
		If (amount_ofitems>=(pocketinventory_row+1)-((inventory_amountofseenitems-1)/2) And amount_ofitems<=(pocketinventory_row+1)+((inventory_amountofseenitems-1)/2)) Then ; display seen items
			amount_ofdisplitems=amount_ofdisplitems+1
			DrawImage3d(debug_item,0,0+((inventory_iconsize+(25*ui_scaling))*amount_ofdisplitems)-((inventory_iconsize+(10*ui_scaling))*3), 0,0,scale,0)
			If selected=1 Then currentfont=vb20s Else currentfont=vb20 ; font
			text3d currentfont,-StringWidth3d(currentfont,itype\name)/2,0+(inventory_iconsize+(25*ui_scaling))*amount_ofdisplitems-((inventory_iconsize+(10*ui_scaling))*3)-inventory_iconsize/2,itype\name
		End If
		;show that there is more items that is unseen
		If amount_ofitems=(pocketinventory_row+1)-((inventory_amountofseenitems-1)/2)+1 Then DrawImage3d(inventory_cantseeitem, 0,-inventory_iconsize*3, 0,0,-0.4,0)
		If amount_ofitems=(pocketinventory_row+1)+((inventory_amountofseenitems-1)/2)+1 Then DrawImage3d(inventory_cantseeitem,0,inventory_iconsize*(amount_ofdisplitems-1), 0,0,0.4,0)
	Forever
	; ---------------------
	; inventory limites
	Select inventory_collumn
		Case 0 ; wear
			If inventory_row<0 Then inventory_row = 0
			If inventory_row>amount_ofclothes-1 Then inventory_row= amount_ofclothes-1
		Case 1 ; items
			If inventory_row<0 Then inventory_row = 0
			If inventory_row>amount_ofitems-1 Then inventory_row= amount_ofitems-1
		Case 2 ; hands
			If inventory_row<0 Then inventory_row=0
			If inventory_row>1 Then inventory_row=1
	End Select
	; 3 collumns
	If inventory_collumn<0 Then inventory_collumn=0
	If inventory_collumn>2 Then inventory_collumn=2
	
;	If inventory_collumn=1 And amount_ofitems=0 Then ; if items selected, but no items then switch to next collumn
;		If old_collumn=0 Then inventory_collumn=2
;		If old_collumn=2 Then inventory_collumn=0
;	End If
;	If inventory_collumn=0 And amount_ofclothes=0 Then inventory_collumn=2 ; if clothes collumn selected but no clothes weared then select hands
	old_collumn = inventory_collumn
End Function

Function wield_interaction()
	itemtip=""
	If Len(client_stronghand)>0 Then itemtip="[O] To wear" If signal_wear Then client_wear = client_wear + "<" + client_stronghand + ">" client_stronghand="" 
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
	End If
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D