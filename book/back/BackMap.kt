package childhood.book.back

import android.content.Context
import android.graphics.*
import android.util.Log

/**
 * Created by Administrator on 2017/12/31.
 */

/**
 * Created by work on 2017/12/21.
 * using:
 * load()
 * setOnZoomListener()
 * reset()
 * moveTo()
 * visibleKeys() ...
 * extraKeys() ...
 *
 */

private inline fun buildKey(rowInd: Int, colInd: Int, type: Int = 0)
        = (rowInd shl 14) or colInd
private const val MaxInd = (1 shl 14) - 1
private inline val Int.rowInd: Int
    get() = (this ushr 14) and MaxInd
private inline val Int.colInd: Int
    get() = this and MaxInd

public class BackMap() {
    companion object {
        const val CellWidth = 256
        const val CellHeight = 256
        const val CellExtraWidth = CellWidth * 2
        const val CellExtraHeight = CellHeight * 2

        const val GridCorlor = Color.GRAY
        const val BackColor = Color.LTGRAY
        val GridPaint = Paint().apply {
            style = Paint.Style.FILL
            color = GridCorlor
        }

        fun keyToString(key: Int) = "(${key.rowInd},${key.colInd})"

        /*fun coverKeys(left: Int, top: Int, right: Int, bottom: Int, perWidth: Int, perHeight: Int): Array<Int> {
            fun calcTimes(num: Int, one: Int): Int {
                var factor = num / one
                if (factor > 0) factor--
                if (num % one > 0) factor++
                return factor
            }

            val colMin = if (left > 0) left / perWidth else 0
            val rowMin = if (top > 0) top / perHeight else 0
            val colMax = calcTimes(right, perWidth)
            val rowMax = calcTimes(bottom, perHeight)
            val array = Array<Int>((rowMax - rowMin + 1) * (colMax - colMin + 1), { 0 })
            var i = 0
            for (row in rowMin..rowMax) {
                for (col in colMin..colMax) {
                    array[i++] = buildKey(row, col)
                }
            }
            return array
        }
        fun coverCellKeys(left: Int, top: Int, right: Int, bottom: Int) = coverKeys(left, top, right, bottom, CellWidth, CellHeight)*/
    }

    fun setOnZoomListener(listener: (zoomFlag: Int, zooom: Float) -> Unit) {
        onZoomFlagListener = listener
    }

