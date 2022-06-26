package kenneth.app.starlightlauncher.setup

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kenneth.app.starlightlauncher.MainActivity
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.ActivitySetupBinding
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
internal class SetupActivity : AppCompatActivity() {
    @Inject
    @Named(MAIN_DISPATCHER)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @Named(IO_DISPATCHER)
    lateinit var ioDispatcher: CoroutineDispatcher

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
        binding = ActivitySetupBinding.inflate(layoutInflater)
            .apply {
                setupPager.isUserInputEnabled = false
            }.also {
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
            CoroutineScope(mainDispatcher).launch {
                withContext(ioDispatcher) {
                    runFinalSetup()
                }
                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                finish()
            }
            binding.isLoading = true
        } else {
            binding.apply {
                continueBtn.text = getString(R.string.continue_btn_label)
                setupPager.currentItem += 1
            }
        }
    }

    private suspend fun runFinalSetup() = withContext(mainDispatcher) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@SetupActivity)

        sharedPreferences.edit(commit = true) {
            putBoolean(
                getString(R.string.pref_key_appearance_blur_effect_enabled),
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            )
        }

        sharedPreferences.edit(commit = true) {
            putBoolean(getString(R.string.pref_key_setup_finished), true)
        }
    }
}
