package kenneth.app.starlightlauncher.setup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enable edge-to-edge app experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySetupBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        with(binding) {
            setupPager.adapter = SetupPagerAdapter(this@SetupActivity)

            continueBtn.setOnClickListener { goToNextStep() }
        }
    }

    private fun goToNextStep() {
        if (binding.setupPager.currentItem == SETUP_STEP_COUNT - 1) {
            binding.continueBtn.text = getString(R.string.action_finish)
        } else {
            binding.setupPager.currentItem += 1
        }
    }
}
