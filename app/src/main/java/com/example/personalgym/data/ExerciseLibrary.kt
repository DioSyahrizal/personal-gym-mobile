package com.example.personalgym.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.example.personalgym.ui.illustration.HandProp
import com.example.personalgym.ui.illustration.Illustration
import com.example.personalgym.ui.illustration.P
import com.example.personalgym.ui.illustration.Scene
import com.example.personalgym.ui.illustration.front
import com.example.personalgym.ui.illustration.side

private object Poses {
    // Standing, side view facing right
    val stand = side(P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(50, 52), P(50, 72), P(49, 91))
    val standFront = front(P(50, 15), P(50, 25), P(50, 52), P(60, 38), P(61, 51), P(55, 72), P(56, 91))
    val squat = side(P(50, 38), P(46, 47), P(35, 70), P(36, 51), P(44, 44), P(57, 73), P(49, 91))

    // Lying on a flat bench (head to the right), feet on the floor
    fun benchLying(elbow: com.example.personalgym.ui.illustration.Pt, hand: com.example.personalgym.ui.illustration.Pt) =
        side(P(74, 59), P(64, 61), P(40, 61), elbow, hand, P(25, 66), P(23, 90))

    // Plank on hands
    val highPlank = side(P(80, 62), P(72, 66), P(44, 78), P(72, 78), P(72, 90), P(30, 84), P(15, 90))
}

private fun hinge(elbow: com.example.personalgym.ui.illustration.Pt, hand: com.example.personalgym.ui.illustration.Pt) =
    side(P(74, 38), P(66, 42), P(42, 55), elbow, hand, P(47, 73), P(45, 91))

object ExerciseLibrary {

