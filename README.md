# Vestra — AI Try-On Studio

An Android app that shows any garment worn on a person — from a single photo of the garment. Point it at a dress on a hanger, a flat-lay, or a catalog shot, and Vestra renders it on your own photo or on an AI model from the built-in gallery.

**Generation runs fully on-device.** No internet is required to create images; model packs are downloaded once and everything after that happens locally. An optional cloud tier (explicit opt-in) offers maximum quality when online.

## Generation engines

| Tier | Where it runs | Devices | Model pack |
|---|---|---|---|
| **Lite** | On device — segmentation, pose, garment warp, harmonization | All supported phones (Android 8.0+) | ~300 MB |
| **Pro** | On device — quantized try-on diffusion (NPU) | Flagships (≥8 GB RAM, modern NPU) | ~2.5–4 GB |
| **Cloud** | Supabase Edge Function → Replicate | Any, online only | none |

The engine is user-selectable in Settings; **Auto** picks the best installed on-device engine and never touches the network.

## Project layout

- `composeApp/` — Android app (Jetpack Compose, cinematic dark UI)
- `shared/` — Kotlin Multiplatform core: domain models, engine routing, pack manager, cloud client. Pure Kotlin `commonMain`, ready for the iOS port (`docs/IOS_PORT.md`)
- `ml/` — Python export tooling that produces the downloadable model packs (not shipped in the app)
- `supabase/` — Edge Functions for the cloud tier and AI-content reports
- `docs/` — architecture, Play Store compliance, privacy policy

## Building

Requires JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew :composeApp:assembleDebug   # build the app
./gradlew :shared:testDebugUnitTest   # run core unit tests
```

## License

GPL-3.0 — see [LICENSE](LICENSE).
