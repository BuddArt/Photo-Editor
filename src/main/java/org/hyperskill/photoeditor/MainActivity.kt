package org.hyperskill.photoeditor

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.google.android.material.slider.Slider
import kotlinx.coroutines.*

import org.hyperskill.photoeditor.FiltersActivity.brightnessOfImage
import org.hyperskill.photoeditor.FiltersActivity.contrastOfImage
import org.hyperskill.photoeditor.FiltersActivity.gammaOfImage
import org.hyperskill.photoeditor.FiltersActivity.saturationOfImage
import org.hyperskill.photoeditor.FiltersActivity.totalBrightness


class MainActivity : AppCompatActivity() {
    private val btnGallery: Button by lazy {
        findViewById(R.id.btnGallery)
    }
    private val btnSave: Button by lazy {
        findViewById(R.id.btnSave)
    }
    private val currentImage: ImageView by lazy {
        findViewById(R.id.ivPhoto)
    }
    private val sliderBrightness: Slider by lazy {
        findViewById(R.id.slBrightness)
    }
    private val sliderContrast: Slider by lazy {
        findViewById(R.id.slContrast)
    }
    private val sliderSaturation: Slider by lazy {
        findViewById(R.id.slSaturation)
    }
    private val sliderGamma: Slider by lazy {
        findViewById(R.id.slGamma)
    }
    private val activityResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoUri = result.data?.data ?: return@registerForActivityResult
                currentImage.setImageURI(photoUri)
                currentImageDrawable = currentImage.drawable as BitmapDrawable?
            }
        }

    private var currentImageDrawable: BitmapDrawable? = null
    private var lastJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        currentImage.setImageBitmap(createBitmap())

        currentImageDrawable = currentImage.drawable as BitmapDrawable?
    }

    private fun bindViews() {
        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
        }

        /*
        sliderBrightness.addOnChangeListener { slider, value, fromUser ->
            val bitmap = currentImageDrawable?.bitmap ?: return@addOnChangeListener
            val copyImage = bitmap.copy(Bitmap.Config.RGB_565, true)
            val height = bitmap.height
            val width = bitmap.width
            val intValue = value.toInt()

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val color = bitmap.getPixel(x, y)
                    val oldRed = Color.red(color)
                    val oldGreen = Color.green(color)
                    val oldBlue = Color.blue(color)

                    val newRed = when {
                        oldRed + intValue > 255 -> 255
                        oldRed + intValue < 0 -> 0
                        else -> oldRed + intValue
                    }
                    val newGreen = when {
                        oldGreen + intValue > 255 -> 255
                        oldGreen + intValue < 0 -> 0
                        else -> oldGreen + intValue
                    }
                    val newBlue = when {
                        oldBlue + intValue > 255 -> 255
                        oldBlue + intValue < 0 -> 0
                        else -> oldBlue + intValue
                    }

                    copyImage[x, y] = Color.rgb(newRed, newGreen, newBlue)
                }
            }

            currentImage.setImageBitmap(copyImage)
        }
        */

        btnSave.setOnClickListener { _ ->
            if (hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                currentImageDrawable = currentImage.drawable as BitmapDrawable?
                val bitmap: Bitmap = currentImageDrawable?.bitmap ?: return@setOnClickListener
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
                values.put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)

                val uri = this@MainActivity.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@setOnClickListener

                contentResolver.openOutputStream(uri).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            } else {
                requestPermissions(listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }


        sliderBrightness.addOnChangeListener(this::sliderChanges)
        sliderContrast.addOnChangeListener(this::sliderChanges)
        sliderSaturation.addOnChangeListener(this::sliderChanges)
        sliderGamma.addOnChangeListener(this::sliderChanges)
    }
/*
    private fun sliderChanges(slider: Slider, value: Float, fromUser: Boolean) {
        val bitmap = currentImageDrawable?.bitmap ?: return

        val brightnessValue = sliderBrightness.value.toInt()
        val brightnessCopy = bitmap.brightnessOfImage(brightnessValue)

        val contrastValue = sliderContrast.value.toInt()
        val averageBrightness = brightnessCopy.totalBrightness()
        val contrastCopy = brightnessCopy.contrastOfImage(contrastValue, averageBrightness)

        val saturationValue = sliderSaturation.value.toInt()
        val saturationCopy = contrastCopy.saturationOfImage(saturationValue, contrastCopy)

        val gammaValue = sliderGamma.value
        val gammaCopy = saturationCopy.gammaOfImage(gammaValue)


        currentImage.setImageBitmap(gammaCopy)
    }
    */


    private fun sliderChanges(slider: Slider, sliderValue: Float, fromUser: Boolean) {

        lastJob?.cancel()

        lastJob = GlobalScope.launch(Dispatchers.Default) {
            //  I/System.out: onSliderChanges job making calculations running on thread DefaultDispatcher-worker-1
            println("onSliderChanges " + "job making calculations running on thread ${Thread.currentThread().name}")

            val bitmap = currentImageDrawable?.bitmap ?: return@launch

            val brightenCopyDeferred: Deferred<Bitmap> = this.async {
                val brightnessValue = sliderBrightness.value.toInt()
                bitmap.brightnessOfImage(brightnessValue)
            }
            val brightenCopy: Bitmap = brightenCopyDeferred.await()

            val contrastedCopyDeferred: Deferred<Bitmap> = this.async {
                val contrastValue = sliderContrast.value.toInt()
                val averageBrightness = brightenCopy.totalBrightness()
                brightenCopy.contrastOfImage(contrastValue, averageBrightness)
            }
            val contrastedCopy = contrastedCopyDeferred.await()

            val saturatedCopyDeferred: Deferred<Bitmap> = this.async {
                val saturationValue = sliderSaturation.value.toInt()
                contrastedCopy.saturationOfImage(saturationValue, contrastedCopy)
            }
            val saturatedCopy = saturatedCopyDeferred.await()

            val gammaCopyDeferred: Deferred<Bitmap> = this.async {
                val gammaValue = sliderGamma.value
                saturatedCopy.gammaOfImage(gammaValue)
            }
            val gammaCopy = gammaCopyDeferred.await()

            runOnUiThread {
                //  I/System.out: onSliderChanges job updating view running on thread main
                println("onSliderChanges " + "job updating view running on thread ${Thread.currentThread().name}")
                currentImage.setImageBitmap(gammaCopy)
            }
        }
    }

    private fun hasPermission(manifestPermission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkSelfPermission(manifestPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            PermissionChecker.checkSelfPermission(
                this,
                manifestPermission
            ) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions(permissionsToRequest: List<String>) {
        permissionsToRequest.filter { permissionToRequest ->
            hasPermission(permissionToRequest).not()
        }.also {
            if (it.isEmpty().not()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.i("Permission", "requestPermissions")
                    this.requestPermissions(it.toTypedArray(), 0)
                } else {
                    Log.i("Permission", "missing required permission")
                }
            } else {
                Log.i("Permission", "All required permissions are granted")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEachIndexed { index: Int, result: Int ->
            if (result == PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionRequest", "${permissions[index]} granted")
                if (permissions[index] == android.Manifest.permission.READ_EXTERNAL_STORAGE) {
                    btnGallery.callOnClick()
                } else if (permissions[index] == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    btnSave.callOnClick()
                }
            } else {
                Log.d("PermissionRequest", "${permissions[index]} denied")
            }
        }
    }

    // do not change this function
    private fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var r: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                r = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120

                pixels[index] = Color.rgb(r, G, B)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }
}