    /** The exercises that ship with the app. */
    val builtIn: List<Exercise> = listOf(
        // ───────────── CHEST ─────────────
        Exercise(
            id = "barbell_bench", name = "Barbell Bench Press", muscle = Muscle.CHEST, equipment = Equipment.BARBELL,
            illustration = Illustration(
                Poses.benchLying(P(57, 68), P(63, 55)), Poses.benchLying(P(64, 49), P(64, 36)),
                HandProp.BARBELL, Scene.BENCH,
            ),
            steps = listOf(
                "Lie on the bench, eyes under the bar, feet flat on the floor.",
                "Grip slightly wider than shoulders and unrack with straight arms.",
                "Lower the bar to mid-chest with elbows ~45° from your body.",
                "Press back up until arms are straight.",
            ),
            tip = "Keep shoulder blades squeezed together and your butt on the bench.",
            alternativeId = "db_bench", met = 5.0,
        ),
        Exercise(
            id = "db_bench", name = "Dumbbell Bench Press", muscle = Muscle.CHEST, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                Poses.benchLying(P(57, 68), P(63, 55)), Poses.benchLying(P(64, 49), P(64, 36)),
                HandProp.DUMBBELL, Scene.BENCH,
            ),
            steps = listOf(
                "Sit with dumbbells on your thighs, then lie back and bring them to chest level.",
                "Press the dumbbells up until arms are straight above the chest.",
                "Lower slowly until elbows are slightly below the bench.",
            ),
            tip = "Let the dumbbells travel in a slight arc so they nearly touch at the top.",
            met = 5.0,
        ),
        Exercise(
            id = "incline_db_press", name = "Incline Dumbbell Press", muscle = Muscle.CHEST, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(62, 34), P(57, 43), P(42, 66), P(49, 54), P(57, 41), P(26, 64), P(24, 90)),
                side(P(62, 34), P(57, 43), P(42, 66), P(60, 30), P(62, 17), P(26, 64), P(24, 90)),
                HandProp.DUMBBELL, Scene.INCLINE_BENCH,
            ),
            steps = listOf(
                "Set the bench to 30–45° and sit back with dumbbells at shoulder level.",
                "Press up and slightly inward until arms are straight.",
                "Lower under control back to the upper chest.",
            ),
            tip = "The incline shifts the work to the upper chest — don't arch too much.",
            met = 5.0,
        ),
        Exercise(
            id = "machine_chest_press", name = "Machine Chest Press", muscle = Muscle.CHEST, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(P(41, 28), P(39, 38), P(40, 64), P(30, 44), P(44, 37), P(58, 64), P(60, 90)),
                side(P(41, 28), P(39, 38), P(40, 64), P(53, 39), P(66, 38), P(58, 64), P(60, 90)),
                HandProp.HANDLE, Scene.SEAT,
            ),
            steps = listOf(
                "Adjust the seat so the handles line up with mid-chest.",
                "Push the handles forward until your arms are almost straight.",
                "Return slowly until you feel a stretch in the chest.",
            ),
            tip = "Keep your back against the pad the whole time.",
            alternativeId = "db_bench", met = 4.5,
        ),
        Exercise(
            id = "pec_deck", name = "Pec Deck (Chest Fly Machine)", muscle = Muscle.CHEST, equipment = Equipment.MACHINE,
            illustration = Illustration(
                front(P(50, 27), P(50, 36), P(50, 62), P(72, 37), P(76, 24), P(57, 70), P(58, 90)),
                front(P(50, 27), P(50, 36), P(50, 62), P(62, 40), P(54, 27), P(57, 70), P(58, 90)),
                HandProp.HANDLE, Scene.SEAT_FRONT,
            ),
            steps = listOf(
                "Sit tall with forearms or hands on the pads at chest height.",
                "Squeeze the pads together in front of your chest.",
                "Open slowly until you feel a stretch across the chest.",
            ),
            tip = "Think of hugging a big tree — the movement comes from the shoulders.",
            alternativeId = "db_fly", met = 4.0,
        ),
        Exercise(
            id = "cable_crossover", name = "Cable Crossover", muscle = Muscle.CHEST, equipment = Equipment.CABLE,
            illustration = Illustration(
                front(P(50, 15), P(50, 25), P(50, 52), P(68, 28), P(80, 24), P(55, 72), P(57, 91)),
                front(P(50, 15), P(50, 25), P(50, 52), P(61, 38), P(53, 48), P(55, 72), P(57, 91)),
                HandProp.HANDLE, Scene.CABLE_DUAL,
            ),
            steps = listOf(
                "Set both pulleys high and grab a handle in each hand.",
                "Step forward, slight lean, elbows softly bent.",
                "Bring the handles down and together in front of your hips.",
                "Return slowly with control.",
            ),
            tip = "Keep the elbow bend fixed — don't turn it into a press.",
            alternativeId = "db_fly", met = 4.0,
        ),
        Exercise(
            id = "db_fly", name = "Dumbbell Fly", muscle = Muscle.CHEST, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                // Top-down view, lying on the bench
                front(P(50, 18), P(50, 27), P(50, 52), P(66, 30), P(81, 33), P(58, 70), P(61, 86)),
                front(P(50, 18), P(50, 27), P(50, 52), P(58, 28), P(53, 29), P(58, 70), P(61, 86)),
                HandProp.DUMBBELL, Scene.BENCH_TOP,
            ),
            steps = listOf(
                "Lie on a flat bench, dumbbells above your chest, palms facing each other.",
                "With a slight elbow bend, open your arms wide until you feel a chest stretch.",
                "Bring the dumbbells back together over the chest.",
            ),
            tip = "Use lighter weights than pressing; the stretch is the point.",
            met = 4.0,
        ),
        Exercise(
            id = "push_up", name = "Push-up", muscle = Muscle.CHEST, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                Poses.highPlank,
                side(P(80, 78), P(72, 81), P(44, 85), P(63, 84), P(72, 90), P(30, 88), P(15, 90)),
                scene = Scene.MAT, periodMs = 1100,
            ),
            steps = listOf(
                "Hands slightly wider than shoulders, body in a straight line.",
                "Lower your chest until it nearly touches the floor.",
                "Push back up to straight arms.",
            ),
            tip = "Too hard? Do them with knees on the floor or hands on a bench.",
            met = 8.0,
        ),

        // ───────────── BACK ─────────────
        Exercise(
            id = "lat_pulldown", name = "Lat Pulldown", muscle = Muscle.BACK, equipment = Equipment.MACHINE,
            illustration = Illustration(
                front(P(50, 27), P(50, 36), P(50, 62), P(68, 18), P(72, 7), P(57, 52), P(58, 90)),
                front(P(50, 27), P(50, 36), P(50, 62), P(72, 44), P(66, 31), P(57, 52), P(58, 90)),
                HandProp.WIDE_BAR, Scene.CABLE_PULLDOWN,
            ),
            steps = listOf(
                "Sit with thighs under the pad and grab the bar wider than shoulders.",
                "Pull the bar down to your upper chest, driving elbows down.",
                "Let the bar rise slowly until arms are straight.",
            ),
            tip = "Lead with the elbows and keep your chest up — don't swing back.",
            alternativeId = "db_pullover", met = 4.5,
        ),
        Exercise(
            id = "db_pullover", name = "Dumbbell Pullover", muscle = Muscle.BACK, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                Poses.benchLying(P(65, 48), P(66, 36)),
                Poses.benchLying(P(77, 54), P(89, 60)),
                HandProp.DUMBBELL, Scene.BENCH,
            ),
            steps = listOf(
                "Lie on a bench holding one dumbbell with both hands above the chest.",
                "With a slight bend in the elbows, lower it back behind your head.",
                "Pull it back over the chest using your lats.",
            ),
            tip = "Keep your core braced so your lower back doesn't arch.",
            met = 4.0,
        ),
        Exercise(
            id = "pull_up", name = "Pull-up", muscle = Muscle.BACK, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                front(P(50, 22), P(50, 31), P(50, 57), P(67, 19), P(69, 7), P(54, 75), P(53, 90)),
                front(P(50, 3), P(50, 13), P(50, 39), P(73, 16), P(69, 7), P(54, 57), P(53, 72)),
                scene = Scene.PULLUP_BAR, periodMs = 1300,
            ),
            steps = listOf(
                "Hang from the bar with hands just wider than shoulders.",
                "Pull your chest toward the bar until your chin is over it.",
                "Lower all the way down with control.",
            ),
            tip = "Can't do one yet? Use a resistance band or do slow negatives.",
            met = 8.0,
        ),
        Exercise(
            id = "cable_row", name = "Seated Cable Row", muscle = Muscle.BACK, equipment = Equipment.CABLE,
            illustration = Illustration(
                side(P(55, 42), P(48, 49), P(35, 72), P(61, 58), P(73, 63), P(52, 64), P(70, 72)),
                side(P(42, 38), P(38, 47), P(35, 72), P(28, 58), P(44, 62), P(52, 64), P(70, 72)),
                HandProp.HANDLE, Scene.CABLE_ROW,
            ),
            steps = listOf(
                "Sit with feet on the plate, knees soft, holding the handle.",
                "Pull the handle to your belly while squeezing shoulder blades together.",
                "Extend your arms slowly without rounding your back.",
            ),
            tip = "Sit tall — the torso should only move a little.",
            alternativeId = "db_row", met = 4.5,
        ),
        Exercise(
            id = "db_row", name = "Dumbbell Bent-over Row", muscle = Muscle.BACK, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                hinge(P(66, 55), P(66, 68)),
                hinge(P(55, 38), P(58, 51)),
                HandProp.DUMBBELL,
            ),
            steps = listOf(
                "Hold dumbbells, hinge forward at the hips with a flat back.",
                "Row the dumbbells toward your hips, elbows close to the body.",
                "Lower until your arms are straight.",
            ),
            tip = "Pause for a second at the top and squeeze your back.",
            met = 5.0,
        ),
        Exercise(
            id = "barbell_row", name = "Barbell Row", muscle = Muscle.BACK, equipment = Equipment.BARBELL,
            illustration = Illustration(
                hinge(P(66, 55), P(66, 68)),
                hinge(P(55, 38), P(58, 51)),
                HandProp.BARBELL,
            ),
            steps = listOf(
                "Hold the bar with an overhand grip, hinge forward to ~45°.",
                "Pull the bar to your lower ribs.",
                "Lower it under control until your arms are straight.",
            ),
            tip = "Brace your core hard — your torso angle should not change.",
            alternativeId = "db_row", met = 5.0,
        ),
        Exercise(
            id = "deadlift", name = "Barbell Deadlift", muscle = Muscle.BACK, equipment = Equipment.BARBELL,
            illustration = Illustration(
                side(P(67, 47), P(60, 52), P(40, 64), P(59, 65), P(57, 78), P(55, 73), P(49, 91)),
                side(P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(51, 54), P(50, 72), P(49, 91)),
                HandProp.BARBELL, periodMs = 1700,
            ),
            steps = listOf(
                "Stand with the bar over mid-foot, grip just outside your knees.",
                "Flatten your back, chest up, and push the floor away.",
                "Stand tall with hips locked, then lower the bar along your legs.",
            ),
            tip = "The bar should stay in contact with (or very close to) your legs.",
            alternativeId = "db_rdl", met = 6.0,
        ),
        Exercise(
            id = "db_rdl", name = "Dumbbell Romanian Deadlift", muscle = Muscle.LEGS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(51, 52), P(50, 72), P(49, 91)),
                side(P(72, 40), P(64, 44), P(40, 54), P(64, 57), P(64, 70), P(48, 73), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 1700,
            ),
            steps = listOf(
                "Stand holding dumbbells in front of your thighs.",
                "Push your hips back, sliding the dumbbells down your legs, knees soft.",
                "Stop when you feel a strong hamstring stretch, then drive hips forward to stand.",
            ),
            tip = "Back stays flat — it's a hip hinge, not a squat.",
            met = 5.0,
        ),
        Exercise(
            id = "face_pull", name = "Cable Face Pull", muscle = Muscle.SHOULDERS, equipment = Equipment.CABLE,
            illustration = Illustration(
                side(P(52, 16), P(50, 26), P(50, 52), P(64, 27), P(77, 25), P(50, 72), P(49, 91)),
                side(P(52, 16), P(50, 26), P(50, 52), P(42, 22), P(56, 17), P(50, 72), P(49, 91)),
                HandProp.HANDLE, Scene.CABLE_MID,
            ),
            steps = listOf(
                "Set a rope at face height and hold it with palms facing in.",
                "Pull the rope toward your face, elbows high and wide.",
                "Return slowly until arms are straight.",
            ),
            tip = "Great for posture and healthy shoulders — go light and controlled.",
            alternativeId = "db_rear_fly", met = 3.5,
        ),
        Exercise(
            id = "db_rear_fly", name = "Dumbbell Rear Delt Fly", muscle = Muscle.SHOULDERS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                front(P(50, 30), P(50, 37), P(50, 54), P(58, 49), P(58, 61), P(55, 73), P(56, 91)),
                front(P(50, 30), P(50, 37), P(50, 54), P(70, 39), P(83, 41), P(55, 73), P(56, 91)),
                HandProp.DUMBBELL,
            ),
            steps = listOf(
                "Hinge forward with a flat back, dumbbells hanging below your chest.",
                "Raise the dumbbells out to the sides with slightly bent elbows.",
                "Lower slowly.",
            ),
            tip = "Lead with your elbows and keep it light — rear delts are small.",
            met = 3.5,
        ),

        // ───────────── LEGS ─────────────
        Exercise(
            id = "back_squat", name = "Barbell Back Squat", muscle = Muscle.LEGS, equipment = Equipment.BARBELL,
            illustration = Illustration(
                side(P(52, 16), P(50, 26), P(50, 52), P(41, 31), P(47, 24), P(50, 72), P(49, 91)),
                side(P(51, 37), P(46, 46), P(35, 70), P(37, 51), P(43, 44), P(57, 73), P(49, 91)),
                HandProp.BARBELL, periodMs = 1600,
            ),
            steps = listOf(
                "Rest the bar on your upper back, feet shoulder-width apart.",
                "Sit down and back, knees tracking over your toes.",
                "Go to at least parallel, then drive up through your whole foot.",
            ),
            tip = "Keep your chest up and heels on the floor.",
            alternativeId = "goblet_squat", met = 6.0,
        ),
        Exercise(
            id = "goblet_squat", name = "Dumbbell Goblet Squat", muscle = Muscle.LEGS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(52, 16), P(50, 26), P(50, 52), P(54, 41), P(57, 32), P(50, 72), P(49, 91)),
                side(P(51, 37), P(46, 46), P(35, 70), P(52, 57), P(54, 48), P(57, 73), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 1500,
            ),
            steps = listOf(
                "Hold one dumbbell vertically against your chest.",
                "Squat down between your knees, elbows inside the knees.",
                "Stand back up, squeezing your glutes at the top.",
            ),
            tip = "A great squat teacher — keep the dumbbell glued to your chest.",
            met = 5.5,
        ),
        Exercise(
            id = "leg_press", name = "Leg Press", muscle = Muscle.LEGS, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(P(19, 41), P(24, 50), P(41, 70), P(32, 66), P(40, 72), P(45, 50), P(60, 57)),
                side(P(19, 41), P(24, 50), P(41, 70), P(32, 66), P(40, 72), P(57, 57), P(72, 45)),
                scene = Scene.LEG_PRESS, footPlate = true,
            ),
            steps = listOf(
                "Sit with your back flat on the pad, feet shoulder-width on the plate.",
                "Release the safety and lower the plate until knees are ~90°.",
                "Press back up without locking your knees.",
            ),
            tip = "Don't let your lower back peel off the seat at the bottom.",
            alternativeId = "goblet_squat", met = 5.0,
        ),
        Exercise(
            id = "leg_extension", name = "Leg Extension", muscle = Muscle.LEGS, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(P(43, 27), P(42, 36), P(44, 63), P(44, 50), P(50, 62), P(62, 63), P(64, 84)),
                side(P(43, 27), P(42, 36), P(44, 63), P(44, 50), P(50, 62), P(62, 63), P(81, 59)),
                HandProp.HANDLE, Scene.SEAT, footRoller = true,
            ),
            steps = listOf(
                "Sit with the pad on your lower shins, knees at the edge of the seat.",
                "Straighten your legs fully and squeeze the quads.",
                "Lower slowly.",
            ),
            tip = "Pause one second at the top for better quad activation.",
            alternativeId = "bulgarian_split_squat", met = 3.5,
        ),
        Exercise(
            id = "bulgarian_split_squat", name = "Dumbbell Bulgarian Split Squat", muscle = Muscle.LEGS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(
                    P(53, 16), P(51, 26), P(50, 52), P(51, 39), P(51, 52), P(57, 72), P(60, 91),
                    kneeF = P(39, 70), footF = P(26, 62), toeF = P(21, 61),
                ),
                side(
                    P(53, 31), P(51, 41), P(48, 66), P(51, 54), P(51, 67), P(65, 73), P(60, 91),
                    kneeF = P(40, 84), footF = P(26, 62), toeF = P(21, 61),
                ),
                HandProp.DUMBBELL, Scene.BOX,
            ),
            steps = listOf(
                "Stand in front of a bench and put your rear foot on it.",
                "Lower straight down until the front thigh is about parallel.",
                "Drive through the front heel to stand back up. Switch legs.",
            ),
            tip = "Most of your weight stays on the front leg.",
            met = 5.0,
        ),
        Exercise(
            id = "leg_curl", name = "Lying Leg Curl", muscle = Muscle.LEGS, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(P(84, 57), P(74, 60), P(48, 61), P(77, 70), P(83, 66), P(28, 62), P(10, 62), toe = P(6, 64)),
                side(P(84, 57), P(74, 60), P(48, 61), P(77, 70), P(83, 66), P(28, 62), P(23, 43), toe = P(19, 40)),
                HandProp.HANDLE, Scene.BENCH, footRoller = true,
            ),
            steps = listOf(
                "Lie face down with the pad just above your heels.",
                "Curl your heels toward your glutes.",
                "Lower slowly until legs are almost straight.",
            ),
            tip = "Keep your hips pressed into the pad.",
            alternativeId = "db_rdl", met = 3.5,
        ),
        Exercise(
            id = "walking_lunge", name = "Dumbbell Walking Lunge", muscle = Muscle.LEGS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(
                    P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(50, 52), P(56, 72), P(60, 91),
                    kneeF = P(44, 72), footF = P(38, 91),
                ),
                side(
                    P(51, 32), P(49, 42), P(48, 68), P(49, 55), P(49, 68), P(64, 74), P(63, 91),
                    kneeF = P(43, 87), footF = P(27, 90), toeF = P(31, 91),
                ),
                HandProp.DUMBBELL,
            ),
            steps = listOf(
                "Hold dumbbells at your sides and take a long step forward.",
                "Lower until both knees are about 90°, rear knee near the floor.",
                "Push off and step through into the next lunge.",
            ),
            tip = "Keep your torso upright and front knee over the ankle.",
            met = 6.0,
        ),
        Exercise(
            id = "calf_raise_machine", name = "Standing Calf Raise Machine", muscle = Muscle.LEGS, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(P(52, 15), P(50, 25), P(50, 50), P(46, 22), P(50, 19), P(50, 70), P(49, 90), toe = P(55, 88)),
                side(P(52, 8), P(50, 18), P(50, 43), P(46, 15), P(50, 12), P(50, 63), P(50, 83), toe = P(55, 88)),
                HandProp.HANDLE, Scene.STEP, periodMs = 1000,
            ),
            steps = listOf(
                "Put your shoulders under the pads, balls of feet on the platform.",
                "Rise up onto your toes as high as possible.",
                "Lower your heels below the platform for a full stretch.",
            ),
            tip = "Slow down — bouncing takes the work away from the calves.",
            alternativeId = "db_calf_raise", met = 3.0,
        ),
        Exercise(
            id = "db_calf_raise", name = "Dumbbell Calf Raise", muscle = Muscle.LEGS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(52, 15), P(50, 25), P(50, 50), P(50, 38), P(50, 51), P(50, 70), P(49, 90), toe = P(55, 88)),
                side(P(52, 8), P(50, 18), P(50, 43), P(50, 31), P(50, 44), P(50, 63), P(50, 83), toe = P(55, 88)),
                HandProp.DUMBBELL, Scene.STEP, periodMs = 1000,
            ),
            steps = listOf(
                "Hold dumbbells, stand with the balls of your feet on a step.",
                "Rise onto your toes and squeeze.",
                "Lower your heels below the step.",
            ),
            tip = "Do them one leg at a time for more challenge.",
            met = 3.0,
        ),
        Exercise(
            id = "hip_thrust", name = "Barbell Hip Thrust", muscle = Muscle.LEGS, equipment = Equipment.BARBELL,
            illustration = Illustration(
                side(P(21, 59), P(28, 63), P(42, 81), P(36, 71), P(42, 76), P(60, 68), P(65, 90)),
                side(P(20, 57), P(28, 62), P(47, 63), P(38, 63), P(47, 58), P(65, 62), P(66, 90)),
                HandProp.BARBELL, Scene.LOW_BENCH, weightAtHip = true,
            ),
            steps = listOf(
                "Sit with your upper back against a bench, bar over your hips.",
                "Drive through your heels and lift your hips until your body is flat.",
                "Squeeze your glutes hard, then lower.",
            ),
            tip = "Tuck your chin and keep your shins vertical at the top.",
            alternativeId = "db_hip_thrust", met = 4.0,
        ),
        Exercise(
            id = "db_hip_thrust", name = "Dumbbell Hip Thrust", muscle = Muscle.LEGS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(21, 59), P(28, 63), P(42, 81), P(36, 71), P(42, 76), P(60, 68), P(65, 90)),
                side(P(20, 57), P(28, 62), P(47, 63), P(38, 63), P(47, 58), P(65, 62), P(66, 90)),
                HandProp.DUMBBELL, Scene.LOW_BENCH, weightAtHip = true,
            ),
            steps = listOf(
                "Upper back on a bench, one dumbbell resting on your hips.",
                "Drive your hips up until knees, hips and shoulders line up.",
                "Pause and lower slowly.",
            ),
            tip = "Push through your heels, not your toes.",
            met = 4.0,
        ),

        // ───────────── SHOULDERS ─────────────
        Exercise(
            id = "machine_shoulder_press", name = "Machine Shoulder Press", muscle = Muscle.SHOULDERS, equipment = Equipment.MACHINE,
            illustration = Illustration(
                front(P(50, 25), P(50, 34), P(50, 62), P(70, 41), P(70, 27), P(57, 70), P(58, 90)),
                front(P(50, 25), P(50, 34), P(50, 62), P(67, 22), P(64, 8), P(57, 70), P(58, 90)),
                HandProp.HANDLE, Scene.SEAT_FRONT,
            ),
            steps = listOf(
                "Adjust the seat so the handles start at shoulder height.",
                "Press overhead until arms are nearly straight.",
                "Lower back to shoulder level slowly.",
            ),
            tip = "Keep your back on the pad; don't shrug your shoulders up.",
            alternativeId = "db_shoulder_press", met = 4.5,
        ),
        Exercise(
            id = "db_shoulder_press", name = "Dumbbell Shoulder Press", muscle = Muscle.SHOULDERS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                front(P(50, 25), P(50, 34), P(50, 62), P(70, 41), P(70, 27), P(57, 70), P(58, 90)),
                front(P(50, 25), P(50, 34), P(50, 62), P(67, 22), P(64, 8), P(57, 70), P(58, 90)),
                HandProp.DUMBBELL, Scene.SEAT_FRONT,
            ),
            steps = listOf(
                "Sit upright with dumbbells at shoulder height, palms forward.",
                "Press the dumbbells overhead until arms are straight.",
                "Lower back to the shoulders.",
            ),
            tip = "Brace your core so your lower back doesn't over-arch.",
            met = 4.5,
        ),
        Exercise(
            id = "lateral_raise", name = "Dumbbell Lateral Raise", muscle = Muscle.SHOULDERS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                Poses.standFront,
                front(P(50, 15), P(50, 25), P(50, 52), P(70, 29), P(82, 30), P(55, 72), P(56, 91)),
                HandProp.DUMBBELL,
            ),
            steps = listOf(
                "Stand tall with dumbbells at your sides.",
                "Raise your arms out to the sides up to shoulder height.",
                "Lower slowly — fight gravity on the way down.",
            ),
            tip = "Slight bend in the elbows; lead with the elbows, not the hands.",
            met = 3.5,
        ),

        // ───────────── ARMS ─────────────
        Exercise(
            id = "db_curl", name = "Dumbbell Bicep Curl", muscle = Muscle.ARMS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                Poses.stand,
                side(P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(57, 28), P(50, 72), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 1200,
            ),
            steps = listOf(
                "Stand with dumbbells at your sides, palms forward.",
                "Curl the weights up while keeping elbows pinned to your sides.",
                "Lower all the way down slowly.",
            ),
            tip = "No swinging — if you need momentum, the weight is too heavy.",
            met = 3.5,
        ),
        Exercise(
            id = "hammer_curl", name = "Dumbbell Hammer Curl", muscle = Muscle.ARMS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                Poses.stand,
                side(P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(57, 28), P(50, 72), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 1200,
            ),
            steps = listOf(
                "Hold dumbbells with palms facing your body.",
                "Curl up keeping the neutral (hammer) grip.",
                "Lower under control.",
            ),
            tip = "Hits the forearms and brachialis — great for thicker arms.",
            met = 3.5,
        ),
        Exercise(
            id = "barbell_curl", name = "Barbell Curl", muscle = Muscle.ARMS, equipment = Equipment.BARBELL,
            illustration = Illustration(
                Poses.stand,
                side(P(52, 16), P(50, 26), P(50, 52), P(50, 39), P(57, 28), P(50, 72), P(49, 91)),
                HandProp.BARBELL, periodMs = 1200,
            ),
            steps = listOf(
                "Hold the bar shoulder-width with palms up.",
                "Curl the bar to your shoulders, elbows fixed at your sides.",
                "Lower slowly to full extension.",
            ),
            tip = "Stand tall — don't lean back to lift the bar.",
            alternativeId = "db_curl", met = 3.5,
        ),
        Exercise(
            id = "tricep_pushdown", name = "Cable Tricep Pushdown", muscle = Muscle.ARMS, equipment = Equipment.CABLE,
            illustration = Illustration(
                side(P(53, 16), P(51, 26), P(50, 52), P(52, 40), P(61, 30), P(50, 72), P(49, 91)),
                side(P(53, 16), P(51, 26), P(50, 52), P(52, 40), P(58, 52), P(50, 72), P(49, 91)),
                HandProp.HANDLE, Scene.CABLE_HIGH, periodMs = 1100,
            ),
            steps = listOf(
                "Stand facing a high pulley, holding a bar or rope.",
                "Keep elbows at your sides and push down until arms are straight.",
                "Let the handle rise to chest level slowly.",
            ),
            tip = "Only your forearms should move.",
            alternativeId = "db_overhead_extension", met = 3.5,
        ),
        Exercise(
            id = "db_overhead_extension", name = "Dumbbell Overhead Tricep Extension", muscle = Muscle.ARMS, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(53, 16), P(50, 26), P(50, 52), P(53, 12), P(44, 21), P(50, 72), P(49, 91)),
                side(P(53, 16), P(50, 26), P(50, 52), P(53, 12), P(55, 1), P(50, 72), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 1200,
            ),
            steps = listOf(
                "Hold one dumbbell overhead with both hands.",
                "Lower it behind your head by bending the elbows.",
                "Straighten your arms back overhead.",
            ),
            tip = "Keep your elbows pointing forward, close to your head.",
            met = 3.5,
        ),

        // ───────────── CORE ─────────────
        Exercise(
            id = "plank", name = "Plank", muscle = Muscle.CORE, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                side(P(80, 68), P(72, 72), P(44, 76), P(72, 89), P(83, 89), P(29, 82), P(14, 89)),
                side(P(80, 67), P(72, 71), P(44, 74), P(72, 89), P(83, 89), P(29, 81), P(14, 89)),
                scene = Scene.MAT, periodMs = 2200,
            ),
            steps = listOf(
                "Forearms on the floor, elbows under your shoulders.",
                "Lift your body into a straight line from head to heels.",
                "Hold while breathing steadily.",
            ),
            tip = "Squeeze glutes and pull belly button in — don't let hips sag.",
            track = TrackType.TIME, met = 3.8,
        ),
        Exercise(
            id = "crunch", name = "Crunch", muscle = Muscle.CORE, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                side(P(75, 84), P(66, 86), P(44, 86), P(73, 76), P(77, 82), P(34, 70), P(24, 89)),
                side(P(70, 71), P(62, 77), P(44, 86), P(68, 67), P(73, 72), P(34, 70), P(24, 89)),
                scene = Scene.MAT, periodMs = 1000,
            ),
            steps = listOf(
                "Lie on your back, knees bent, hands lightly behind your head.",
                "Curl your shoulders off the floor using your abs.",
                "Lower slowly.",
            ),
            tip = "Don't pull on your neck — look at the ceiling.",
            met = 3.8,
        ),
        Exercise(
            id = "russian_twist", name = "Russian Twist", muscle = Muscle.CORE, equipment = Equipment.BODYWEIGHT,
            illustration = run {
                val a = front(
                    P(50, 47), P(50, 56), P(50, 80), P(62, 66), P(70, 74), P(58, 70), P(61, 85),
                    elbowF = P(44, 66), handF = P(66, 76),
                )
                Illustration(a, a.mirrored(), scene = Scene.MAT, periodMs = 800)
            },
            steps = listOf(
                "Sit with knees bent, lean back slightly, feet on or off the floor.",
                "Rotate your torso to bring your hands beside one hip.",
                "Twist to the other side. Each side counts as one rep.",
            ),
            tip = "Hold a dumbbell for extra challenge.",
            met = 4.0,
        ),
        Exercise(
            id = "mountain_climber", name = "Mountain Climbers", muscle = Muscle.CARDIO, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                side(P(80, 62), P(72, 66), P(44, 77), P(72, 78), P(72, 90), P(60, 80), P(50, 88), kneeF = P(30, 84), footF = P(15, 90)),
                side(P(80, 62), P(72, 66), P(44, 77), P(72, 78), P(72, 90), P(30, 84), P(15, 90), kneeF = P(60, 80), footF = P(50, 88)),
                scene = Scene.MAT, periodMs = 450,
            ),
            steps = listOf(
                "Start in a high plank.",
                "Drive one knee toward your chest, then quickly switch legs.",
                "Keep a fast pace while keeping hips low.",
            ),
            tip = "Shoulders stay over your wrists the whole time.",
            track = TrackType.TIME, met = 8.0,
        ),

        // ───────────── CARDIO ─────────────
        Exercise(
            id = "jumping_jack", name = "Jumping Jacks", muscle = Muscle.CARDIO, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                front(P(50, 15), P(50, 25), P(50, 52), P(58, 38), P(59, 51), P(53, 72), P(53, 91)),
                front(P(50, 13), P(50, 23), P(50, 50), P(66, 13), P(64, 2), P(60, 70), P(68, 90)),
                periodMs = 450,
            ),
            steps = listOf(
                "Stand with feet together, arms at your sides.",
                "Jump feet out while raising arms overhead.",
                "Jump back to the start. Keep a steady rhythm.",
            ),
            tip = "Land softly on the balls of your feet.",
            track = TrackType.TIME, met = 8.0,
        ),
        Exercise(
            id = "burpee", name = "Burpee", muscle = Muscle.CARDIO, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                side(P(52, 16), P(50, 26), P(50, 52), P(52, 14), P(54, 2), P(50, 72), P(49, 91)),
                Poses.highPlank,
                scene = Scene.MAT, periodMs = 900,
            ),
            steps = listOf(
                "From standing, squat and place your hands on the floor.",
                "Jump your feet back into a plank (add a push-up if you like).",
                "Jump your feet back in and explode up, arms overhead.",
            ),
            tip = "Step back instead of jumping to make it easier.",
            met = 10.0,
        ),
        Exercise(
            id = "high_knees", name = "High Knees", muscle = Muscle.CARDIO, equipment = Equipment.BODYWEIGHT,
            illustration = Illustration(
                side(
                    P(52, 16), P(50, 26), P(50, 52), P(44, 38), P(51, 45), P(64, 54), P(62, 72),
                    elbowF = P(55, 36), handF = P(60, 26), kneeF = P(50, 72), footF = P(48, 91),
                ),
                side(
                    P(52, 16), P(50, 26), P(50, 52), P(55, 36), P(60, 26), P(50, 72), P(48, 91),
                    elbowF = P(44, 38), handF = P(51, 45), kneeF = P(64, 54), footF = P(62, 72),
                ),
                periodMs = 350,
            ),
            steps = listOf(
                "Run in place, driving your knees up to hip height.",
                "Pump your arms in rhythm with your legs.",
                "Stay on the balls of your feet.",
            ),
            tip = "Go as fast as you can while keeping good posture.",
            track = TrackType.TIME, met = 8.0,
        ),
        Exercise(
            id = "treadmill_run", name = "Treadmill Run", muscle = Muscle.CARDIO, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(
                    P(54, 14), P(51, 24), P(48, 50), P(44, 36), P(51, 42), P(60, 64), P(58, 84),
                    elbowF = P(57, 34), handF = P(62, 26), kneeF = P(44, 68), footF = P(32, 78),
                ),
                side(
                    P(54, 14), P(51, 24), P(48, 50), P(57, 34), P(62, 26), P(44, 68), P(32, 78),
                    elbowF = P(44, 36), handF = P(51, 42), kneeF = P(60, 64), footF = P(58, 84),
                ),
                scene = Scene.TREADMILL, periodMs = 380,
            ),
            steps = listOf(
                "Start walking to warm up, then increase to a comfortable run.",
                "Keep an upright posture and relaxed shoulders.",
                "For fat loss try intervals: 1 min fast / 1 min easy.",
            ),
            tip = "Don't hold the rails while running.",
            alternativeId = "high_knees", track = TrackType.TIME, met = 9.0,
        ),
        Exercise(
            id = "rowing_machine", name = "Rowing Machine", muscle = Muscle.CARDIO, equipment = Equipment.MACHINE,
            illustration = Illustration(
                side(P(50, 49), P(44, 56), P(34, 77), P(56, 64), P(66, 68), P(48, 62), P(60, 81)),
                side(P(10, 48), P(16, 56), P(22, 77), P(10, 66), P(24, 66), P(42, 75), P(60, 81)),
                scene = Scene.ROWER, periodMs = 1100,
            ),
            steps = listOf(
                "Strap your feet in, grab the handle, knees bent (the catch).",
                "Push with your legs first, then lean back and pull the handle to your ribs.",
                "Reverse: arms, body, then legs back to the catch.",
            ),
            tip = "Power comes ~60% from the legs — don't just pull with your arms.",
            alternativeId = "db_thruster", track = TrackType.TIME, met = 7.0,
        ),
        Exercise(
            id = "db_thruster", name = "Dumbbell Thruster", muscle = Muscle.CARDIO, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(51, 37), P(46, 46), P(35, 70), P(54, 52), P(52, 42), P(57, 73), P(49, 91)),
                side(P(52, 16), P(50, 26), P(50, 52), P(52, 13), P(53, 1), P(50, 72), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 1000,
            ),
            steps = listOf(
                "Hold dumbbells at your shoulders and squat down.",
                "Drive up explosively and press the dumbbells overhead in one motion.",
                "Bring them back to the shoulders as you drop into the next squat.",
            ),
            tip = "Use the leg drive to help the press.",
            met = 8.0,
        ),
        Exercise(
            id = "kb_swing", name = "Kettlebell Swing", muscle = Muscle.CARDIO, equipment = Equipment.KETTLEBELL,
            illustration = Illustration(
                side(P(68, 37), P(60, 42), P(38, 56), P(53, 54), P(45, 64), P(46, 73), P(48, 91)),
                side(P(52, 16), P(50, 26), P(50, 52), P(62, 28), P(76, 28), P(50, 72), P(49, 91)),
                HandProp.KETTLEBELL, periodMs = 800,
            ),
            steps = listOf(
                "Hold the kettlebell with both hands, feet wider than hips.",
                "Hike it back between your legs by hinging at the hips.",
                "Snap your hips forward to swing it to chest height.",
            ),
            tip = "It's a hip movement — your arms are just ropes.",
            alternativeId = "db_swing", met = 9.0,
        ),
        Exercise(
            id = "db_swing", name = "Dumbbell Swing", muscle = Muscle.CARDIO, equipment = Equipment.DUMBBELL,
            illustration = Illustration(
                side(P(68, 37), P(60, 42), P(38, 56), P(53, 54), P(45, 64), P(46, 73), P(48, 91)),
                side(P(52, 16), P(50, 26), P(50, 52), P(62, 28), P(76, 28), P(50, 72), P(49, 91)),
                HandProp.DUMBBELL, periodMs = 800,
            ),
            steps = listOf(
                "Hold one dumbbell by the end with both hands.",
                "Hinge and swing it back between your legs.",
                "Drive your hips forward to swing it to chest height.",
            ),
            tip = "Keep a tight grip and a flat back.",
            met = 8.0,
        ),
    )

    private val builtInById = builtIn.associateBy { it.id }

    // Observable so pickers and lists update as soon as a custom exercise is added or changed.
    private var customList by mutableStateOf<List<CustomExercise>>(emptyList())
    private var customById by mutableStateOf<Map<String, Exercise>>(emptyMap())

    /** Everything you can pick: built-in plus custom exercises that aren't archived. */
    val all: List<Exercise>
        get() = builtIn + customList.filterNot { it.archived }.mapNotNull { customById[it.id] }

    fun setCustom(list: List<CustomExercise>) {
        customList = list
        customById = list.associate { it.id to it.toExercise() }
    }

    fun get(id: String): Exercise = find(id) ?: builtIn.first()

    /** Finds built-in and custom exercises, archived ones included (they're still in history). */
    fun find(id: String): Exercise? = builtInById[id] ?: customById[id]

    fun alternativeFor(ex: Exercise): Exercise? = ex.alternativeId?.let { find(it) }

    /** Figure shown for a custom exercise: a representative built-in move for its muscle group. */
    private val templateFor = mapOf(
        Muscle.CHEST to "db_bench",
        Muscle.BACK to "db_row",
        Muscle.LEGS to "goblet_squat",
        Muscle.SHOULDERS to "db_shoulder_press",
        Muscle.ARMS to "db_curl",
        Muscle.CORE to "crunch",
        Muscle.CARDIO to "jumping_jack",
    )

    private fun CustomExercise.toExercise(): Exercise {
        val template = builtInById.getValue(templateFor.getValue(muscle))
        return Exercise(
            id = id, name = name, muscle = muscle, equipment = equipment,
            illustration = template.illustration,
            steps = notes.lines().map { it.trim() }.filter { it.isNotEmpty() },
            tip = "",
            track = track,
            met = template.met,
            isCustom = true,
        )
    }
}
