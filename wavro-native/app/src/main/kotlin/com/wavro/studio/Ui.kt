package com.wavro.studio
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Canvas as GCanvas
import androidx.compose.ui.graphics.Paint as GPaint
import androidx.compose.ui.graphics.Path as GPath
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

fun hx(s:String):Color=runCatching{val h=s.removePrefix("#");val n=h.toLong(16)
 if(h.length==8)Color(((n shr 24)and 255).toInt(),((n shr 16)and 255).toInt(),((n shr 8)and 255).toInt(),(n and 255).toInt()) else Color(((n shr 16)and 255).toInt(),((n shr 8)and 255).toInt(),(n and 255).toInt())}.getOrDefault(Color.White)
fun blendOf(s:String)=when(s){"Screen"->BlendMode.Screen;"Add","AddAlpha"->BlendMode.Plus;"Multiply"->BlendMode.Multiply;"Darken"->BlendMode.Darken;"Lighten"->BlendMode.Lighten;"ColorDodge"->BlendMode.ColorDodge;"ColorBurn"->BlendMode.ColorBurn;"Overlay"->BlendMode.Overlay;"SoftLight"->BlendMode.Softlight;"HardLight"->BlendMode.Hardlight;"Difference"->BlendMode.Difference;"Exclusion"->BlendMode.Exclusion;"Hue"->BlendMode.Hue;"Saturation"->BlendMode.Saturation;"Color"->BlendMode.Color;"Luminosity"->BlendMode.Luminosity;else->BlendMode.SrcOver}

