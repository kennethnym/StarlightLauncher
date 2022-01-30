package kenneth.app.starlightlauncher.animations

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

private const val INITIAL_SCALE = 0.7f
private const val INITIAL_OPACITY = 0f

private const val END_SCALE = 1f
private const val END_OPACITY = 1f

/**
 * Creates a card animation that animates the appearance of a card (fade and scale animation).
 */
class CardAnimation(
    private val targetView: View,
    private val delay: Long,
) {
    private val combinedAnimation = AnimatorSet().also {
        val scaleXAnim = ObjectAnimator.ofFloat(
            targetView,
            "scaleX",
            INITIAL_SCALE,
        )

        val scaleYAnim = ObjectAnimator.ofFloat(
            targetView,
            "scaleY",
            INITIAL_SCALE,
        )

        val fadeAnim = ObjectAnimator.ofFloat(
            targetView,
            "alpha",
            INITIAL_OPACITY,
        )

        it.play(scaleXAnim)
            .with(scaleYAnim)
            .with(fadeAnim)
            .after(delay)
    }

    private val animator = AnimatorSet()
        .also { it.play(combinedAnimation) }

    /**
     * Shows the target card with animation.
     */
    fun showCard() {
        (combinedAnimation.childAnimations[0] as ObjectAnimator)
            .setFloatValues(END_SCALE)

        (combinedAnimation.childAnimations[1] as ObjectAnimator)
            .setFloatValues(END_SCALE)

        (combinedAnimation.childAnimations[2] as ObjectAnimator)
            .setFloatValues(END_OPACITY)

        animator.start()
    }

    /**
     * Hides the target card with animation.
     */
    fun hideCard() {
        (combinedAnimation.childAnimations[0] as ObjectAnimator)
            .setFloatValues(INITIAL_SCALE)

        (combinedAnimation.childAnimations[1] as ObjectAnimator)
            .setFloatValues(INITIAL_SCALE)

        (combinedAnimation.childAnimations[2] as ObjectAnimator)
            .setFloatValues(INITIAL_OPACITY)

        animator.start()
    }
}