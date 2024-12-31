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

Global sfx_inventory_priority = LoadSound("sounds/interactions/inventory_rearange.ogg")

; item data: :HANDLER/FLAGS/CONDITION/STACKSIZE/[LOCALINVENTORY];

; TODO: it should be inventory_collumn, instead of row, and inventory_row should be inventory_collumn, but im lazy.
Function inventory_gui(sorting$="")
	cam_pitch = cam_pitch + 0.25 ; move camera down
	old_off1=0
	amount_ofitems=0
	amount_ofdisplitems=0
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
		If amount_ofitems=(inventory_row+1) Then scale#=2+(Sin(MilliSecs()*0.25)*0.05) selected=1 Else scale#=1.5 selected=0 
		If amount_ofitems>=(inventory_row+1)-2 And amount_ofitems<=(inventory_row+1)+2 Then ; display seen items
			amount_ofdisplitems=amount_ofdisplitems+1
			DrawImage3d(debug_item,0,0+(130*amount_ofdisplitems)-(130*3), 0,0,scale,0)
			If selected=1 Then currentfont=vb20s Else currentfont=vb20 ; font
			text3d currentfont,-StringWidth3d(currentfont,itype\name)/2,0+(130*amount_ofdisplitems)-(130*3)-70,itype\name
		End If
		; show that there is more items that is unseen
		If amount_ofitems=(inventory_collumn+1)-3 Then DrawImage3d(inventory_cantseeitem, 0,-130, 0,0,1.25,0)
		If amount_ofitems=(inventory_collumn+1)+3 Then DrawImage3d(inventory_cantseeitem, 0,0+(130*amount_ofdisplitems+1)-130*2, 0,0,1.25,0)
	Forever
	If inventory_row<0 Then inventory_row = 0
	If inventory_row>amount_ofitems-1 Then inventory_row= amount_ofitems-1
End Function

Function wield_interaction()
	
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
		client_inventory=client_inventory + "<"+di\idata+">"
	End If
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D