package org.hyperskill.photoeditor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import org.hyperskill.photoeditor.FiltersActivity.brightnessOfImage
import org.hyperskill.photoeditor.FiltersActivity.brightnessOfPixel
import org.hyperskill.photoeditor.FiltersActivity.contrastOfPixel
import org.hyperskill.photoeditor.FiltersActivity.saturationOfPixel
import kotlin.math.pow

object FiltersActivity {

    private fun Int.extractRGB(): Triple<Int, Int, Int> {
        val color = this
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        return Triple(red, green, blue)
    }

    fun Bitmap.totalBrightness(): Int {
        val bitmap = this
        val height = bitmap.height
        val width = bitmap.width
        var totalBrightness = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val (red, green, blue) = this.getPixel(x, y).extractRGB()
                totalBrightness += red + green + blue
            }
        }

        return totalBrightness / (height * width * 3)
    }

    private fun Bitmap.brightnessOfPixel(x: Int, y: Int, value: Int): Int {
        val (red, green, blue) = this.getPixel(x, y).extractRGB()
        val newValue = { startValue: Int ->
            when {
                startValue > 255 -> 255
                startValue < 0 -> 0
                else -> startValue
            }
        }
        val newRed = newValue(red + value)
        val newGreen = newValue(green + value)
        val newBlue = newValue(blue + value)

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun Bitmap.contrastOfPixel(x: Int, y: Int, contrast: Int, averageBrightness: Int): Int {
        val alpha = (255 + contrast).toDouble() / (255 - contrast)
        val (red, green, blue) = this.getPixel(x, y).extractRGB()
        val newValue = { oldColor: Int ->
            val startValue = (alpha * (oldColor - averageBrightness) + averageBrightness).toInt()
            when {
                startValue > 255 -> 255
                startValue < 0 -> 0
                else -> startValue
            }
        }
        val newRed = newValue(red)
        val newGreen = newValue(green)
        val newBlue = newValue(blue)

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun Bitmap.saturationOfPixel(x: Int, y: Int, saturation: Int, rgbAvg: Int): Int {
        val alpha = (255 + saturation).toDouble() / (255 - saturation)
        val (red, green, blue) = this.getPixel(x, y).extractRGB()
        val newValue = { oldColor: Int ->
            val startValue = (alpha * (oldColor - rgbAvg) + rgbAvg).toInt()
            when {
                startValue > 255 -> 255
                startValue <0 -> 0
                else -> startValue
            }
        }
        val newRed = newValue(red)
        val newGreen = newValue(green)
        val newBlue = newValue(blue)

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun Bitmap.gammaOfPixel(x: Int, y: Int, gamma: Double): Int {
        val (red, green, blue) = this.getPixel(x, y).extractRGB()
        val newValue = { oldColor: Int ->
            val startValue = (255 * (oldColor / 255.0).pow(gamma)).toInt()
            when {
                startValue > 255 -> 255
                startValue <0 -> 0
                else -> startValue
            }
        }
        val newRed = newValue(red)
        val newGreen = newValue(green)
        val newBlue = newValue(blue)

        return Color.rgb(newRed, newGreen, newBlue)
    }

    fun Bitmap.brightnessOfImage(value: Int): Bitmap {
        val bitmap = this
        val height = bitmap.height
        val width = bitmap.width
        val copy = bitmap.copy(Bitmap.Config.RGB_565, true)
        for (y in 0 until height) {
            for (x in 0 until width) {
                copy[x, y] = bitmap.brightnessOfPixel(x, y, value)
            }
        }

        return copy
    }

    fun Bitmap.contrastOfImage(value: Int, averageBrightness: Int): Bitmap {
        val bitmap = this
        val height = bitmap.height
        val width = bitmap.width
        val copy = bitmap.copy(Bitmap.Config.RGB_565, true)
        for (y in 0 until height) {
            for (x in 0 until width) {
                copy[x, y] = bitmap.contrastOfPixel(x, y, value, averageBrightness)
            }
        }

        return copy
    }

    fun Bitmap.saturationOfImage(value: Int, origBitmap: Bitmap): Bitmap {
        val bitmap = this
        val height = bitmap.height
        val width = bitmap.width
        val copy = bitmap.copy(Bitmap.Config.RGB_565, true)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val (red, green, blue) = origBitmap.getPixel(x, y).extractRGB()
                val rgbAvg = (red + green + blue) / 3
                copy[x, y] = bitmap.saturationOfPixel(x, y, value, rgbAvg)
            }
        }

        return copy
    }

    fun Bitmap.gammaOfImage(value: Float): Bitmap {
        val bitmap = this
        val height = bitmap.height
        val width = bitmap.width
        val copy = bitmap.copy(Bitmap.Config.RGB_565, true)
        for (y in 0 until height) {
            for (x in 0 until width) {
                copy[x, y] = bitmap.gammaOfPixel(x, y, value.toDouble())
            }
        }

        return copy
    }
}