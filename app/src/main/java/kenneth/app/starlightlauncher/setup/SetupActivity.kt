package kenneth.app.starlightlauncher.setup

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.MainActivity
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.ActivitySetupBinding

@AndroidEntryPoint
internal class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding

    private lateinit var sharedPreferences: SharedPreferences

    private val onPageChangedListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.continueBtn.text =
                if (position == SETUP_STEP_COUNT - 1) {
                    getString(R.string.setup_finish_btn_label)
                } else {
                    getString(R.string.continue_btn_label)
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enable edge-to-edge app experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        binding = ActivitySetupBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        with(binding) {
            setupPager.adapter = SetupPagerAdapter(this@SetupActivity)

            setupPager.registerOnPageChangeCallback(onPageChangedListener)
            continueBtn.setOnClickListener { goToNextStep() }
        }
    }

    override fun onDestroy() {
        binding.setupPager.unregisterOnPageChangeCallback(onPageChangedListener)
        super.onDestroy()
    }

    private fun goToNextStep() {
        if (binding.setupPager.currentItem == SETUP_STEP_COUNT - 1) {
            sharedPreferences.edit(commit = true) {
                putBoolean(getString(R.string.pref_key_setup_finished), true)
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            binding.apply {
                continueBtn.text = getString(R.string.continue_btn_label)
                setupPager.currentItem += 1
            }
        }
    }
}
