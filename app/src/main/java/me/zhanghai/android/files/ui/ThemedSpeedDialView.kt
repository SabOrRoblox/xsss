package me.zhanghai.android.files.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.leinardi.android.speeddial.FabWithLabelView
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.compat.createCompat
import me.zhanghai.android.files.compat.drawableCompat
import me.zhanghai.android.files.compat.foregroundCompat
import me.zhanghai.android.files.compat.setTextAppearanceCompat
import me.zhanghai.android.files.util.ParcelableState
import me.zhanghai.android.files.util.asColor
import me.zhanghai.android.files.util.dpToDimensionPixelSize
import me.zhanghai.android.files.util.getColorByAttr
import me.zhanghai.android.files.util.getParcelableSafe
import me.zhanghai.android.files.util.getResourceIdByAttr
import me.zhanghai.android.files.util.isMaterial3Theme
import me.zhanghai.android.files.util.shortAnimTime
import me.zhanghai.android.files.util.withModulatedAlpha

class ThemedSpeedDialView : SpeedDialView {
    private var onChangeListener: OnChangeListener? = null
    private var mainFabAnimator: Animator? = null
    private var blurView: View? = null
    private var originalElevation: Float = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        mainFab.apply {
            updateLayoutParams<MarginLayoutParams> {
                setMargins(context.dpToDimensionPixelSize(16))
            }
            useCompatPadding = false
            originalElevation = elevation
            elevation = 12f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                translationZ = 4f
            }
        }

        val context = context
        if (context.isMaterial3Theme) {
            mainFabClosedBackgroundColor = Color.TRANSPARENT
            mainFabClosedIconColor =
                context.getColorByAttr(com.google.android.material.R.attr.colorOnSecondaryContainer)
            mainFabOpenedBackgroundColor = Color.TRANSPARENT
            mainFabOpenedIconColor =
                context.getColorByAttr(com.google.android.material.R.attr.colorOnPrimary)
        } else {
            mainFabClosedBackgroundColor = Color.TRANSPARENT
            mainFabClosedIconColor =
                context.getColorByAttr(com.google.android.material.R.attr.colorOnSecondary)
            mainFabOpenedBackgroundColor = Color.TRANSPARENT
            mainFabOpenedIconColor =
                context.getColorByAttr(com.google.android.material.R.attr.colorOnPrimary)
        }

        val mainFabDrawable = RotateDrawable::class.createCompat().apply {
            drawableCompat = mainFab.drawable
            toDegrees = mainFabAnimationRotateAngle
        }
        mainFabAnimationRotateAngle = 0f
        setMainFabClosedDrawable(mainFabDrawable)

        setupGlassMorphismBackground()

        super.setOnChangeListener(object : OnChangeListener {
            override fun onMainActionSelected(): Boolean =
                onChangeListener?.onMainActionSelected() ?: false

            override fun onToggleChanged(isOpen: Boolean) {
                mainFabAnimator?.cancel()
                mainFabAnimator = createMainFabAnimator(isOpen).apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            mainFabAnimator = null
                        }
                    })
                    start()
                }
                animateBlur(isOpen)
                animateElevation(isOpen)
                onChangeListener?.onToggleChanged(isOpen)
            }
        })
    }

    private fun setupGlassMorphismBackground() {
        blurView = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setBackgroundColor(Color.TRANSPARENT)
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                    20f, 20f, android.graphics.Shader.TileMode.CLAMP
                ))
            }
            (parent as? ViewGroup)?.addView(this, 0)
        }
    }

    private fun animateBlur(isOpen: Boolean) {
        blurView?.let { blur ->
            blur.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val startAlpha = if (isOpen) 0f else 0.6f
                val endAlpha = if (isOpen) 0.6f else 0f
                blur.alpha = startAlpha
                blur.animate()
                    .alpha(endAlpha)
                    .setDuration(context.shortAnimTime.toLong())
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction {
                        if (!isOpen) blur.visibility = View.GONE
                    }
                    .start()
            } else {
                val startAlpha = if (isOpen) 0f else 0.3f
                val endAlpha = if (isOpen) 0.3f else 0f
                blur.alpha = startAlpha
                blur.animate()
                    .alpha(endAlpha)
                    .setDuration(context.shortAnimTime.toLong())
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction {
                        if (!isOpen) blur.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    private fun animateElevation(isOpen: Boolean) {
        val targetElevation = if (isOpen) 24f else 12f
        val targetTranslationZ = if (isOpen) 12f else 4f
        
        ValueAnimator.ofFloat(mainFab.elevation, targetElevation).apply {
            duration = context.shortAnimTime.toLong()
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                mainFab.elevation = it.animatedValue as Float
            }
            start()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ValueAnimator.ofFloat(mainFab.translationZ, targetTranslationZ).apply {
                duration = context.shortAnimTime.toLong()
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener {
                    mainFab.translationZ = it.animatedValue as Float
                }
                start()
            }
        }
    }

    override fun setOnChangeListener(onChangeListener: OnChangeListener?) {
        this.onChangeListener = onChangeListener
    }

    private fun createMainFabAnimator(isOpen: Boolean): Animator {
        val context = context
        val backgroundColor = if (isOpen) mainFabOpenedBackgroundColor else mainFabClosedBackgroundColor
        val iconColor = if (isOpen) mainFabOpenedIconColor else mainFabClosedIconColor
        
        val bgAnimator = ObjectAnimator.ofArgb(
            mainFab, VIEW_PROPERTY_BACKGROUND_TINT, backgroundColor
        )
        
        val iconAnimator = ObjectAnimator.ofArgb(
            mainFab, IMAGE_VIEW_PROPERTY_IMAGE_TINT, iconColor
        )
        
        val rotationAnimator = ObjectAnimator.ofFloat(
            mainFab.drawable, DRAWABLE_PROPERTY_ROTATION, if (isOpen) 0f else 45f
        )
        
        val scaleAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(mainFab, View.SCALE_X, if (isOpen) 0.9f else 1f, 1f),
                ObjectAnimator.ofFloat(mainFab, View.SCALE_Y, if (isOpen) 0.9f else 1f, 1f)
            )
        }
        
        return AnimatorSet().apply {
            playTogether(bgAnimator, iconAnimator, rotationAnimator, scaleAnimator)
            duration = (context.shortAnimTime * 1.2f).toLong()
            interpolator = SpringInterpolator()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupOverlayWithDepth()
    }

    private fun setupOverlayWithDepth() {
        val overlayLayout = overlayLayout
        if (overlayLayout != null) {
            val surfaceColor = context.getColorByAttr(com.google.android.material.R.attr.colorSurface)
            val overlayColor = ColorUtils.setAlphaComponent(surfaceColor, 200)
            overlayLayout.setBackgroundColor(overlayColor)
            overlayLayout.elevation = 8f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                overlayLayout.translationZ = 2f
            }
        }
    }

    override fun addActionItem(
        actionItem: SpeedDialActionItem,
        position: Int,
        animate: Boolean
    ): FabWithLabelView? {
        val context = context
        val isMaterial3Theme = context.isMaterial3Theme
        
        val fabImageTintColor = if (isMaterial3Theme) {
            context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
        } else {
            context.getColorByAttr(com.google.android.material.R.attr.colorSecondary)
        }
        
        val fabBackgroundColor = Color.TRANSPARENT
        val labelColor = context.getColorByAttr(android.R.attr.textColorPrimary)
        val labelBackgroundColor = Color.TRANSPARENT
        
        val actionItem = SpeedDialActionItem.Builder(
            actionItem.id,
            actionItem.getFabImageDrawable(null)
        )
            .setLabel(actionItem.getLabel(context))
            .setFabImageTintColor(fabImageTintColor)
            .setFabBackgroundColor(fabBackgroundColor)
            .setLabelColor(labelColor)
            .setLabelBackgroundColor(labelBackgroundColor)
            .setLabelClickable(actionItem.isLabelClickable)
            .setTheme(actionItem.theme)
            .create()
        
        return super.addActionItem(actionItem, position, animate)?.apply {
            fab.apply {
                updateLayoutParams<MarginLayoutParams> {
                    val horizontalMargin = context.dpToDimensionPixelSize(20)
                    setMargins(horizontalMargin, 0, horizontalMargin, 0)
                }
                useCompatPadding = false
                elevation = 4f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    translationZ = 2f
                }
                
                val gradientBackground = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(2, ColorUtils.setAlphaComponent(Color.WHITE, 128))
                }
                background = gradientBackground
            }
            
            if (isMaterial3Theme) {
                labelBackground.apply {
                    useCompatPadding = false
                    setContentPadding(0, 0, 0, 0)
                    foregroundCompat = null
                    elevation = 0f
                    (getChildAt(0) as TextView).apply {
                        setTextAppearanceCompat(
                            context.getResourceIdByAttr(
                                com.google.android.material.R.attr.textAppearanceBodyLarge
                            )
                        )
                        setShadowLayer(8f, 0f, 2f, Color.BLACK)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = (super.onSaveInstanceState() as Bundle)
            .getParcelableSafe<Parcelable>("superState")
        return State(superState, isOpen)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        state as State
        super.onRestoreInstanceState(state.superState)
        if (state.isOpen) {
            toggle(false)
        }
    }

    companion object {
        private val VIEW_PROPERTY_BACKGROUND_TINT =
            object : Property<View, Int>(Int::class.java, "backgroundTint") {
                override fun get(view: View): Int? = view.backgroundTintList?.defaultColor ?: Color.TRANSPARENT
                override fun set(view: View, value: Int?) {
                    view.backgroundTintList = ColorStateList.valueOf(value ?: Color.TRANSPARENT)
                }
            }

        private val IMAGE_VIEW_PROPERTY_IMAGE_TINT =
            object : Property<ImageView, Int>(Int::class.java, "imageTint") {
                override fun get(view: ImageView): Int? = view.imageTintList?.defaultColor ?: Color.TRANSPARENT
                override fun set(view: ImageView, value: Int?) {
                    view.imageTintList = ColorStateList.valueOf(value ?: Color.TRANSPARENT)
                }
            }

        private val DRAWABLE_PROPERTY_ROTATION =
            object : Property<Drawable, Float>(Float::class.java, "rotation") {
                override fun get(drawable: Drawable): Float? = 0f
                override fun set(drawable: Drawable, value: Float?) {
                    (drawable as? RotateDrawable)?.setLevel((value?.div(45f)?.times(10000) ?: 0f).toInt())
                }
            }
    }

    class SpringInterpolator : TimeInterpolator {
        override fun getInterpolation(input: Float): Float {
            return if (input < 0.5f) {
                val t = input * 2f
                val t2 = t * t
                val t3 = t2 * t
                (0.5f * (1.5f * t3 - 2.5f * t2 + 2f * t))
            } else {
                val t = (input - 0.5f) * 2f
                val t2 = t * t
                val t3 = t2 * t
                0.5f + 0.5f * (1f - (1f - t) * (1f - t) * (1f - t))
            }
        }
    }

    @Parcelize
    private class State(val superState: Parcelable?, val isOpen: Boolean) : ParcelableState
}
