package kenneth.app.starlightlauncher.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.*
import kenneth.app.starlightlauncher.api.view.PREF_KEY_BLUR_EFFECT_ENABLED
import kenneth.app.starlightlauncher.databinding.ActivitySetupBinding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private suspend fun runFinalSetup() {
        dataStore.edit {
            it[PREF_KEY_BLUR_EFFECT_ENABLED] =
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            it[PREF_SETUP_FINISHED] = true
        }
    }
}
