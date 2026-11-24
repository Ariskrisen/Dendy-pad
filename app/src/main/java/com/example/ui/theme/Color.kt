package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.hid.DendySkin

// Core Dendy / Material 3 palette tokens
val DendyCrimson = Color(0xFFB81D24)
val DendyCrimsonBright = Color(0xFFD9262E)
val DendyCrimsonDark = Color(0xFF7A0E1B)
val DendyIvory = Color(0xFFEAE2D0)
val DendyIvoryLight = Color(0xFFF6F1E6)
val DendyCharcoal = Color(0xFF232124)
val DendySlate = Color(0xFF363437)
val DendyAmber = Color(0xFFE8A317)
val DendyLedGreen = Color(0xFF39D353)
val DendyLedRed = Color(0xFFFF3B30)
val DendySurfaceDark = Color(0xFF151416)
val DendySurfaceContainer = Color(0xFF1F1D21)
val DendySurfaceHigh = Color(0xFF2B282E)
val DendyOutline = Color(0xFF47434B)

data class DendySkinPalette(
    val shellOuter: Color,
    val shellBevelLight: Color,
    val shellBevelDark: Color,
    val faceplateBg: Color,
    val stripeColor: Color,
    val stripeSecondary: Color,
    val buttonWellBg: Color,
    val dpadBody: Color,
    val dpadHighlight: Color,
    val abButtonBg: Color,
    val abButtonPressed: Color,
    val turboButtonBg: Color,
    val selectStartBg: Color,
    val labelOnFaceplate: Color,
    val labelOnShell: Color,
    val badgeBg: Color,
    val badgeText: Color,
)

fun DendySkin.palette(): DendySkinPalette = when (this) {
    DendySkin.DENDY_CLASSIC -> DendySkinPalette(
        shellOuter = Color(0xFFEAE2D0),
        shellBevelLight = Color(0xFFF7F2E7),
        shellBevelDark = Color(0xFFC7BDA6),
        faceplateBg = Color(0xFF343235),
        stripeColor = Color(0xFFB81D24),
        stripeSecondary = Color(0xFF7A1217),
        buttonWellBg = Color(0xFFEAE2D0),
        dpadBody = Color(0xFF1B1A1C),
        dpadHighlight = Color(0xFFFF4B4B),
        abButtonBg = Color(0xFFC91E26),
        abButtonPressed = Color(0xFF8F1218),
        turboButtonBg = Color(0xFFAD1B22),
        selectStartBg = Color(0xFF252326),
        labelOnFaceplate = Color(0xFFE6DEC8),
        labelOnShell = Color(0xFFB81D24),
        badgeBg = Color(0xFFB81D24),
        badgeText = Color(0xFFF6F1E6),
    )

    DendySkin.DENDY_JUNIOR -> DendySkinPalette(
        shellOuter = Color(0xFFDCD9D2),
        shellBevelLight = Color(0xFFEEECE7),
        shellBevelDark = Color(0xFFB3AFA6),
        faceplateBg = Color(0xFF222124),
        stripeColor = Color(0xFF9E1B24),
        stripeSecondary = Color(0xFF545158),
        buttonWellBg = Color(0xFFDCD9D2),
        dpadBody = Color(0xFF141315),
        dpadHighlight = Color(0xFFFF5252),
        abButtonBg = Color(0xFFD42029),
        abButtonPressed = Color(0xFF911319),
        turboButtonBg = Color(0xFF47444A),
        selectStartBg = Color(0xFF181719),
        labelOnFaceplate = Color(0xFFE0DDD5),
        labelOnShell = Color(0xFF9E1B24),
        badgeBg = Color(0xFF1B1A1D),
        badgeText = Color(0xFFD42029),
    )

    DendySkin.NES_RETRO -> DendySkinPalette(
        shellOuter = Color(0xFFC9C9CC),
        shellBevelLight = Color(0xFFE0E0E3),
        shellBevelDark = Color(0xFF9B9BA0),
        faceplateBg = Color(0xFF48494D),
        stripeColor = Color(0xFF828489),
        stripeSecondary = Color(0xFF333438),
        buttonWellBg = Color(0xFFC9C9CC),
        dpadBody = Color(0xFF191A1C),
        dpadHighlight = Color(0xFFFF453A),
        abButtonBg = Color(0xFFD92323),
        abButtonPressed = Color(0xFF941414),
        turboButtonBg = Color(0xFFB82020),
        selectStartBg = Color(0xFF232427),
        labelOnFaceplate = Color(0xFFE6E6EA),
        labelOnShell = Color(0xFFD92323),
        badgeBg = Color(0xFFD92323),
        badgeText = Color(0xFFFFFFFF),
    )

    DendySkin.FAMICOM_GOLD -> DendySkinPalette(
        shellOuter = Color(0xFFF3EAD3),
        shellBevelLight = Color(0xFFFCF7EB),
        shellBevelDark = Color(0xFFCFC19F),
        faceplateBg = Color(0xFF7D1920),
        stripeColor = Color(0xFFC9A23F),
        stripeSecondary = Color(0xFF5B1015),
        buttonWellBg = Color(0xFFC9A23F),
        dpadBody = Color(0xFF1F1918),
        dpadHighlight = Color(0xFFFFD166),
        abButtonBg = Color(0xFF251E1E),
        abButtonPressed = Color(0xFF0E0B0B),
        turboButtonBg = Color(0xFF3D2E2A),
        selectStartBg = Color(0xFF29211F),
        labelOnFaceplate = Color(0xFFF5E6C4),
        labelOnShell = Color(0xFF7D1920),
        badgeBg = Color(0xFFC9A23F),
        badgeText = Color(0xFF2A140E),
    )
}
