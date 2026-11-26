# NLauncher (NorxLauncher) — repository notes

## Build
- Gradle + Android app module `ZalithLauncher`, `compileSdk 37`, `minSdk 26`, Kotlin 2.4, Compose BOM 2026.09.00, Material3 1.5.0-beta02.
- No Android SDK in the dev sandbox: verify changes through the GitHub Actions `Build CI`
  workflow (runs on every push, one job per ABI).
- Kotlin reports every frontend error at once; read the full `e: file://` list from the job log:
  `curl -sL -H "Authorization: Bearer $GITHUB_TOKEN" .../actions/jobs/<id>/logs`.

## UI conventions
- Liquid glass design system: `ui/theme/glass/GlassSpec.kt` + `LiquidGlass.kt`
  (`Modifier.liquidGlass`, `LiquidGlassCard`, `LiquidGlassSurface`, `rememberGlassSpec`).
- `Modifier.backgroundGlass()` silently draws nothing when no launcher background is
  available, so always draw the glass tint separately (`Modifier.background`).
- Use `Modifier.border(brush = ...)` instead of `drawOutline` for the glass edge ring.
- `LocalContentColor` comes from `androidx.compose.material3`.

## Community (group chat + voice)
- Firebase is initialised at runtime with `FirebaseBootstrap` (named app
  `nlauncher-community`) from keys the user enters in the app; there is no
  `google-services.json` and the google-services Gradle plugin is intentionally absent.
- Data model (Firestore): `groups/{gid}` (memberIds array, inviteCode, lastMessage…),
  `groups/{gid}/messages`, `groups/{gid}/members/{uid}` (presence + typingUntil),
  `groups/{gid}/voice/{uid}` (participants) and `groups/{gid}/voice/{uid}/signals`
  (WebRTC offer/answer/candidate).
- Voice is an audio-only WebRTC mesh (`io.github.webrtc-sdk:android`); the peer with the
  smaller uid creates the offer. STUN defaults to Google, TURN is optional.
- `CommunityController` is the facade (StateFlows), `VoiceRoomController` owns the mesh.
- **Maintenance switch**: `community/CommunityAvailability.ENABLED` is `false`, so the top
  bar entry is hidden and `CommunityHostScreen` renders `CommunityMaintenanceScreen`
  instead. Set it back to `true` to restore the feature; no other change is needed.

## Feature flags and modes
- Power saver (`LauncherSettings.launcherPowerSaver`) forces `powerSave` in
  `rememberGlassSpec` and makes `rememberSwapTween`/`rememberTransitionSpec` snap.
- Music sleep timer lives in `MusicPlayer.startSleepTimer(minutes)`; state field
  `MusicPlaybackState.sleepTimerEndsAt` (0 = off).
- `WhatsNewDialog` shows once per `BuildConfig.VERSION_CODE`, tracked by
  `LauncherSettings.launcherWhatsNewSeen`.

## Package / applicationId
- `applicationId` comes from `launcher_application_id` in `ZalithLauncher/gradle.properties`
  (`com.norxlauncher`, debug `com.norxlauncher.debug`); the Kotlin `namespace` stays
  `com.movtery.zalithlauncher` so sources and the R class are untouched.
- Intent action strings (`com.movtery.zalithlauncher.filemanager.EVENT_SERVICE`, music
  actions) are internal names, not the package: keep code and manifest in sync if renamed.

## Control preset catalog (editor)
- `ui/screens/main/control_editor/preset/ControlPreset.kt` holds `ControlPresets`: every
  entry is a ready-made `NormalData` button whose `ClickEvent`s are already bound to a
  GLFW keycode (`ControlEventKeycode.*`) or a `LAUNCHER_EVENT_*` string. Add a new control
  by appending one `ControlPreset(...)` to the matching private list; labels need a
  `control_preset_*` string in `values` (and `values-in`).
- `StarterPackPlacements` + `findPreset()` describe the one-tap "quick combat pack"
  (attack/use/jump/sneak/inventory at fixed 0~10000 coordinates).
- `ControlPresetCatalogDialog` is opened from the editor menu (`add_preset_catalog` item,
  wired in `EditorMenu`), and calls `EditorViewModel.addWidget(layers) { … }` so presets
  land on the currently selected layer.

## Animation tuning
- `Modifier.animated(...)` gained a `scaleIn` factor (1f = off) driven by the same progress
  as the slide, so cards can bloom instead of popping. The home grid uses `scaleIn = 0.96f`.
- Exit tweens use `EaseOutFluent(2)` instead of `LinearEasing`; `rememberSwapTween` uses
  `EaseOutFluent(3)`. Power saver still snaps everything.
- `MenuButtonLayout` adds a spring press-scale (`pressScale`) via `interactionSource`; keep
  the power-saver branch snapping when adding similar press feedback elsewhere.
