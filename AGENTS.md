# NLauncher (NorxLauncher) — repository notes

## Build
- Gradle + Android app module `ZalithLauncher`, `compileSdk 37`, `minSdk 26`, Kotlin 2.4, Compose BOM 2026.09.00, Material3 1.5.0-beta02.
- No Android SDK in the dev sandbox: verify changes through the GitHub Actions `Build CI`
  workflow (runs on every push, one job per ABI).
- Kotlin reports every frontend error at once; read the full `e: file://` list from the job log:
  `curl -sL -H "Authorization: Bearer $GITHUB_TOKEN" .../actions/jobs/<id>/logs`.

## UI conventions
- **Norx design system** (`ui/theme/NorxDesign.kt`): flat bordered panels, hairline
  outlines, gradient accent lines, medium geometric corners (6/10/14/18/24dp in
  `AppShapes`). Use `cardColor()`/`norxPanelColor()`, `norxCardBorder()`,
  `norxAccentBrush()`, `NorxAccentStripe` and `NorxTokens` for new surfaces.
- Typography is Space Grotesk (`ui/theme/Type.kt`, `NorxFontFamily`) for every text
  style; titles use negative tracking, labels positive tracking.
- Legacy glass helpers still exist for opt-in overlays: `ui/theme/glass/GlassSpec.kt` +
  `LiquidGlass.kt` (`Modifier.liquidGlass`, `LiquidGlassCard`, `LiquidGlassSurface`,
  `rememberGlassSpec`). `BackgroundCard` and `GlassAlertDialog` no longer use them.
- Settings navigation is a horizontal tab strip (`TabStrip` in `SettingsScreen.kt`);
  the content below it must use `Modifier.weight(1f)`, not `fillMaxHeight()`.
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

## Branding assets
- Launcher icon lives in `mipmap-*/ic_launcher(.webp|_round.webp)`; the adaptive icon
  (`mipmap-anydpi-v26`) uses `@color/ic_launcher_background` + PNG foreground/monochrome
  in `drawable-xxxhdpi` (no vectors any more). Replace all of them together when the logo
  changes, and keep `drawable/img_launcher.png` (in-app logo: welcome, loading, About) in sync.
- The splash screen points at `@drawable/img_launcher`, so it follows the logo automatically.
- Founder credit uses `drawable/img_avatar_patrick.png` through `CreditItem` in
  `AboutInfoScreen`; add new people next to it rather than inventing another card layout.

## Update notifications
- `upgrade/LauncherReleaseChecker` reads `releases/latest` of `yanzzhshdb/ZalithLauncher2`
  and compares the tag against `BuildConfig.VERSION_NAME` with a semver compare (a `v`
  prefix is fine). It is the source of truth for "new build" notifications.
- `MainActivity.checkLauncherRelease()` runs at most once every 6 hours (`UpgradePrefs
  .lastReleaseCheck`) and skips a tag already notified (`UpgradePrefs.notifiedReleaseTag`).
- `notification/LauncherUpdateNotification` posts on the `launcher.update` channel and opens
  the release page on tap; it silently no-ops when notifications are disabled.

## Playtime tracking
- `game/session/PlaytimeTracker` stores per-version totals, session counts, last-played
  timestamps and daily buckets in the launcher MMKV (`playtime.*` keys). A session is opened
  by `VMActivity.createSession` right before `GameLauncher` starts and settled in its `onExit`.
- Sessions shorter than 10 s are ignored, longer than 12 h are treated as stale, and
  `MainActivity.onCreate` drops a pending session because a fresh activity means the game
  is not running (the process was recycled).
- `PlaytimeScreen` (Settings → Playtime) reads `PlaytimeTracker.snapshot()` once per visit;
  the home `ContinuePlayingCard` resolves the last played version through
  `VersionsManager.loadVersion` on the IO dispatcher and hides itself when the version is gone.
- `GameSessionNotification` posts the "session finished" summary on the `game.session` channel.

