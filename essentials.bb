; This is an include file with functions that i use frequently, and maybe i will use that in other projects.
; - nekhon

Dim debug_messages_list$(25)
Global debug_messages_list_offset
Global essentials_deltatime_timer% = MilliSecs()
Function CreateLogFile(folder$) ; creates log file
	If FileType(folder)=0 Then CreateDir folder
	a = WriteFile(folder+"LOG "+CurrentDate()+" "+(MilliSecs()/1000)+".txt")
	Return a
End Function
Function AdvDebuglog(message$, stream=0, log_checkvar=2) ; logs in debugger and stream (file)
	If stream = 0 Then stream = log_file
	If log_checkvar = 2 Then  log_checkvar = debug_do_logs
	DebugLog("["+CurrentTime()+"] " + message)
	If log_checkvar=1 Then
		WriteLine stream,"["+CurrentTime()+"] " + message 
	End If 
	
	; write in messages array
	debug_messages_list_offset = debug_messages_list_offset + 1
	If debug_messages_list_offset > 25
		debug_messages_list_offset = debug_messages_list_offset - 1
		For i = 2 To 25
			debug_messages_list(i-1) = debug_messages_list(i)
		Next
		debug_messages_list(25) = message
	Else
		debug_messages_list(debug_messages_list_offset) = message
	EndIf
End Function 
Function FullCls() Cls:Locate 0,0:FlushKeys End Function ; Clears screen and changes print cursor to 0,0
Function CreateQuad(parent%=0)   ; create quad, flat mesh made of 2 triangles
	Local Mesh%=CreateMesh()
	Local Surf% = CreateSurface(Mesh)
	Local t%=AddVertex (Surf,0,0,0,0,1)
	AddVertex (Surf,0,0,2,0,0)
	AddVertex (Surf,2,0,2,1,0)
	AddVertex (Surf,2,0,0,1,1)
	AddTriangle Surf,t,t+2,t+3
	AddTriangle Surf,t,t+1,t+2
	PositionMesh Mesh,-1,0,-1 
	EntityParent Mesh,parent
	Return Mesh
End Function
Function LoadTextureWithCheck(texture$,flags=0) ; loads texture, but crashes game if texture is invalid
	a = LoadTexture(texture,flags)
	If a=0 Then 
		AdvDebuglog("FATAL ERROR, FAILED TO LOAD "+texture,log_file)
		RuntimeError("Failed to load "+texture+". Try to reinstall the game.")
	ElseIf a<>0 Then
		Return a
	End If
End Function
Function LoadMeshWithCheck(mesh$,texturehandler%=0,parent=0) ; loads mesh but wawa..
	a=LoadMesh(mesh,parent)
	If a = 0 Then 
		AdvDebuglog("FATAL ERROR, FAILED TO LOAD "+mesh,log_file)
		RuntimeError("Failed to load "+mesh+". Try to reinstall the game.")
	End If
	If texturehandler>0 Then EntityTexture a,texturehandler
	Return a
End Function

Function CameraQuickFog(camtoassignfog%,near#,far#,r=255,g=255,b=255) ; quicly assign camera fog
	CameraFogMode camtoassignfog,1:CameraFogRange camtoassignfog,near,far:CameraFogColor camtoassignfog,r,g,b
End Function

Function CreateQuickCamera(rangenear#=0.1,rangefar#=550,fov%=80,parent%=0) ; quicly create camera with range and fov
	a = CreateCamera(parent):CameraRange a,rangenear,rangefar:CameraFOV a,fov:Return a
End Function

Function CameraFOV(cam, fov#):CameraZoom cam, 1.0 / Tan(fov#/2.0):End Function ; camera fov
Function Clamp#(value#,min#,max#) ; limits value to min and max
	a# = value#
	If a < min Then a = min
	If a > max Then a = max
	Return a#
End Function
Function Drag#(sp#,dc#=0.1,deltat#=1,min# = 0.05) ; slowly decreasing value by dc * deltatime, an then drops it to zero if it lower than limit, can be used for simple physics
	If sp<-min Or sp>min Then a# = sp - ((dc * Clamp(sp,-1,1))*deltat)
	If sp>-min And sp<min Then a=0
	Return a#
End Function

Function LinePickFrom(entity%,dx#=0,dy#=0,dz#=1,radius#=0) ; quick linepick from entity
	If entity = 0 Then RuntimeError("Essentials -> LinePickFrom(): Entity "+entity+" doesnt exists.") ; fail check
	TFormPoint dx,dy,dz,entity,0
	a% = LinePick(EntityX(entity,1),EntityY(entity,1),EntityZ(entity,1),TFormedX(),TFormedY(),TFormedZ(), radius)
	Return a
End Function

Function Erase$(s$,where%,towhere%) ; erases in string from point 1 to point 2
	If where<1 Or towhere<1 Then RuntimeError("Essentials -> erase(): where or towhere is <1, pars: <"+s+">;"+where+";"+towhere) ; fail check
	s$ = Left(s,where-1)+Right(s,Len(s)-towhere+1)
	Return s
End Function

Function PutIn$(s$,where%,what$) ; inserts something somewher in string
	If where<1 Then RuntimeError("Essentials -> putIn(): where is <1, pars: <"+s+">;"+where+";"+"<"+what+">") ; fail check
	If Len(s)-where<0 Then RuntimeError("Essentials -> putIn(): len(s)-where < 0!, pars: <"+s+"> (len:"+Len(s)+");"+where+";"+"<"+what+">")
	
	If where=1 Then le$="" Else le$=Left(s,where)
	If where=Len(s) Then ri$="" Else ri$=Right(s,Len(s)-where)
	s$ = le + what + ri
	Return s
End Function

; get amount of specified words/symbols in string
Function WordsAmount(s$,find$)
	For i=1 To Len(s)
		If Mid(s,i,Len(find)) = find Then a=a+1
	Next
	Return a
End Function
; 1d distance
Function LinDist(x#,y#):d#=Sqr(Abs(x-y)^2):Return d#:End Function 
; 2d distance
Function FlatDist#(x1#,x2#,y1#,y2#):a# = x1-x2:b = y1-y2:c# = Sqr(Abs(a^2) + Abs(b^2)):Return c#:End Function

Function SAsc$(st$) ; it returns full assci sequence of string
	For i=1 To Len(st)
		s$ = s$ + Asc(Mid(st,i,1))
	Next
	Return s$
End Function

Function IAsc%(st$) ; it returns int based on assci sequence of string
	For i=1 To Len(st)
		a = a + Asc(Mid(st,i,1))
	Next
	Return a
End Function

Function IntToBool$(a) ; mostly for debug hud purposes.
	If a=0 Then Return "False" Else Return "True"
End Function

Function IntLen(value%) ; amount of digits in int
	Return Len(Str(value))
End Function

Function IntMax():Return 2147483647:End Function ; biggest possible int
Function FloatMax$():Return "2.14748e+009":End Function  ; biggest possible float
;~IDEal Editor Parameters:
;~C#Blitz3D