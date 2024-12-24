; string arrays
; good for "infinity" inventory array in types, or for sending/parsing network things
; - nekhon
; p.s. im bad at english (and because of that im bad of making commentaries)
;Global testarr$

; add something to array
Function ar_add$(dest$,what$)
	Return "{"+what+"}"+dest$
End Function

; get element of array
Function ar_poke$(s$,where%)
	If Len(s)>0 Or Instr(s,"{",1)=0 Then
		oldof=0
		i=-1
		Repeat
			i=i+1
			of = Instr(s,"{",oldof+1)
			If of=0 Then ; out of bounds
				where=where Mod i
				i=-1
			End If
			If i=where Then d$ = Mid(s,of+1,Instr(s,"}",of)-of-1) Return d
			oldof=of
		Forever
	End If
	DebugLog("ar_poke: len(s)=0 or instr(s,{,1)=0")
End Function

; get element of array
Function ar_replace$(s$,where%,with$)
	If Len(s)>0 Or Instr(s,"{",1)=0 Then
		oldof=0
		i=-1
		Repeat
			i=i+1
			of = Instr(s,"{",oldof+1)
			If of=0 Then ; out of bounds
				where=where Mod i
				i=-1
			End If
			If i=where Then
				of2 = Instr(s,"}",of)
				d$ = Left(s,of-1) + "{"+with+"}"+ Right(s,Len(s)-of2)
				Return d
			End If
			oldof=of
		Forever
	End If
	DebugLog("ar_replace: len(s)=0 or instr(s,{,1)=0")
End Function

Function ar_len(s$)
	If Len(s)>0 Then
		oldof=0
		i=-1
		Repeat ;NOTHING WORKS
			of1 = Instr(s,"{",oldof+1)
			oldof=of1
			If of1=0 Then Return i Else i=i+1
		Forever
	End If
	Return -1
End Function

; remove element of array
Function ar_remove$(s$,where)
	If Mid(s,1,Len(s))="}" Then Return ""
	If Len(s)>0
		oldof=0
		i=-1
		Repeat
			i=i+1
			of1 = Instr(s,"{",oldof+1)
			If of1=0 Then  ; out of bounds
				where = where Mod i
				i=-1
				oldof=0
				If Len(s)=0 Or Instr(s,"{",1)=0 Then Exit
			End If
			If i=where Then 
				a=0
				of2=Instr(s,"}",of1)
				If of1=1 Then f$="" Else f$ = Left(s,of1-1) 
				If of2=Len(s) Then g$="" Else g$=Right(s,Len(s)-of2)
				Return f+g
			End If
			oldof=of1
		Forever
	End If
	Return ""
End Function

; get order of array element if its insides is known
Function ar_seek$(s$,what$)
	If Mid(s,1,Len(s))="}" Then Return 0
	If Len(s)>0 Then
		i=-1
		oldof=0
		Repeat
			i=i+1
			of1=Instr(s,"{",oldof+1)
			If of1=0 Then ; couldnt find
				Return -1
			End If
			oldof=of1
			If Mid(s,of1+1,Len(what))=what Then Return i
		Forever
	End If
End Function

;;For i=0 To 18
;;	testarr=ar_add(testarr,Rand(0,35655))
;;Next
;;Print(testarr)
;;WaitKey()
;;Repeat
;;	g=g+1
;;	Print("g: "+g+" :"+ar_seek(testarr,ar_poke(testarr,g)))
;;	Delay(100)
;;Forever
;;WaitKey()
;;End

;~IDEal Editor Parameters:
;~C#Blitz3D