/* ---------------- navigation ---------------- */
@Composable fun App(vm:VM=viewModel()){
 val perm=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){};LaunchedEffect(Unit){perm.launch(android.Manifest.permission.RECORD_AUDIO)}
 var open by rememberSaveable{mutableStateOf<String?>(null)};val mod=MODS.firstOrNull{it.id==open}
 BackHandler(mod!=null){open=null}
 if(mod==null)Menu(vm){open=it} else Details(vm,mod){open=null}
}
@Composable fun Menu(vm:VM,go:(String)->Unit){
 val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null)vm.load(u)}
 Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("WAVRO",fontWeight=FontWeight.Bold,fontSize=22.sp,letterSpacing=5.sp,color=MaterialTheme.colorScheme.primary)
  Preview(vm,Modifier.fillMaxWidth().aspectRatio(16/9f))
  Row(verticalAlignment=Alignment.CenterVertically){
   FilledTonalButton({pick.launch(arrayOf("audio/*"))}){Text("♪ Import")};Spacer(Modifier.width(6.dp))
   FilledTonalButton({vm.toggle()}){Text(if(vm.playing)"❚❚" else "▶")}
   Slider(vm.pos,{vm.seek(it)},Modifier.weight(1f).padding(start=8.dp))}
  LazyVerticalGrid(GridCells.Fixed(2),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   items(MODS){m->ElevatedButton({go(m.id)},Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(14.dp)){Text(m.title,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Details(vm:VM,mod:Mod,back:()->Unit){
 var info by remember{mutableStateOf(false)}
 if(info)AlertDialog({info=false},confirmButton={TextButton({info=false}){Text("OK")}},title={Text(mod.title)},text={Text(mod.info)})
 Scaffold(topBar={TopAppBar(title={Text(mod.title)},navigationIcon={TextButton(back){Text("←",fontSize=22.sp)}},
  actions={TextButton({vm.reset(mod.id)}){Text("↺",fontSize=22.sp)};TextButton({info=true}){Text("ⓘ",fontSize=22.sp)}})}){pd->
  Column(Modifier.padding(pd)){
   Preview(vm,Modifier.fillMaxWidth().height(170.dp).padding(horizontal=12.dp))
   LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    items(mod.cats){Acc(vm,mod.id,it,0)}
    if(mod.id=="SHADER")item{Card{Column(Modifier.padding(12.dp)){Text("Variable Mappings",fontWeight=FontWeight.Bold)
     vm.s("SHADER","vars").split(";").filter{it.isNotEmpty()}.forEach{v->val(n,ty)=v.split(":")
      if(ty=="Color")Ctl(vm,"SHADER",co("v.$n","$n (Color)")) else repeat(if(ty=="Float")1 else if(ty=="Vector2")2 else 3){i->Ctl(vm,"SHADER",sl("v.$n.$i","$n.$i ($ty)"))}}}}}}}}
}
@Composable fun Acc(vm:VM,id:String,c:Cat,depth:Int){
 var ex by rememberSaveable(id,c.name){mutableStateOf(depth==0&&c.name=="General")}
 Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(if(depth==0)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)){
  Column{Row(Modifier.fillMaxWidth().clickable{ex=!ex}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(c.name,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Text(if(ex)"▾" else "▸")}
   AnimatedVisibility(ex){Column(Modifier.padding(start=14.dp,end=14.dp,bottom=12.dp)){c.items.forEach{Ctl(vm,id,it)};c.subs.forEach{Spacer(Modifier.height(6.dp));Acc(vm,id,it,depth+1)}}}}}
}
@Composable fun Ctl(vm:VM,id:String,d:Def){
 val v=vm.m["$id.${d.k}"]?:d.d;val ctx=LocalContext.current
 Column(Modifier.padding(vertical=4.dp)){when(d.t){
  T.SLIDER->{Row{Text(d.label,Modifier.weight(1f));Text("%.3f".format(v as Float),color=MaterialTheme.colorScheme.secondary)};Slider(v,{vm.set(id,d.k,it)},valueRange=d.min..d.max)}
  T.CHECK->Row(Modifier.fillMaxWidth().clickable{vm.set(id,d.k,!(v as Boolean))},verticalAlignment=Alignment.CenterVertically){Text(d.label,Modifier.weight(1f));Checkbox(v as Boolean,{vm.set(id,d.k,it)})}
  T.COLOR->Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(CircleShape).background(hx(v as String)).border(1.dp,Color.Gray,CircleShape))
   OutlinedTextField(v,{vm.set(id,d.k,it)},Modifier.weight(1f).padding(start=8.dp),label={Text(d.label+" #RRGGBBAA")},singleLine=true)}
  T.DROP,T.FONT->{var o by remember{mutableStateOf(false)};Text(d.label,color=Color.Gray,fontSize=12.sp)
   Box{OutlinedButton({o=true},Modifier.fillMaxWidth()){Text(v as String)};DropdownMenu(o,{o=false}){(if(d.t==T.FONT)FONTS else d.opts).forEach{x->DropdownMenuItem({Text(x)},{vm.set(id,d.k,x);o=false})}}}}
  T.TEXT->OutlinedTextField(v as String,{vm.set(id,d.k,it)},Modifier.fillMaxWidth(),label={Text(d.label)})
  T.NUM->OutlinedTextField((v as Float).let{if(it%1==0f)it.toInt().toString() else it.toString()},{vm.set(id,d.k,it.toFloatOrNull()?:0f)},Modifier.fillMaxWidth(),label={Text(d.label)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
  T.CODE->OutlinedTextField(v as String,{vm.set(id,d.k,it)},Modifier.fillMaxWidth(),label={Text(d.label)},minLines=5,textStyle=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace,fontSize=12.sp))
  T.IMAGE->{val l=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null){runCatching{ctx.contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION)};vm.set(id,d.k,u.toString())}}
   Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton({l.launch(arrayOf("image/*"))}){Text(d.label)};Text((v as String).substringAfterLast('/').take(22),Modifier.padding(start=8.dp),color=Color.Gray)
    if(v.isNotEmpty())TextButton({vm.set(id,d.k,"")}){Text("✕")}}}
  T.BUTTON->Button({vm.action(id,d.k)}){Text(d.label)}}}
}
/* ---------------- renderer ---------------- */
class FX(val vm:VM,val t:Float,val beat:Float,val vol:Float,val pitch:Float){fun m(id:String,k:String)=meas(vm.s(id,k),vm.f(id,"mA"),vm.f(id,"mB"),t,beat,vm.pos,vol,pitch)}
fun DrawScope.layer(fx:FX,id:String,body:DrawScope.()->Unit){val v=fx.vm;if(!v.b(id,"visible"))return
 val sx=v.f(id,"scaleX")*(1+fx.m(id,"mScale"));val sy=if(v.b(id,"scaleUni"))sx else v.f(id,"scaleY")*(1+fx.m(id,"mScale"))
 drawIntoCanvas{c:GCanvas->c.saveLayer(Rect(0f,0f,size.width,size.height),GPaint().apply{blendMode=blendOf(v.s(id,"blend"))})
  withTransform({translate(v.f(id,"posX")*size.width+fx.m(id,"mPos")*size.width*.05f,v.f(id,"posY")*size.height);rotate(v.f(id,"rot")+fx.m(id,"mRot")*30f,Offset.Zero);scale(sx,sy,Offset.Zero)}){body()}
  c.restore()}}
