package me.zhanghai.android.files.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar

abstract class ToolbarActionMode(
    private val bar: ViewGroup,
    private val toolbar: Toolbar
) {
    @MenuRes
    private var menuRes = 0

    private var callback: Callback? = null
    private var currentAnimator: Animator? = null

    init {
        toolbar.setNavigationOnClickListener { callback?.onToolbarNavigationIconClicked(this) }
        toolbar.setOnMenuItemClickListener {
            callback?.onToolbarActionModeMenuItemClicked(this, it) ?: false
        }
    }

    val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    var navigationIcon: Drawable?
        get() = toolbar.navigationIcon
        set(value) {
            toolbar.navigationIcon = value
        }

    var navigationContentDescription: CharSequence?
        get() = toolbar.navigationContentDescription
        set(value) {
            toolbar.navigationContentDescription = value
        }

    fun setNavigationIcon(@DrawableRes iconRes: Int, @StringRes contentDescriptionRes: Int) {
        toolbar.setNavigationIcon(iconRes)
        toolbar.setNavigationContentDescription(contentDescriptionRes)
    }

    var title: CharSequence?
        get() = toolbar.title
        set(value) {
            toolbar.title = value
        }

    fun setTitle(@StringRes titleRes: Int) {
        toolbar.setTitle(titleRes)
    }

    var subtitle: CharSequence?
        get() = toolbar.subtitle
        set(value) {
            toolbar.subtitle = value
        }

    fun setSubtitle(@StringRes subtitleRes: Int) {
        toolbar.setSubtitle(subtitleRes)
    }

    val menu: Menu
        get() = toolbar.menu

    fun setMenuResource(@MenuRes menuRes: Int) {
        if (this.menuRes == menuRes) {
            return
        }
        this.menuRes = menuRes
        toolbar.menu.clear()
        if (menuRes != 0) {
            toolbar.inflateMenu(menuRes)
        }
    }

    val isActive: Boolean
        get() = callback != null

    fun start(callback: Callback, animate: Boolean = true) {
        this.callback = callback
        onBackPressedCallback.isEnabled = true
        show(animate)
        callback.onToolbarActionModeStarted(this)
    }

    private fun show(animate: Boolean) {
        currentAnimator?.cancel()
        if (animate) {
            bar.alpha = 0f
            bar.visibility = ViewGroup.VISIBLE
            bar.translationY = -bar.height.toFloat()
            val animator = ValueAnimator.ofFloat(-bar.height.toFloat(), 0f).apply {
                duration = 280
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    bar.translationY = it.animatedValue as Float
                    bar.alpha = 1f - (it.animatedFraction * 0.3f)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        currentAnimator = null
                    }
                })
                start()
            }
            currentAnimator = animator
        } else {
            bar.translationY = 0f
            bar.alpha = 1f
            bar.visibility = ViewGroup.VISIBLE
        }
    }

    fun finish(animate: Boolean = true) {
        val callback = callback ?: return
        this.callback = null
        onBackPressedCallback.isEnabled = false
        toolbar.menu.close()
        hide(animate)
        callback.onToolbarActionModeFinished(this)
    }

    private fun hide(animate: Boolean) {
        currentAnimator?.cancel()
        if (animate) {
            val animator = ValueAnimator.ofFloat(0f, -bar.height.toFloat()).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    bar.translationY = it.animatedValue as Float
                    bar.alpha = 1f - (it.animatedFraction * 0.5f)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        bar.visibility = ViewGroup.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
            currentAnimator = animator
        } else {
            bar.visibility = ViewGroup.GONE
            bar.translationY = -bar.height.toFloat()
            bar.alpha = 0f
        }
    }

    interface Callback {
        fun onToolbarActionModeStarted(toolbarActionMode: ToolbarActionMode) {}

        fun onToolbarNavigationIconClicked(toolbarActionMode: ToolbarActionMode) {
            toolbarActionMode.finish()
        }

        fun onToolbarActionModeMenuItemClicked(
            toolbarActionMode: ToolbarActionMode,
            item: MenuItem
        ): Boolean

        fun onToolbarActionModeFinished(toolbarActionMode: ToolbarActionMode)
    }
}