    //region    calculate Keys
    private fun calcTimes(num: Int, one: Int, threshold:Int=0): Int {
        var factor = num / one
        if (factor > 0) factor--
        if (num % one > threshold) factor++
        return factor
    }
    private fun calcPageTimes(num: Int, one: Int, threshold:Int=0): Int {
        if (num<=threshold) return -1
        var factor = num/(one+threshold)
        if(num.rem(one+threshold)>threshold) factor++
        return factor
    }
    fun getValidateCellKeys(x: Int, y:Int, w:Int,h:Int): List<Int> {
        val colMin = if (x > 0) x / CellWidth else 0
        val rowMin = if (y > 0) y / CellHeight else 0
        val colMax = calcTimes(Math.min(x + w, width), CellWidth)
        val rowMax = calcTimes(Math.min(y + h, height), CellHeight)
        val result = MutableList<Int>(0, { 0 })
        for (row in rowMin..rowMax) {
            for (col in colMin..colMax) {
                if (col * CellWidth >= innerEndX && row * CellHeight >= innerEndY) continue
                result.add(buildKey(row, col))
            }
        }
        return result
    }
    fun getValidatePageKeysNouse(x: Int, y:Int, w:Int,h:Int): List<Int> {
        val perWidth = pageHorGrid + (pagePerWidth * zoom).toInt()
        val perHeight = pageVerGrid + (pagePerHeight * zoom).toInt()

        val colMin = if (x > pageHorGrid) (x - pageHorGrid) / perWidth else 0
        val rowMin = if (y > pageVerGrid) (y - pageVerGrid) / perHeight else 0
        val colMax = calcTimes(Math.min(x + w, width), perWidth, pageHorGrid)
        val rowMax = calcTimes(Math.min(y + h, height), perHeight, pageVerGrid)
        val result = MutableList<Int>(0, { 0 })
        var i = 0
        for (row in rowMin..rowMax) {
            for (col in colMin..colMax) {
                if (++i > pageCount) return result
                result.add(buildKey(row, col))
            }
        }
        return result
    }
    fun getValidatePageInds_old(x: Int, y:Int, w:Int,h:Int): List<Int>{
        val perWidth = pageHorGrid + (pagePerWidth * zoom).toInt()
        val perHeight = pageVerGrid + (pagePerHeight * zoom).toInt()
        var px = x+w
        if (px>width) px = width
        var py = y+h
        if(py>height) py = height

        val colMin = if (x > pageHorGrid) (x - pageHorGrid) / perWidth else 0
        val rowMin = if (y > pageVerGrid) (y - pageVerGrid) / perHeight else 0
        var colMax = calcTimes(px, perWidth, pageHorGrid)
        colMax = Math.min(colMax, pageGridColumn-1)
        var rowMax = calcTimes(py, perHeight, pageVerGrid)
        rowMax = Math.min(rowMax, pageGridRow-1)
        val result = MutableList<Int>(0, { 0 })
        var ind = 0
        for (row in rowMin..rowMax) {
            for (col in colMin..colMax) {
                ind = row*pageGridColumn+col
                if(ind > pageCount-1) return result
                result.add(row*pageGridColumn+col)
            }
        }
        return result
    }
    fun calcPageIndMin(a:Int, b:Int, max:Int, value:Int,maxResult:Int):Int {
        if (value < a + b) return 0
        if (value >= max - a) return -1
        //return value / (a + b)
        val result = value / (a + b)
        if (result > maxResult) return -1
        return result
    }
    fun calcPageIndMax(a:Int, b:Int, max:Int, value:Int,maxResult:Int):Int {
        if (value <= a) return -1
        var reValue = if (value > max) max else value
        reValue -= a
        var factor = reValue / (a + b)
        if (reValue.rem(a + b) == 0) factor--
        if (factor>maxResult) factor = maxResult
        return factor
    }
    fun getValidatePageInds(x: Int, y:Int, w:Int,h:Int): List<Int>{
        val result = MutableList<Int>(0, { 0 })
        val perWidth =  (pagePerWidth * zoom).toInt()
        val perHeight = (pagePerHeight * zoom).toInt()
        val maxCol = pageGridColumn-1
        val maxRow = pageGridRow-1
        val colMin = calcPageIndMin(pageHorGrid,perWidth,width,x,maxCol)
        if(colMin == -1) return result
        val rowMin = calcPageIndMin(pageVerGrid,perHeight,height,y,maxRow)
        if(rowMin == -1) return result
        val colMax = calcPageIndMax(pageHorGrid,perWidth,width,x+w,maxCol)
        if(colMax == -1)return result
        val rowMax = calcPageIndMax(pageVerGrid,perHeight,height,y+h,maxRow)
        if(rowMax == -1) return result

        var ind = 0
        for (row in rowMin..rowMax) {
            for (col in colMin..colMax) {
                ind = row*pageGridColumn+col
                if(ind > pageCount-1) return result
                result.add(row*pageGridColumn+col)
            }
        }
        return result
    }
    //endregion

    private var onZoomFlagListener: ((Int, Float) -> Unit)? = null

    //region    width,height,margin,zoom,grid
    var visibleX: Int = 0
        private set
    var visibleY: Int = 0
        private set
    private inline fun calcWidth(colCount:Int) = pageHorGrid * (colCount + 1) + (pagePerWidth * colCount * zoom).toInt()
    private inline fun calcHeight(rowCount:Int) = pageVerGrid * (rowCount + 1) + (pagePerHeight * rowCount * zoom).toInt()
    val width: Int get() = calcWidth(pageGridColumn)
    val height: Int get() = calcHeight(pageGridRow)
    /*val width: Float
        get() = pageHorGrid * (pageGridColumn + 1) + pagePerWidth * pageGridColumn * zoom
    val height: Float
        get() = pageVerGrid * (pageGridRow + 1) + pagePerHeight * pageGridRow * zoom*/

