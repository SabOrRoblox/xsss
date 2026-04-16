package me.zhanghai.android.files.fileaction

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java8.nio.file.Path
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.R
import me.zhanghai.android.files.compat.requireViewByIdCompat
import me.zhanghai.android.files.databinding.ArchivePasswordDialogBinding
import me.zhanghai.android.files.provider.archive.archiveAddPassword
import me.zhanghai.android.files.provider.archive.archiveFile
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.ParcelableParceler
import me.zhanghai.android.files.util.ParcelableState
import me.zhanghai.android.files.util.RemoteCallback
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.getArgs
import me.zhanghai.android.files.util.getState
import me.zhanghai.android.files.util.hideTextInputLayoutErrorOnTextChange
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.putState
import me.zhanghai.android.files.util.readParcelable
import me.zhanghai.android.files.util.setOnEditorConfirmActionListener

class ArchivePasswordDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()
    private lateinit var binding: ArchivePasswordDialogBinding
    private var isListenerNotified = false
    private var backgroundAnimator: ValueAnimator? = null
    private var blurAnimator: ValueAnimator? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val hierarchyState = SparseArray<Parcelable>()
            .apply { binding.root.saveHierarchyState(this) }
        outState.putState(State(hierarchyState))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(getTitle(context))
            .setMessage(getMessage(args.path.archiveFile.fileName, context))
            .apply {
                binding = ArchivePasswordDialogBinding.inflate(context.layoutInflater)
                binding.passwordEdit.hideTextInputLayoutErrorOnTextChange(binding.passwordLayout)
                binding.passwordEdit.setOnEditorConfirmActionListener { onOk() }
                if (savedInstanceState != null) {
                    val state = savedInstanceState.getState<State>()
                    binding.root.restoreHierarchyState(state.hierarchyState)
                }
                setupIOSGlassStyle()
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onOk() }
                    startEntryAnimation()
                }
                window?.apply {
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setBackgroundDrawableResource(android.R.color.transparent)
                    } else {
                        setBackgroundDrawableResource(android.R.color.transparent)
                    }
                }
            }
        
        return dialog
    }

    private fun setupIOSGlassStyle() {
        binding.root.apply {
            background = createGlassBackground()
            elevation = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                translationZ = 0f
            }
            setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }
        
        binding.passwordLayout.boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
        binding.passwordLayout.boxStrokeColor = ColorUtils.setAlphaComponent(Color.WHITE, 100)
        binding.passwordLayout.boxStrokeWidth = 2
        binding.passwordLayout.boxStrokeWidthFocused = 2
        
        binding.passwordEdit.apply {
            setTextColor(Color.WHITE)
            setHintTextColor(ColorUtils.setAlphaComponent(Color.WHITE, 120))
            background = null
        }
        
        (dialog as? AlertDialog)?.let {
            it.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun createGlassBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(ColorUtils.setAlphaComponent(Color.WHITE, 25))
            setStroke(2, ColorUtils.setAlphaComponent(Color.WHITE, 80))
        }
    }

    private fun startEntryAnimation() {
        val dialogView = dialog?.window?.decorView?.findViewById<View>(android.R.id.content)
        dialogView?.apply {
            alpha = 0f
            scaleX = 0.95f
            scaleY = 0.95f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        
        startDynamicGlassEffect()
    }

    private fun startDynamicGlassEffect() {
        backgroundAnimator?.cancel()
        backgroundAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                val fraction = animatedValue as Float
                val alpha = 25 + (15 * fraction).toInt()
                val strokeAlpha = 80 + (20 * fraction).toInt()
                val newBackground = createGlassBackground().apply {
                    setColor(ColorUtils.setAlphaComponent(Color.WHITE, alpha))
                    setStroke(2, ColorUtils.setAlphaComponent(Color.WHITE, strokeAlpha))
                }
                binding.root.background = newBackground
            }
            start()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = requireDialog() as AlertDialog
        if (binding.root.parent == null) {
            dialog.window?.setContentView(binding.root)
            binding.passwordEdit.requestFocus()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundAnimator?.cancel()
        blurAnimator?.cancel()
    }

    private fun onOk() {
        val password = binding.passwordEdit.text!!.toString()
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.file_action_archive_password_error_empty)
            startShakeAnimation()
            return
        }
        args.path.archiveAddPassword(password)
        notifyListenerOnce(true)
        startExitAnimation(true)
    }

    private fun startShakeAnimation() {
        binding.passwordEdit.animate()
            .translationX(20f)
            .setDuration(50)
            .withEndAction {
                binding.passwordEdit.animate()
                    .translationX(-20f)
                    .setDuration(50)
                    .withEndAction {
                        binding.passwordEdit.animate()
                            .translationX(10f)
                            .setDuration(50)
                            .withEndAction {
                                binding.passwordEdit.animate()
                                    .translationX(0f)
                                    .setDuration(50)
                                    .start()
                            }.start()
                    }.start()
            }.start()
    }

    private fun startExitAnimation(success: Boolean) {
        val dialogView = dialog?.window?.decorView?.findViewById<View>(android.R.id.content)
        dialogView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.95f)
            ?.scaleY(0.95f)
            ?.setDuration(200)
            ?.setInterpolator(DecelerateInterpolator())
            ?.withEndAction {
                if (success) {
                    finish()
                } else {
                    dialogView.alpha = 1f
                    dialogView.scaleX = 1f
                    dialogView.scaleY = 1f
                }
            }
            ?.start()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        startExitAnimation(false)
        notifyListenerOnce(false)
        finish()
    }

    fun onFinish() {
        notifyListenerOnce(false)
    }

    private fun notifyListenerOnce(successful: Boolean) {
        if (isListenerNotified) return
        args.listener(successful)
        isListenerNotified = true
    }

    companion object {
        fun getTitle(context: Context): String =
            context.getString(R.string.file_action_archive_password_title)

        fun getMessage(archiveFile: Path, context: Context): String =
            context.getString(R.string.file_action_archive_password_message_format, archiveFile.fileName)
    }

    @Parcelize
    class Args(
        val path: @WriteWith<ParcelableParceler> Path,
        val listener: @WriteWith<ListenerParceler>() (Boolean) -> Unit
    ) : ParcelableArgs {
        object ListenerParceler : Parceler<(Boolean) -> Unit> {
            override fun create(parcel: Parcel): (Boolean) -> Unit =
                parcel.readParcelable<RemoteCallback>()!!.let {
                    { successful ->
                        it.sendResult(Bundle().putArgs(ListenerArgs(successful)))
                    }
                }

            override fun ((Boolean) -> Unit).write(parcel: Parcel, flags: Int) {
                parcel.writeParcelable(
                    RemoteCallback {
                        val args = it.getArgs<ListenerArgs>()
                        this(args.successful)
                    }, flags
                )
            }

            @Parcelize
            private class ListenerArgs(
                val successful: Boolean
            ) : ParcelableArgs
        }
    }

    @Parcelize
    private class State(
        val hierarchyState: SparseArray<Parcelable>
    ) : ParcelableState
}
