package childhood.book.back

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File

/**
 * Created by Administrator on 2017/12/31.
 */

class PdfRawFile(filePath:String, ctx: Context) {
    companion object {
        private var _pdfCore: PdfiumCore? = null
        private val pdfCore: PdfiumCore get() { return _pdfCore!!}
    }
    var currentInd = -1
        private set
    //private lateinit var pdfCore: PdfiumCore
    private lateinit var pdfDoc: PdfDocument
    private var canDisposed = false
    val pageCount: Int by lazy(LazyThreadSafetyMode.NONE) {
        pdfCore.getPageCount(pdfDoc)
    }

    init {
        /*if(pdfCore == null){
            pdfCore = PdfiumCore(ctx)
        }*/
        //pdfCore = PdfiumCore(ctx)
        if(_pdfCore == null) _pdfCore = PdfiumCore(ctx)
        //region    pdfDoc
        val f = File(filePath)
        val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfDoc = pdfCore.newDocument(pfd)
        //pdfCore.openPage(pdfDoc, 0)
        next()
        canDisposed = true
        //endregion
    }
    fun next():Boolean{
        if(currentInd<pageCount-1) {
            currentInd++
            pdfCore.openPage(pdfDoc, currentInd)
            return true
        }
        return false
    }
    fun pageSize(pageInd: Int): Pair<Int,Int> {
        //if (!open(pageInd)) return Pair(-1, -1)
        val size = pdfCore.getPageSize(pdfDoc, pageInd)
        return Pair<Int, Int>(size.width, size.height)
    }

    fun bitmap(rect: Rect, pWidth:Int, pHeight:Int, pageInd: Int = 0): Bitmap {
        //open(pageInd)
        val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565)
        pdfCore.renderPageBitmap(pdfDoc, bitmap, currentInd,
                -rect.left, -rect.top, pWidth, pHeight)
        return bitmap
    }


    //region    open
    /*private fun open(pageInd: Int): Boolean {
        if (pageInd >= pageCount) return false
        pdfCore.openPage(pdfDoc, pageInd)
        return true
    }*/
    //endregion

    //region    dispose
    fun dispose() {
        if (canDisposed) {
            pdfCore.closeDocument(pdfDoc)
        }
    }
    //endregion
}