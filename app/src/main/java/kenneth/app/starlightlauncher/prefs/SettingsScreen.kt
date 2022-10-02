package kenneth.app.starlightlauncher.prefs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kenneth.app.starlightlauncher.Manrope
import kenneth.app.starlightlauncher.R

@Composable
fun SettingsScreen(
    title: String,
    description: String? = null,
    gutter: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(8.dp)
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
                style = MaterialTheme.typography.h1
            )
            description?.let {
                Text(it)
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
