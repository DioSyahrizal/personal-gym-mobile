# Personal Gym — agent guide

A personal Android workout app: weekly plans (fat loss / build muscle), workout logging with a rest
timer, goals, body tracking and progress analytics. Single user, single device, no backend.
Kotlin + Jetpack Compose, one module (`:app`), package `com.example.personalgym`.

Read this before changing anything. Keep it up to date when you change how things work.

## Build and run

There is no `java` on the PATH; use Android Studio's bundled JDK.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug      # dev build  -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease    # signed     -> app/build/outputs/apk/release/app-release.apk
```

- Versions are in `gradle/libs.versions.toml`: AGP 9.4.1 (built-in Kotlin, KGP 2.2.10), Compose BOM
  2026.02.01, material-icons-core 1.7.8. Keep the compose-compiler plugin version equal to the
  Kotlin version.
- There are no tests yet beyond the template ones. Verification so far has been manual, on device.

### Debug vs release

| | Debug | Release |
|---|---|---|
| App ID | `com.example.personalgym.debug` | `com.example.personalgym` |
| Label | "Gym (dev)" (`app/src/debug/res`) | "Personal Gym" |
| Signing | debug key | `release.jks` via `keystore.properties` |

Debug and release install side by side. **Test on the debug build; the release app holds the
owner's real data.** Never uninstall or overwrite the release app's data without asking.

Release builds shrink the whole app with R8 (`optimization { enable = true }`). Do not add a
`packageScope` limited to libraries: that left app classes extending renamed Kotlin classes and the
app crashed on launch.

### Signing key (do not lose, do not commit)

`release.jks` and `keystore.properties` sit in the repo root and are gitignored. Updates to the
installed release app must be signed with this key. If `keystore.properties` is missing, release
builds come out unsigned and debug builds are unaffected. `release/` (gitignored) holds copies of
built APKs, e.g. `release/PersonalGym-1.0.apk`.

## Testing on a device

- The owner's phone is a **Xiaomi** (model 25113PN0EG, serial `6006362e`, 1220×2656) connected over
  USB. Use `ANDROID_SERIAL=6006362e ~/Library/Android/sdk/platform-tools/adb ...`.
- Xiaomi asks for an on-screen confirmation for every `adb install`; it fails with
  `INSTALL_FAILED_USER_RESTRICTED` if the phone is locked or nobody taps Install. Ask the owner.
- The screen locks quickly ("Stay awake" keeps switching itself off). If
  `dumpsys window | grep isKeyguardShowing` says true, stop and ask the owner to unlock. Never
  unlock the phone yourself.
- Drive the UI with `input tap`/`input text`/`exec-out screencap -p`. Taps sometimes land mid-
  animation; wait ~1.5 s between steps and re-screenshot after dialogs open or close.
- Inspect or seed app data with `adb shell run-as <app id> cat shared_prefs/personal_gym.xml`
  (works for debug and release). Back the file up before changing it and restore it afterwards.
- Notification permission is requested on first workout; leave that choice to the owner on the
  release app.
- Emulator `Medium_Phone_API_37.0` exists but is very slow; it needs `-gpu swiftshader_indirect`
  or the screen stays black. Prefer the phone.

## Code map

```
AppViewModel.kt        All app state (mutableStateOf) and every action; persists via Storage.
MainActivity.kt        Theme selection, notification channels, foreground tracking.
RestTimerAlerts.kt     Rest-timer lock-screen countdown + exact alarm "time's up" notification.
WorkoutReminders.kt    Daily training-day reminder (alarm fires daily, decides at fire time).
data/
  Models.kt            Exercise, PlanDay/WorkoutPlan, WorkoutSession/SetLog, Goal, WeightEntry,
                       Measurement/BodySite, Profile/Level, DayEdits.
  ExerciseLibrary.kt   ~48 exercises: animation poses, steps, tip, dumbbell `alternativeId`.
  Plans.kt             3/4/5-day plans per goal, beginner adjustment, applying user day edits.
  Stats.kt             Analytics: streaks, weekly buckets, PRs, e1RM, overload hints, goal progress.
  Storage.kt           SharedPreferences ("personal_gym") JSON storage + backup encode/decode.
  Units.kt             WeightUnit (kg·cm / lb·in), ThemeMode, formatNumber.
