package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.hid.DendySkin
import com.example.hid.NesButton
import com.example.ui.theme.DendyLedGreen
import com.example.ui.theme.DendyLedRed
import com.example.ui.theme.DendySkinPalette
import com.example.ui.theme.PressStartFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.palette
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

private data class ControllerGeometry(
    val shellRect: Rect,
    val faceRect: Rect,
    val dpadCenter: Offset,
    val dpadRadius: Float,
    val centerX: Float,
    val centerBandWidth: Float,
    val selectCenter: Offset,
    val startCenter: Offset,
    val sysBtnSize: Size,
    val turboBCenter: Offset,
    val turboACenter: Offset,
    val bCenter: Offset,
    val aCenter: Offset,
    val actionWellSize: Float,
)

private fun computeGeometry(width: Float, height: Float): ControllerGeometry {
    val maxAspect = 2.38f
    val minAspect = 1.52f
    val availAspect = if (height > 0f) width / height else maxAspect

    val shellW: Float
    val shellH: Float
    if (availAspect > maxAspect) {
        shellH = height * 0.94f
        shellW = shellH * maxAspect
    } else if (availAspect < minAspect) {
        shellW = width * 0.97f
        shellH = shellW / minAspect
    } else {
        shellW = width * 0.96f
        shellH = height * 0.94f
    }

    val shellLeft = (width - shellW) / 2f
    val shellTop = (height - shellH) / 2f
    val shellRect = Rect(shellLeft, shellTop, shellLeft + shellW, shellTop + shellH)

    val bezelX = shellW * 0.038f
    val bezelY = shellH * 0.078f
    val faceRect = Rect(
        shellRect.left + bezelX,
        shellRect.top + bezelY,
        shellRect.right - bezelX,
        shellRect.bottom - bezelY,
    )

    val fw = faceRect.width
    val fh = faceRect.height

    val dpadRadius = min(fw * 0.145f, fh * 0.35f)
    val dpadCenter = Offset(faceRect.left + fw * 0.195f, faceRect.top + fh * 0.51f)

    val centerX = faceRect.left + fw * 0.485f
    val centerBandWidth = fw * 0.225f
    val sysBtnY = faceRect.top + fh * 0.72f
    val sysBtnSize = Size(centerBandWidth * 0.34f, fh * 0.105f)
    val selectCenter = Offset(centerX - centerBandWidth * 0.215f, sysBtnY)
    val startCenter = Offset(centerX + centerBandWidth * 0.215f, sysBtnY)

    val actionWellSize = min(fw * 0.128f, fh * 0.33f)
    val colGap = actionWellSize * 1.16f
    val rowGap = actionWellSize * 1.14f
    val clusterCenterX = faceRect.left + fw * 0.805f
    val topRowY = faceRect.top + fh * 0.50f - rowGap * 0.50f
    val botRowY = faceRect.top + fh * 0.50f + rowGap * 0.50f

    return ControllerGeometry(
        shellRect = shellRect,
        faceRect = faceRect,
        dpadCenter = dpadCenter,
        dpadRadius = dpadRadius,
        centerX = centerX,
        centerBandWidth = centerBandWidth,
        selectCenter = selectCenter,
        startCenter = startCenter,
        sysBtnSize = sysBtnSize,
        turboBCenter = Offset(clusterCenterX - colGap * 0.5f, topRowY),
        turboACenter = Offset(clusterCenterX + colGap * 0.5f, topRowY),
        bCenter = Offset(clusterCenterX - colGap * 0.5f, botRowY),
        aCenter = Offset(clusterCenterX + colGap * 0.5f, botRowY),
        actionWellSize = actionWellSize,
    )
}

