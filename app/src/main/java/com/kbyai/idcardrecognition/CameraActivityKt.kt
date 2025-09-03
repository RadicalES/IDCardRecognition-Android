package com.kbyai.idcardrecognition;

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbyai.idsdk.IDSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import org.json.JSONObject

class CameraActivityKt : AppCompatActivity() {

    val TAG = CameraActivityKt::class.java.simpleName
    val PREVIEW_WIDTH = 1280
    val PREVIEW_HEIGHT = 720

    private lateinit var cameraView: CameraView
    private lateinit var faceView: FaceView
    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context
    private var documenName: String = ""
    private var positionRect: Rect = Rect()

    private var recognized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        faceView = findViewById(R.id.faceView)

        fotoapparat = Fotoapparat.with(this)
            .into(cameraView)
//            .cameraId("100")
            .lensPosition(back())
            .frameProcessor(FaceFrameProcessor())
            .previewResolution { Resolution(PREVIEW_WIDTH,PREVIEW_HEIGHT) }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            faceView.setScaleType(fotoapparat.scaleType)
            fotoapparat.start()
        }
    }

    override fun onResume() {
        super.onResume()
        recognized = false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
        faceView.setDocumentInfos(null, "")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fotoapparat.start()
            }
        }
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {

            if(recognized == true) {
                return
            }

//            val bitmap = IDSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, 6)
//            val halfWidth = frame.size.width / 2f
//            val halfHeight = frame.size.height / 2f
//            val matrix = Matrix()
//            matrix.postRotate(-frame.rotation.toFloat(), halfWidth, halfHeight)
//            matrix.postRotate(-frame.rotation.toFloat())
//            val rbitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val result = IDSDK.idcardRecognition(frame.image)
            frame.image.recycle()

            try {
                val jsonResult = JSONObject(result)
                val positionObj = jsonResult["Position"] as JSONObject
                val hasMrz = jsonResult.has("MRZ")
                documenName = jsonResult["Document Name"] as String
                val quality = jsonResult["Quality"] as Int
                val x1 = positionObj["x1"] as Int
                val y1 = positionObj["y1"] as Int
                val x2 = positionObj["x2"] as Int
                val y2 = positionObj["y2"] as Int
                positionRect = Rect(x1, y1, x2, y2)

                Log.d(TAG, "process: quality = $quality")
                Log.d(TAG, "process: position = $positionRect")

                if (quality > 86 && (documenName != "Unknown" || hasMrz == true)) {
                    recognized = true

                    runOnUiThread {
                        faceView.setFrameSize(Size(frame.width, frame.height))
                        faceView.setDocumentInfos(positionRect, documenName)
                    }

                    runOnUiThread {
                        ResultActivity.resultString = jsonResult.toString()
                        val intent = Intent(context, ResultActivity::class.java)
                        startActivity(intent)
                    }
                }
                documenName = documenName + " " + quality
            } catch (e1: Exception) {
                documenName = ""
                positionRect = Rect()
            }

            runOnUiThread {
                faceView.setFrameSize(Size(frame.width, frame.height))
                faceView.setDocumentInfos(positionRect, documenName)
                faceView.clear()
//                faceView.add(FaceGraphicOverlay(faceView, frame.image))
            }
        }
    }
}