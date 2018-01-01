package childhood.book

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * Created by Administrator on 2018/1/1.
 */


internal val emptyPaint = Paint()
internal val whitePaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = 3F
}
internal val redPaint = Paint().apply {
    color = Color.RED
    style = Paint.Style.STROKE
    strokeWidth = 3F
}
internal val bluePaint = Paint().apply {
    color = Color.BLUE
    style = Paint.Style.STROKE
    strokeWidth = 11F
}
internal val linePaint = Paint().apply {
    color = Color.BLACK
    style = Paint.Style.STROKE
    strokeWidth = 3F
}
internal val textPaint = Paint().apply {
    flags = Paint.ANTI_ALIAS_FLAG
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
    textSize = 38F
    color = Color.RED
}
internal val textWhitePaint = Paint().apply {
    flags = Paint.ANTI_ALIAS_FLAG
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
    textSize = 38F
    color = Color.WHITE
}
internal val fontDeltaHeight: Int
    get() {
        val f = textPaint.fontMetricsInt
        return (f.top + f.bottom) / 2
    }

internal interface DebugLog{
    fun drawFrame(canvas: Canvas, rect: Rect, msg: Any? = null) {
        canvas.drawRect(rect, linePaint)
        msg?.let {
            canvas.drawText(msg.toString(), rect.centerX().toFloat(), (rect.centerY() - fontDeltaHeight).toFloat(), textPaint)
        }
    }
    fun drawWhiteFrame(canvas: Canvas, rect: Rect, msg: Any? = null) {
        canvas.drawRect(rect, whitePaint)
        msg?.let {
            canvas.drawText(msg.toString(), rect.centerX().toFloat(), (rect.centerY() - fontDeltaHeight).toFloat(), textWhitePaint)
        }
    }
    fun drawRedFrame(canvas: Canvas, rect: Rect, msg: Any? = null) {
        canvas.drawRect(rect, redPaint)
        msg?.let {
            canvas.drawText(msg.toString(), rect.centerX().toFloat(), (rect.centerY() - fontDeltaHeight).toFloat(), textWhitePaint)
        }
    }
    fun drawBlueFrame(canvas: Canvas, rect: Rect, msg: Any? = null) {
        canvas.drawRect(rect, bluePaint)
        msg?.let {
            canvas.drawText(msg.toString(), rect.centerX().toFloat(), (rect.centerY() - fontDeltaHeight).toFloat(), textWhitePaint)
        }
    }
}