package whtong

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager

import android.util.Log
import android.util.SparseBooleanArray
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import java.io.File

import kotlin.coroutines.experimental.*

/**
 * Created by work on 2018/1/3.
 */

class ShareBlock private constructor(queryLength: Int) {
    var length: Int = queryLength

    init {
        Log.i("_shareblock", "$queryLength")
        Log.i("_shareblock", "$length")
    }

    companion object {
        @Volatile
        var instance: ShareBlock? = null

        fun getInstance(length: Int): ShareBlock {
            if (instance == null) {
                synchronized(ShareBlock::class) {
                    if (instance == null) {
                        instance = ShareBlock(length)
                    }
                }
            }
            return instance!!
        }
    }
}

class StorageUtils {
    companion object {
        val storageIn: String by lazy {
            "${Environment.getExternalStorageDirectory()}"
        }
        val storageOut: String
            get() = storageOutString!!


        @Volatile
        private var storageOutString: String? = null

        fun initStorageOut(activity: Activity): Boolean {
            if (storageOutString == null) {
                synchronized(StorageUtils::class) {
                    if (storageOutString == null) {
                        val storageOutString =
                                (activity.applicationContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager)
                                        .storageVolumes.find { it.isRemovable }?.let {
                                    val str = it.toString()
                                    val device = str.substring(str.indexOf('(') + 1, str.indexOf(')'))
                                    "/storage/$device"
                                } ?: ""
                    }
                }
            }
            return true
        }
    }
}

class TimeUtils {
    companion object {
        //计算毫秒数
        fun calcMs(task: () -> Unit, repeatCount: Int = 1): Long {
            val begTick = System.currentTimeMillis()
            var i = 0
            while (i < repeatCount) {
                task()
                i++
            }
            return System.currentTimeMillis() - begTick
        }
    }
}

class PdfFile(private val filePath:String,private val ctx: Context) {
    companion object {
        @Volatile
        private var globalPdfiumCore: PdfiumCore? = null

        fun initPdfiumCore(ctx: Context) {
            if (globalPdfiumCore == null) {
                synchronized(PdfFile::class) {
                    if (globalPdfiumCore == null) {
                        globalPdfiumCore = PdfiumCore(ctx)
                    }
                }
            }
        }

        val pdfiumCore: PdfiumCore
            get() {
                return globalPdfiumCore!!
            }
    }

    private lateinit var pdfDocument: PdfDocument
    private var canDisposed = false
    private val openedPages: SparseBooleanArray
    private val pageLock = Any()
    /*var currentInd: Int = -1
        private set*/
    val pageCount: Int by lazy(LazyThreadSafetyMode.NONE) {
        pdfiumCore!!.getPageCount(pdfDocument)
    }

    init {
        initPdfiumCore(ctx)
        val f = File(filePath)
        val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfDocument = pdfiumCore.newDocument(pfd)
        canDisposed = true
        openedPages = SparseBooleanArray()
    }


    fun pageSize(ind: Int) = pdfiumCore.getPageSize(pdfDocument, ind)
    fun takeRect(rect: Rect, pageWidth: Int, pageHeight: Int, ind: Int, fetch: (Bitmap) -> Unit): Boolean {
        if (openPage(ind)) {
            val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565)
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, ind,
                    -rect.left, -rect.top, pageWidth, pageHeight)
            fetch(bitmap)
            return true
        }
        return false
    }

    fun dispose() {
        if (canDisposed) pdfiumCore.closeDocument(pdfDocument)
    }

    fun preLoad(indBeg: Int, count: Int = 5) {
        if (indBeg > pageCount) return
        val indEnd = Math.min(pageCount - 1, indBeg + count - 1)
        for (i in indBeg..indEnd) {
            openPage(i)
        }
    }

    private fun openPage(ind: Int): Boolean {
        if (ind < 0) return false
        synchronized(pageLock) {
            if (openedPages.indexOfKey(ind) < 0) {
                try {
                    pdfiumCore.openPage(pdfDocument, ind)
                    openedPages.put(ind, true)
                    return true
                } catch (e: Exception) {
                    openedPages.put(ind, false)
                    //throw ...
                }
            } else {
                return openedPages.get(ind)
            }
        }
        return false
    }

    /*private inline fun checkInd(ind: Int) = openedPages.get(ind, false)*/

    val visInds = buildSequence {
        yield(1..5)
    }
}

class GridSysBack(val cellWH: Int){
    companion object {
        inline fun buildKey(rowInd: Int, colInd: Int, type: Int = 0)
                = (rowInd shl 14) or colInd
        const val MaxInd = (1 shl 14) - 1
        const val InValid = -1
        inline val Int.rowInd: Int
            get() = (this ushr 14) and MaxInd
        inline val Int.colInd: Int
            get() = this and MaxInd
        inline fun keyToString(key: Int) = "(${key.rowInd},${key.colInd})"
        //[0,maxValue], [a1,a2)
        inline fun boundUpper(a:Int, b:Int, value:Int,maxValue:Int):Int {
            if (value < a + b) return 0
            var result = value / (a + b)

            if (result > maxValue) return InValid
            return result
        }
        //[0,maxValue], (a1,a2]
        inline fun boundLower(a:Int, b:Int, value:Int,maxValue:Int):Int {
            if (value <= a) return InValid
            var adValue = value-a
            var result = adValue/(a+b)
            if(adValue.rem(a+b)==0) result--
            if(result>maxValue) result = maxValue
            return result
        }
    }

    //region    inline

    //region
}

data class GridConf(
        var zoom:Float,
        var visibleX:Int,
        var visibleY:Int,
        var pageCount:Int,
        var pageGridRow:Int,
        var pageGridColumn:Int,
        var pageHorGrid:Int = 0,
        var pageVerGrid:Int=0
        )

