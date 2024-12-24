; TODO
; Somtimes name generator returning parameter is negative (FIXED)
Global characterslist$
Type character
	Field id%
	Field master_player%
	Field master_nickname$
	Field char_name$
	
	Field directionY#
	
	; 
	Field mesh%
	
	; stats
	Field calories% = 4000
	Field water% = 5000
	Field walk_speed# = 0.7
	Field max_weight% = 40000 ; max weight that can be carried, 40k grams
	Field height% = 180 ; in CM
	
	; -- Health
	Field temperature% = 36 
	Field weight% = 75000 ; in grams
	Field vitamins$ ; TBD
	
	; --- Inventory
	Field wear$ ; clothes
	Field stronghand$ ; for example, right hand
	Field weakhand$ ;
	Field knownitems$ ; to show dilletant name and description if no knowledge about item
	Field lefthanded% ; if lefthanded
	Field shortwield$ ; so, im a lazy person with a lack of drawing skills, and to prevent some hacking, clients will only know what TYPE of item character holding (pistol, assault rifle), not the item itself
	; sprite
	Field olddir;
	
	; networking
	Field oldx# 
	Field oldy#
	Field oldz#
	End Type

Function CreateCharacter(x%=0,y%=0,z%=0, master% = 0,name$ = "",calories% = 4000,water% = 5000,height%=0,weight%=0)
	char.character = New character
	char\master_player = master
	If id=0 Then char\id = Handle(char)
	; NAMES
	If name = "" Then char\char_name = get_randomname(0) + " " + get_randomname(1) Else char\char_name = name
	; ------------------------
	; Physical
	char\walk_speed#  = Rnd(4,6)
	If height=0 Then char\height=Rand(160,200) Else char\height=height ; height
	If weight=0 Then ; Weight
		char\weight = (char\height * 0.1)*1000 ; * 1000 because weight in grams
	Else
		char\weight = weight
	End If
	; Calories
	char\calories = calories
	char\water = water
	; Traits
	If Rand(0,100)>80 Then char\lefthanded=1
	; ----------------------------
	; Mesh
	char\mesh = CreateCube()
	ScaleMesh char\mesh,Float((char\weight/1000))/100,char\height / 100,0.25
	char\directionY = Rand(0,360)
	PositionEntity char\mesh,x,y,z
	; color
	oldseed = RndSeed()
	seed=0
	SeedRnd(iasc(char\char_name))
	EntityColor char\mesh,255-Rand(0,150),255-Rand(0,150),255-Rand(0,150)
	EntityTexture char\mesh,debug_texture
	SeedRnd(oldseed)
	
End Function

Function update_characterdata()
	characterslist=""
	For char.character = Each character
		s$ = ":"+char\id+"/"+char\master_player+"/"+char\char_name+";"
		characterslist$=characterslist$+s
	Next
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D