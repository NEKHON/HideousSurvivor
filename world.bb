Global worldhours=13
Global worldminutes=40
Global worldseconds=27
Global worldday=21
Global worldmonth=6
Global worldyear=2012
Global worldleapyear=0
If (worldyear Mod 4) = 0 Then worldleapyear=1 Else worldleapyear=0
Global worldtemperature=2
Global monthname$
Global monthtemp%

Global secondold
Global timespeedup%=1
Global defsecondlength=80
Global secondlength

Function timer()
	newmonth=0
	newday=0
	secondlength=clamp(defsecondlength/clamp(timespeedup,0,10),1,defsecondlength)
	If MilliSecs()>secondold+secondlength
		worldseconds=worldseconds+1
		secondold=MilliSecs()
	End If
	If worldseconds>59 Then worldseconds=0 worldminutes=worldminutes+1
	If worldminutes>59 Then ; new hour
		worldminutes=0 
		worldhours=worldhours+1
		; temperature
		a$=Sin(worldhours)
		daytempdecrease=Int(Right(a,Instr(a,".",1)))/10 ; that formula is too cursed, make better
		worldtemperature=monthtemp-daytempdecrease+Rand(-3,3)
		cameraquickfog(player_camera,80+(worldtemperature*3),120+(worldtemperature*3),150,25,1)
	End If
	If worldhours>23 Then ; new day
		worldhours=0 
		worldday=worldday+1
	End If 
	
	For month.month = Each month
		If month\sequence = worldmonth Then ; this month
			If worldday>month\amountofdays Then ; new month
				worldmonth=worldmonth+1 
				worldday=1
				; is it new year?
				newyear=1
				For month2.month = Each month
					If month2\sequence>worldmonth Then newyear=0 Exit ; there is next month so no new year
				Next
				If newyear=1 Then worldmonth=1 worldyear=worldyear+1
			End If
			monthname = month\name
		End If
		; leap year
		monthtemp% = month\averagetemp
		If (worldyear Mod 4) = 0 Then worldleapyear=1 Else worldleapyear=0
	Next
End Function

Type month
	Field name$
	Field averagetemp ; average temperature
	Field amountofdays ; amount of days in month
	Field sequence ;
End Type

month.month = New month ;
month\name = LOC_MONTH_JANUARY
month\averagetemp = -10
month\amountofdays = 28
month\sequence = 1


month.month = New month ;
month\name = LOC_MONTH_MARCH
month\averagetemp = 0
month\amountofdays = 31
month\sequence=2

month.month = New month ;
month\name = LOC_MONTH_JUNY
month\averagetemp = 15
month\amountofdays = 31
month\sequence=3

month.month = New month ;
month\name = LOC_MONTH_AUGUST
month\averagetemp = 28
month\amountofdays = 31
month\sequence=4

month.month = New month ;
month\name = LOC_MONTH_SEMPTEMBER
month\averagetemp = 28
month\amountofdays = 30
month\sequence=5

month.month = New month ;
month\name = LOC_MONTH_DECEMBER
month\averagetemp = -10
month\amountofdays = 31
month\sequence=6

; #$#$#$#$#$#$#$#$#$#$#$#$#$#$
;~IDEal Editor Parameters:
;~C#Blitz3D