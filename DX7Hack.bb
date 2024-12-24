Const D3DTSS_MAGFILTER      = 16
Const D3DTSS_MINFILTER      = 17
Const D3DTSS_MIPFILTER      = 18
Const D3DTSS_MIPMAPLODBIAS  = 19
Const D3DTSS_MAXMIPLEVEL    = 20

Const D3DTFG_POINT        = 1
Const D3DTFG_LINEAR       = 2
Const D3DTFP_NONE         = 1
Const D3DTFN_POINT        = 1
Const D3DTFN_LINEAR       = 2

Function InitDX7Hack()
	DX7_SetSystemProperties( SystemProperty("Direct3D7"), SystemProperty("Direct3DDevice7"), SystemProperty("DirectDraw7"), SystemProperty("AppHWND"), SystemProperty("AppHINSTANCE") )
End Function

Function DisableTextureFilters()
	;DX7_SetMipmapLODBias( -10.0, 0 )
	For Level = 0 To 7
		DX7_SetTextureStageState( Level, D3DTSS_MAGFILTER, D3DTFG_POINT )
		DX7_SetTextureStageState( Level, D3DTSS_MINFILTER, D3DTFN_POINT )
	;	DX7_SetTextureStageState( Level, D3DTSS_MIPFILTER, D3DTFP_NONE  )
	Next
End Function