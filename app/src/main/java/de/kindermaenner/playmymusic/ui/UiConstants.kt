package de.kindermaenner.playmymusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kindermaenner.playmymusic.R

object UiConstants {

    val Audiowide = FontFamily(
        Font(R.font.audiowide_regular)
    )

    // Farben
    val NeonGreen = Color(0xFF00FF7F)
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)

    // Abstände
    val DefaultPadding = 24.dp
    val ButtonCornerRadius = 20.dp

    // Schriftgrößen
    val TitleSize = 40.sp
    val ButtonTextSize = 18.sp
}
