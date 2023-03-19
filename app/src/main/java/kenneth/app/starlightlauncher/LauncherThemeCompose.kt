package kenneth.app.starlightlauncher

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

val typography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontFamily = Manrope,
        fontSize = 36.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontFamily = Manrope,
        fontSize = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontSize = 16.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
)

@Composable
fun LauncherTheme(
    content: @Composable () -> Unit,
) {
    val currentContentColor =
        if (isSystemInDarkTheme()) Color.White else Color.Black

    MaterialTheme(
        typography = typography,
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF009DFF)
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides currentContentColor) {
            content()
        }
    }
}