@Composable
fun DendyControllerCanvas(
    pressedButtons: Set<NesButton>,
    turboPhaseOn: Boolean,
    skin: DendySkin,
    eightWayDpad: Boolean,
    swapAB: Boolean,
    isConnected: Boolean,
    onButtonsChanged: (Set<NesButton>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = skin.palette()
    val textMeasurer = rememberTextMeasurer()

    val latestEightWay by rememberUpdatedState(eightWayDpad)
    val latestOnButtonsChanged by rememberUpdatedState(onButtonsChanged)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("dendy_controller_surface")
            .semantics { contentDescription = "Игровой контроллер Dendy" }
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val geo = computeGeometry(wPx, hPx)
        val latestGeo by rememberUpdatedState(geo)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val g = latestGeo
                            val next = HashSet<NesButton>(6)

                            for (change in event.changes) {
                                if (!change.pressed) continue
                                change.consume()
                                val p = change.position

                                // 1. D-Pad hit zone
                                val dx = p.x - g.dpadCenter.x
                                val dy = p.y - g.dpadCenter.y
                                val dist = hypot(dx, dy)
                                if (dist <= g.dpadRadius * 1.42f) {
                                    if (dist >= g.dpadRadius * 0.16f) {
                                        if (latestEightWay) {
                                            if (abs(dx) > g.dpadRadius * 0.22f && abs(dx) >= abs(dy) * 0.42f) {
                                                next.add(if (dx > 0f) NesButton.RIGHT else NesButton.LEFT)
                                            }
                                            if (abs(dy) > g.dpadRadius * 0.22f && abs(dy) >= abs(dx) * 0.42f) {
                                                next.add(if (dy > 0f) NesButton.DOWN else NesButton.UP)
                                            }
                                        } else {
                                            if (abs(dx) > abs(dy)) {
                                                next.add(if (dx > 0f) NesButton.RIGHT else NesButton.LEFT)
                                            } else {
                                                next.add(if (dy > 0f) NesButton.DOWN else NesButton.UP)
                                            }
                                        }
                                    }
                                    continue
                                }

                                // 2. Action buttons (A, B, TURBO A, TURBO B)
                                val hitR = g.actionWellSize * 0.60f
                                if (hypot(p.x - g.turboBCenter.x, p.y - g.turboBCenter.y) <= hitR) {
                                    next.add(NesButton.TURBO_B)
                                    continue
                                }
                                if (hypot(p.x - g.turboACenter.x, p.y - g.turboACenter.y) <= hitR) {
                                    next.add(NesButton.TURBO_A)
                                    continue
                                }
                                if (hypot(p.x - g.bCenter.x, p.y - g.bCenter.y) <= hitR) {
                                    next.add(NesButton.B)
                                    continue
                                }
                                if (hypot(p.x - g.aCenter.x, p.y - g.aCenter.y) <= hitR) {
                                    next.add(NesButton.A)
                                    continue
                                }

                                // 3. SELECT & START
                                val sysHitW = g.sysBtnSize.width * 0.78f
                                val sysHitH = g.sysBtnSize.height * 1.5f
                                if (abs(p.x - g.selectCenter.x) <= sysHitW &&
                                    abs(p.y - g.selectCenter.y) <= sysHitH
                                ) {
                                    next.add(NesButton.SELECT)
                                    continue
                                }
                                if (abs(p.x - g.startCenter.x) <= sysHitW &&
                                    abs(p.y - g.startCenter.y) <= sysHitH
                                ) {
                                    next.add(NesButton.START)
                                    continue
                                }
                            }

                            latestOnButtonsChanged(next)
                        }
                    }
                }
        ) {
            drawControllerShell(geo, palette, skin)
            drawCenterBridge(geo, palette, skin, pressedButtons, turboPhaseOn, isConnected, textMeasurer)
            drawDpad(geo, palette, pressedButtons)
            drawActionCluster(geo, palette, pressedButtons, turboPhaseOn, swapAB, textMeasurer)
        }
    }
}

private fun DrawScope.drawControllerShell(
    geo: ControllerGeometry,
    palette: DendySkinPalette,
    skin: DendySkin,
) {
    val shellCorner = geo.shellRect.height * 0.085f
    val faceCorner = geo.faceRect.height * 0.055f

    // Drop shadow behind shell
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(geo.shellRect.left, geo.shellRect.top + 8f),
        size = geo.shellRect.size,
        cornerRadius = CornerRadius(shellCorner, shellCorner),
    )

    // Outer molded shell with subtle gradient bevel
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.shellBevelLight, palette.shellOuter, palette.shellBevelDark),
            startY = geo.shellRect.top,
            endY = geo.shellRect.bottom,
        ),
        topLeft = geo.shellRect.topLeft,
        size = geo.shellRect.size,
        cornerRadius = CornerRadius(shellCorner, shellCorner),
    )

    // Shell edge stroke
    drawRoundRect(
        color = palette.shellBevelDark.copy(alpha = 0.7f),
        topLeft = geo.shellRect.topLeft,
        size = geo.shellRect.size,
        cornerRadius = CornerRadius(shellCorner, shellCorner),
        style = Stroke(width = 3f),
    )

    // Inner dark faceplate
    drawRoundRect(
        color = palette.faceplateBg,
        topLeft = geo.faceRect.topLeft,
        size = geo.faceRect.size,
        cornerRadius = CornerRadius(faceCorner, faceCorner),
    )

    // Classic subtle cord bump at top center
    val bumpW = geo.shellRect.width * 0.065f
    val bumpH = geo.shellRect.height * 0.032f
    drawRoundRect(
        color = palette.shellBevelDark,
        topLeft = Offset(geo.centerX - bumpW / 2f, geo.shellRect.top - bumpH * 0.35f),
        size = Size(bumpW, bumpH),
        cornerRadius = CornerRadius(6f, 6f),
    )
}