## Built-in screen recorder
- `recorder/ScreenRecorder` is the controller: it exposes `recording` / `startedAt` /
  `lastFile` StateFlows, builds the service intent from the `MediaProjection` consent result
  and stores videos in `getExternalFilesDir(null)/recordings` (no storage permission needed).
  Share/open intents go through the existing `FileProvider` (`${packageName}.provider`).
- `recorder/ScreenRecorderService` is a `mediaProjection` foreground service using
  `MediaRecorder` (H.264 + optional AAC from the microphone). Order matters: start the
  foreground service first, then `getMediaProjection`, then `prepare()` the recorder,
  take `recorder.surface`, create the `VirtualDisplay`, and only then `start()`. The capture
  size is always the real screen size scaled by `RecorderQuality.scale`, rounded to even
  numbers; the bitrate is `width * height * fps * bitsPerPixel` clamped to 2–40 Mbps.
- `RecorderSettings` keeps quality/fps/audio in MMKV, and `ui/screens/game/elements/RecordMenu`
  renders the switch plus options inside the in-game menu. Recording state survives the menu
  closing because it lives in the service/object, not in Compose.
- Videos stop automatically when the user revokes the projection (`MediaProjection.Callback.onStop`),
  and short recordings whose `stop()` throws are deleted instead of leaving broken MP4s.

## Performance presets
- `game/performance/PerformancePreset` groups the knobs for Balanced / Max performance /
  Best graphics: resolution percentage, sustained-performance flag and the `options.txt`
  values to write.
- `game/performance/PerformanceOptimizer.apply` patches only the keys the preset defines,
  keeps the first original as `options.txt.nlbackup`, writes atomically through a `.tmp`
  file and also updates `RendererSettings` (sustained performance, max refresh rate,
  resolution ratio when the rule is percentage). `restore` copies the backup back.
- `PerformanceSettingsScreen` (Settings → Performance) picks the target version from
  `VersionsManager.versions`; it warns when `options.txt` is missing, which only happens
  before the version has been launched once.

## Built-in AI assistant
- `ai/AiAssistant` holds the maintenance switch (`MAINTENANCE = true`). While it is on, no AI request is
  sent and every AI entry point shows the maintenance UI.
- `ai/AxAiClient` has an empty `PROXY_URL` on purpose: the API key must live on a server-side proxy, never in
  the APK, `BuildConfig`, `gradle.properties` or the settings UI. Prompts go in the query string of the
  proxy call, so keep them short (`MAX_PROMPT_CHARS`, 2400). Only the exception type is logged, never the URL.
  or the gateway answers 414. The response is `{status, msg, result:{model, text}}`.
- `AiSettings` stores only the context/log/auto-crash toggles and the target version. It keeps no key:
  `clearLegacySecrets()` wipes the old `aiApiKey` entry on startup.
  and the instance used for context; `SettingsInitializer` reloads it with the other registries.
- `ai/AiContextBuilder` turns local state into prompt text: instance summary (version, loader,
  renderer, RAM, resolution, mod list capped at 20 names) plus a keyword-filtered log digest
  (900 chars). Never send whole logs - the API is URL based.
- `AiChatSession` is a singleton holding the conversation and `busy` state so the chat survives
  navigation; it builds prompts and calls the client on `Dispatchers.IO`.
- UI: `ui/screens/main/ai/AiChatPanel` (bubbles, chips, input) is embedded by
  `AiChatActivity` (standalone chat, also used by the crash flow) and by
  `AiAssistantScreen` (Settings → AI Assistant, key + toggles + shortcuts).
- Crash flow: `ErrorActivity` passes `onAnalyzeAiClick` into `ErrorScreen`; the button opens
  `AiChatActivity` with the log path, the crash message and `versionName` (from
  `VMViewModel.gameVersionName`, set when a session is created). With
  `AiSettings.autoAnalyzeCrash` on, the analysis starts as soon as the chat opens.

## Purple brand theme
- The launcher now defaults to `ColorThemeType.NORX_PURPLE` (seed `0xFF7C3AED`), generated at runtime with
  `customLight`/`customDark` + materialkolor, so no new `ColorTheme` fields are needed.