ui/
  GymApp.kt            Navigation: layered `when` (onboarding > exercise > session > settings >
                       edit day > plan day > tabs). Tabs: Home, Plan, Exercises, Goals, Stats.
  illustration/Figure.kt  Canvas stick-figure renderer: two poses lerped, plus equipment/scenes.
  components/          Shared cards, charts (bar/line), ValueInputDialog.
  screens/             One file per screen; Measurements.kt, ExerciseProgress.kt,
                       ReminderSetting.kt are sections used inside other screens.
  theme/Theme.kt       Dark + light schemes; Coral/Sky/Violet adapt to the theme; figureColors().
```

## Conventions and invariants

- **Units:** weights are always stored in kg, lengths in cm, body fat in %. `WeightUnit` converts
  only for display and input. Never store a converted value.
- **Persistence:** everything is JSON in one SharedPreferences file. When adding stored data, also
  add it to `Backup`/`BackupFormat` in `Storage.kt` (new fields optional with defaults so older
  backups still restore) and to `currentBackup()`/`restore()` in `AppViewModel`.
- **Plans:** built-in plans live in code; user edits are stored per plan id as `DayEdits`
  (day → exercise list) and applied in `Plans.forProfile`. The two original 5-day plan ids
  (`fat_loss`, `build_muscle`) must not change — saved data refers to them.
- **Exercises:** ids are stored in history; never rename or remove one. Every machine/cable/barbell/
  kettlebell move should have a dumbbell (or bodyweight) `alternativeId`. New moves need two poses
  in the 100×100 figure space (floor at y=92, side view faces right; see `side()`/`front()`).
- **Input dialogs:** use `ValueInputDialog` (pre-selects the old value so typing replaces it, and
  validates a range). Body weight 25–350 kg, lengths 10–250 cm, body fat 2–70%.
- **Platform declaration clash:** a `var x` with a private setter clashes with a `fun setX(...)`.
  Name helpers `storeX` instead (see `storeActive`, `storePlanEdits`, `storeMeasurements`).
- Compose is all Material 3; experimental APIs are opted in per file with `@file:OptIn`.
- Commit messages: imperative summary line, wrapped body explaining what and why. The repo has no
  remote; commits are local only.

## Feature status

Done (see `git log` for detail):
- Workouts: start from plan or empty, log kg/reps per set, rest timer (also on the lock screen),
  timed sets, swap to dumbbell alternative, add/remove exercises and sets, resume after restart.
- Last-session numbers per set and progressive-overload suggestions (prefilled weight).
- Plans: fat loss and build muscle in 3/4/5 days, beginner/intermediate, per-day editing.
- Onboarding (goal, level, days, equipment, body weight), redo from Settings.
- Goals: workouts/week, total workouts, body weight, lift a weight, custom.
- Body weight and body measurements with history, edit and delete.
- Stats: totals, 8-week charts, weekly sets per muscle vs 10–20 range, PRs with e1RM, history.
- Exercise detail: animated how-to, alternative, per-exercise progress chart and recent sets.
- Settings: backup export/restore (JSON via file picker), kg·cm / lb·in, System/Light/Dark,
  dumbbell-only mode, workout reminders.
- New vector app icon (themed-icon ready) and signed release build.

Known gaps / ideas, roughly in priority order:
1. Open a past workout from History to view or fix sets (today you can only delete it).
2. Automated unit tests for `Stats`, `Units`, `Plans.forProfile`, `BackupFormat`.
3. Progress photos.
4. Custom exercises (generic figure), plate calculator, supersets.
5. Home-screen widget, Health Connect, iPhone version.
6. Before any Play Store release: change the `com.example` app ID and revisit `USE_EXACT_ALARM`
   (Play only allows it for alarm/timer apps).

Untested on device: reminder skipping on rest/already-trained days, reminder reschedule after
reboot, the flat-line chart fix.
