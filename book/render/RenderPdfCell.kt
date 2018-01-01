package childhood.book.render

import android.os.Looper
import android.os.Message
import android.util.Log
import childhood.book.back.BackMap

/**
 * Created by Administrator on 2018/1/1.
 */

class RenderPdfCell(looper: Looper,
                    val task:(key:Int,flag:Int)->Unit,
                    val commit:(key:Int,flag:Int)->Unit,
                    val runout:(key:Int,flag:Int)->Unit):android.os.Handler(looper){
    companion object {
        val MSG_RENDER_CELLTASK = 1
    }
    @Volatile
    private var runTick = 0

    var running:Boolean = false
        set(value) {
            if(field == value) return
            field = value
            if(!field) {
                removeMessages(MSG_RENDER_CELLTASK)
                //取消正在执行的任务
                //...
            }else{
                ++runTick
            }
        }
    fun request(key:Int, flag:Int){
        if(running) {
            val data = CellMsg(key, flag, runTick)
            Log.i("_abc", "请求:${BackMap.keyToString(key)}")
            sendMessage(obtainMessage(MSG_RENDER_CELLTASK, data))
        }
    }
    override fun handleMessage(msg: Message) {
        if (msg.what == MSG_RENDER_CELLTASK) {
            val data = msg.obj as CellMsg
            //第一层过滤
            //Thread.sleep(100)
            if (data.tick != runTick){
                //已过期
                runout(data.key,data.flag)
                return
            }
            task(data.key, data.flag)
            if (running){
                commit(data.key, data.flag)
            }
        }
    }
    private data class CellMsg(val key:Int,val flag:Int,val tick:Int)
}