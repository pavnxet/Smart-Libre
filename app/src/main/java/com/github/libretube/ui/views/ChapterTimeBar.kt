package com.github.libretube.ui.views

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.core.view.marginLeft
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.extensions.dpToPx

/**
 * TimeBar that displays chapters as breaks in the progress bar.
 */
@UnstableApi
class ChapterTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null,
) : MarkableTimeBar(context, attributeSet) {
    private var chapters = listOf<ChapterSegment>()
    private var length: Int = 0

    private val progressBarHeight = 2f.dpToPx()
    private val chapterBreakWidth = 4f.dpToPx()
    private val eraserPaint = Paint().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = BlendMode.CLEAR
        } else {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private var timestampMarkers = listOf<Long>()
    private val markerDotRadius = 2.5f.dpToPx()
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD54F.toInt() // Amber / Gold accent dot for high visibility
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        val saveCount = canvas.saveLayer(null, null)
        super.onDraw(canvas)
        drawChapters(canvas)
        drawTimestampMarkers(canvas)

        canvas.restoreToCount(saveCount)
    }

    private fun drawChapters(canvas: Canvas) {
        if (exoPlayer == null) return

        val horizontalOffset = (parent as View).marginLeft
        length = width - horizontalOffset * 2
        val marginY = (height - progressBarHeight) / 2

        // skip the first chapter as it always starts at 0:00
        chapters.drop(1).forEach {
            val center = it.start.toLength() + horizontalOffset

            canvas.drawRect(
                Rect(
                    center - (chapterBreakWidth / 2),
                    marginY,
                    center + (chapterBreakWidth / 2),
                    marginY + progressBarHeight
                ),
                eraserPaint,
            )
        }
    }

    private fun drawTimestampMarkers(canvas: Canvas) {
        if (exoPlayer == null || timestampMarkers.isEmpty() || exoPlayer!!.duration <= 0) return

        val horizontalOffset = (parent as View).marginLeft
        length = width - horizontalOffset * 2
        val centerY = height / 2f

        timestampMarkers.forEach { ts ->
            val cx = (ts * 1.0f / exoPlayer!!.duration * length) + horizontalOffset
            canvas.drawCircle(cx, centerY, markerDotRadius.toFloat(), markerPaint)
        }
    }

    private fun Long.toLength(): Int {
        return (this * 1000f / exoPlayer!!.duration * length).toInt()
    }

    fun setChapters(chapters: List<ChapterSegment>) {
        this.chapters = chapters
    }

    fun setTimestampMarkers(markers: List<Long>) {
        this.timestampMarkers = markers
        invalidate()
    }
}
