Const loglength=32
Dim chlog$(loglength)
Dim oldlog$(loglength)
Global beforelogclean%
Global logcleanrate=1337

Function clog(msg$)
	beforelogclean%=logcleanrate
	freeslot=-1
	For i=0 To loglength
		If Len(chlog(i))=0 Then freeslot=i Exit
	Next
	If freeslot=-1 Then
		For i=0 To loglength
		oldlog(i)=chlog(i)
	Next
	For i=0 To loglength-1
		chlog(i)=oldlog(i+1)
	Next
	chlog(loglength)=msg
	Else
		chlog(freeslot)=msg
End If

End Function

Function draw_log()
	If Len(chlog(0))<>0 Then beforelogclean%=beforelogclean%-sigmillisec
	If beforelogclean=<0 Then
		beforelogclean%=logcleanrate
		For i=0 To loglength-1
			oldlog(i+1)=chlog(i+1)
		Next
		chlog(0)=""
		chlog(loglength)=""
		For i=0 To loglength-1
			chlog(i)=oldlog(i+1)
		Next
	End If
	For i=0 To loglength
		Text3D clogf,-center_x,center_y-80-i*8,chlog(i)
	Next
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D