private fun DrawScope.drawCenterBridge(
    geo: ControllerGeometry,
    palette: DendySkinPalette,
    skin: DendySkin,
    pressed: Set<NesButton>,
    turboPhaseOn: Boolean,
    isConnected: Boolean,
    textMeasurer: TextMeasurer,
) {
    val left = geo.centerX - geo.centerBandWidth / 2f
    val stripeH = geo.faceRect.height * 0.065f
    val gap = geo.faceRect.height * 0.032f
    val stripeR = CornerRadius(stripeH * 0.35f, stripeH * 0.35f)

    // Top 2 horizontal Dendy stripes
    for (i in 0..1) {
        val y = geo.faceRect.top + geo.faceRect.height * 0.045f + i * (stripeH + gap)
        drawRoundRect(
            color = if (i == 0) palette.stripeColor else palette.stripeSecondary,
            topLeft = Offset(left, y),
            size = Size(geo.centerBandWidth, stripeH),
            cornerRadius = stripeR,
        )
    }

    // Center Brand Plate ("DENDY" / "NES" / "FAMICOM")
    val brandText = when (skin) {
        DendySkin.DENDY_CLASSIC -> "DENDY"
        DendySkin.DENDY_JUNIOR -> "JUNIOR"
        DendySkin.NES_RETRO -> "NINTENDO"
        DendySkin.FAMICOM_GOLD -> "FAMICOM"
    }
    val plateY = geo.faceRect.top + geo.faceRect.height * 0.255f
    val plateH = geo.faceRect.height * 0.145f
    drawRoundRect(
        color = palette.badgeBg,
        topLeft = Offset(left, plateY),
        size = Size(geo.centerBandWidth, plateH),
        cornerRadius = CornerRadius(8f, 8f),
    )

    val brandStyle = TextStyle(
        fontFamily = PressStartFontFamily,
        fontSize = (plateH * 0.42f).coerceIn(8f, 18f).toSp(),
        color = palette.badgeText,
        letterSpacing = 1.sp,
    )
    val brandMeasured = textMeasurer.measure(brandText, brandStyle)
    drawText(
        textLayoutResult = brandMeasured,
        topLeft = Offset(
            geo.centerX - brandMeasured.size.width / 2f,
            plateY + (plateH - brandMeasured.size.height) / 2f,
        ),
    )

    // Status + Turbo LEDs between badge and SELECT/START tray
    val ledY = geo.faceRect.top + geo.faceRect.height * 0.46f
    val ledR = (geo.faceRect.height * 0.022f).coerceIn(4f, 9f)
    // Link LED
    val linkColor = if (isConnected) DendyLedGreen else Color(0xFF555258)
    drawCircle(
        color = linkColor.copy(alpha = if (isConnected) 0.35f else 0.15f),
        radius = ledR * 2.1f,
        center = Offset(geo.centerX - geo.centerBandWidth * 0.22f, ledY),
    )
    drawCircle(
        color = linkColor,
        radius = ledR,
        center = Offset(geo.centerX - geo.centerBandWidth * 0.22f, ledY),
    )
    // Turbo LED
    val turboActive = (NesButton.TURBO_A in pressed || NesButton.TURBO_B in pressed) && turboPhaseOn
    val turboColor = if (turboActive) DendyLedRed else Color(0xFF552628)
    drawCircle(
        color = turboColor.copy(alpha = if (turboActive) 0.45f else 0.12f),
        radius = ledR * 2.2f,
        center = Offset(geo.centerX + geo.centerBandWidth * 0.22f, ledY),
    )
    drawCircle(
        color = turboColor,
        radius = ledR,
        center = Offset(geo.centerX + geo.centerBandWidth * 0.22f, ledY),
    )

    // Ivory tray behind SELECT & START
    val trayH = geo.faceRect.height * 0.25f
    val trayTop = geo.selectCenter.y - trayH * 0.56f
    drawRoundRect(
        color = palette.buttonWellBg,
        topLeft = Offset(left, trayTop),
        size = Size(geo.centerBandWidth, trayH),
        cornerRadius = CornerRadius(12f, 12f),
    )

    // SELECT & START label + rubber capsule buttons
    drawSystemPill(
        center = geo.selectCenter,
        size = geo.sysBtnSize,
        label = "SELECT",
        isPressed = NesButton.SELECT in pressed,
        palette = palette,
        textMeasurer = textMeasurer,
    )
    drawSystemPill(
        center = geo.startCenter,
        size = geo.sysBtnSize,
        label = "START",
        isPressed = NesButton.START in pressed,
        palette = palette,
        textMeasurer = textMeasurer,
    )

    // Bottom Dendy stripe below tray
    val botStripeY = trayTop + trayH + gap * 1.1f
    val remainingH = (geo.faceRect.bottom - geo.faceRect.height * 0.04f) - botStripeY
    if (remainingH > 4f) {
        drawRoundRect(
            color = palette.stripeColor,
            topLeft = Offset(left, botStripeY),
            size = Size(geo.centerBandWidth, min(stripeH, remainingH)),
            cornerRadius = stripeR,
        )
    }
}

