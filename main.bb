Global window_hwnd = SystemProperty("AppHWND")
Const gameLogicFPS = 60 ; dont touch it. never.
Const core_version$ = "tbd" ; not sure how to do versioning
Global core_gamename$ = "Hideous Survivor" ; lmao im making chinese pirates job easier
Global debug_quickhost% ; host server and throw right in game after start, ignoring main menu (NYI)
Global debug_basemodonly% ; load only base mod
Global debug_mode
Global info_fps%
; console
If Instr(CommandLine(),"-debug_mode",1)>0 Then debug_mode=1
If Instr(CommandLine(),"-debug_quickgame",1)>0 Then debug_quickhost=1
If Instr(CommandLine(),"-debug_basemodonly",1)>0 Then debug_basemodonly=1 ; NYI
SeedRnd MilliSecs()
; Includes
Include "localisation.bb"
Include "essentials.bb"
;Include "stringarrays.bb"
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

If debug_quickhost=1 Then
	AppTitle(core_gamename+". Host : " + localName + " (port:" + net_defaultport + "/ ID=" + localID + ") DEBUG QUICK HOST")
	localname = "quick host"
	localmode = 2
	localid = Net_CreatePlayer(localName) 
Else
	localName = get_randomname$(0)+Int(iasc( get_randomname$(1)))
	localMode = Net_StartInput() ; bring the library "console" to connect
	
	If localMode = 1 Or localMode = 2 ; 1 means we are client, 2 means we are server
		
		localID = Net_CreatePlayer(localName) ; this command inits the server or local client
		
		If localID = 0 ; error
			Net_StopNetwork() ; close the network properly
			RuntimeError("Failed to create player.")
		EndIf
		If debug_mode=1 Then
			If localMode = 1 Then AppTitle(core_gamename+". Client : " + localName + " (port:" + net_defaultport + "/ ID=" + localID + ")")
			If localMode = 2 Then AppTitle(core_gamename+". Host : " + localName + " (port:" + net_defaultport + "/ ID=" + localID + ")")
		Else
			AppTitle(core_gamename)
		End If
			
	Else ; error
		Net_StopNetwork()
		RuntimeError("Failed to start game.")
	EndIf
End If

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

Graphics3D 1280,760,32,2
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

Include "ui.bb"

; FOnts
Global ui_scaling# = (GraphicsWidth()/GraphicsHeight()*1.1)
Global vb20=FontRange3D(LoadImage3D("Fonts\verdanabold20.png",2,2,0,-100)):SetFont3D(vb20,ui_scaling#*0.6,ui_scaling#*1,-2,0)
Global vb20s=FontRange3D(LoadImage3D("Fonts\verdanabold20select.png",2,2,0,-100)):SetFont3D(vb20s,0.6,1,-2,0)
Global clogf=FontRange3D(LoadImage3D("Fonts\verdanabold20.png",2,2,0,-100)):SetFont3D(clogf,0.5,0.8,-2,0)
Global img=loadimage3d("Default\a.png",2,2,0,-1)
Global vignette=LoadImage3D("ui/vignette.png",2,2,0,-1)
Global debug_item=LoadImage3D("default\base_itemplaceholder.png",1,1,0,-1)
Global inventory_cantseeitem=LoadImage3D("default\base_unknitem.png",1,1,0,-1)
Global inventory_background=LoadImage3D("ui\ui_inventory.png",2,2,0,-1)

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
If KeyHit(57) And localmode=2 Then create_droppeditem(EntityX(player_camera),EntityY(player_camera),EntityZ(player_camera),"",Rand(1,2),"",Rand(0,100))

sigmillisec=0
If MilliSecs()-oldsigmil<>0 Then sigmillisec=1 oldsigmil = MilliSecs()
frameselapsed = frameselapsed + 1
If MilliSecs()>=fpstime+1000 Then
	info_fps = frameselapsed
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
	If state_mouselock>0 Then
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
	; characterss
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
			RotateEntity player_camera,cam_pitch,EntityYaw(char\mesh),0 ; camera rotaitonh
			MoveEntity char\mesh,(char\walk_speed*signal_strafe)*deltaTime,0,(char\walk_speed*signal_walk)*deltaTime ; movement
			If signal_strafe<>0 Or signal_walk<>0 Then MoveEntity player_camera,Sin(MilliSecs()/3)*0.05,Abs(Sin(MilliSecs()/3)*0.05),0 ; cameraboob
			success=1
			
			; --
			itemtip=""
			rhitname=client_stronghand
			lhitname=client_weak
			wield_interaction()
			
			If localmode=1 Then ; PLAYER CONTROLS
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
			; ------------------
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
		If success=0 Then ; ASK HOST FOR CHARACTER
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
	timer() ; update world time
Next ; ---------

; -------------- RENDER ; -------------------------------
If (api_getfocus()<>0 Or render_whenunfocused=1) Then; skip render if game is not focused
	RenderWorld tweeningRate
	Clear3D()
; ---- inventory
	If inventory_open=1 Then inventory_interface()
If debug_mode=1 Then : debug_info() : Else:Text3D vb20,-center_x,center_y-10,"FPS: "+ info_fps:End If
	draw_ui()
	draw_charnames()
	draw_playerslist()
Else
	Text GraphicsWidth()/2,GraphicsHeight()/2,"Window Unfocused"
End If
Flip(render_vsync)
; ---------------------------------------------
Goto MainLoop ; ------------------------
RuntimeError("End of MainLoop.")
 
;~C#Blitz3D
;~IDEal Editor Parameters:
;~L#-debug_mode
 
;~C#Blitz3D