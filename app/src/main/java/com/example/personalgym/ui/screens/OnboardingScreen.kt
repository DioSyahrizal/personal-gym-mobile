package com.example.personalgym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.Level
import com.example.personalgym.data.PlanGoal
import com.example.personalgym.data.Plans
import com.example.personalgym.data.Profile
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.Pill
import com.example.personalgym.ui.components.ProgressBar

private enum class Step { WELCOME, GOAL, LEVEL, DAYS, EQUIPMENT, WEIGHT, SUMMARY }

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val start = vm.profile
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableStateOf(start.goal) }
    var level by rememberSaveable { mutableStateOf(start.level) }
    var days by rememberSaveable { mutableIntStateOf(start.daysPerWeek) }
    var dumbbells by rememberSaveable { mutableStateOf(vm.dumbbellOnly) }
    val unit = vm.weightUnit
    var weightText by rememberSaveable { mutableStateOf(vm.latestWeight?.let { unit.format(it) } ?: "") }

    val steps = Step.entries
    val current = steps[step]
    // Typed in the chosen unit, kept in kg.
    val weight = weightText.replace(',', '.').toDoubleOrNull()?.let(unit::toKg)?.takeIf { it in 25.0..350.0 }
    fun next() { if (step < steps.lastIndex) step++ }
    fun back() { if (step > 0) step-- else vm.cancelOnboarding() }

    BackHandler(enabled = step > 0 || vm.canCancelOnboarding) { back() }

    // Surface supplies the light content colour; a bare Column would leave text black.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current != Step.WELCOME) {
                    ProgressBar(step.toFloat() / steps.lastIndex, Modifier.weight(1f), height = 6.dp)
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (vm.canCancelOnboarding) {
                    TextButton(onClick = vm::cancelOnboarding) { Text("Cancel") }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * dir / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 4 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "step",
            ) { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
                    when (steps[s]) {
                        Step.WELCOME -> Welcome()
                        Step.GOAL -> Question("What's your main goal?", "You can change this any time on the Plan tab.") {
                            Choice("🔥", "Lose fat", "Burn calories with circuits, HIIT and higher reps", goal == PlanGoal.FAT_LOSS) { goal = PlanGoal.FAT_LOSS }
                            Choice("🏋️", "Build muscle", "Get stronger and bigger with progressive lifting", goal == PlanGoal.BUILD_MUSCLE) { goal = PlanGoal.BUILD_MUSCLE }
                        }
                        Step.LEVEL -> Question("How experienced are you?", "Beginners get a little less volume and more rest.") {
                            Choice("🌱", Level.BEGINNER.label, Level.BEGINNER.blurb, level == Level.BEGINNER) { level = Level.BEGINNER }
                            Choice("💪", Level.INTERMEDIATE.label, Level.INTERMEDIATE.blurb, level == Level.INTERMEDIATE) { level = Level.INTERMEDIATE }
                        }
                        Step.DAYS -> Question("How many days a week can you train?", "Pick what you can stick to — consistency beats intensity.") {
                            Plans.dayOptions.forEach { d ->
                                val plan = Plans.base(goal, d)
                                Choice(
                                    emoji = "$d", title = "$d days · ${plan.name}",
                                    subtitle = plan.days.filter { !it.isRest && it.exercises.size > 1 }.joinToString(" · ") { it.title },
                                    selected = days == d,
                                    badge = if (recommendedDays(level) == d) "Recommended" else null,
                                ) { days = d }
                            }
                        }
                        Step.EQUIPMENT -> Question("What equipment do you have?", "Every machine move has a dumbbell alternative.") {
                            Choice("🏢", "Full gym", "Machines, cables, barbells and dumbbells", !dumbbells) { dumbbells = false }
                            Choice("🏠", "Dumbbells only", "Home or hotel gym — machine moves are swapped automatically", dumbbells) { dumbbells = true }
                        }
                        Step.WEIGHT -> Question("What's your body weight?", "Optional. Used for calorie estimates and your weight trend.") {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) },
                                label = { Text("Weight (${unit.label})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (weightText.isNotBlank() && weight == null) {
                                Text("Enter a weight between ${unit.show(25.0)} and ${unit.show(350.0)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Step.SUMMARY -> Summary(Profile(goal, level, days), dumbbells)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = ::back) { Text("Back") }
                Spacer(Modifier.weight(1f))
                when (current) {
                    Step.WELCOME -> Button(onClick = ::next, Modifier.height(52.dp)) { Text("Get started") }
                    Step.WEIGHT -> {
                        if (weightText.isBlank()) TextButton(onClick = ::next) { Text("Skip") }
                        else Button(onClick = ::next, enabled = weight != null, modifier = Modifier.height(52.dp)) { Text("Next") }
                    }
                    Step.SUMMARY -> Button(
                        onClick = {
                            // Don't add a duplicate weight entry when redoing setup with an unchanged weight.
                            val newWeight = weight?.takeIf { unit.format(it) != vm.latestWeight?.let(unit::format) }
                            vm.completeOnboarding(Profile(goal, level, days), dumbbells, newWeight)
                        },
                        modifier = Modifier.height(52.dp),
                    ) { Text("Start training") }
                    else -> Button(onClick = ::next, Modifier.height(52.dp)) { Text("Next") }
                }
            }
        }
    }
}

private fun recommendedDays(level: Level) = if (level == Level.BEGINNER) 3 else 4

@Composable
private fun Welcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("goblet_squat", "db_curl", "jumping_jack").forEach {
                ExerciseThumb(ExerciseLibrary.get(it), 96.dp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Welcome to Personal Gym", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Answer a few quick questions and we'll build a weekly plan that fits your goal, " +
                "experience and schedule.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            "📋" to "A weekly plan for fat loss or muscle",
            "🎬" to "Animated how-to for every exercise",
            "📈" to "Tracks your progress and suggests when to go heavier",
        ).forEach { (icon, text) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Question(title: String, subtitle: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable
private fun Choice(
    emoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    badge?.let {
                        Spacer(Modifier.width(8.dp))
                        Pill(it, color = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Summary(profile: Profile, dumbbells: Boolean) {
    val plan = Plans.forProfile(profile)
    Text("Your plan is ready 🎉", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(6.dp))
    Text(
        "${profile.goal.label} · ${profile.level.label} · ${profile.daysPerWeek} days a week" +
            if (dumbbells) " · dumbbells only" else "",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(plan.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                plan.days.forEach { d ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(DAY_SHORT[d.dayOfWeek - 1], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(
                                if (d.isRest) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (d.isRest) "–" else d.title.take(1),
                                fontWeight = FontWeight.Bold,
                                color = if (d.isRest) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    plan.days.filter { !it.isRest }.forEach { d ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(DAY_SHORT[d.dayOfWeek - 1], Modifier.width(48.dp), style = MaterialTheme.typography.titleSmall)
            Column(Modifier.weight(1f)) {
                Text(d.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${d.focus} · ${d.exercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Rest ${plan.restSeconds}s between sets." +
            if (profile.level == Level.BEGINNER) " As a beginner you'll do a little less volume to start." else "",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