- `NORX_AURORA` is the First Release event skin (seed `0xFF9B4DFF`). `Theme.kt` silently falls back to
  `NORX_PURPLE` when the event is over and the unlock was never claimed, and the theme picker disables the
  radio through `getRadioEnable`.

## First Release event
- `event/FirstReleaseEvent` keeps everything in MMKV: start timestamp, playtime baseline, unlocked flag and
  claim time. The 6 day window starts on the first launch after install (`loadAllSettings` calls
  `ensureStarted()`), and the permanent unlock needs 15 minutes of playtime measured as
  `PlaytimeTracker.snapshot().totalMillis - baseline + pendingSessionMillis()` (AFK in game counts).
- Claiming is allowed after the event ends as long as the goal was reached; only the exclusive UI locks.
- UI: `ui/screens/event/FirstReleaseScreen` (countdown, live progress, claim, rewards, playtime stats) inside
  `FirstReleaseActivity`, plus `ui/screens/content/home/FirstReleaseBanner` registered as the
  `system_first_release` home card while the event is available.

## Release artifacts (all ABIs)
- `build.yml` builds one ABI per matrix job with `-Darch=<arch>`; the "all" job is the universal APK
  (`NorxLauncher-<version>.apk`), the others are ABI splits (`NorxLauncher-<version>-<abi>.apk`).
- Uploads are strict now (`if-no-files-found: error`, no `continue-on-error`) and a `Verify Release APK`
  step fails the job when the split APK is missing or still contains foreign `lib/<abi>` entries.
- `Release CI` (`release_ci.yml`) runs on `release: published` and on `workflow_dispatch` (optional `tag`
  input). Its `Bundle` job re-checks that universal + armeabi-v7a + arm64-v8a + x86 + x86_64 APKs all exist,
  uploads them as the single artifact `release apk (all abis)`, and attaches them to the release when a tag
  is given.

## Dependency resolution in CI
- `settings.gradle.kts` and the root `build.gradle.kts` list
  `https://maven-central.storage-download.googleapis.com/maven2/` (Google's official Maven Central mirror)
  before `mavenCentral()`, because Central regularly answers HTTP 429 to GitHub runners. `build.yml` also
  retries `assemble<Variant>` four times with exponential backoff for the same reason.

## AI assistant is in maintenance (1.7.1)
- `AiAssistant.MAINTENANCE = true` gates every AI entry point: the settings page shows a maintenance card,
  the chat panel hides its input, and the crash screen / event screen hide their AI buttons.
- No API key ships in the client: `AiSettings` has no key field, `clearLegacySecrets()` wipes the old
  `aiApiKey` MMKV entry, `AxAiClient.PROXY_URL` is empty, and only the exception type is logged. When the
  assistant comes back, the key must stay on a server-side proxy.

## Norx Tools (2.0.0)
- New settings page `ui/screens/content/settings/ToolsScreen.kt`, key `NormalNavKey.Settings.Tools`,
  strings prefixed `tools_` (English in `values/`, Indonesian in `values-in/`).
- Logic lives in `tools/` next to `com.movtery.zalithlauncher`: `ToolsStorage` (size/clear helpers),
  `InstanceTools` (instance sizes, health check, duplicate mod ids, big files), `LogTools` (log list and
  retention cleanup), `RamTuner` (RAM/JVM recommendation + apply to global settings or `VersionConfig`),
  `InstanceNotes` (MMKV per-instance note), `SettingsBackup` (SAF json export/import of `FILE_SETTINGS`),
  `ReportBundler` (zip of device info + launcher logs + instance logs, shared via the existing FileProvider).
- Everything runs offline; the report zip stays in `PathManager.DIR_CACHE` and is shared through
  `cache-path` in `provider_paths.xml`.
- Bumping `launcher_version_code` in `ZalithLauncher/gradle.properties` re-triggers `WhatsNewDialog`;
  keep `whats_new_body` updated in both `values/` and `values-in/`.