    val innerEndX:Int
        get() {
            var value = pageCount.rem(pageGridColumn)
            if (value == 0) value = pageGridColumn
            return calcWidth(value)
        }
    val innerEndY:Int get() = calcHeight(pageGridRow-1)
    var pageCount: Int = 0
        private set
    var visibleWidth: Int = 0
        private set
    var visibleHeight: Int = 0
        private set
    var pagePerWidth: Int = 0
        private set
    var pagePerHeight: Int = 0
        private set
    var pageHorGrid: Int = 0
        private set
    var pageVerGrid: Int = 0
        private set
    var zoom: Float = 1.0F
        private set(value) {
            if (value == field) return
            field = value
            ++zoomFlag
            if (zoomFlag == Int.MAX_VALUE) zoomFlag = 1
            onZoomFlagListener?.invoke(zoomFlag, field)
        }
    var zoomFlag: Int = 1
        private set
    var pageGridRow: Int = 0
        private set
    var pageGridColumn: Int = 0
        private set
    //endregion

    fun setGridWidth(pageHorGrid: Int, pageVerGrid: Int) {
        this.pageHorGrid = pageHorGrid
        this.pageVerGrid = pageVerGrid
    }

    private fun preLoad() {
        onZoomFlagListener = null
        zoom = 1F
        zoomFlag = 0
        visibleX = 0
        shockX = 0F
        visibleY = 0
        shockY = 0F
    }

    fun load(pageCount: Int, pagePerWidth: Int, pagePerHeight: Int, pageGridColumn: Int = 0, pageGridRow: Int = 0) {
        preLoad()
        this.pageCount = pageCount

        this.pagePerWidth = pagePerWidth
        this.pagePerHeight = pagePerHeight
        if (pageGridColumn > 0) {
            this.pageGridColumn = pageGridColumn
            this.pageGridRow = pageCount / this.pageGridColumn + if (pageCount % this.pageGridColumn == 0) 0 else 1
        } else if (pageGridRow > 0) {
            this.pageGridRow = pageGridRow
            this.pageGridColumn = pageCount / this.pageGridRow + if (pageCount % this.pageGridRow == 0) 0 else 1
        } else {
            this.pageGridColumn = Math.pow(pageCount.toDouble(), 0.5).toInt()
            this.pageGridRow = pageCount / this.pageGridColumn + if (pageCount % this.pageGridColumn == 0) 0 else 1
        }
    }

    //region    reset,moveTo,zoomTo ==> visible extra Keys Inds
    fun reset(width: Int, height: Int) {
        visibleWidth = width
        visibleHeight = height
    }
    private var shockX:Float = 0F
    private var shockY:Float = 0F
    fun moveOffset(deltaX:Float,deltaY:Float){
        shockX += deltaX
        shockY += deltaY
        /*val debugOffset = Pair( shockX.toInt(),shockY.toInt())
        if(debugOffset.first == visibleX && debugOffset.second == visibleY){
            throw Exception()
        }*/
        visibleX = shockX.toInt()
        visibleY = shockY.toInt()
    }

    fun moveTo(x: Int, y: Int) {
        visibleX = x
        visibleY = y
        shockX = visibleX.toFloat()
        shockY = visibleY.toFloat()
    }

    fun moveXTo(x: Int) {
        visibleX = x
        shockX = visibleX.toFloat()
    }

    fun moveYTo(y: Int) {
        visibleY = y
        shockY = visibleY.toFloat()
    }

    fun zoomTo(zoom: Float) {
        this.zoom = zoom
    }

    val visibleKeys get() = getValidateCellKeys(visibleX,visibleY,visibleWidth,visibleHeight)
    val visibleInds get() = getValidatePageInds(visibleX,visibleY,visibleWidth,visibleHeight)

    val extraKeys get() = getValidateCellKeys(visibleX- CellExtraWidth,visibleY- CellExtraHeight,
            visibleWidth+2* CellExtraWidth,visibleHeight+2* CellExtraHeight)
    val extraInds get() = getValidatePageInds(visibleX- CellExtraWidth,visibleY- CellExtraHeight,
            visibleWidth+2* CellExtraWidth,visibleHeight+2* CellExtraHeight)

    //endregion

    //region hasVisible
    val hasVisible: Boolean get() = visibleWidth > 0 && visibleHeight > 0

    //endregion

