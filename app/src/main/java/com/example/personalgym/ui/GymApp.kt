package com.example.personalgym.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.WorkoutSession
import com.example.personalgym.ui.screens.EditDayScreen
import com.example.personalgym.ui.screens.ExerciseDetailScreen
import com.example.personalgym.ui.screens.GoalsScreen
import com.example.personalgym.ui.screens.HomeScreen
import com.example.personalgym.ui.screens.LibraryScreen
import com.example.personalgym.ui.screens.OnboardingScreen
import com.example.personalgym.ui.screens.PlanDayScreen
import com.example.personalgym.ui.screens.PlanScreen
import com.example.personalgym.ui.screens.SessionScreen
import com.example.personalgym.ui.screens.SettingsScreen
import com.example.personalgym.ui.screens.StatsScreen
import com.example.personalgym.ui.screens.WorkoutDetailScreen
import com.example.personalgym.ui.screens.WorkoutSummaryDialog

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    PLAN("Plan", Icons.Filled.DateRange),
    LIBRARY("Exercises", Icons.AutoMirrored.Filled.List),
    GOALS("Goals", Icons.Filled.Star),
    STATS("Stats", Icons.Filled.ThumbUp),
}

/** Callbacks shared by screens to open detail pages or start workouts. */
class Nav(
    val openExercise: (String) -> Unit,
    val openDay: (PlanDay) -> Unit,
    val startDay: (PlanDay) -> Unit,
    val openSession: () -> Unit,
    val openSettings: () -> Unit,
    val openWorkout: (Long) -> Unit,
    val goTo: (Tab) -> Unit,
)

@Composable
fun GymApp(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var exerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var dayIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSession by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editingDay by rememberSaveable { mutableStateOf(false) }
    var workoutId by rememberSaveable { mutableStateOf<Long?>(null) }
    var summary by remember { mutableStateOf<WorkoutSession?>(null) }

    val nav = Nav(
        openExercise = { exerciseId = it },
        openDay = { day -> dayIndex = vm.activePlan.days.indexOf(day).takeIf { it >= 0 } },
        startDay = { day ->
            vm.startWorkout(day)
            dayIndex = null
            showSession = true
        },
        openSession = { showSession = true },
        openSettings = { showSettings = true },
        openWorkout = { workoutId = it },
        goTo = { tab = it },
    )

    // Layered navigation: exercise detail > session > past workout > settings > plan day > tabs
    val day = dayIndex?.let { vm.activePlan.days.getOrNull(it) }
    val session = vm.active
    when {
        vm.needsOnboarding -> OnboardingScreen(vm)
        exerciseId != null -> {
            BackHandler { exerciseId = null }
            ExerciseDetailScreen(vm, exerciseId!!, onBack = { exerciseId = null }, onOpen = { exerciseId = it })
        }
        showSession && session != null -> {
            BackHandler { showSession = false }
            SessionScreen(
                vm, session, nav,
                onMinimize = { showSession = false },
                onFinished = { summary = it; showSession = false; tab = Tab.HOME },
            )
        }
        workoutId != null -> WorkoutDetailScreen(vm, workoutId!!, onBack = { workoutId = null }, onOpenExercise = { exerciseId = it })
        showSettings -> {
            BackHandler { showSettings = false }
            SettingsScreen(vm, onBack = { showSettings = false })
        }
        day != null && editingDay -> EditDayScreen(vm, day, onDone = { editingDay = false })
        day != null -> {
            BackHandler { dayIndex = null }
            PlanDayScreen(vm, day, nav, onBack = { dayIndex = null }, onEdit = { editingDay = true })
        }
        else -> TabsScaffold(vm, tab, nav) { tab = it }
    }

    summary?.let { WorkoutSummaryDialog(it, vm.weightUnit, onDismiss = { summary = null }) }
}

@Composable
private fun TabsScaffold(vm: AppViewModel, tab: Tab, nav: Nav, onTab: (Tab) -> Unit) {
    if (tab != Tab.HOME) BackHandler { onTab(Tab.HOME) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { onTab(t) },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.HOME -> HomeScreen(vm, nav)
                Tab.PLAN -> PlanScreen(vm, nav)
                Tab.LIBRARY -> LibraryScreen(vm, nav)
                Tab.GOALS -> GoalsScreen(vm)
                Tab.STATS -> StatsScreen(vm, nav)
            }
        }
    }
}