fun GPath.poly(n:Int,r:Float,rot:Float=-PI.toFloat()/2){for(i in 0 until n){val a=rot+i*2*PI.toFloat()/n;val x=cos(a)*r;val y=sin(a)*r;if(i==0)moveTo(x,y) else lineTo(x,y)};close()}
fun shapePath(s:String,r:Float):GPath=GPath().apply{when(s){
 "Circle","WaveformPath","CustomPath"->addOval(Rect(Offset.Zero,r));"Oval"->addOval(Rect(-r,-r*.7f,r,r*.7f))
 "Triangle"->poly(3,r);"Pentagon"->poly(5,r);"Hexagon","SidedPolygon"->poly(6,r)
 "Star"->{for(i in 0 until 10){val a=-PI.toFloat()/2+i*PI.toFloat()/5;val q=if(i%2==0)r else r*.45f;if(i==0)moveTo(cos(a)*q,sin(a)*q) else lineTo(cos(a)*q,sin(a)*q)};close()}
 "Heart"->{moveTo(0f,r*.9f);cubicTo(-r*1.3f,r*.1f,-r*.7f,-r*.9f,0f,-r*.35f);cubicTo(r*.7f,-r*.9f,r*1.3f,r*.1f,0f,r*.9f);close()}
 "Capsule"->addRoundRect(RoundRect(-r,-r*.5f,r,r*.5f,CornerRadius(r*.5f)))
 "Square"->addRect(Rect(-r,-r,r,r));"Line","HorizontalLine"->addRect(Rect(-r,-r*.03f,r,r*.03f));"VerticalLine"->addRect(Rect(-r*.03f,-r,r*.03f,r))
 else->addRect(Rect(-r,-r*.6f,r,r*.6f))}}
fun DrawScope.barShape(s:String,x:Float,w:Float,h:Float,c:Color,bm:BlendMode){val hh=abs(h);val y0=if(h>0)-hh else 0f
 when(s){"Capsule","Circle","Oval"->drawRoundRect(c,Offset(x-w/2,y0),Size(w,hh),CornerRadius(w/2),blendMode=bm)
  "Triangle"->drawPath(GPath().apply{moveTo(x-w/2,0f);lineTo(x,if(h>0)-hh else hh);lineTo(x+w/2,0f);close()},c,blendMode=bm)
  "Line","VerticalLine"->drawRect(c,Offset(x-w*.1f,y0),Size(w*.2f,hh),blendMode=bm)
  else->drawRect(c,Offset(x-w/2,y0),Size(w,hh),blendMode=bm)}}
fun DrawScope.bars(fx:FX,d:FloatArray,sm:FloatArray){val v=fx.vm;val id="BARS";val n=v.f(id,"count").toInt().coerceIn(4,128);val soft=v.f(id,"soft")
 for(i in 0 until n){val j=if(v.b(id,"flip"))n-1-i else i;val a=(d.size*(j.toFloat()/n).pow(2)).toInt().coerceAtMost(d.size-1);val e=max(a+1,(d.size*((j+1f)/n).pow(2)).toInt()).coerceAtMost(d.size)
  var s=0f;for(q in a until e)s+=d[q];val x=s/(e-a);sm[i]=if(x>sm[i])x else max(x,sm[i]-(1-soft)*.06f)}
 val slot=size.width*.84f/n;val bw=slot*(1-v.f(id,"space"))*v.f(id,"width");val rgb=v.b("RGB","visible")
 val amt=v.f("RGB","amount")*40*(1+if(v.b("RGB","glitch"))fx.beat*3 else 0f);val ang=v.f("RGB","angle")*2*PI.toFloat()
 for(sg in 1..3){if(!v.b(id,"s${sg}on"))continue
  val chans=if(rgb)listOf(Color.Red to "rOff",Color.Green to "gOff",Color.Blue to "bOff") else listOf(null to "")
  for((cc,key) in chans){val o=if(cc!=null)(v.f("RGB",key)-.5f)*2*amt else 0f
   withTransform({translate(cos(ang)*o,sin(ang)*o)}){
   for(i in 0 until n){val tt=(i.toFloat()/n+v.f(id,"blendOff"))%1f
    val col=cc?:lerp(hx(v.s(id,"s${sg}from")),hx(v.s(id,"s${sg}to")),tt)
    val fixed=v.f(id,"s${sg}fixed");val lv=(sm[i]*v.f(id,"hScale")).coerceIn(v.f(id,"minH"),max(v.f(id,"minH"),v.f(id,"maxH")))
    val h=size.height*.4f*(if(fixed>0)fixed else lv*v.f(id,"s${sg}mult"))*(1+fx.beat*.1f);val x=(i-n/2f+.5f)*slot
    val up=!(v.b(id,"s${sg}flip")&&i%2==1);val bm=if(cc!=null)BlendMode.Plus else BlendMode.SrcOver;val sh=v.s(id,"s${sg}shape")
    if(up)barShape(sh,x,bw,h,col,bm) else barShape(sh,x,bw,-h,col,bm);if(v.b(id,"s${sg}mirror"))barShape(sh,x,bw,if(up)-h*.4f else h*.4f,col.copy(alpha=.4f),bm)}}}}}
