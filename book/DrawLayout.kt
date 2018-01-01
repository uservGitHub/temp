package childhood.book

import android.content.Context
import android.graphics.*
import android.view.ViewManager
import android.widget.RelativeLayout
import android.graphics.drawable.Drawable
import android.os.HandlerThread
import android.util.Log
import android.view.MotionEvent
import childhood.book.back.*
import childhood.book.render.RenderPdfCell


/**
 * Created by Administrator on 2017/12/31.
 */

class DrawLayout(ctx:Context):RelativeLayout(ctx),DebugLog{
    val backMap: BackMap
    val cellBuffer: CellBuffer
    val pageBuffer: PageBuffer
    val moveManager: MoveManager
    val renderingHandlerThread: HandlerThread
    var renderPdfCell: RenderPdfCell? = null
    private var resetIndex = 0
    val emptyPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint()
    }
    var zoomFlag: Int = -1
        private set
    var isLoad = false
        private set
    init {
        setWillNotDraw(false)
        backMap = BackMap()
        cellBuffer = CellBuffer(12, 12)
        pageBuffer = PageBuffer(context)
        moveManager = MoveManager(this)
        renderingHandlerThread = HandlerThread("RenderHandle:$resetIndex")
    }
    fun onClick(e: MotionEvent):Boolean{
        val pageInd = backMap.getPageIndFromScreen(e.x,e.y)
        //Log.i("_abc", pageInd.toString())
        val page = pageBuffer.fromInd(pageInd)
        if(page.rawFile.next()){
            synchronized(lockKeys) {
                backMap.getCellKeyFromInd(pageInd).forEach {
                    cellBuffer.fromKey(it).reset()
                }
            }
            invalidate()
        }
        return true
    }
    var backColor: Int = Color.LTGRAY
    val lockKeys = Any()
    override fun onDraw(canvas: Canvas) {
        if (isInEditMode()) {
            return;
        }
        canvas.drawColor(backColor)

        if(!isLoad) return

        backMap.reset(width,height)

        /*val pagePaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.GREEN
        }
        backMap.drawBackOutline(canvas, pagePaint)*/

        val cellCount = backMap.visibleKeys.size
        //Log.i("_abc", "cellCount:$cellCount")

        synchronized(lockKeys){
            val visRect = backMap.getVisibleRect()
            backMap.visibleKeys.forEachIndexed { index, i ->
                val bmpCell = cellBuffer.fromKey(i)
                if(bmpCell.select(i, zoomFlag)){
                    renderPdfCell!!.request(i, zoomFlag)
                    return
                }
                if(bmpCell.key != i || bmpCell.flag!=zoomFlag){
                    Log.i("_abc", "标志不对")
                }
                if ( bmpCell.canUse){
                    renderCell(canvas, bmpCell, visRect)
                }
            }
        }

        //其他层处理
        //...
    }

    fun moveOffset(dx:Float, dy:Float){
        backMap.moveOffset(dx, dy)
        invalidate()
    }
    fun zoomOffset(dr:Float){
        val visRect = backMap.getVisibleRect()
        zoomOffset(dr, visRect.exactCenterX(), visRect.exactCenterY())
    }
    fun zoomOffsetFromScreen(dr:Float,cx:Float,cy:Float){
        val x = cx+backMap.visibleX
        val y = cy+backMap.visibleY
        zoomOffset(dr,x,y)
    }
    fun zoomOffset(dr:Float,cx:Float,cy:Float){
        backMap.moveOffset(-cx, -cy)
        backMap.zoomTo(backMap.zoom*dr)
        backMap.moveOffset(cx*dr,cy*dr)
        invalidate()
    }

    //region    页的宽度占满屏宽，优先处理竖向滚动
    private var lockPageWidth: Boolean = false
    //定要可以选中当前页
    fun fitWidthPageToScreen(){
        lockPageWidth = true
        lockPageHeight = false
        backMap.fitPageWidth = true
        invalidate()
    }
    //endregion
    //region    页的高度占满屏幕，优先处理横向滚动
    private var lockPageHeight: Boolean = false
    //定要可以选中当前页
    fun fitHeightPageToScreen(){
        lockPageHeight = true
        lockPageWidth = false
        backMap.fitPageHeight = true
        invalidate()
    }
    //endregion
    fun load(names: List<String>) {
        try {
            pageBuffer.reset(names)
            backMap.load(pageBuffer.size, 500, 700, 2)
            backMap.setGridWidth(4, 6)
            backMap.setOnZoomListener { zoomFlag, zooom ->
                this.zoomFlag = zoomFlag
            }
            cellBuffer.reset()
            if (!renderingHandlerThread.isAlive) renderingHandlerThread.start()
            renderPdfCell?.running = false
            if (renderPdfCell == null) {
                renderPdfCell = RenderPdfCell(renderingHandlerThread.looper,
                        { key, flag ->
                            drawCell(key, flag)
                        },
                        { key, flag ->
                            this@DrawLayout.post {
                                this@DrawLayout.invalidate()
                            }
                        },
                        { key, flag ->
                            Log.i("_abc", "运行标志已过期")
                        })

            }

            this.zoomFlag = backMap.zoomFlag
            renderPdfCell?.running = true
            isLoad = true
            moveManager.enable()
            invalidate()
        } catch (e: Exception) {
            //info { e.toString() }
        }

    }

    fun drawCell(key:Int, flag:Int) {
        val cell = cellBuffer.fromKey(key)
        //已经处理完，退出
        if(cell.key == key && cell.flag==flag && cell.canUse){
            Log.i("_abc", "已经处理完，退出 ${BackMap.keyToString(key)},${cell.enterCount}")
            return
        }

        val bitmap = cell.bitmap
        val canvas = Canvas(bitmap)
        val cellSize = backMap.getCellSize(key)
        val bound = object {
            val right = Math.min(cellSize.right, backMap.width)
            val bottom = Math.min(cellSize.bottom, backMap.height)
        }
        if (bound.right != cellSize.right || bound.bottom != cellSize.bottom) {
            val boundRect = Rect(0, 0, bound.right - cellSize.left, bound.bottom - cellSize.top)
            canvas.drawRect(boundRect, BackMap.GridPaint)
        } else {
            canvas.drawColor(BackMap.GridCorlor)    //以后clip
        }

        var indColor = 0
        var bmpColor = 0
        var interCount = 0
        backMap.getPageIndFromKey(key).forEach {
            val pageSize = backMap.getPageSize(it)
            if (pageSize.left < cellSize.right && cellSize.left < pageSize.right &&
                    pageSize.top < cellSize.bottom && cellSize.top < pageSize.bottom) {
                val rect = Rect(Math.max(pageSize.left, cellSize.left),
                        Math.max(pageSize.top, cellSize.top),
                        Math.min(pageSize.right, cellSize.right),
                        Math.min(pageSize.bottom, cellSize.bottom))
                if (false) {
                    val rectBmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565).apply {
                        when (indColor) {
                            0 -> bmpColor = Color.RED
                            1 -> bmpColor = Color.GREEN
                            2 -> bmpColor = Color.YELLOW
                            3 -> bmpColor = Color.BLUE
                        }
                        eraseColor(bmpColor)
                        indColor++
                    }
                    rect.offset(-cellSize.left, -cellSize.top)
                    canvas.drawBitmap(rectBmp, Rect(0, 0, rectBmp.width, rectBmp.height), rect, emptyPaint)
                    interCount++
                } else {
                    if(!cell.check(zoomFlag)) {
                        Log.i("_abc", "未渲染完毕，被中断 ${BackMap.keyToString(key)}")
                        return
                    }
                    val rectPage = Rect(rect.left - pageSize.left, rect.top - pageSize.top, rect.right - pageSize.left, rect.bottom - pageSize.top)
                    val rectBmp = pageBuffer.fromInd(it).rawFile.bitmap(rectPage, pageSize.width, pageSize.height)
                    rect.offset(-cellSize.left, -cellSize.top)
                    canvas.drawBitmap(rectBmp, Rect(0, 0, rectBmp.width, rectBmp.height), rect, emptyPaint)
                    interCount++
                    //info { rectPage }
                }
                //canvas.drawCircle(rect.exactCenterX(),rect.exactCenterY(),10F, whitePaint)
                //canvas.drawRect(Rect(rect.left+5,rect.top+5,rect.right-10,rect.bottom-10), linePaint)
            }else{
                Log.i("_abc", "错误不在范围内 ${BackMap.keyToString(key)},$it")
            }
        }
        val cellRect = backMap.getCellSize(key).toRect()
        cellRect.offsetTo(0,0)
        //drawWhiteFrame(canvas, cellRect, "${BackMap.keyToString(key)},${cell.opTick}")
        cell.opEnd(zoomFlag)
    }
    private fun renderCell(canvas: Canvas, bmpCell: BmpCell, visRect: Rect) {
        val cellSize = backMap.getCellSize(bmpCell.key)
        val sRect = Rect(Math.max(visRect.left, cellSize.left),
                Math.max(visRect.top, cellSize.top),
                Math.min(visRect.right, cellSize.right),
                Math.min(visRect.bottom, cellSize.bottom))
        sRect.offset(-cellSize.left, -cellSize.top)
        val dRect = Rect(Math.max(visRect.left, cellSize.left),
                Math.max(visRect.top, cellSize.top),
                Math.min(visRect.right, cellSize.right),
                Math.min(visRect.bottom, cellSize.bottom))
        dRect.offset(-visRect.left, -visRect.top)
        canvas.drawBitmap(bmpCell.bitmap, sRect, dRect, emptyPaint)
    }
    override fun onDetachedFromWindow() {
        renderPdfCell?.running = false
        renderingHandlerThread.quit()
        super.onDetachedFromWindow()
    }
}

