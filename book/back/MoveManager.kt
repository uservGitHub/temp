package childhood.book.back

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import childhood.book.DrawLayout



/**
 * Created by Administrator on 2017/12/31.
 */

class MoveManager(val host: DrawLayout): GestureDetector.OnDoubleTapListener,View.OnTouchListener, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    init {
        gestureDetector = GestureDetector(host.context, this)
        scaleGestureDetector = ScaleGestureDetector(host.context, this)
        host.setOnTouchListener(this)
    }
    fun enable(){canTouch = true}
    fun disable(){canTouch = false}
    private var canTouch = false
    private var scrolling = false
    private var scaling = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return host.onClick(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        host.zoomOffsetFromScreen(1.2F, e.x, e.y)
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onTouch(view: View?, e: MotionEvent?): Boolean {
        val event = e!!
        if (!canTouch) return false

        var retVal = scaleGestureDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal

        if (event.action == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false
                onScrollEnd(event)
            }
        }
        return retVal
    }

    private fun onScrollEnd(event: MotionEvent){

    }

    override fun onShowPress(e: MotionEvent) = Unit

    override fun onSingleTapUp(e: MotionEvent) = false

    override fun onDown(e: MotionEvent?) = true //终止 如动画
    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float)
            = true //不允许惯性滚动 收了

    override fun onLongPress(e: MotionEvent?) = Unit

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        scrolling = true
        host.moveOffset(distanceX, distanceY)
        /*scrolling = true;
        if (pdfView.isZooming() || pdfView.isSwipeEnabled()) {
            pdfView.moveRelativeTo(-distanceX, -distanceY);
        }
        if (!scaling || pdfView.doRenderDuringScale()) {
            pdfView.loadPageByOffset();
        }
        return true;*/
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        scaling = true
        //info { "onScaleBegin" }
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        //info { "onScaleEnd" }
        //pdfView.loadPages()
        scaling = false
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val factor = detector.scaleFactor
        //val dr = detector?.getScaleFactor()
        host.zoomOffsetFromScreen(factor, detector.focusX,detector.focusY)
        //info { "onScale $factor" }
        /*float wantedZoom = pdfView.getZoom() * dr;
        if (wantedZoom < MINIMUM_ZOOM) {
            dr = MINIMUM_ZOOM / pdfView.getZoom();
        } else if (wantedZoom > MAXIMUM_ZOOM) {
            dr = MAXIMUM_ZOOM / pdfView.getZoom();
        }
        pdfView.zoomCenteredRelativeTo(dr, new PointF(detector.getFocusX(), detector.getFocusY()));*/
        return true
    }
}
