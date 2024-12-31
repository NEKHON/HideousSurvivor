Global window_hwnd = SystemProperty("AppHWND")
Const gameLogicFPS = 60
Const core_version$ = "0.7"
Global debug_quickhost% ; host server and throw right in game after start, ignoring main menu (NYI)
Global debug_basemodonly% ; load only base mod
; console
If Instr(CommandLine(),"-debug_mode",1)>0 Then debug_mode=1
If Instr(CommandLine(),"-debug_quickgame",1)>0 Then debug_quickhost=1
If Instr(CommandLine(),"-debug_basemodonly",1)>0 Then debug_basemodonly=1 ; NYI
SeedRnd MilliSecs()
; Includes
Include "localisation.bb"
Include "essentials.bb"
Include "stringarrays.bb"
Include "UDPNetwork_lib.bb"
Include "networking.bb"
Include "draw3d2.bb"
Include "modloader.bb"
Include "characters.bb"
Include "controls.bb"
Include "chat.bb"
Include "world.bb"
Include "inventory.bb"
load_mods("Mods")

;If debug_quickhost=1 Then
;	AppTitle("Hideous Survior. Host : " + localName + " (port:" + net_defaultport + "/ ID=" + localID + ") DEBUG QUICK HOST")
;	localname = "quick host"
;	localmode = 2
;	localid = Net_CreatePlayer(localName) 
;	Goto debug_quickhostgoto
;End If 

localName = get_randomname$(0)+Int(iasc( get_randomname$(1)))
localMode = Net_StartInput() ; bring the library "console" to connect

If localMode = 1 Or localMode = 2 ; 1 means we are client, 2 means we are server
	
	localID = Net_CreatePlayer(localName) ; this command inits the server or local client
	
	If localID = 0 ; error
		Net_StopNetwork() ; close the network properly
		RuntimeError("Failed to create player.")
	EndIf
	
	If localMode = 1 Then AppTitle("Hideous Survior. Client : " + localName + " (port:" + net_defaultport + "/ ID=" + localID + ")")
	If localMode = 2 Then AppTitle("Hideous Survior. Host : " + localName + " (port:" + net_defaultport + "/ ID=" + localID + ")")
	
Else ; error
	Net_StopNetwork()
	RuntimeError("Failed to start game.")
EndIf

.debug_quickhostgoto
; state
Global state_characterupdated
Global state_mouselock=1
Global interaction_cast%
Global highlight$
; settings
Global render_whenunfocused=1
Global render_vsync=0
Global gfx_vignette=-1

;------------------------------------------------------;

Graphics3D 800,600,32,2
SetBuffer BackBuffer()
Global center_x = GraphicsWidth()/2
Global center_y = GraphicsHeight()/2

; World
Global player_camera = CreateCamera()
CameraRange player_camera,0.1,120
cameraquickfog(player_camera,50,100,150,25,1)
CameraClsColor player_camera,150,25,1

Global ui_camera = CreateCamera()
CameraRange ui_camera,1,1000
PositionEntity	 ui_camera,0,1000,0
CameraClsMode ui_camera,0,1
DrawInit3D(ui_camera)