fun DrawScope.particles(fx:FX,rnd:FloatArray,img:ImageBitmap?){val v=fx.vm;val id="PART";val n=min(v.f(id,"limit").toInt(),3900);val life=max(.2f,v.f(id,"life"));val H=size.height
 val spd=v.f(id,"speed")*(1+meas(v.s(id,"mSpeed"),v.f(id,"ax")*4,v.f(id,"by")*4+1,fx.t,fx.beat,v.pos,fx.vol,fx.pitch))
 val force=v.s(id,"force");val cf=hx(v.s(id,"cFrom"));val ct=hx(v.s(id,"cTo"))
 for(i in 0 until n){val r=i*4;val age=(fx.t*v.f(id,"spawnT")*2+rnd[r]*life)%life;val u=age/life
  var x=0f;var y=0f;when(v.s(id,"spawn")){"Rectangle"->{x=(rnd[r+1]-.5f)*size.width;y=(rnd[r+2]-.5f)*H};"Circle"->{val a=rnd[r+1]*6.28f;val q=sqrt(rnd[r+2])*H*.4f;x=cos(a)*q;y=sin(a)*q};"Line"->{x=(rnd[r+1]-.5f)*size.width;y=H*.4f}}
  val a=if(v.b(id,"vecR"))rnd[r+3]*6.28f else v.f(id,"vecA")*PI.toFloat()/180+(if(v.b(id,"vecC"))atan2(y,x) else 0f)
  val sp=spd*(1+(rnd[(r+3)%rnd.size]-.5f)*v.f(id,"speedR"))*H*.05f
  x+=cos(a)*sp*age+.5f*(v.f(id,"gx")-.5f)*age*age*H*.1f+sin(age*v.f(id,"wf")*10+rnd[r+1]*6)*v.f(id,"wm")*H*.08f
  y+=sin(a)*sp*age+.5f*(v.f(id,"gy")-.5f)*age*age*H*.1f+sin(age*v.f(id,"wf")*8+rnd[r+2]*6)*v.f(id,"wm")*H*.08f
  when(force){"Gravity"->y+=age*age*H*.05f;"Wind"->x+=age*H*.08f;"Vortex"->{val c=cos(age*.5f);val s=sin(age*.5f);val nx=x*c-y*s;y=x*s+y*c;x=nx};"Attractor"->{x*=1-u*.8f;y*=1-u*.8f};"Repeller"->{x*=1+u*.8f;y*=1+u*.8f}}
  val f=min(1f,min(u,1-u)/max(.02f,v.f(id,"fade")*.5f));val col=lerp(cf,ct,u).let{it.copy(alpha=it.alpha*f)};val r0=lerp(v.f(id,"sStart"),v.f(id,"sEnd"),u)*v.f(id,"pScale")*H*.03f*(1+fx.beat*.5f)
  val pts=mutableListOf(Offset(x,y));if(v.b(id,"mirX"))pts.add(Offset(-x,y));if(v.b(id,"mirY"))pts.add(Offset(x,-y))
  pts.forEach{p->if(img!=null)drawImage(img,IntOffset((p.x-r0).toInt(),(p.y-r0).toInt()),alpha=col.alpha) else drawCircle(col,r0,p)}}}
@Composable fun rememberBmp(uri:String):State<ImageBitmap?>{val ctx=LocalContext.current
 return produceState<ImageBitmap?>(null,uri){value=if(uri.isEmpty())null else withContext(Dispatchers.IO){runCatching{ctx.contentResolver.openInputStream(Uri.parse(uri))?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=2})?.asImageBitmap()}}.getOrNull()}}}
