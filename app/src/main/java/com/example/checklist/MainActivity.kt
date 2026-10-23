package com.example.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklist.theme.ChecklistTheme
import com.example.checklist.ui.ChecklistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChecklistViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        viewModel.checkAndAdvanceByTime()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            ChecklistTheme(
                darkThemeConfig = state.settings.darkThemeConfig,
                accentTheme = state.settings.accentTheme,
                uiSize = state.settings.uiSize
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.example.checklist.ui.components.LocalUiSize provides state.settings.uiSize,
                    com.example.checklist.ui.components.LocalLargeButtonMode provides (state.settings.uiSize == com.example.checklist.data.UiSize.LARGE)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
