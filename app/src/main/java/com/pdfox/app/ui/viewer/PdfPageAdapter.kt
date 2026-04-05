package com.pdfox.app.ui.viewer

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class PdfPageAdapter(
    private val pdfRenderer: PdfRenderer
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    override fun getItemCount(): Int = pdfRenderer.pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.MATRIX
            setImageResource(android.R.drawable.ic_menu_gallery)
        }
        return PdfPageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bindPage(position)
    }

    override fun onViewRecycled(holder: PdfPageViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class PdfPageViewHolder(
        private val imageView: ImageView
    ) : RecyclerView.ViewHolder(imageView) {

        private var renderedBitmap: Bitmap? = null
        private var currentPageIndex: Int = -1

        private val matrix = Matrix()
        private var scale = 1f
        private var lastScale = 1f

        private val scaleGestureDetector = ScaleGestureDetector(
            imageView.context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val newScale = (scale * scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                    val actualScaleFactor = newScale / scale

                    matrix.postScale(
                        actualScaleFactor,
                        actualScaleFactor,
                        detector.focusX,
                        detector.focusY
                    )

                    scale = newScale
                    imageView.imageMatrix = matrix
                    return true
                }
            }
        )

        init {
            imageView.setOnTouchListener { _, event ->
                if (scale > 1f) {
                    imageView.parent.requestDisallowInterceptTouchEvent(true)
                }
                scaleGestureDetector.onTouchEvent(event)

                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    if (scale <= 1.05f) {
                        resetZoom()
                    }
                    imageView.parent.requestDisallowInterceptTouchEvent(false)
                }
                true
            }
        }

        fun bindPage(pageIndex: Int) {
            if (pageIndex == currentPageIndex) return
            currentPageIndex = pageIndex

            if (scale != 1f) {
                resetZoom()
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    val width = page.width * 2
                    val height = page.height * 2

                    val bitmap = Bitmap.createBitmap(
                        width.coerceAtMost(MAX_BITMAP_SIZE),
                        height.coerceAtMost(MAX_BITMAP_SIZE),
                        Bitmap.Config.ARGB_8888
                    )

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    withContext(Dispatchers.Main) {
                        if (currentPageIndex == pageIndex) {
                            renderedBitmap?.recycle()
                            renderedBitmap = bitmap
                            imageView.setImageBitmap(bitmap)
                            matrix.reset()
                            imageView.imageMatrix = matrix
                        } else {
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to render page $pageIndex")
                }
            }
        }

        fun recycle() {
            renderedBitmap?.recycle()
            renderedBitmap = null
            currentPageIndex = -1
            resetZoom()
        }

        private fun resetZoom() {
            scale = 1f
            matrix.reset()
            imageView.imageMatrix = matrix
        }

        companion object {
            private const val MIN_SCALE = 1f
            private const val MAX_SCALE = 5f
            private const val MAX_BITMAP_SIZE = 4096
        }
    }
}
