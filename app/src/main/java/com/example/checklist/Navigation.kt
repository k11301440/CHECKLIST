package com.example.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.checklist.theme.SpaceBlack
import com.example.checklist.ui.ChecklistViewModel
import com.example.checklist.ui.calendar.CalendarScreen
import com.example.checklist.ui.guide.UserGuideScreen
import com.example.checklist.ui.home.HomeScreen
import com.example.checklist.ui.phase.PhaseScreen
import com.example.checklist.ui.policy.PrivacyPolicyScreen
import com.example.checklist.ui.settings.SettingsScreen
import com.example.checklist.ui.template.TemplateManageScreen

@Composable
fun Modifier.adaptiveSystemBarsPadding(): Modifier {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomDp = with(density) {
        if (imeBottom > 0) {
            imeBottom.toDp()
        } else {
            navBottom.toDp()
        }
    }
    return this
        .statusBarsPadding()
        .padding(bottom = bottomDp)
}

@Composable
fun MainNavigation(
    viewModel: ChecklistViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(PhaseNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack),
        entryProvider = entryProvider {
            entry<PhaseNavKey> {
                PhaseScreen(
                    state = state,
                    viewModel = viewModel,
                    onNavigateHome = { backStack.add(HomeNavKey) },
                    onNavigateSettings = { backStack.add(SettingsNavKey) },
                    onNavigateCalendar = { backStack.add(CalendarNavKey) },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }

            entry<HomeNavKey> {
                HomeScreen(
                    state = state,
                    viewModel = viewModel,
                    onSelectPhase = { _ ->
                        backStack.removeLastOrNull()
                    },
                    onNavigateSettings = { backStack.add(SettingsNavKey) },
                    onNavigateCalendar = { backStack.add(CalendarNavKey) },
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }

            entry<CalendarNavKey> {
                CalendarScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }

            entry<SettingsNavKey> {
                SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenTemplateEditor = { templateId ->
                        backStack.add(TemplateEditorNavKey(templateId))
                    },
                    onNavigatePrivacyPolicy = { backStack.add(PrivacyPolicyNavKey) },
                    onNavigateUserGuide = { backStack.add(UserGuideNavKey) },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }

            entry<UserGuideNavKey> {
                UserGuideScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }

            entry<PrivacyPolicyNavKey> {
                PrivacyPolicyScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }

            entry<TemplateEditorNavKey> { key ->
                TemplateManageScreen(
                    templateId = key.templateId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.adaptiveSystemBarsPadding()
                )
            }
        }
    )
}
