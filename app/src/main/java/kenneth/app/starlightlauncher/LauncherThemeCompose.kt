package kenneth.app.starlightlauncher

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Manrope = FontFamily(
    Font(R.font.manrope_regular),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val Typography = Typography(
    h1 = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontFamily = Manrope,
        fontSize = 36.sp
    ),
    body1 = TextStyle(
        fontFamily = Manrope,
        fontSize = 16.sp
    ),
)

@Composable
fun LauncherTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        typography = Typography,
        content = content,
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF009DFF)
        )
    )
}
