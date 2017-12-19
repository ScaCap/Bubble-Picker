package com.igalata.bubblepicker.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.igalata.bubblepicker.model.PickerItem
import com.igalata.bubblepicker.physics.CircleBody
import com.igalata.bubblepicker.rendering.BubbleShader.U_MATRIX
import com.igalata.bubblepicker.toTexture
import org.jbox2d.common.Vec2

/**
 * Created by irinagalata on 1/19/17.
 */
data class Item(val context: Context, val pickerItem: PickerItem, val circleBody: CircleBody) {

    val x: Float
        get() = circleBody.physicalBody.position.x

    val y: Float
        get() = circleBody.physicalBody.position.y

    val radius: Float
        get() = circleBody.radius

    val initialPosition: Vec2
        get() = circleBody.position

    val currentPosition: Vec2
        get() = circleBody.physicalBody.position

    private var isVisible = true
        get() = circleBody.isVisible
    private var texture: Int = 0
    private var imageTexture: Int = 0
    private val currentTexture: Int
        get() = if (circleBody.increased || circleBody.isIncreasing) imageTexture else texture
    private val bitmapSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96f, context.resources.displayMetrics)
    private val bitmapRect = Rect().apply {
        set(0, 0, bitmapSize.toInt(), bitmapSize.toInt())
    }
    private val squareRect = Rect().apply {
        val halfXY = Math.round((bitmapSize * Math.sqrt(2.0)) / 4.0)
        val l = bitmapRect.centerX() - halfXY
        val t = bitmapRect.centerY() - halfXY
        val r = bitmapRect.centerX() + halfXY
        val b = bitmapRect.centerY() + halfXY
        set(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }

    private val viewIcon: AppCompatImageView = AppCompatImageView(context).apply {
        setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            weight = 1f
        })
        setImageDrawable(pickerItem.icon)
        setScaleType(ImageView.ScaleType.CENTER)
    }

    private val viewText: AppCompatTextView = AppCompatTextView(context).apply {
        setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            weight = 1f
        })
        setText(pickerItem.title)
        setGravity(Gravity.CENTER)

        TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 2, 16, 1, TypedValue.COMPLEX_UNIT_SP)
    }

    private val viewLayout: LinearLayout = LinearLayout(context).apply {
        setLayoutParams(ViewGroup.LayoutParams(squareRect.width(), squareRect.height()))
        setOrientation(LinearLayout.VERTICAL)

        addView(viewIcon)
        addView(viewText)
    }

    fun drawItself(programId: Int, index: Int, scaleX: Float, scaleY: Float) {
        glActiveTexture(GL_TEXTURE)
        glBindTexture(GL_TEXTURE_2D, currentTexture)
        glUniform1i(glGetUniformLocation(programId, BubbleShader.U_TEXT), 0)
        glUniform1i(glGetUniformLocation(programId, BubbleShader.U_VISIBILITY), if (isVisible) 1 else -1)
        glUniformMatrix4fv(glGetUniformLocation(programId, U_MATRIX), 1, false, calculateMatrix(scaleX, scaleY), 0)
        glDrawArrays(GL_TRIANGLE_STRIP, index * 4, 4)
    }

    fun bindTextures(textureIds: IntArray, index: Int) {
        texture = bindTexture(textureIds, index * 2, false)
        imageTexture = bindTexture(textureIds, index * 2 + 1, true)
    }

    fun createBitmap(isSelected: Boolean): Bitmap {
        var bitmap = Bitmap.createBitmap(bitmapSize.toInt(), bitmapSize.toInt(), Bitmap.Config.ARGB_4444)
        val bitmapConfig: Bitmap.Config = bitmap.config ?: Bitmap.Config.ARGB_8888
        bitmap = bitmap.copy(bitmapConfig, true)

        val canvas = Canvas(bitmap)

        drawBackground(canvas, isSelected)

        viewText.setMaxLines(1)
        viewText.setTextColor(if (isSelected) pickerItem.selectedTextColor!! else pickerItem.textColor!!)
        viewIcon.setVisibility(if (isSelected) View.VISIBLE else View.GONE)

        measure()
        layout()

        // If we set the max lines as 2, then AutoResizeTextView will push for filling up both lines.
        // So we ask for a max lines of 1 initially but if the threshold is crossed for minimum text size
        // We set maxLines to two and then go through the measure, layout pass again.
        val currTextSize = viewText.textSize / context.resources.displayMetrics.density
        if (currTextSize <= 8) {
            pickerItem.title?.let {
                viewText.text = breakString(it)
            }
            viewText.maxLines = 2
            measure()
            layout()
        }

        canvas.matrix = null
        canvas.translate((squareRect.left - bitmapRect.left).toFloat(), (squareRect.top - bitmapRect.top).toFloat())
        viewLayout.draw(canvas)

        return bitmap
    }

    private fun measure() {
        viewLayout.measure(View.MeasureSpec.makeMeasureSpec(squareRect.width(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(squareRect.height(), View.MeasureSpec.EXACTLY))
    }

    private fun layout() {
        viewLayout.layout(squareRect.left, squareRect.top, squareRect.right, squareRect.bottom)
    }

    private fun drawBackground(canvas: Canvas, isSelected: Boolean) {
        val bgPaint = Paint()
        bgPaint.style = Paint.Style.FILL
        pickerItem.color?.let {
            if (isSelected) {
                bgPaint.color = pickerItem.selectedColor!!
            } else {
                bgPaint.color = pickerItem.color!!
            }
        }
        canvas.drawRect(0f, 0f, bitmapSize, bitmapSize, bgPaint)
    }

    private fun bindTexture(textureIds: IntArray, index: Int, withImage: Boolean): Int {
        glGenTextures(1, textureIds, index)
        createBitmap(withImage).toTexture(textureIds[index])
        return textureIds[index]
    }

    private fun calculateMatrix(scaleX: Float, scaleY: Float) = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.translateM(this, 0, currentPosition.x * scaleX - initialPosition.x,
                currentPosition.y * scaleY - initialPosition.y, 0f)
    }

    private fun breakString(target: String): String {
        // TextView should break on a space automatically.
        if (target.contains(' ')) return target

        val m = target.length / 2
        return "${target.substring(0, m)}-\n${target.substring(m)}"
    }

}