package kenneth.app.starlightlauncher.api.compose.pref

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kenneth.app.starlightlauncher.api.R

@Composable
fun SettingsScreen(
    title: String,
    description: String? = null,
    gutter: Boolean = true,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(8.dp)
            .systemBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "Starlight Launcher icon",
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
            )
            Text(
                "Starlight",
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }

        Column(Modifier.padding(horizontal = 8.dp, vertical = 32.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge
            )
            description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Box(
            modifier = Modifier.padding(
                horizontal = if (gutter) 8.dp else 0.dp
            )
        ) {
            content()
        }
    }
}
