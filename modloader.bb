If FileType("MODS")=0 Then CreateDir("MODS")
If FileType("MODS")<>2 And FileType("MODS")>0 Then RuntimeError("WHAT? MODS Folder is not a folder! TF You did??")
Global modlist$=""
Global handlerslist$="" ; list of handlers to item types

Global firstnames$
Global surnames$

Function load_mods(modsdir$)
	modsdir = ReadDir(modsdir)
	Repeat
		dir$ = NextFile(modsdir)
		If dir="" Then modlist=Left(modlist,Len(modlist)-2)+"." Exit
		If dir="." Or dir=".." Then dir=""
		If FileType("MODS/"+dir)=2 And Len(dir)>0 Then
		; check dependencies
			If FileType("MODS/"+dir+"/info.nti")=1 Then infofile = ReadFile("MODS/"+dir+"/info.nti") Else RuntimeError("Mod "+dir+" Dont have mod info file (info.nti)")
			infostring$ = NTI_parseinstring$(infofile%)
			requiredmod$ = NTI_parameterproperty(infostring, Instr(infostring,"$requiresmods",1))
			If FileType("MODS/"+requiredmod)<>2 Then RuntimeError("Mod loading error: "+dir+" Requires "+requiredmod+" mod.")
			
		;add mod name to mod list
			modlist=modlist+NTI_parameterproperty(infostring, Instr(infostring,"$name",1))+" v"+NTI_parameterproperty(infostring, Instr(infostring,"$version",1))+", "
			; names
			If FileType("Mods/"+dir+"/charnames.nti")<>0 Then
				charnames = OpenFile("Mods/"+dir+"/charnames.nti")
				names$ = NTI_parseinstring$(charnames)
				oldoffset1 = 0
				Repeat
					offset1=Instr(names,"$name",oldoffset1+1)
					If offset1=0 Then Exit
					oldoffset1 = offset1
					firstnames=firstnames+":"+NTI_parameterproperty(names,offset1)+";"
				Forever
				oldoffset1 = 0
				Repeat
					offset1=Instr(names,"$surname",oldoffset1+1)
					If offset1=0 Then Exit
					oldoffset1 = offset1
					surnames=surnames+":"+NTI_parameterproperty(names,offset1)+";"
				Forever
				names=""
			End If
			; items
			If FileType("Mods/"+dir+"/items.nti")<>0 Then
				itemsnti% = OpenFile("Mods/"+dir+"/items.nti")
				items$ = NTI_parseinstring$(itemsnti)
				oldoffset1 = 0
				Repeat
					offset1 = Instr(items,"$handler",oldoffset1+1)
					oldoffset1=offset1
					If offset1=0 Then Exit
					it.item_type = New item_type
					handlerslist = handlerslist + ":"+NTI_parameterproperty(items,offset1)+"/"+Handle(it)+";"
					; -
					it\name$ = NTI_parameterproperty(items,Instr(items,"$name",offset1))
					it\description$ = NTI_parameterproperty(items,Instr(items,"$description",offset1))
					
					it\globaltype$ = NTI_parameterproperty(items,Instr(items,"$globaltype",offset1))
				Forever
				items=""
			End If
		End If
	Forever
	CloseDir modsdir
End Function

Function NTI_parameterproperty$(st$,parameterstart%)
	of1 = Instr(st,":",parameterstart)
	of2 = Instr(st,";",parameterstart)
	s$ = Mid(st,of1+1,of2-of1-1)
	Return s
End Function

Function NTI_parseinstring$(source%)
	Repeat
		s$=ReadLine(source)
		If Not Mid(s,1,1)="#" Then g$=g$+s 
	Until Eof(source)
	CloseFile source
	Return g$
End Function

; Get some funcs
Function get_randomname$(name_or_sur)
	Select name_or_sur
	Case 0 ; name
		a=wordsamount(firstnames,":")
		oldoffset1=0
		a=Rand(1,a)
		For i=0 To a
			offset1=Instr(firstnames,":",oldoffset1+1)
			oldoffset1=offset1
		Next
		If offset1=0 Then offset1=Instr(surnames,":",1) ; FAIL KLUDGE
		Return Mid(firstnames,offset1+1,Instr(firstnames,";",offset1)-offset1-1)
	Default ; surname
		a=wordsamount(surnames,":")
		oldoffset1=0
		a=Rand(1,a)
		For i=0 To a
			offset1=Instr(surnames,":",oldoffset1+1)
			oldoffset1=offset1
		Next
		If offset1=0 Then offset1=Instr(surnames,":",1) ; FAIL KLUDGE
		Return Mid(surnames,offset1+1,Instr(surnames,";",offset1)-offset1-1)
	End Select
End Function

; TYPES
Type item_type
	Field handler$
	; - visual
	Field name$
	Field diletantname$
	Field description$
	Field diletantdescription$
	Field spritesheet$ = "NOTEXTURE"
	Field droppedsize%
	; - usage
	Field globaltype$
	Field localtype$
	; - physical properties
	Field stackable%
	Field volume%
	; - localinventory
	Field localinventory_acceptableitems$
	Field localinventory_type%
	Field localinventory_size% 	; - cloth
	Field cloth_covers$ ; what parts cloth are covering
	Field cloth_tempprotection% ; 
End Type


;~IDEal Editor Parameters:
;~C#Blitz3D