Global control_walkforward% = 17
Global control_walkback% = 31
Global control_strafeleft% = 30
Global control_straferight% = 32
Global control_interact = 18
Global control_inventory = 15
Global control_switchhands% = 33
Global control_wear% = 24
Global control_drop% = 16

Global mxspeed#
Global myspeed#
Global cam_pitch#

Global control_fire = 1
Global control_aim = 2

Global signal_walk%
Global signal_strafe%
Global signal_fire%
Global signal_aim%
Global signal_interact%
Global signal_wear%
Global signal_drop%
Global signal_switchhands
Global inventory_open=-1
Global inventory_collumn
Global inventory_row
Global old_scroll
Global signal_invswap

Global nextscrollcd

Global sfx_inventory_row=LoadSound("sounds/interactions/inventory_scroll.ogg")
Global sfx_inventory_open=LoadSound("sounds/interactions/inventory_check.ogg")

Function control_signals()
	signal_switchhands = KeyHit(control_switchhands)
	If inventory_open=1 And MouseHit(1) Then signal_invswap=1 Else signal_invswap=0
	signal_walk = KeyDown(control_walkforward)-KeyDown(control_walkback)
	signal_strafe = KeyDown(control_straferight)-KeyDown(control_strafeleft)
	signal_wear = KeyHit(control_wear)
	If KeyHit(control_inventory) Then inventory_open=-inventory_open PlaySound(sfx_inventory_open)
	nextscrollcd=nextscrollcd-1
	If (inventory_open=1 And (Abs(mxspeed)<>4 Or myspeed<>0)) And nextscrollcd=<0  Then
		nextscrollcd=5
		If mxspeed>4 Then inventory_collumn=inventory_collumn-1 PlaySound(sfx_inventory_row)
		If mxspeed<-4 Then inventory_collumn=inventory_collumn+1 PlaySound(sfx_inventory_row)
		If myspeed>4 Then inventory_row=inventory_row+1 PlaySound(sfx_inventory_row)
		If myspeed<-4 Then inventory_row=inventory_row-1 PlaySound(sfx_inventory_row)
	End If
	old_scroll = inventory_row+1
	signal_interact = KeyHit(control_interact)
	signal_drop = KeyHit(control_drop)
	
	;signal_fire = MouseHit(control_fire)
	signal_aim = MouseHit(control_aim)
	
	; --------
	If KeyHit(66) Then gfx_vignette=-gfx_vignette ; enable/disable viggnete
	; DEBUG FUNCS
	If KeyHit(27) Then clog(client_inventory) ; display inventory
	If KeyHit(2) Then state_mouselock=-state_mouselock:If state_mouselock=0 Then state_mouselock=-1
End Function


;~IDEal Editor Parameters:
;~C#Blitz3D