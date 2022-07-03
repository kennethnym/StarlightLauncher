package kenneth.app.starlightlauncher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kenneth.app.starlightlauncher.setup.SetupActivity

/**
 * A NoDisplay activity that determines whether to launch setup activity or main launcher activity
 * depending on whether the user has completed setup.
 */
internal class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val setupFinished = sharedPreferences.getBoolean(
            getString(R.string.pref_key_setup_finished),
            false
        )

        finish()

        if (setupFinished) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        } else {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("starlight", "intent received in loading activity")
    }
}