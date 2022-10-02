package kenneth.app.starlightlauncher.prefs

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kenneth.app.starlightlauncher.BuildConfig
import kenneth.app.starlightlauncher.R

@Composable
fun InfoSettingsScreen() {
    val context = LocalContext.current

    fun openLauncherSourceCodeLink() {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(context.getString(R.string.starlight_launcher_source_code_url))
            )
        )
    }

    fun openSendEmailActivity() {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(
                    Intent.EXTRA_EMAIL,
                    context.getString(R.string.starlight_launcher_author_email)
                )
            }
        )
    }

    SettingsScreen(
        title = stringResource(R.string.launcher_info_title),
        description = ""
    ) {
        SettingsList {
            SettingsListItem(
                title = stringResource(R.string.launcher_version_title),
                summary = BuildConfig.VERSION_NAME,
                icon = painterResource(R.drawable.ic_info_circle),
            )

            SettingsListItem(
                title = stringResource(R.string.launcher_source_code_title),
                summary = stringResource(R.string.launcher_source_code_summary),
                icon = painterResource(R.drawable.ic_brackets_curly),
                onTap = { openLauncherSourceCodeLink() }
            )

            SettingsListItem(
                title = stringResource(R.string.launcher_provide_feedback_title),
                summary = stringResource(R.string.launcher_provide_feedback_summary),
                icon = painterResource(R.drawable.ic_comment_dots),
                onTap = { openSendEmailActivity() }
            )
        }
    }
}