; FOnts
Global ui_scaling# = (GraphicsWidth()/GraphicsHeight()*1.1)
Global vb20=FontRange3D(LoadImage3D("Fonts\verdanabold20.png",2,2,0,-100)):SetFont3D(vb20,ui_scaling#*0.6,ui_scaling#*1,-2,0)
Global vb20s=FontRange3D(LoadImage3D("Fonts\verdanabold20select.png",2,2,0,-100)):SetFont3D(vb20s,0.6,1,-2,0)
Global clogf=FontRange3D(LoadImage3D("Fonts\verdanabold20.png",2,2,0,-100)):SetFont3D(clogf,0.5,0.8,-2,0)
Global img=loadimage3d("Default\a.png",2,2,0,-1)
Global vignette=LoadImage3D("ui/vignette.png",2,2,0,-1)
Global debug_item=LoadImage3D("default\base_itemplaceholder.png",1,1,0,-1)
Global inventory_cantseeitem=LoadImage3D("default\base_unknitem.png",1,1,0,-1)

Global debug_playertex = LoadAnimTexture("player.png",16+32+4+512+1024,64,64,0,8)
Global debug_infectedground = LoadTexture("Default/base_infectedground.png")
Global debug_texture = LoadTexture("Default/base_texture.png")
Global debug_tile = LoadTexture("Default/base_tile.png")
Global debug_plane = CreateCube()
ScaleMesh debug_plane,1024,0,1024
EntityTexture debug_plane,debug_infectedground
ScaleTexture debug_infectedground,0.1,0.1

PositionEntity debug_plane,0,-1.5,0
EntityColor debug_plane,100,100,100
If localMode=2 Then
	For i=1 To 4
		createcharacter(i*2,0,5)
	Next
	
	char.character = First character
	char\master_player = localID
	char\master_nickname = localName
	nc.net_client = First net_client
	nc\masteredcharacter=char\id
Else ; Sync
	; local char
	lc.local_client = New local_client
	lc\id = localid
	lc\name = localname
	Cls
	Repeat
		Cls
		Text 10,GraphicsHeight()/2,("Syncing worlds...")
		While Net_CheckMessage() 
			If net_msgtype=125 Then
				If net_msgstring="$" Then
					finished_sync=1
					Exit
				Else
					characterslist$=net_msgstring
					Print(net_msgstring)
					Text 10,GraphicsHeight()/2-15,("Recieving character data")
					Exit
				End If
			End If
		Wend
		If MilliSecs()>oldtime+500 Then
			oldtime = MilliSecs()
			net_sendmessage(90,characterslist,0)
		End If
		If Len(characterslist)>0 Then Text 10,GraphicsHeight()/2-15,("Validating") Else Text 10,GraphicsHeight()/2-15,("Getting character data")
	Until finished_sync
	Print("Valid. Creating " + wordsamount(characterslist,":")+" characters...")
	DebugLog(characterslist)
	oldoffset1=0
	Repeat
		offset1 = Instr(characterslist,":",oldoffset1+1)
		If offset1=0 Then Exit 
		oldoffset1=offset1
		offset2= Instr(characterslist,";",offset1)
		; ------------------------------
		tmp_s$ = Mid(characterslist,offset1,offset2-offset1)
		firstslash = Instr(tmp_s$, "/",1)
		secondslash = Instr(tmp_s$ ,"/",firstslash+1)
		id = Mid(tmp_s$ ,2,firstslash-offset1-1)
		master = Mid(tmp_s$,firstslash+1,secondslash-firstslash-1)
		surnamedivider=Instr(tmp_s$ ," ",secondslash)
		name$=Mid(tmp_s$ ,secondslash+1,surnamedivider-secondslash-1)
		surname$=Right(tmp_s$ ,Len(tmp_s)-surnamedivider)
		createcharacter(0,0,0,id,master,name+" "+surname)
		Print("Created "+name+" "+surname+" "+id)
	Forever
End If

tweeningPeriod# = 1000 / gameLogicFPS
deltaTime# = tweeningPeriod / 1000
tweeningTime = MilliSecs() - tweeningPeriod

Global sigmillisec
oldsigmil=MilliSecs()
.MainLoop ; --------------
If KeyHit(1) Then End

sigmillisec=0
If MilliSecs()-oldsigmil<>0 Then sigmillisec=1 oldsigmil = MilliSecs()
frameselapsed = frameselapsed + 1
If MilliSecs()>=fpstime+1000 Then
	fps = frameselapsed
	frameselapsed = 0
	fpstime = MilliSecs()
End If
network_funcs()
; --------------- TWEENING
Repeat
	tweeningElapsed = MilliSecs() - tweeningTime
Until tweeningElapsed
tweeningTicks = tweeningElapsed / tweeningPeriod
tweeningRate# = Float(tweeningElapsed Mod tweeningPeriod)/Float(tweeningPeriod)
 ; -------------------------------------
ui_scaling = ui_scaling + MouseZSpeed()*0.1
SetFont3D(vb20,ui_scaling#*0.6,ui_scaling#*1,-2,0)

For k = 1 To tweeningTicks
	tweeningTime = tweeningTime + tweeningPeriod
	If k = tweeningTicks Then CaptureWorld
	; ---------------- logics here
		; control
	If mouselock>0 Then
		mxspeed#=0
		myspeed#=0
		mxspeed# = mxspeed# + (center_x - MouseX()) * 0.21
		myspeed# = myspeed# + (center_y - MouseY()) * 0.21
		cam_pitch = cam_pitch - myspeed#
		If cam_pitch>80 Then cam_pitch=80
		If cam_pitch<-80 Then cam_pitch=-80
		control_signals()
		MoveMouse center_x,center_y
		HidePointer
	End If 
	; ---- DEBUG
	If KeyHit(57) And localmode=2 Then create_droppeditem(EntityX(player_camera),EntityY(player_camera),EntityZ(player_camera),"",Rand(1,2),"",Rand(0,100))
	; characterss
	If localMode=1 Then TurnEntity player_camera,0,1,0
	names$ = ""
	success=0
	For char.character = Each character ; update characters
		If char\master_player = localID Then ; its our guy!!
			mc_handle = Handle(char)
			HideEntity char\mesh
			PositionEntity player_camera,EntityX(char\mesh),EntityY(char\mesh)+(char\height / 100)-0.1,EntityZ(char\mesh)
			char\directiony = char\directiony+mxspeed#
			RotateEntity char\mesh,0,char\directionY,0 ; mesh rotation
			cam_dir = char\directionY
			RotateEntity player_camera,cam_pitch,EntityYaw(char\mesh),0 ; camera rotaiton
			MoveEntity char\mesh,(char\walk_speed*signal_strafe)*deltaTime,0,(char\walk_speed*signal_walk)*deltaTime ; movement
			If signal_strafe<>0 Or signal_walk<>0 Then MoveEntity player_camera,Sin(MilliSecs()/3)*0.05,Abs(Sin(MilliSecs()/3)*0.05),0 ; cameraboob
			success=1
			
			; --
			itemtip=""
			rhitname=client_stronghand
			lhitname=client_weak
			wield_interaction()
			; switch hands
			; ---- SWITCH HANDS
			If signal_switchhands<>0 Then
				If Len(client_weak)>0 And Len(client_stronghand)>0 Then 
					s$ = client_stronghand
					client_stronghand = client_weak
					client_weak = s
				ElseIf Len(client_weak)>0 And Len(client_stronghand)=0
					client_stronghand = client_weak
					client_weak=""
				Else
					client_weak = client_stronghand
					client_stronghand=""
				End If
			End If
			
			If localmode=1 Then
				dist# = flatdist(my_oldx,EntityX(char\mesh,1),my_oldz,EntityZ(char\mesh,1))+lindist(char\directiony,my_olddir)
				If MilliSecs()>client_beforenextpositionsend+client_mcpositionrate And dist=>mp_positionupdatemin# Then
					client_beforenextpositionsend=MilliSecs()
					my_oldx = EntityX(char\mesh,1)
					my_oldz = EntityZ(char\mesh,1)
					my_olddir = char\directiony
					s$ = EntityX(char\mesh)+"/"+EntityZ(char\mesh)+"/"+Int(char\directiony)
					net_sendmessage(130,s,localid,1)
				End If
			End If
			If KeyHit(27) Then clog(client_inventory)
		Else ; other char/nps
;			; sprite direction
;			a = Abs((char\directiony/45 Mod 8) - (cam_dir/45 Mod 8))
;			If a=0 Then a=8
;			If a<>char\olddir Then EntityTexture char\mesh,debug_playertex,a-1 char\olddir=a
;			ShowEntity char\mesh ; if it was hidden before..
;			; point sprit
;			PointEntity (char\mesh,player_camera)
;			RotateEntity char\mesh,0,EntityYaw(char\mesh)+180,Sin(MilliSecs()/a+5)*0.5
			; NAME
			RotateEntity char\mesh,0,char\directionY,0
			If EntityInView(char\mesh,player_camera) And EntityDistance(char\mesh,player_camera)<5
				CameraProject(player_camera,EntityX(char\mesh),EntityY(char\mesh)+2,EntityZ(char\mesh))
				If char\master_player<>0 Then h$="("+idtoname(char\master_player)+")" Else h="(NPC)"
				names$ = names$ + ":"+(ProjectedX()-center_x)+"/"+(center_y-ProjectedY())+"/"+char\char_name+ h+";"
			End If
		End If
		
		If char\directiony>360 Then char\directiony=0 ElseIf char\directiony<0 Then char\directiony=360
		If success=0 Then 
			net_sendmessage(115,"")
		End If 
	Next
	; ITEMS
	update_droppeditems(deltatime)
	; INTERACTION
	interaction_cast = CameraPick(player_camera,center_x,center_y)
	highlight=""
	If interaction_cast<>0 Then
		s$ = EntityName(interaction_cast)
		Select Mid(s,1,1)
			Case "I" ; ITEM ----------------------------
				handler% = Mid(s,2,Len(s)-1)
				di.dropped_item = Object.dropped_item(handler)
				highlight=di\idata
				If signal_interact Then
					droppeditem_pickup(Handler)
				End If
		End Select
	End If
	timer()
Next ; ---------

; -------------- RENDER ; -------------------------------
If (api_getfocus()<>0 Or render_whenunfocused=1) Then; skip render if fps too low or game is not focused
	RenderWorld tweeningRate
	Clear3D()
; Some debug info
; -----------------------------------------
	If KeyHit(2) Then mouselock=-mouselock:If mouselock=0 Then mouselock=-1
	Text3D vb20,-center_x,center_y-10,"fps: "+ fps+ ", logic fps: "+gameLogicFPS+", dt: "+deltaTime ; fps
	Text3D vb20,-center_x,center_y-25,"host?: "+inttobool(localmode)+" | ping: "+my_ping ; mp mode
	Text3D vb20,-center_x,center_y-40,"servvars: PosUpdMin: "+mp_positionupdatemin+", ServOclussion: (NYI)"+inttobool(mp_serverocclusion)
	Text3D vb20,-center_x,center_y-55,"wtime: "+worldhours+":"+worldminutes+":"+worldseconds
	Text3D vb20,-center_x,center_y-65,monthname+" "+worldday+", "+worldyear+" (leap year?: "+inttobool(worldleapyear)+"), temp: "+worldtemperature+"c"
	Text3D vb20,-center_x,-center_y+60,lhitname,0,0,10
	Text3D vb20,center_x-StringWidth3d(vb20,rhitname),-center_y+60,rhitname,0,0,-10
	text3d vb20,center_x-stringwidth3d(vb20,itemtip),-center_y+50,itemtip
	;Text3D vb20,-center_x,-center_y+25,"handlers ["+handlerslist+"]"
	Text3D vb20,-center_x,-center_y+10,"mods: ["+modlist+"] >w<"
	If Len(highlight)>0 Then Text3d vb20,-StringWidth3d(vb20,highlight)/2,0,highlight Else oval3D(img, 0, 0, 2, 2)
	; 
	;If gfx_vignette=1 Then DrawImage3d(vignette,0,0,0,0,GraphicsWidth()/512+GraphicsHeight()/512-1)
	;If KeyHit(66) Then gfx_vignette=-gfx_vignette
	draw_log() ; log
; ---- inventory
	If inventory_open=1 Then inventory_gui()
; -----------------------------------------
	oldoffset1=0
	Repeat ; draw characters names from string, so no need to for - each again.
		offset1 = Instr(names,":",oldoffset1+1)
		If offset1=0 Then Exit
		oldoffset1 = offset1
		offset2 = Instr(names,";",oldoffset1+1)
		firstslash = Instr(names,"/",offset1)
		secondslash = Instr(names,"/",firstslash+1)
		x=Mid(names,offset1+1,firstslash-offset1-1) ; x
		y=Mid(names,firstslash+1,secondslash-firstslash-1) ; y
		s$=Mid(names,secondslash+1,offset2-secondslash-1) ; name
		Color 200,200,200
		Text3D(vb20,x-StringWidth3d(vb20,s)/2,y,s)
		Color 255,255,255
	Forever
; players list
	i=0
	For nc.net_client = Each net_client
		i=i+1
		If nc\id=localID Then h$="(You)" Color 255,0,0
		s$ = h+" "+nc\NAME+"(ID: "+nc\id+"/MC :"+nc\masteredcharacter+")"+" ping: "+nc\ping
		h=""
		my_ping = nc\ping
		Text3D(vb20,center_x-StringWidth3d(vb20,s),center_y-10-i*15,s)
	Next
	For lc.local_client = Each local_client
		i=i+1
		If lc\id=localID Then h$="(You)" Color 255,0,0
		s$ = h+" "+lc\name+"(ID: "+lc\id+"/MC :"+lc\masteredcharacter+")"+" ping: "+lc\ping
		h=""
		my_ping = lc\ping
		Text3D(vb20,center_x-StringWidth3d(vb20,s),center_y-10-i*15,s)
	Next
	a=0
	For char.character = Each character
		a=a+1
	Next
; cheat
;;i=0
;;For char.character = Each character
;;	i=i+1
;;	CameraProject player_camera,EntityX(char\mesh),1,EntityZ(char\mesh)
;;	Line center_x,center_y,ProjectedX(),ProjectedY()
;;	Text center_x+25,center_y+(i*25),char\char_name
;;Next
; -------------------------------
	Text3D(vb20,center_x/2+50,center_y-10,LOC_PLAYERS+":"+i+","+LOC_CHARACTERS+":"+a)
Else
	Text GraphicsWidth()/2,GraphicsHeight()/2,"Window Unfocused"
End If
Flip(render_vsync)
Goto MainLoop
End

 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
 
;~C#Blitz3D
;~IDEal Editor Parameters:
;~L#-debug_quickgame
 
;~C#Blitz3D