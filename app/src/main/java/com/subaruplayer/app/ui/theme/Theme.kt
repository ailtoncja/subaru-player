package com.subaruplayer.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subaruplayer.app.R

/**
 * Cores do "Rally Precision Dark" (as mesmas do Subaru DL): azul de rally como cor principal,
 * dourado como destaque, superfícies em camadas.
 */
object C {
    val Surface = Color(0xFF0F131C)
    val SurfaceLowest = Color(0xFF0A0E17)
    val Card = Color(0xFF181B25)
    val CardHigh = Color(0xFF262A34)
    val CardHighest = Color(0xFF31353F)
    val Container = Color(0xFF1C1F29)
    val Border = Color(0xFF2B3040)
    val Outline = Color(0xFF434655)
    val Text = Color(0xFFDFE2EF)
    val Muted = Color(0xFFC4C5D7)
    val Dim = Color(0xFF8E90A0)

    val Blue = Color(0xFF1D4ED8)
    val BlueLight = Color(0xFF2563EB)
    val BluePale = Color(0xFFB7C4FF)
    val Gold = Color(0xFFFACC15)
    val GoldDeep = Color(0xFFEEC200)
    val OnGold = Color(0xFF3C2F00)
    val CyanPale = Color(0xFF7BD0FF)
    val Error = Color(0xFFFFB4AB)
    val ErrorStrong = Color(0xFFF87171)
    val Success = Color(0xFF4ADE80)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val SpaceGrotesk = FontFamily(
    variable(R.font.spacegrotesk, 400), variable(R.font.spacegrotesk, 500), variable(R.font.spacegrotesk, 600), variable(R.font.spacegrotesk, 700),
)
val Manrope = FontFamily(
    variable(R.font.manrope, 400), variable(R.font.manrope, 500), variable(R.font.manrope, 600), variable(R.font.manrope, 700),
)
val Mono = FontFamily(
    variable(R.font.jetbrainsmono, 400), variable(R.font.jetbrainsmono, 500), variable(R.font.jetbrainsmono, 600), variable(R.font.jetbrainsmono, 700),
)

/** Escala tipográfica: títulos em Space Grotesk, texto e rótulos em Manrope, números em JetBrains Mono. */
object T {
    val headlineLg = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp, color = C.Text)
    val headlineMd = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp, color = C.Text)
    val headlineSm = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = C.Text)
    val title = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, color = C.Text)
    val button = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.1.sp, color = C.Text)
    val bodyLg = TextStyle(fontFamily = Manrope, fontSize = 16.sp, lineHeight = 24.sp, color = C.Text)
    val bodyMd = TextStyle(fontFamily = Manrope, fontSize = 15.sp, lineHeight = 22.sp, color = C.Text)
    val bodySm = TextStyle(fontFamily = Manrope, fontSize = 13.sp, lineHeight = 19.sp, color = C.Muted)
    val labelLg = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, color = C.Text)
    val labelMd = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp, color = C.Muted)
    val labelSm = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp, color = C.Muted)
    /** Números que mudam (tempo, velocidade): largura fixa, sem "tremer". */
    val number = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = C.Text)
}

/** O app é só escuro, como o Subaru DL. */
@Composable
fun SubaruTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = C.BluePale, onPrimary = Color(0xFF002682), primaryContainer = C.Blue, onPrimaryContainer = Color(0xFFCAD3FF),
        secondary = C.Gold, onSecondary = C.OnGold, secondaryContainer = Color(0xFF574500), onSecondaryContainer = Color(0xFFFFE083),
        tertiary = C.CyanPale, onTertiary = Color(0xFF00354A), tertiaryContainer = Color(0xFF006387), onTertiaryContainer = Color(0xFFA3DCFF),
        background = C.Surface, onBackground = C.Text, surface = C.Surface, onSurface = C.Text,
        surfaceVariant = C.CardHighest, onSurfaceVariant = C.Muted, surfaceTint = C.BluePale,
        surfaceContainerLowest = C.SurfaceLowest, surfaceContainerLow = C.Card, surfaceContainer = C.Container,
        surfaceContainerHigh = C.CardHigh, surfaceContainerHighest = C.CardHighest,
        outline = C.Dim, outlineVariant = C.Outline, error = C.Error, onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = C.Text, inverseOnSurface = Color(0xFF2C303A), inversePrimary = Color(0xFF2151DA),
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp), extraLarge = RoundedCornerShape(28.dp),
        ),
        typography = Typography(
            displayLarge = T.headlineLg, displayMedium = T.headlineLg, displaySmall = T.headlineMd,
            headlineLarge = T.headlineLg, headlineMedium = T.headlineMd, headlineSmall = T.headlineSm,
            titleLarge = T.headlineSm, titleMedium = T.title, titleSmall = T.labelLg,
            bodyLarge = T.bodyLg, bodyMedium = T.bodyMd, bodySmall = T.bodySm,
            labelLarge = T.labelLg, labelMedium = T.labelMd, labelSmall = T.labelSm,
        ),
        content = content,
    )
}