private fun DrawScope.drawSystemPill(
    center: Offset,
    size: Size,
    label: String,
    isPressed: Boolean,
    palette: DendySkinPalette,
    textMeasurer: TextMeasurer,
) {
    val labelStyle = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (size.height * 0.55f).coerceIn(7f, 12f).toSp(),
        color = palette.labelOnShell,
        letterSpacing = 0.6.sp,
    )
    val measured = textMeasurer.measure(label, labelStyle)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            center.x - measured.size.width / 2f,
            center.y - size.height * 0.5f - measured.size.height - size.height * 0.18f,
        ),
    )

    val offsetY = if (isPressed) 2.5f else 0f
    val r = CornerRadius(size.height / 2f, size.height / 2f)
    // Shadow
    if (!isPressed) {
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f + 3f),
            size = size,
            cornerRadius = r,
        )
    }
    drawRoundRect(
        color = if (isPressed) palette.abButtonBg else palette.selectStartBg,
        topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f + offsetY),
        size = size,
        cornerRadius = r,
    )
}

private fun DrawScope.drawDpad(
    geo: ControllerGeometry,
    palette: DendySkinPalette,
    pressed: Set<NesButton>,
) {
    val c = geo.dpadCenter
    val r = geo.dpadRadius
    val armHalf = r * 0.33f
    val wellSize = r * 2.24f

    // Recessed circular/rounded-square ivory bezel around D-pad
    drawRoundRect(
        color = palette.buttonWellBg.copy(alpha = 0.16f),
        topLeft = Offset(c.x - wellSize / 2f, c.y - wellSize / 2f),
        size = Size(wellSize, wellSize),
        cornerRadius = CornerRadius(r * 0.38f, r * 0.38f),
    )

    // Cross shadow
    val armCorner = CornerRadius(r * 0.12f, r * 0.12f)
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(c.x - armHalf, c.y - r + 4f),
        size = Size(armHalf * 2f, r * 2f),
        cornerRadius = armCorner,
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(c.x - r, c.y - armHalf + 4f),
        size = Size(r * 2f, armHalf * 2f),
        cornerRadius = armCorner,
    )

    // D-Pad outer rim highlight
    val rimPad = r * 0.06f
    drawRoundRect(
        color = palette.buttonWellBg.copy(alpha = 0.85f),
        topLeft = Offset(c.x - armHalf - rimPad, c.y - r - rimPad),
        size = Size((armHalf + rimPad) * 2f, (r + rimPad) * 2f),
        cornerRadius = CornerRadius(r * 0.16f, r * 0.16f),
    )
    drawRoundRect(
        color = palette.buttonWellBg.copy(alpha = 0.85f),
        topLeft = Offset(c.x - r - rimPad, c.y - armHalf - rimPad),
        size = Size((r + rimPad) * 2f, (armHalf + rimPad) * 2f),
        cornerRadius = CornerRadius(r * 0.16f, r * 0.16f),
    )

    // Main black cross body
    drawRoundRect(
        color = palette.dpadBody,
        topLeft = Offset(c.x - armHalf, c.y - r),
        size = Size(armHalf * 2f, r * 2f),
        cornerRadius = armCorner,
    )
    drawRoundRect(
        color = palette.dpadBody,
        topLeft = Offset(c.x - r, c.y - armHalf),
        size = Size(r * 2f, armHalf * 2f),
        cornerRadius = armCorner,
    )

    // Active directional arm glow + embossed arrows
    val arrowDist = r * 0.67f
    val arrowSize = r * 0.19f

    drawDpadDirection(
        center = Offset(c.x, c.y - arrowDist),
        direction = NesButton.UP,
        active = NesButton.UP in pressed,
        arrowSize = arrowSize,
        highlightColor = palette.dpadHighlight,
    )
    drawDpadDirection(
        center = Offset(c.x, c.y + arrowDist),
        direction = NesButton.DOWN,
        active = NesButton.DOWN in pressed,
        arrowSize = arrowSize,
        highlightColor = palette.dpadHighlight,
    )
    drawDpadDirection(
        center = Offset(c.x - arrowDist, c.y),
        direction = NesButton.LEFT,
        active = NesButton.LEFT in pressed,
        arrowSize = arrowSize,
        highlightColor = palette.dpadHighlight,
    )
    drawDpadDirection(
        center = Offset(c.x + arrowDist, c.y),
        direction = NesButton.RIGHT,
        active = NesButton.RIGHT in pressed,
        arrowSize = arrowSize,
        highlightColor = palette.dpadHighlight,
    )

    // Center pivot concave circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.65f), Color.White.copy(alpha = 0.08f)),
            center = c,
            radius = armHalf * 0.72f,
        ),
        radius = armHalf * 0.68f,
        center = c,
    )
}

