Function debug_info()
	Text3D vb20,-center_x,center_y-10,"fps: "+ info_fps+ ", logic fps: "+gameLogicFPS+", dt: "+deltaTime ; fps
	Text3D vb20,-center_x,center_y-25,"host?: "+inttobool(localmode)+" | ping: "+my_ping ; mp mode
	Text3D vb20,-center_x,center_y-40,"servvars: PosUpdMin: "+mp_positionupdatemin+", ServOclussion: (NYI)"+inttobool(mp_serverocclusion)
	Text3D vb20,-center_x,center_y-55,"wtime: "+worldhours+":"+worldminutes+":"+worldseconds
	Text3D vb20,-center_x,center_y-65,monthname+" "+worldday+", "+worldyear+" (leap year?: "+inttobool(worldleapyear)+"), temp: "+worldtemperature+"c"
	Text3D vb20,-center_x,-center_y+60,lhitname,0,0,10
	Text3D vb20,center_x-StringWidth3d(vb20,rhitname),-center_y+60,rhitname,0,0,-10
	text3d vb20,center_x-stringwidth3d(vb20,itemtip),-center_y+50,itemtip
	Text3D vb20,-center_x,-center_y+10,"mods: ["+modlist+"] >w<"
End Function

Function draw_ui()
	If Len(highlight)>0 Then Text3d vb20,-StringWidth3d(vb20,highlight)/2,0,highlight Else oval3D(img, 0, 0, 2, 2) ; Highlight or crosshair
	If gfx_vignette=1 Then DrawImage3d(vignette,0,0,0,0,GraphicsWidth()/512+GraphicsHeight()/512-1) ; draw viggnete
	draw_log() ; log
End Function

Function draw_charnames()
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
End Function

Function draw_playerslist()
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
	Text3D(vb20,center_x/2+50,center_y-10,LOC_PLAYERS+":"+i+","+LOC_CHARACTERS+":"+a)
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D