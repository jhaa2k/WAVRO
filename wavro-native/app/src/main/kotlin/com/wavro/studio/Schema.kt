package com.wavro.studio
enum class T{SLIDER,CHECK,COLOR,DROP,TEXT,NUM,IMAGE,FONT,CODE,BUTTON}
class Def(val k:String,val label:String,val t:T,val d:Any,val opts:List<String> = emptyList(),val min:Float=0f,val max:Float=1f)
class Cat(val name:String,val items:List<Def> = emptyList(),val subs:List<Cat> = emptyList())
class Mod(val id:String,val title:String,val info:String,val cats:List<Cat>)
private fun L(s:String)=s.split(",")
val BLEND=L("Alpha,PreMulAlpha,Screen,Add,AddAlpha,Multiply,Darken,Lighten,ColorDodge,ColorBurn,Overlay,SoftLight,HardLight,Difference,Exclusion,Hue,Saturation,Color,Luminosity")
val ANCH=L("unset,start,center,end,top,bottom,topLeft,topCenter,topRight,bottomLeft,bottomCenter,bottomRight,centerLeft,centerRight,justify,fill,stretch")
val MEAS=L("Nothing,Beat,TotalTime,TotalTimeBackward,TotalTimeWhenPlaying,TotalTimeAndBeat,TrackPosition,BeatRandomShake,BeatCamShakeRotMore,BeatCamShakeRotLess,ConstantShakeRotMore,ConstantShakeRotLess,BeatTriggerAnim,BeatCamShakeOnly,BeatPulseScale,BeatFlashColor,BeatGlowIntensity,BeatHorizontalStretch,BeatVerticalBounce,ConstantSlowRotate,ConstantOrbitMove,ConstantLinearScroll,TrackProgressPercentage,TrackRemainingTime,TrackIntroOutroTrigger,VolumeLevelResponsive,PitchFrequencyResponsive")
val SHAPES=L("None,Circle,SidedPolygon,Line,HorizontalLine,VerticalLine,Rectangle,Square,Triangle,Star,Heart,WaveformPath,CustomPath,Oval,Capsule,Pentagon,Hexagon")
val FONTS=L("sans-serif,serif,monospace,sans-serif-condensed,cursive,casual")
fun sl(k:String,l:String,d:Float=.5f,max:Float=1f,min:Float=0f)=Def(k,l,T.SLIDER,d,min=min,max=max)
fun ck(k:String,l:String,d:Boolean=false)=Def(k,l,T.CHECK,d)
fun co(k:String,l:String,d:String="#FFFFFFFF")=Def(k,l,T.COLOR,d)
fun dd(k:String,l:String,o:List<String>,d:String=o[0])=Def(k,l,T.DROP,d,o)
fun tx(k:String,l:String,d:String="")=Def(k,l,T.TEXT,d)
fun nm(k:String,l:String,d:Float=0f)=Def(k,l,T.NUM,d)
fun im(k:String,l:String)=Def(k,l,T.IMAGE,"")
fun gen(px:Float=.5f,py:Float=.5f)=Cat("General",listOf(tx("tag","Tag"),ck("visible","Visible",true),dd("blend","Blend Mode",BLEND),sl("posX","Position X",px),sl("posY","Position Y",py),
 dd("anchorX","Anchor X",ANCH),dd("anchorY","Anchor Y",ANCH),sl("alignX","Alignment Position X"),sl("alignY","Alignment Position Y"),ck("scaleUni","Scale Is Uniform",true),
 sl("scaleX","Scale X",1f,10f),sl("scaleY","Scale Y",1f,10f),sl("rot","Rotation",0f,360f),sl("pivX","Pivot X"),sl("pivY","Pivot Y")),
 listOf(Cat("Measure Engine",listOf(dd("mPos","Measure Pos",MEAS),dd("mScale","Measure Scale",MEAS),dd("mRot","Measure Rot",MEAS),sl("mA","A (Amount)",.5f,10f),sl("mB","B (Speed)",1f,10f)))))
