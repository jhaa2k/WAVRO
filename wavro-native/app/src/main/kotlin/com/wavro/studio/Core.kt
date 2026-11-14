package com.wavro.studio
import android.app.Application
import android.graphics.BitmapFactory
import android.media.*
import android.media.audiofx.Visualizer
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.*

class Engine{
 var raw=FloatArray(256);var playing=false;var ok=false;private var vis:Visualizer?=null
 fun attach(sid:Int){vis?.release();ok=false
  vis=try{Visualizer(sid).apply{captureSize=Visualizer.getCaptureSizeRange()[1]
   setDataCaptureListener(object:Visualizer.OnDataCaptureListener{
    override fun onWaveFormDataCapture(v:Visualizer?,b:ByteArray?,r:Int){}
    override fun onFftDataCapture(v:Visualizer?,b:ByteArray?,r:Int){b?:return
     raw=FloatArray(b.size/2){i->val re=b[2*i].toInt();val im=b[2*i+1].toInt();(sqrt((re*re+im*im).toFloat())/70f).coerceIn(0f,1f)}}
   },Visualizer.getMaxCaptureRate()/2,false,true);enabled=true;ok=true}}catch(e:Exception){null}}
 fun data(t:Float,pre:Float):FloatArray=if(playing&&ok)FloatArray(raw.size){(raw[it]*pre).coerceIn(0f,1f)} else FloatArray(256){i->val x=i/256f;val k=(1-(t%.5f)/.5f).let{it*it*it}
  ((.5f+.5f*sin(t*1.3f+i*.18f))*(.85f-.55f*x.pow(.4f))*(.5f+.5f*sin(t*.7f+i*.05f))+k*.7f*exp(-i/30f)).coerceIn(0f,1f)}
}
class VM(app:Application):AndroidViewModel(app){
 private val sp=app.getSharedPreferences("wavro",0)
 val m=mutableStateMapOf<String,Any>();private val defs=HashMap<String,Any>();private var dirty=false
 val eng=Engine();var mp:MediaPlayer?=null
 var title by mutableStateOf("Unknown");var artist by mutableStateOf("");var art by mutableStateOf<ImageBitmap?>(null)
 var playing by mutableStateOf(false);var pos by mutableStateOf(0f)
 private fun walk(c:Cat,id:String){c.items.forEach{defs["$id.${it.k}"]=it.d};c.subs.forEach{walk(it,id)}}
 init{MODS.forEach{md->md.cats.forEach{walk(it,md.id)}}
  runCatching{val j=JSONObject(sp.getString("s","{}")!!);j.keys().forEach{k->val v=j.get(k);m[k]=if(v is Number)v.toFloat() else v}}
  viewModelScope.launch{var n=0;while(true){delay(100);n++
   if(n%10==0&&dirty){dirty=false;sp.edit().putString("s",JSONObject(HashMap(m)).toString()).apply()}
   mp?.let{p->playing=p.isPlaying;eng.playing=playing;if(p.duration>0)pos=p.currentPosition/p.duration.toFloat();val v=f("AUDIO","volume");p.setVolume(v,v)}}}}
 fun f(id:String,k:String):Float=((m["$id.$k"]?:defs["$id.$k"]) as? Float)?:0f
 fun b(id:String,k:String):Boolean=((m["$id.$k"]?:defs["$id.$k"]) as? Boolean)?:false
 fun s(id:String,k:String):String=((m["$id.$k"]?:defs["$id.$k"]) as? String)?:""
 fun set(id:String,k:String,v:Any){m["$id.$k"]=v;dirty=true}
 fun reset(id:String){m.keys.filter{it.startsWith("$id.")}.forEach{m.remove(it)};dirty=true}
 fun action(id:String,k:String){val c=getApplication<Application>()
  if(k=="compile"){set(id,"compiledAt",System.currentTimeMillis().toFloat());Toast.makeText(c,"Shader saved. GPU compile is the next step.",Toast.LENGTH_SHORT).show()}
  if(k=="addvar"){val nv=s(id,"newVar");if(nv!="None"){val cur=s(id,"vars");val n=cur.split(";").count{it.isNotEmpty()};set(id,"vars",cur+"v$n:$nv;")}}}
 fun load(u:Uri){val c=getApplication<Application>();mp?.release();mp=MediaPlayer().apply{setDataSource(c,u);prepare();start()};eng.attach(mp!!.audioSessionId)
  runCatching{val r=MediaMetadataRetriever();r.setDataSource(c,u);title=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?:"Unknown";artist=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?:""
   art=r.embeddedPicture?.let{BitmapFactory.decodeByteArray(it,0,it.size)?.asImageBitmap()};r.release()}}
 fun toggle(){mp?.let{if(it.isPlaying)it.pause() else it.start()}}
 fun seek(v:Float){mp?.let{it.seekTo((v*it.duration).toInt())}}
 fun beat(d:FloatArray):Float{fun av(a:Int,z:Int):Float{val e=min(z,d.size);var s=0f;for(i in a until e)s+=d[i];return s/max(1,e-a)}
  return when(s("AUDIO","measure")){"Mid"->av(6,40);"Treble"->av(41,120);"Volume"->av(0,d.size/2);"Peak"->d.maxOrNull()?:0f;else->av(0,6)}}
}
fun meas(mode:String,a:Float,b:Float,t:Float,beat:Float,pos:Float,vol:Float,pitch:Float):Float=when(mode){
 "Beat","BeatPulseScale","BeatFlashColor","BeatGlowIntensity","BeatHorizontalStretch","BeatVerticalBounce","BeatTriggerAnim"->beat*a
 "BeatRandomShake","BeatCamShakeRotMore","BeatCamShakeRotLess","BeatCamShakeOnly"->sin(t*b*23)*beat*a
 "ConstantShakeRotMore","ConstantShakeRotLess"->sin(t*b*23)*a*.3f
 "TotalTime","TotalTimeWhenPlaying","ConstantSlowRotate","ConstantOrbitMove","ConstantLinearScroll"->t*b*.1f*a
 "TotalTimeAndBeat"->(t*b*.1f+beat)*a
 "TotalTimeBackward"->-t*b*.1f*a
 "TrackPosition","TrackProgressPercentage"->pos*a
 "TrackRemainingTime"->(1-pos)*a
 "TrackIntroOutroTrigger"->if(pos<.05f||pos>.95f)a else 0f
 "VolumeLevelResponsive"->vol*a
 "PitchFrequencyResponsive"->pitch*a
 else->0f}