    fun getPageIndFromKey(key: Int):List<Int>{
        val cellSize = getCellSize(key)
        return getValidatePageInds(cellSize.left,cellSize.top, CellWidth, CellHeight)
    }
    fun getCellKeyFromInd(ind: Int):List<Int>{
        val pageSize = getPageSize(ind)
        return getValidateCellKeys(pageSize.left,pageSize.top,pageSize.width,pageSize.height)
    }
    fun getCellSize(key: Int) = CellSize(key)
    fun getPageSize(ind: Int) = PageSize(ind)
    fun getVisibleRect() = Rect(visibleX,visibleY,visibleX+visibleWidth,visibleY+visibleHeight)
    fun getPageIndFromScreen(x:Float,y:Float):Int{
        var px = x.toInt()
        var py = y.toInt()
        px += visibleX
        py += visibleY
        visibleInds.forEach { it ->
            val ps = getPageSize(it)
            if(ps.left<px && ps.right>px && ps.top<py && ps.bottom>py) return it
        }
        return -1
    }

    fun drawBackOutline(canvas: Canvas, pagePaint: Paint){
        //canvas.translate(visibleX.toFloat(), visibleY.toFloat())
        val visRect = getVisibleRect()
        var horPaint: Paint? = null
        var verPaint: Paint? = null
        if(pageHorGrid > 0){
            horPaint = Paint().apply {
                color = GridCorlor
                style = Paint.Style.STROKE
                strokeWidth = pageVerGrid.toFloat()
            }
        }
        if(pageVerGrid > 0){
            verPaint = Paint().apply {
                color = GridCorlor
                style = Paint.Style.STROKE
                strokeWidth = pageVerGrid.toFloat()
            }
        }
        canvas.translate(-shockX,-shockY)
        visibleInds.forEach {
            val pageRect = getPageSize(it).toRect()
            horPaint?.let {
                val x1 = pageRect.left.toFloat()-it.strokeWidth
                val x2 = pageRect.right.toFloat()+it.strokeWidth
                val yTop = pageRect.top.toFloat()-it.strokeWidth/2
                canvas.drawLine(x1,yTop,x2,yTop,it)
                val yBottom = pageRect.bottom.toFloat()+it.strokeWidth/2
                canvas.drawLine(x1,yBottom,x2,yBottom,it)
            }
            verPaint?.let {
                val y1 = pageRect.top.toFloat()-it.strokeWidth
                val y2 = pageRect.bottom.toFloat()+it.strokeWidth
                val xLeft = pageRect.left.toFloat()-it.strokeWidth/2
                canvas.drawLine(xLeft,y1,xLeft,y2,it)
                val xRight = pageRect.right.toFloat()+it.strokeWidth/2
                canvas.drawLine(xRight,y1,xRight,y2,it)
            }
            pageRect.intersect(visRect)
            canvas.drawRect(pageRect, pagePaint)
            canvas.drawText(it.toString(),pageRect.left.toFloat(),pageRect.top.toFloat(), Paint())
        }
        canvas.translate(shockX,shockY)
    }

    //region    fit...Width,Height
    var fitPageWidth:Boolean = false
        set(value) {
            field = value
            if(field){
                val pageInd = getPageIndFromScreen(visibleWidth.toFloat()/2,visibleHeight.toFloat()/2)
                if(pageInd != -1) {
                    fitFullWidth = false
                    zoom = (visibleWidth - pageHorGrid * 2) / pagePerWidth.toFloat()
                    val page = getPageSize(pageInd)
                    moveTo(page.left-pageHorGrid, page.top-pageVerGrid)
                }
            }
        }
    var fitFullWidth: Boolean = false
        set(value) {
            field = value
            if (field) {
                fitPageWidth = false
                zoom = (visibleWidth - pageHorGrid * (pageGridColumn + 1)) / (pagePerWidth * pageGridColumn).toFloat()
            }
        }
    var fitPageHeight: Boolean = false
        set(value) {
            field = value
            if (field) {
                val pageInd = getPageIndFromScreen(visibleWidth.toFloat()/2,visibleHeight.toFloat()/2)
                if(pageInd != -1) {
                    fitFullHeight = false
                    zoom = (visibleHeight - pageVerGrid * 2) / pagePerHeight.toFloat()
                    val page = getPageSize(pageInd)
                    moveTo(page.left-pageHorGrid, page.top-pageVerGrid)
                }
            }
        }
    var fitFullHeight: Boolean = false
        set(value) {
            field = value
            if (field) {
                fitPageHeight = false
                zoom = (visibleHeight - pageVerGrid * (pageGridRow + 1)) / (pagePerHeight * pageGridRow).toFloat()
            }
        }
    //endregion