private fun DrawScope.drawDpadDirection(
    center: Offset,
    direction: NesButton,
    active: Boolean,
    arrowSize: Float,
    highlightColor: Color,
) {
    if (active) {
        drawCircle(
            color = highlightColor.copy(alpha = 0.34f),
            radius = arrowSize * 1.9f,
            center = center,
        )
    }

    val path = Path().apply {
        when (direction) {
            NesButton.UP -> {
                moveTo(center.x, center.y - arrowSize)
                lineTo(center.x - arrowSize * 0.85f, center.y + arrowSize * 0.6f)
                lineTo(center.x + arrowSize * 0.85f, center.y + arrowSize * 0.6f)
                close()
            }
            NesButton.DOWN -> {
                moveTo(center.x, center.y + arrowSize)
                lineTo(center.x - arrowSize * 0.85f, center.y - arrowSize * 0.6f)
                lineTo(center.x + arrowSize * 0.85f, center.y - arrowSize * 0.6f)
                close()
            }
            NesButton.LEFT -> {
                moveTo(center.x - arrowSize, center.y)
                lineTo(center.x + arrowSize * 0.6f, center.y - arrowSize * 0.85f)
                lineTo(center.x + arrowSize * 0.6f, center.y + arrowSize * 0.85f)
                close()
            }
            NesButton.RIGHT -> {
                moveTo(center.x + arrowSize, center.y)
                lineTo(center.x - arrowSize * 0.6f, center.y - arrowSize * 0.85f)
                lineTo(center.x - arrowSize * 0.6f, center.y + arrowSize * 0.85f)
                close()
            }
            else -> {}
        }
    }

    drawPath(
        path = path,
        color = if (active) highlightColor else Color(0xFF47454A),
    )
}

