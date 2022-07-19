package com.example.textrecognizewithcompose.composable

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@Composable
fun MLKitTextRecognition() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recognizedText = remember {
        mutableStateOf("")
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TextCaptureView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            recognizedText = recognizedText
        )
        Text(
            text = recognizedText.value,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(15.dp)
        )
    }
    
}


@Composable
fun TextCaptureView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    recognizedText : MutableState<String>
) {

    val cameraProviderFeature = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val cameraProvider = cameraProviderFeature.get()
    var preview by remember { mutableStateOf<Preview?>(null) }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val executor = ContextCompat.getMainExecutor(context)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box{
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFeature.addListener({
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(
                                cameraExecutor,
                                ObjectDetectorImageAnalyzer(textRecognizer = textRecognizer, recognizedText = recognizedText)
                            )
                        }

                    val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalysis,
                        preview
                    )

                },executor)
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                previewView
            }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = { Toast.makeText(context, "Back Clicked", Toast.LENGTH_SHORT).show() }
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "back",
                    tint = Color.White
                )
            }
        }
    }
}


class ObjectDetectorImageAnalyzer(
    private val textRecognizer: TextRecognizer,
    private val recognizedText: MutableState<String>
):ImageAnalysis.Analyzer{
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null){
            val image = InputImage.fromMediaImage(mediaImage,imageProxy.imageInfo.rotationDegrees)

            textRecognizer.process(image)
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        recognizedText.value = it.result.text
                    }
                    imageProxy.close()
                }
        }
    }

}