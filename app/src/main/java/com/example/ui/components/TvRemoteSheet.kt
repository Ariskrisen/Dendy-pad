package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.hid.TvAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvRemoteSheet(
    onTvAction: (TvAction) -> Unit,
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Быстрый пульт ТВ",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Отправляет стандартные HID-команды управления телевизором (Home, Назад, Громкость).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TvActionTile(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Назад",
                    onClick = { onTvAction(TvAction.BACK) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_action_back"),
                )
                TvActionTile(
                    icon = Icons.Default.Home,
                    label = "Домой",
                    onClick = { onTvAction(TvAction.HOME) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_action_home"),
                )
                TvActionTile(
                    icon = Icons.Default.Menu,
                    label = "Меню",
                    onClick = { onTvAction(TvAction.MENU) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_action_menu"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TvActionTile(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    label = "Громче -",
                    onClick = { onTvAction(TvAction.VOL_DOWN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_action_vol_down"),
                )
                TvActionTile(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    label = "Без звука",
                    onClick = { onTvAction(TvAction.MUTE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_action_mute"),
                )
                TvActionTile(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Громче +",
                    onClick = { onTvAction(TvAction.VOL_UP) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_action_vol_up"),
                )
            }

            FilledTonalButton(
                onClick = { onTvAction(TvAction.PLAY_PAUSE) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tv_action_play_pause"),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Пауза / Воспроизведение")
            }
        }
    }
}

@Composable
private fun TvActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(68.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
