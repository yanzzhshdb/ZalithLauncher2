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
