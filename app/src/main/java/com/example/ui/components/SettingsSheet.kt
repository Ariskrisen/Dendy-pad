package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hid.CompatibilityProfile
import com.example.hid.DendySkin
import com.example.hid.GamepadSettings
import com.example.hid.KeyboardKeyPreset
import com.example.hid.TurboRate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: GamepadSettings,
    onSelectProfile: (CompatibilityProfile) -> Unit,
    onSelectKeyboardPreset: (KeyboardKeyPreset) -> Unit,
    onSelectSkin: (DendySkin) -> Unit,
    onSelectTurboRate: (TurboRate) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleSwapAB: (Boolean) -> Unit,
    onToggleEightWay: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Настройки совместимости и Dendy",
                style = MaterialTheme.typography.headlineSmall,
            )

            // 1. Compatibility Profiles
            Text(
                text = "Профиль Bluetooth HID (для ТВ)",
                style = MaterialTheme.typography.titleMedium,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompatibilityProfile.entries.forEach { profile ->
                    val selected = settings.compatibilityProfile == profile
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_${profile.name}")
                            .clickable { onSelectProfile(profile) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (selected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectProfile(profile) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Keyboard Mapping (shown prominently when Keyboard or Dual Hybrid is selected)
            if (settings.compatibilityProfile == CompatibilityProfile.DUAL_HYBRID ||
                settings.compatibilityProfile == CompatibilityProfile.SMART_TV_KEYBOARD
            ) {
                HorizontalDivider()
                Text(
                    text = "Раскладка клавиш (для режима Клавиатуры)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyboardKeyPreset.entries.forEach { preset ->
                        val selected = settings.keyboardPreset == preset
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kb_preset_${preset.name}")
                                .clickable { onSelectKeyboardPreset(preset) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectKeyboardPreset(preset) },
                                )
                                Column {
                                    Text(preset.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        preset.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // 3. Dendy Skin
            Text(
                text = "Дизайн корпуса",
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DendySkin.entries.forEach { skin ->
                    FilterChip(
                        selected = settings.skin == skin,
                        onClick = { onSelectSkin(skin) },
                        label = { Text(skin.title) },
                        modifier = Modifier.testTag("skin_${skin.name}"),
                    )
                }
            }

            // 4. Turbo Speed
            Text(
                text = "Скорость TURBO A / B",
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TurboRate.entries.forEach { rate ->
                    FilterChip(
                        selected = settings.turboRate == rate,
                        onClick = { onSelectTurboRate(rate) },
                        label = { Text(rate.label) },
                        modifier = Modifier.testTag("turbo_${rate.name}"),
                    )
                }
            }

            HorizontalDivider()

            // 5. Ergonomics & Toggles
            SettingSwitchRow(
                title = "Поменять кнопки A ↔ B",
                subtitle = "Меняет местами A и B для Android TV меню или японских игр",
                checked = settings.swapAB,
                onCheckedChange = onToggleSwapAB,
                testTag = "switch_swap_ab",
            )
            SettingSwitchRow(
                title = "8-позиционная крестовина",
                subtitle = "Включите диагонали для Contra / Battletoads или выключите для Tetris",
                checked = settings.eightWayDpad,
                onCheckedChange = onToggleEightWay,
                testTag = "switch_eight_way",
            )
            SettingSwitchRow(
                title = "Тактильный отклик (вибрация)",
                subtitle = "Короткий вибро-клик при нажатии кнопок",
                checked = settings.hapticEnabled,
                onCheckedChange = onToggleHaptic,
                testTag = "switch_haptic",
            )
            SettingSwitchRow(
                title = "8-битный клик кнопок",
                subtitle = "Ретро-щелчок при нажатии",
                checked = settings.soundEnabled,
                onCheckedChange = onToggleSound,
                testTag = "switch_sound",
            )
            SettingSwitchRow(
                title = "Не выключать экран",
                subtitle = "Экран остаётся активным во время игры на ТВ",
                checked = settings.keepScreenOn,
                onCheckedChange = onToggleKeepScreenOn,
                testTag = "switch_keep_screen_on",
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
