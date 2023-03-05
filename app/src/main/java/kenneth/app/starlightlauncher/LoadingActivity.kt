package kenneth.app.starlightlauncher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.setup.PREF_SETUP_FINISHED
import kenneth.app.starlightlauncher.setup.SetupActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A NoDisplay activity that determines whether to launch setup activity or main launcher activity
 * depending on whether the user has completed setup.
 */
@AndroidEntryPoint
internal class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val setupFinished = dataStore.data.first()[PREF_SETUP_FINISHED] ?: false

            if (setupFinished) {
                startActivity(
                    Intent(
                        this@LoadingActivity.applicationContext,
                        MainActivity::class.java
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
            } else {
                startActivity(
                    Intent(
                        this@LoadingActivity.applicationContext,
                        SetupActivity::class.java
                    )
                )
            }

            finish()
        }
    }
}