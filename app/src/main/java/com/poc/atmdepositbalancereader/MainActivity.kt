package com.poc.atmdepositbalancereader

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    }

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    /* private lateinit var viewModel: MLExecutionViewModel
     private var useGPU = false
     private var ocrModel: OCRModelExecutor? = null
     private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
     private val mainScope = MainScope()
     private val mutex = Mutex()*/
    private var listValue: ArrayList<String>? = null

    private var mImageMaxWidth: Int? = null
    private var mImageMaxHeight: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            askPermission()
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        // initViewModel()
    }

    private fun startCamera() {
        val pvImageCapture: PreviewView = findViewById(R.id.pv_image_capture)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(pvImageCapture.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                atv_Capture?.setOnClickListener {
                    val mDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, mDateFormat.format(Date()))
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "Pictures/ATMDepositBalanceReader"
                            )
                        }
                    }

                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                        contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ).build()
                    imageCapture?.takePicture(
                        outputFileOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                Log.e(TAG, "onImageSaved: ${outputFileResults.savedUri}")
                            }

                            override fun onError(error: ImageCaptureException) {
                                error.printStackTrace()
                                Log.e(TAG, "onError: ")
                            }

                        })

                    imageCapture?.takePicture(cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val planeProxy = image.planes[0]
                                val buffer: ByteBuffer = planeProxy.buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val captureImage =
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                Log.e(TAG, "onCaptureSuccess: $captureImage")
                                //detectText(captureImage)
                                runTextRecognition(captureImage)
                                super.onCaptureSuccess(image)
                            }
                        })
                }

            } catch (exc: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.tv_error), Toast.LENGTH_SHORT)
                    .show()
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /*
    private fun initViewModel() {
        viewModel = ViewModelProvider.AndroidViewModelFactory(application)
            .create(MLExecutionViewModel::class.java)
        viewModel.resultingBitmap.observe(
            this, androidx.lifecycle.Observer {
                for ((word, color) in it.itemsFound) {
                    Log.e(TAG, "initViewModel: $word")
                }
            }
        )
        mainScope.async(inferenceThread) { createModelExecutor(useGPU) }
    }

    private suspend fun createModelExecutor(useGPU: Boolean) {
        mutex.withLock {
            if (ocrModel != null) {
                ocrModel!!.close()
                ocrModel = null
            }
            try {
                ocrModel = OCRModelExecutor(this, useGPU)
            } catch (e: Exception) {
                Log.e(TAG, "Fail to create OCRModelExecutor: ${e.message}")
            }
        }
    }

    private fun detectText(image: Bitmap) {
        mainScope.async(inferenceThread) {
            mutex.withLock {
                if (ocrModel != null) {
                    viewModel.onApplyModel(image, ocrModel, inferenceThread)
                } else {
                    Log.d(
                        TAG,
                        "Skipping running OCR since the ocrModel has not been properly initialized ..."
                    )
                }
            }
        }
    }*/

    private fun getTargetedWidthHeight(): Pair<Int, Int> {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode: Int = 250
        val maxHeightForPortraitMode: Int = 250
        targetWidth = maxWidthForPortraitMode
        targetHeight = maxHeightForPortraitMode
        return Pair(targetWidth, targetHeight)
    }

    private fun runTextRecognition(image: Bitmap) {

        // Get the dimensions of the View
        val targetedSize: Pair<Int, Int> = getTargetedWidthHeight()

        val targetWidth = targetedSize.first
        val maxHeight = targetedSize.second

        // Determine how much to scale down the image
        val scaleFactor: Float = max(
            image.width.toFloat() / targetWidth.toFloat(),
            image.height.toFloat() / maxHeight.toFloat()
        )

        val resizedBitmap = Bitmap.createScaledBitmap(
            image,
            (image.width / scaleFactor).toInt(),
            (image.height / scaleFactor).toInt(),
            true
        )

        val image: InputImage = InputImage.fromBitmap(resizedBitmap, 0)
        val recognizer: TextRecognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { processTextRecognitionResult(it) }
            .addOnFailureListener { }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            Toast.makeText(baseContext, "Unable to detect text", Toast.LENGTH_SHORT).show()
            return
        }

        p_loader_scan?.visibility = View.VISIBLE

        listValue = ArrayList()
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].getLines()
            var line = StringBuilder()
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].getElements()
                var value = StringBuilder()
                for (k in elements.indices) {
                    value.append(elements[k].text)
                    value.append(" ")
                }
                line.append(value)
                line.append(" ")
                value = StringBuilder()
            }
            Log.e(TAG, line.toString())
            listValue?.add(line.toString())
            line = StringBuilder()
        }

        p_loader_scan?.visibility = View.GONE

        val toDetectedDetailsActivity = Intent(this, DetectedDetailsActivity::class.java)
        toDetectedDetailsActivity.putExtra("Data", listValue)
        startActivity(toDetectedDetailsActivity)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, REQUEST_CODE_PERMISSIONS)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, REQUEST_CODE_PERMISSIONS)
            }
        } else {
            // below android 11
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}