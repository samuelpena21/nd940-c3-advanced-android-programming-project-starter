package com.udacity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.properties.Delegates

private const val TOTAL_MS: Long = 2000L

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var viewBackgroundColor = 0
    private var textColor = 0
    private var animatedCircleBackgroundColor = 0
    private var textSize = 0f

    private var widthSize = 0
    private var heightSize = 0
    private lateinit var rect: RectF
    private var diameter = 100f
    private val radius: Float get() = diameter.div(2)

    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = resources.getDimension(R.dimen.strokeWidth)
    }

    private var displayedText: String = resources.getString(R.string.download)
    private var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val valueAnimator = ValueAnimator()

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _,
        new ->
        when (new) {
            ButtonState.Clicked -> startAnimation()
            ButtonState.Loading -> {
                displayedText = resources.getString(R.string.we_are_loading)
            }
            ButtonState.Completed -> {
                progress = 0f
                displayedText = resources.getString(R.string.download)
            }
        }
    }

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            viewBackgroundColor = getColor(R.styleable.LoadingButton_backgroundColor, 0)
            textColor = getColor(R.styleable.LoadingButton_textColor, 0)
            animatedCircleBackgroundColor =
                getColor(R.styleable.LoadingButton_animatedCircleColor, 0)
            textSize = getDimension(
                R.styleable.LoadingButton_textSize,
                resources.getDimension(R.dimen.defaultTextSize)
            )
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawContainerRectangle(canvas)
        drawProgress(canvas)
        drawAnimation(canvas)
    }

    private fun drawContainerRectangle(canvas: Canvas?) {
        canvas?.drawColor(resources.getColor(R.color.colorPrimary, null))
    }

    private fun drawProgress(canvas: Canvas?) {
        rect = RectF(0f, 0f, 0f, heightSize.toFloat())
        rect.right = progress
        paint.color = viewBackgroundColor
        canvas?.drawRect(rect, paint)
    }

    private fun drawAnimation(canvas: Canvas?) {
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = textSize
        canvas?.drawText(displayedText, widthSize.div(2f), heightSize.div(1.7f), paint)

        val textBounds = Rect()
        paint.getTextBounds(displayedText, 0, displayedText.length, textBounds)

        val textBoundsToF = RectF()
        textBoundsToF.set(
            widthSize.div(2).toFloat() + textBounds.width().div(2),
            heightSize.div(2) - radius,
            widthSize.div(2).toFloat() + textBounds.width().div(2) + diameter,
            heightSize.div(2) + radius
        )

        val arcProgress = (progress / widthSize.toFloat()) * 360f
        paint.color = animatedCircleBackgroundColor

        canvas?.drawArc(textBoundsToF, 270f, arcProgress, true, paint)
    }

    private fun startAnimation() {
        valueAnimator.apply {
            setObjectValues(0f, widthSize.toFloat())
            duration = TOTAL_MS
            repeatMode = ValueAnimator.RESTART
            repeatCount = INFINITE
            addUpdateListener { valueAnimator ->
                progress = valueAnimator.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    isEnabled = false
                    buttonState = ButtonState.Loading
                }

                override fun onAnimationEnd(animation: Animator?) {
                    isEnabled = true
                    buttonState = ButtonState.Completed
                }
            })
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

    override fun performClick(): Boolean {
        buttonState = ButtonState.Clicked
        return super.performClick()
    }

    fun stopLoading() {
        valueAnimator.cancel()
    }
}