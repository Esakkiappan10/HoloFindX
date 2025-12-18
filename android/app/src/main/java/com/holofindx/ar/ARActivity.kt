package com.holofindx.ar

import android.Manifest
import android.content.pm.PackageManager
import android.media.Image
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.mlkit.vision.common.InputImage
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var surfaceView: GLSurfaceView
    private var session: Session? = null
    private val backgroundRenderer = BackgroundRenderer()

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var isTextureSet = false

    // Object detection throttling
    private var frameCounter = 0
    private var isDetecting = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
    }

    // ======================
    // ACTIVITY
    // ======================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = GLSurfaceView(this).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setRenderer(this@ARActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContentView(surfaceView)

        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    // ======================
    // LIFECYCLE
    // ======================

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) return

        if (session == null) {
            if (!createSession()) return
        }

        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
            session = null
            return
        }

        surfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
        session?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        session = null
    }

    // ======================
    // ARCORE SETUP
    // ======================

    private fun createSession(): Boolean {
        when (ArCoreApk.getInstance().requestInstall(this, true)) {
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return false
            ArCoreApk.InstallStatus.INSTALLED -> {}
        }

        return try {
            session = Session(this)

            val config = Config(session).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
            }

            session?.configure(config)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "ARCore not supported", Toast.LENGTH_LONG).show()
            finish()
            false
        }
    }

    // ======================
    // OPENGL
    // ======================

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
        isTextureSet = false
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = session ?: return

        // 🔑 Correct orientation & aspect
        session.setDisplayGeometry(
            when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            },
            viewportWidth,
            viewportHeight
        )

        // 🔑 Set camera texture ONCE
        if (!isTextureSet) {
            session.setCameraTextureName(backgroundRenderer.getTextureId())
            isTextureSet = true
        }

        try {
            val frame = session.update()

            // 🔥 OBJECT DETECTION (every 15 frames)
            frameCounter++
            if (frameCounter % 15 == 0 && !isDetecting) {
                detectObjects(frame)
            }

            backgroundRenderer.draw(frame)

        } catch (_: Exception) {}
    }

    // ======================
    // OBJECT DETECTION
    // ======================

    private fun detectObjects(frame: Frame) {
        val image: Image = try {
            frame.acquireCameraImage()
        } catch (e: Exception) {
            return
        }

        isDetecting = true

        val inputImage = InputImage.fromMediaImage(
            image,
            getRotationDegrees()
        )

        ObjectDetectorHelper.detector.process(inputImage)
            .addOnSuccessListener { objects ->
                for (obj in objects) {
                    val label = obj.labels.firstOrNull()?.text ?: "Unknown"
                    val confidence = obj.labels.firstOrNull()?.confidence ?: 0f
                    val box = obj.boundingBox

                    // 🔍 LOG RESULT (for now)
                    android.util.Log.d(
                        "AR_OBJECT",
                        "Detected: $label ($confidence) at $box"
                    )
                }
            }
            .addOnCompleteListener {
                image.close()       // 🔴 MUST close
                isDetecting = false
            }
    }

    private fun getRotationDegrees(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    // ======================
    // PERMISSIONS
    // ======================

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onResume()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
