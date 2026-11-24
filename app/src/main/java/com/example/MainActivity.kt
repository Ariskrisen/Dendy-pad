package com.example

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DendyPadScreen
import com.example.ui.GamepadViewModel
import com.example.ui.theme.DendyPadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DendyPadTheme {
                val vm: GamepadViewModel = viewModel()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { vm.onPermissionsOrBtChanged() },
                )

                val btActionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { vm.onPermissionsOrBtChanged() },
                )

                DendyPadScreen(
                    viewModel = vm,
                    onRequestPermissions = {
                        val perms = vm.hidController.requiredRuntimePermissions()
                        if (perms.isNotEmpty()) {
                            permissionLauncher.launch(perms)
                        } else {
                            vm.onPermissionsOrBtChanged()
                        }
                    },
                    onRequestEnableBluetooth = {
                        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        btActionLauncher.launch(enableIntent)
                    },
                    onMakeDiscoverable = {
                        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                        }
                        btActionLauncher.launch(discoverableIntent)
                    },
                    onOpenBluetoothSettings = {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                )
            }
        }
    }
}
