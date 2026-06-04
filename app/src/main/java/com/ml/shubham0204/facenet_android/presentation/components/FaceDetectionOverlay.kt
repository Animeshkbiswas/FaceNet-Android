package com.ml.shubham0204.facenet_android.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import java.util.Locale

@SuppressLint("ViewConstructor")
@ExperimentalGetImage
class FaceDetectionOverlay(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val viewModel: DetectScreenViewModel,
) : FrameLayout(context) {
    // Raised threshold so only higher spoof-confidence triggers the stronger alert
    private val spoofAlertThreshold = 0.82f
    private val spoofWarningThreshold = 0.7f

    // Setting `flatSearch` to `true` enables precise calculation
    // of cosine similarity.
    // This is slower than ObjectBox's vector search, which approximates
    // nearest neighbor search
    private val flatSearch: Boolean = false
    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    private var imageTransform: Matrix = Matrix()
    private var boundingBoxTransform: Matrix = Matrix()
    private var isImageTransformedInitialized = false
    private var isBoundingBoxTransformedInitialized = false

    private lateinit var frameBitmap: Bitmap
    private var isProcessing = false
    private var cameraFacing: Int? = null
    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var previewView: PreviewView

    var predictions: Array<Prediction> = arrayOf()

    init {
        doOnLayout {
            overlayHeight = it.measuredHeight
            overlayWidth = it.measuredWidth
        }
    }

    fun initializeCamera(cameraFacing: Int) {
        this.cameraFacing = cameraFacing
        this.isImageTransformedInitialized = false
        this.isBoundingBoxTransformedInitialized = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(cameraFacing).build()
                val frameAnalyzer =
                    ImageAnalysis
                        .Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                frameAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    frameAnalyzer,
                )
            },
            executor,
        )
        if (childCount == 2) {
            removeView(this.previewView)
            removeView(this.boundingBoxOverlay)
        }
        this.previewView = previewView
        addView(this.previewView)

        val boundingBoxOverlayParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.boundingBoxOverlay = BoundingBoxOverlay(context)
        this.boundingBoxOverlay.setWillNotDraw(false)
        this.boundingBoxOverlay.setZOrderOnTop(true)
        addView(this.boundingBoxOverlay, boundingBoxOverlayParams)
    }

    private val analyzer =
        ImageAnalysis.Analyzer { image ->
            if (isProcessing) {
                image.close()
                return@Analyzer
            }
            isProcessing = true

            // Transform android.net.Image to Bitmap
            frameBitmap =
                createBitmap(image.image!!.width, image.image!!.height)
            frameBitmap.copyPixelsFromBuffer(image.planes[0].buffer)

            // Configure frameHeight and frameWidth for output2overlay transformation matrix
            // and apply it to `frameBitmap`
            if (!isImageTransformedInitialized) {
                imageTransform = Matrix()
                imageTransform.apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                isImageTransformedInitialized = true
            }
            frameBitmap =
                Bitmap.createBitmap(
                    frameBitmap,
                    0,
                    0,
                    frameBitmap.width,
                    frameBitmap.height,
                    imageTransform,
                    false,
                )

            if (!isBoundingBoxTransformedInitialized) {
                boundingBoxTransform = Matrix()
                boundingBoxTransform.apply {
                    setScale(
                        overlayWidth / frameBitmap.width.toFloat(),
                        overlayHeight / frameBitmap.height.toFloat(),
                    )
                    if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                        // Mirror the bounding box coordinates
                        // for front-facing camera
                        postScale(
                            -1f,
                            1f,
                            overlayWidth.toFloat() / 2.0f,
                            overlayHeight.toFloat() / 2.0f,
                        )
                    }
                }
                isBoundingBoxTransformedInitialized = true
            }
            CoroutineScope(Dispatchers.Default).launch {
                val predictions = ArrayList<Prediction>()
                val (metrics, results) =
                    viewModel.imageVectorUseCase.getNearestPersonName(
                        frameBitmap,
                        flatSearch,
                    )
                results.forEach { (name, boundingBox, spoofResult) ->
                    val box = boundingBox.toRectF()
                    // Keep recognition label (name) even when spoof is detected
                    var personName = name
                    var spoofAlert = false
                    var spoofLabel = ""
                    var spoofScore = 0f

                    if (viewModel.getNumPeople().toInt() == 0) {
                        personName = ""
                    }

                    if (spoofResult != null && spoofResult.isSpoof) {
                        spoofScore = spoofResult.score
                        val formattedScore = String.format(Locale.US, "%.2f", spoofScore)
                        spoofAlert = spoofScore >= spoofAlertThreshold
                        spoofLabel = if (spoofAlert) {
                            "NOT LIVE ($formattedScore)"
                        } else if (spoofScore > spoofWarningThreshold) {
                            "Possible spoof ($formattedScore)"
                        } else {
                            ""
                        }
                    }

                    boundingBoxTransform.mapRect(box)
                    predictions.add(Prediction(box, personName, spoofAlert, spoofLabel))
                }
                withContext(Dispatchers.Main) {
                    viewModel.faceDetectionMetricsState.value = metrics
                    viewModel.notLiveWarningTextState.value =
                        predictions.firstOrNull { it.spoofAlert }?.spoofWarning.orEmpty()
                    this@FaceDetectionOverlay.predictions = predictions.toTypedArray()
                    boundingBoxOverlay.invalidate()
                    isProcessing = false
                }
            }
            image.close()
        }

    data class Prediction(
        var bbox: RectF,
        var label: String,
        var spoofAlert: Boolean = false,
        var spoofWarning: String = "",
    )

    inner class BoundingBoxOverlay(
        context: Context,
    ) : SurfaceView(context),
        SurfaceHolder.Callback {
        private val boxPaint =
            Paint().apply {
                color = Color.parseColor("#4D90caf9")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
        private val labelBackgroundPaint = Paint().apply { color = Color.parseColor("#B3000000") }
        private val warningBackgroundPaint = Paint().apply { color = Color.parseColor("#D32F2F") }
        private val warningIconPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        private val warningTextPaint = Paint().apply {
            strokeWidth = 2.0f
            textSize = 38f
            color = Color.WHITE
            isAntiAlias = true
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val textPaint =
            Paint().apply {
                strokeWidth = 2.0f
                textSize = 34f
                color = Color.WHITE
            }

        override fun surfaceCreated(holder: SurfaceHolder) {}

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onDraw(canvas: Canvas) {
            predictions.forEach {
                boxPaint.color = if (it.spoofAlert) Color.parseColor("#FFE53935") else Color.parseColor("#B390CAF9")
                boxPaint.strokeWidth = if (it.spoofAlert) 6f else 4f
                // draw a slightly smaller box (inset) so the rectangle doesn't overwhelm the face
                val shrink = 0.12f * kotlin.math.min(it.bbox.width(), it.bbox.height())
                val drawnBox = RectF(it.bbox)
                drawnBox.inset(shrink, shrink)
                canvas.drawRoundRect(drawnBox, 12f, 12f, boxPaint)
                // draw recognized name at the bottom of the box
                if (it.label.isNotBlank()) {
                    drawChip(
                        canvas = canvas,
                        text = it.label,
                        left = drawnBox.left + 12f,
                        top = drawnBox.bottom - 58f,
                        backgroundPaint = labelBackgroundPaint,
                    )
                }

                // if a spoof warning exists, draw a compact warning chip above the box
                if (it.spoofWarning.isNotBlank()) {
                    val warnTop = (drawnBox.top - 52f).coerceAtLeast(8f)
                    drawWarningChip(
                        canvas = canvas,
                        text = it.spoofWarning,
                        left = drawnBox.left + 12f,
                        top = warnTop,
                        backgroundPaint = warningBackgroundPaint,
                    )
                }
            }
        }

        private fun drawChip(
            canvas: Canvas,
            text: String,
            left: Float,
            top: Float,
            backgroundPaint: Paint,
        ) {
            val textBounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val paddingX = 14f
            val paddingY = 10f
            val right = left + textBounds.width() + paddingX * 2
            val bottom = top + textBounds.height() + paddingY * 2
            canvas.drawRoundRect(RectF(left, top, right, bottom), 18f, 18f, backgroundPaint)
            canvas.drawText(text, left + paddingX, bottom - paddingY - textBounds.bottom, textPaint)
        }

        private fun drawWarningChip(
            canvas: Canvas,
            text: String,
            left: Float,
            top: Float,
            backgroundPaint: Paint,
        ) {
            val textBounds = Rect()
            val previousTextSize = warningTextPaint.textSize
            warningTextPaint.getTextBounds(text, 0, text.length, textBounds)
            val paddingX = 16f
            val paddingY = 12f
            val iconSize = 18f
            val iconGap = 12f
            val chipHeight = textBounds.height() + paddingY * 2
            val right = left + paddingX * 2 + iconSize + iconGap + textBounds.width()
            val bottom = top + chipHeight

            canvas.drawRoundRect(RectF(left, top, right, bottom), 18f, 18f, backgroundPaint)

            val iconCenterX = left + paddingX + iconSize / 2f
            val iconCenterY = top + chipHeight / 2f
            canvas.drawCircle(iconCenterX, iconCenterY, iconSize / 2f, warningIconPaint)

            val iconBarPaint = Paint(warningIconPaint).apply { strokeWidth = 2.5f; style = Paint.Style.STROKE }
            canvas.drawLine(iconCenterX, iconCenterY - 5f, iconCenterX, iconCenterY + 3f, iconBarPaint)
            canvas.drawPoint(iconCenterX, iconCenterY + 6f, warningIconPaint)

            canvas.drawText(
                text,
                left + paddingX + iconSize + iconGap,
                bottom - paddingY - textBounds.bottom,
                warningTextPaint,
            )
            warningTextPaint.textSize = previousTextSize
        }
    }
}