private fun DrawScope.drawActionCluster(
    geo: ControllerGeometry,
    palette: DendySkinPalette,
    pressed: Set<NesButton>,
    turboPhaseOn: Boolean,
    swapAB: Boolean,
    textMeasurer: TextMeasurer,
) {
    val labelB = if (swapAB) "A" else "B"
    val labelA = if (swapAB) "B" else "A"

    drawDendyButtonPad(
        center = geo.turboBCenter,
        wellSize = geo.actionWellSize,
        label = "TURBO $labelB",
        isPressed = NesButton.TURBO_B in pressed,
        isTurbo = true,
        turboLit = NesButton.TURBO_B in pressed && turboPhaseOn,
        palette = palette,
        textMeasurer = textMeasurer,
    )
    drawDendyButtonPad(
        center = geo.turboACenter,
        wellSize = geo.actionWellSize,
        label = "TURBO $labelA",
        isPressed = NesButton.TURBO_A in pressed,
        isTurbo = true,
        turboLit = NesButton.TURBO_A in pressed && turboPhaseOn,
        palette = palette,
        textMeasurer = textMeasurer,
    )
    drawDendyButtonPad(
        center = geo.bCenter,
        wellSize = geo.actionWellSize,
        label = labelB,
        isPressed = NesButton.B in pressed,
        isTurbo = false,
        turboLit = false,
        palette = palette,
        textMeasurer = textMeasurer,
    )
    drawDendyButtonPad(
        center = geo.aCenter,
        wellSize = geo.actionWellSize,
        label = labelA,
        isPressed = NesButton.A in pressed,
        isTurbo = false,
        turboLit = false,
        palette = palette,
        textMeasurer = textMeasurer,
    )
}

private fun DrawScope.drawDendyButtonPad(
    center: Offset,
    wellSize: Float,
    label: String,
    isPressed: Boolean,
    isTurbo: Boolean,
    turboLit: Boolean,
    palette: DendySkinPalette,
    textMeasurer: TextMeasurer,
) {
    val wellCorner = CornerRadius(wellSize * 0.16f, wellSize * 0.16f)
    val wellTopLeft = Offset(center.x - wellSize / 2f, center.y - wellSize / 2f)

    // Classic ivory square button pad
    drawRoundRect(
        color = palette.buttonWellBg,
        topLeft = wellTopLeft,
        size = Size(wellSize, wellSize),
        cornerRadius = wellCorner,
    )
    // Subtle inner well recess
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = wellTopLeft,
        size = Size(wellSize, wellSize),
        cornerRadius = wellCorner,
        style = Stroke(width = 2f),
    )

    val capRadius = wellSize * 0.37f
    val pressOffset = if (isPressed) 3f else 0f
    val capCenter = Offset(center.x, center.y + pressOffset)

    // Glow ring if active
    if (isPressed) {
        drawCircle(
            color = (if (turboLit) DendyLedRed else palette.abButtonBg).copy(alpha = 0.38f),
            radius = capRadius * 1.24f,
            center = capCenter,
        )
    }

    // Button shadow
    if (!isPressed) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.42f),
            radius = capRadius,
            center = Offset(center.x, center.y + 4f),
        )
    }

    // Button cap
    val baseColor = when {
        isPressed -> palette.abButtonPressed
        isTurbo -> palette.turboButtonBg
        else -> palette.abButtonBg
    }
    drawCircle(
        color = baseColor,
        radius = capRadius,
        center = capCenter,
    )

    // Specular highlight / concave ring
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
            startY = capCenter.y - capRadius,
            endY = capCenter.y + capRadius,
        ),
        radius = capRadius * 0.88f,
        center = capCenter,
        style = Stroke(width = 2f),
    )

    // Turbo diagonal stripe cue inside Turbo buttons
    if (isTurbo) {
        val barW = capRadius * 0.9f
        val barH = capRadius * 0.18f
        drawRoundRect(
            color = if (turboLit) Color(0xFFFFD166) else Color.White.copy(alpha = 0.35f),
            topLeft = Offset(capCenter.x - barW / 2f, capCenter.y - barH / 2f),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
        )
    }

    // Label in bottom-right / bottom-center below cap inside the ivory square (authentic Dendy style)
    val fontSizePx = if (isTurbo) (wellSize * 0.115f).coerceIn(6.5f, 10f) else (wellSize * 0.16f).coerceIn(8f, 14f)
    val style = TextStyle(
        fontFamily = PressStartFontFamily,
        fontSize = fontSizePx.toSp(),
        color = palette.labelOnShell,
    )
    val measured = textMeasurer.measure(label, style)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            center.x - measured.size.width / 2f,
            wellTopLeft.y + wellSize - measured.size.height - wellSize * 0.03f,
        ),
    )
}