fun seg(n:Int)=Cat("Segment $n",listOf(ck("s${n}on","Enable Segment",n==1),dd("s${n}shape","Shape Path",SHAPES,"Rectangle"),co("s${n}from","Color From","#8B5CF6FF"),co("s${n}to","Color To","#22D3EEFF"),sl("s${n}fixed","Fixed Height",0f),sl("s${n}mult","Bar Height Multiplier",1f,10f),ck("s${n}mirror","Mirror"),ck("s${n}flip","Flip Every Other")))
val MODS=listOf(
Mod("COMP","COMPOSITION","Master canvas. Render settings decide the export resolution.",listOf(gen(),Cat("Render Settings",listOf(nm("resW","Resolution Width",1280f),nm("resH","Resolution Height",720f),dd("fps","FPS Limit",listOf("30","60","120"),"60"),co("bg","Background Color","#0A0B11FF"),sl("alpha","Alpha / Transparency",1f),ck("clear","Clear Canvas on Frame",true))))),
Mod("TEXT","TEXT","Text with %title% %artist% %time% variables, typewriter and animations.",listOf(gen(.5f,.88f),
 Cat("Content",listOf(Def("text","Text Input",T.TEXT,"%title% - %artist%"),ck("meta","Use Metadata Variables",true),sl("tw","Typewriter Speed",0f))),
 Cat("Typography",listOf(Def("font","Font Family",T.FONT,"sans-serif"),sl("size","Font Size",.08f),dd("weight","Font Weight",L("100,200,300,400,500,600,700,800,900"),"700"),dd("style","Style",L("Normal,Italic,Bold,Bold-Italic")),sl("ls","Letter Spacing",.1f),sl("lh","Line Height",.5f),dd("align","Alignment",L("Left,Center,Right,Justify"),"Center"))),
 Cat("Appearance",listOf(co("color","Text Color"),co("shCol","Shadow Color","#000000AA"),sl("shX","Shadow X"),sl("shY","Shadow Y",.6f),sl("shB","Shadow Blur",.3f),co("olCol","Outline/Stroke Color","#000000FF"),sl("olW","Outline Width",0f))),
 Cat("Text Animation",listOf(dd("anim","Animation Type",L("None,Fade,Slide Up,Slide Down,Zoom,Typewriter")),sl("animSp","Animation Speed",.5f))))),
Mod("BARS","BARS","Spectrum bars with up to 3 stacked segments and spring-like softness.",listOf(gen(.5f,.72f),
 Cat("Bars Setup",listOf(ck("flip","Flip Input"),sl("hScale","Height Scale",1f,10f),sl("minH","Min Height Scale",0f),sl("maxH","Max Height Scale",1f),dd("shape","Shape Path",SHAPES,"Rectangle"),sl("blendOff","Color Blend Offset",0f),sl("count","Bar Count",48f,128f,4f),sl("space","Bar Spacing",.25f),sl("width","Bar Width",1f,2f))),
 seg(1),seg(2),seg(3),
 Cat("Behavior",listOf(sl("soft","Softness",.7f),sl("delay","Reaction Delay",0f),sl("accDelay","Reaction Accumulated Delay",0f),nm("provider","Audio Provider Index",0f),dd("channel","Audio Channel",L("Mono,Left,Right,Stereo")))))),
Mod("PART","PARTICLES","Analytic particle system. Count Limit above 2000 may hurt performance.",listOf(gen(),
 Cat("Overall Settings",listOf(co("color","Color"),sl("limit","Count Limit",300f,3000f,0f),ck("mirX","Mirror X"),ck("mirY","Mirror Y"),sl("depth","Perspective Depth"),sl("fov","Perspective Fov"),dd("spawn","Spawn Area",L("Rectangle,Circle,Line,Point")),ck("vecC","Vector Relative To Center"),ck("vecR","Use Random Vector Instead",true),sl("vecA","Vector Angle",0f,360f),sl("vecZ","Vector Angle Z",0f,360f),dd("force","Force Field",L("None,Gravity,Wind,Vortex,Attractor,Repeller")))),
 Cat("Appearance",listOf(im("image","Custom Image"),sl("pScale","Particle Scale",1f,10f),sl("trail","Trail Length",0f),co("cFrom","Color From"),co("cTo","Color To","#FFFFFF00"),sl("fade","Fade In And Out Time",.3f),ck("velAng","Velocity Angle"),sl("aspect","Size Aspect Ratio"),sl("sStart","Start Size",.3f),sl("sFade","Fade In Size"),sl("sEnd","End Size",.1f),sl("sEndR","End Size Random"),sl("sEndZ","End Size ZFactor"),sl("near","Near Camera Fade Out Distance"))),
 Cat("Behaviour",listOf(sl("spawnT","Spawn Time",.5f),dd("mSpeed","Measure Overall Speed",MEAS,"Beat"),sl("ax","A X Amount",.5f),sl("by","B Y Amount",.5f),sl("speed","Speed",.5f,10f),sl("speedR","Speed Random",.5f),sl("life","Lifetime",4f,10f),sl("gx","Gravity X"),sl("gy","Gravity Y"))),
 Cat("Wave Behaviour",listOf(sl("wf","Side Sine Wave Freq",.3f),sl("wfr","Side Sine Wave Freq Random"),sl("wm","Side Sine Wave Mag",.3f),sl("wmr","Side Sine Wave Mag Random"))))),
Mod("IMAGE","IMAGE","Image layer with shape masks. Album art is read from the playing file.",listOf(gen(.5f,.38f),
 Cat("Image Source",listOf(im("image","Pick Image"),ck("keep","Keep Aspect Ratio",true),ck("crop","Crop to Fit",true),dd("shape","Shape",SHAPES,"Circle"),nm("index","Image Index"))),
 Cat("Color Adjustments",listOf(co("tint","Tint"),sl("bri","Brightness"),sl("con","Contrast"),sl("sat","Saturation"),sl("hue","Hue Shift",0f))),
 Cat("Color Key (Chroma Key)",listOf(ck("ckOn","Enable Color Key"),co("ckCol","Key Color","#00FF00FF"),sl("ckThr","Threshold / Tolerance",.3f),sl("ckSoft","Softness",.1f))),
 Cat("Blur",listOf(sl("blurR","Blur Radius",0f),dd("blurT","Blur Type",L("Gaussian,Box,Motion")))),
 Cat("Generated Album Art",listOf(ck("auto","Enable Auto-Fetch",true),im("fallback","Fallback Image"))),
 Cat("Masking",listOf(im("mask","Mask Image"),dd("maskMode","Mask Mode",L("Alpha,Luminance,Inverted Alpha")))))),
Mod("AUDIO","AUDIOPROVIDER","Routes audio. Measure Source decides which band drives the beat.",listOf(
 Cat("General",listOf(ck("visible","Visible",true),dd("source","Audio Source",L("File,Microphone,System Audio")),sl("volume","Volume",.8f),sl("balance","Balance"),sl("preamp","Preamp"),dd("measure","Measure Source",L("Bass,Mid,Treble,Volume,Peak")))),
 Cat("Sync & Offset",listOf(sl("offset","Audio Offset (ms)",0f,1000f),nm("bpm","BPM Sync"),sl("beatOff","Beat Offset",0f))))),
Mod("BLUR","BLUREFFECT","Multi-pass glow blur (currently applies a global blur).",listOf(gen(),
 Cat("Appearance",listOf(dd("blendC","Blend Mode Content",BLEND),co("color","Color"),nm("srcComp","Source Composition Index"),im("mask","Mask Image"),ck("showU","Show Unblurred Content"),ck("showUU","Show Unblurred Content Under"))),
 Cat("Blur Settings",listOf(sl("radius","Blur Radius",0f,50f),sl("mult","Blur Multiplier",1f,10f),sl("l1x","1Layer Scale X",1f,10f),sl("l1y","1Layer Scale Y",1f,10f),sl("l2x","2Layer Scale X",1f,10f),sl("l2y","2Layer Scale Y",1f,10f),sl("l3x","3Layer Scale X",1f,10f),sl("l3y","3Layer Scale Y",1f,10f),co("c1","1Layer Color"),co("c2","2Layer Color"),co("c3","3Layer Color"))))),
Mod("RGB","RGBSPLITEFFECT","Splits R/G/B channels of the bars; can trigger on the beat.",listOf(gen(),
 Cat("Effect Settings",listOf(sl("amount","Split Amount",.2f),sl("angle","Angle / Direction",0f),ck("glitch","Enable Glitch on Beat",true),sl("gDur","Glitch Duration"),sl("rOff","Red Channel Offset",.75f),sl("gOff","Green Channel Offset"),sl("bOff","Blue Channel Offset",.25f))))),
Mod("MBLUR","MOTIONBLUREFFECT","Stretches along the motion of the target.",listOf(gen(),
 Cat("Appearance",listOf(dd("blendC","Blend Mode Content",BLEND),co("color","Color"),im("target","Target Image"),ck("showU","Show Unblurred Content"),ck("showUU","Show Unblurred Content Under"))),
 Cat("Motion Blur",listOf(sl("mult","Blur Amount Multiplier",1f,10f),dd("src","Motion Source",L("EffectTransform,TargetImage,AudioVolume")),ck("rel","Relative Motion Mode"))))),
Mod("MIRROR","MIRROREFFECT","Mirrors half of the target; Kaleidoscope for complex symmetry.",listOf(gen(),
 Cat("Appearance",listOf(im("target","Target Image"),dd("mode","Mirror Mode",L("Horizontal,Vertical,Both,Kaleidoscope")),ck("flip","Flip Mirror"),co("color","Color"),sl("offset","Offset"))))),
Mod("SHADER","CUSTOMSHADER","GLSL fragment shader with uniforms generated from your variables.",listOf(gen(),
 Cat("Appearance",listOf(im("target","Target Image"))),
 Cat("Shader Code",listOf(Def("vert","Shader Vertex",T.CODE,"attribute vec4 a_pos;\nvoid main(){gl_Position=a_pos;}"),Def("frag","Fragment Shader",T.CODE,"precision mediump float;\nuniform float mySpeed;\nvoid main(){gl_FragColor=vec4(1.0);}"),Def("compile","Compile / Apply",T.BUTTON,""))),
 Cat("Variables",listOf(dd("newVar","New Variable",L("None,Float,Vector2,Vector3,Color")),Def("addvar","Add Variable",T.BUTTON,""))))))