    inner class CellSize(val key: Int){
        val left: Int get() = key.colInd * CellWidth
        val top: Int get() = key.rowInd * CellHeight
        val right: Int get() = (key.colInd + 1) * CellWidth
        val bottom: Int get() = (key.rowInd + 1) * CellHeight
        fun toRect() = Rect(left,top,right,bottom)
    }

    inner class PageSize(val ind: Int){
        //region    rowInd,colInd,left,top,right,bottom
        val rowInd: Int get() = ind/pageGridColumn
        val colInd: Int get() = ind.rem(pageGridColumn)
        val width: Int
            get() = (pagePerWidth * zoom).toInt()
        val height: Int
            get() = (pagePerHeight * zoom).toInt()
        val left: Int
            get() = pageHorGrid * (colInd + 1) + (pagePerWidth * colInd * zoom).toInt()
        val top: Int
            get() = pageVerGrid * (rowInd + 1) + (pagePerHeight * rowInd * zoom).toInt()
        val right: Int
            get() = left + width
        val bottom: Int
            get() = top + height
        //endregion
        fun toRect() = Rect(left, top, right, bottom)
    }

}

/**
 * 使用方法：
 * 1 能否使用 key==key && flag==flag && canUse show(cell)
 * 2 进行请求 if(select(key,flag)) request(cell)
 * 3 进行处理 if(check(tick)){ processs opEnd(tick) }
 *
 * 作用：
 * 1 只有select 操作可以修改key和flag
 * 2 防止重复请求
 * 3 方便快速推出
 *
 * 问题：
 * 1 已选中，但未处理（发送不成功，或消息队列被清空）
 */
class BmpCell() {
    val flag: Int get() = opTick
    var key: Int = -1
        private set
    @Volatile var opTick: Int = -1
        private set
    var canUse: Boolean = false
        private set
        get() {
            enterCount++
            return field
        }
    var enterCount: Int = 0
        private set

    //选中返回true，可以进行请求操作
    fun select(key: Int, flag: Int):Boolean {
        if (this.key == key && opTick == flag) return false

        this.key = key
        this.opTick = flag
        canUse = false
        bitmap.eraseColor(BackMap.BackColor)
        return true
    }
    fun check(flagTick:Int):Boolean{
        return flagTick == opTick
    }
    //操作后,操作标志相同，表示成功
    fun opEnd(flagTick: Int) {
        if (flagTick == opTick) {
            canUse = true
            enterCount = 0
            Log.i("_abc", "完成:${BackMap.keyToString(key)}")
        }
    }
    //过期时调用
    fun reset() {
        canUse = false
        key = -1
        opTick = -1
        enterCount = 0
    }

    private var isCreated = false
    fun dispose() {
        if (isCreated && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    val bitmap: Bitmap by lazy {
        isCreated = true
        Bitmap.createBitmap(BackMap.CellWidth, BackMap.CellHeight, Bitmap.Config.RGB_565)
    }
}

class CellBuffer(val rowLength:Int, val colLength:Int) {
    private val array = Array<BmpCell>(rowLength * colLength, { BmpCell() })
    fun fromKey(key: Int) = array[key.rowInd.rem(rowLength)*colLength+key.colInd.rem(colLength)]
    fun reset(){
        array.forEach { it.reset() }
    }
}

class BackPage(val name:String, val ind:Int, val ctx: Context){
    val rawFile: PdfRawFile by lazy(LazyThreadSafetyMode.NONE) {
        PdfRawFile(name, ctx)
    }
}

class PageBufferBackup(val names:List<String>, val ctx: Context){
    private lateinit var array: Array<BackPage>
    init {
        array = Array<BackPage>(names.size, {i -> BackPage(names[i], i, ctx) })
    }
    val size: Int get() = array.size
    fun fromInd(key: Int) = array[key]
}

class PageBuffer(ctx: Context){
    private var array: Array<BackPage> = emptyArray()
    private var ctx: Context
    fun reset(names: List<String>){
        if (array.size > 0) array.forEach { it.rawFile.dispose() }
        array = Array<BackPage>(names.size, {i -> BackPage(names[i], i, ctx) })
    }
    init {
        this.ctx = ctx
    }
    val size: Int get() = array.size
    fun fromInd(key: Int) = array[key]
}