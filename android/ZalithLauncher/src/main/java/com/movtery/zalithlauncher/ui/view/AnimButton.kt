package com.movtery.zalithlauncher.ui.view

import android.animation.AnimatorInflater
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.movtery.zalithlauncher.R
import net.kdt.pojavlaunch.Tools

open class AnimButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {
    init {
        isAllCaps = false
        setRipple(attrs)
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.xml.anim_scale)
        translationZ = Tools.dpToPx(4f)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post {
            pivotX = width / 2f
            pivotY = height / 2f
        }
    }

    /**
     * OrbitX UI modification (recorded in LICENSE-THIRD-PARTY.md).
     *
     * Upstream always wrapped [R.drawable.button_background] in the ripple, which
     * meant a background declared in XML was silently discarded - so the primary
     * Play action could not be restyled without editing this class.  Now an
     * *explicitly declared* `android:background` is preserved as the ripple's
     * content, which lets layouts use `@drawable/orbitx_action_button`.
     *
     * The `hasValue` check is what keeps this behaviour-preserving: every button
     * that does not declare a background (i.e. all pre-existing ones) still gets
     * exactly `button_background` as before.  Reading `background` unconditionally
     * would instead pick up the themed default from `defStyleAttr`, changing the
     * look of every existing button.
     */
    private fun setRipple(attrs: AttributeSet?) {
        val fallback = ResourcesCompat.getDrawable(resources, R.drawable.button_background, context.theme)
        val hasExplicitBackground = attrs?.let { set ->
            val typed = context.obtainStyledAttributes(set, intArrayOf(android.R.attr.background))
            try {
                typed.hasValue(0)
            } finally {
                typed.recycle()
            }
        } ?: false

        val content = if (hasExplicitBackground) (background ?: fallback) else fallback

        val rippleDrawable = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background_ripple_effect)),
            content,
            null
        )

        background = rippleDrawable
    }
}