@Composable fun Preview(vm:VM,mod:Modifier){
 var t by remember{mutableStateOf(0f)};LaunchedEffect(Unit){while(true)withFrameNanos{t=it/1e9f}}
 val d=vm.eng.data(t,vm.f("AUDIO","preamp")*2);val bt=remember{floatArrayOf(0f)};val sm=remember{FloatArray(130)};val rnd=remember{FloatArray(16000){Random.nextFloat()}}
 val raw=vm.beat(d);bt[0]=if(raw>bt[0])raw else bt[0]*.9f+raw*.1f;val fx=FX(vm,t,bt[0],d.average().toFloat(),(d.indices.maxByOrNull{d[it]}?:0)/d.size.toFloat())
 val userImg by rememberBmp(vm.s("IMAGE","image"));val fallback by rememberBmp(vm.s("IMAGE","fallback"));val pImg by rememberBmp(vm.s("PART","image"))
 val blurR=if(vm.b("BLUR","visible"))vm.f("BLUR","radius") else 0f
 Canvas(mod.clip(RoundedCornerShape(16.dp)).background(Color.Black).then(if(blurR>0f)Modifier.blur(blurR.dp) else Modifier)){
  if(vm.b("COMP","visible"))drawRect(hx(vm.s("COMP","bg")).copy(alpha=vm.f("COMP","alpha")))
  layer(fx,"IMAGE"){val r=size.height*.3f;val path=shapePath(vm.s("IMAGE","shape"),r);val pic=userImg?:(if(vm.b("IMAGE","auto"))vm.art else null)?:fallback
   clipPath(path){if(pic!=null){val sc=if(vm.b("IMAGE","crop"))max(2*r/pic.width,2*r/pic.height) else min(2*r/pic.width,2*r/pic.height);val w=pic.width*sc;val h=pic.height*sc
     drawImage(pic,dstOffset=IntOffset((-w/2).toInt(),(-h/2).toInt()),dstSize=IntSize(w.toInt(),h.toInt()))} else drawRect(Brush.linearGradient(listOf(Color(0xFF8B5CF6),Color(0xFF22D3EE)),Offset(-r,-r),Offset(r,r)),Offset(-r,-r),Size(2*r,2*r))
    drawRect(hx(vm.s("IMAGE","tint")),Offset(-r,-r),Size(2*r,2*r),blendMode=BlendMode.Modulate)}}
  layer(fx,"PART"){particles(fx,rnd,pImg)}
  layer(fx,"BARS"){bars(fx,d,sm)}
  layer(fx,"TEXT"){val v=vm;val id="TEXT";val tm=((vm.mp?.currentPosition?:0)/1000).let{"%d:%02d".format(it/60,it%60)}
   var s=v.s(id,"text");if(v.b(id,"meta"))s=s.replace("%title%",vm.title).replace("%artist%",vm.artist).replace("%time%",tm).replace("{ArtistOrTitle}",vm.artist.ifBlank{vm.title})
   if(v.f(id,"tw")>0f)s=s.take(((t*v.f(id,"tw")*20).toInt()%(s.length+8)).coerceAtMost(s.length))
   val an=v.s(id,"anim");val sp=v.f(id,"animSp");val sz=v.f(id,"size")*size.height*(1+(if(an=="Zoom")sin(t*sp*3)*.1f else 0f))
   val ital=v.s(id,"style").contains("Italic");val w=if(v.s(id,"style").contains("Bold"))max(700,v.s(id,"weight").toInt()) else v.s(id,"weight").toInt()
   val tf=if(Build.VERSION.SDK_INT>=28)Typeface.create(Typeface.create(v.s(id,"font"),Typeface.NORMAL),w,ital) else Typeface.create(v.s(id,"font"),if(w>=600)Typeface.BOLD else Typeface.NORMAL)
   val p=Paint().apply{isAntiAlias=true;typeface=tf;textSize=sz;letterSpacing=v.f(id,"ls")*.3f;textAlign=when(v.s(id,"align")){"Left"->Paint.Align.LEFT;"Right"->Paint.Align.RIGHT;else->Paint.Align.CENTER};color=hx(v.s(id,"color")).toArgb()
    setShadowLayer(v.f(id,"shB")*30,(v.f(id,"shX")-.5f)*40,(v.f(id,"shY")-.5f)*40,hx(v.s(id,"shCol")).toArgb())
    if(an=="Fade")alpha=((.5f+.5f*sin(t*sp*3))*255).toInt()}
   val yo=when(an){"Slide Up"->-((t*sp*60)%40);"Slide Down"->(t*sp*60)%40;else->0f}
   s.split("\n").forEachIndexed{i,ln->val y=yo+i*sz*(.8f+v.f(id,"lh"))
    if(v.f(id,"olW")>0f)drawContext.canvas.nativeCanvas.drawText(ln,0f,y,Paint(p).apply{style=Paint.Style.STROKE;strokeWidth=v.f(id,"olW")*20;color=hx(v.s(id,"olCol")).toArgb()})
    drawContext.canvas.nativeCanvas.drawText(ln,0f,y,p)}}}
}
