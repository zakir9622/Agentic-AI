# The Lookbook — AI Garment Photoshoot Studio

An Android app that turns a single photo of a garment into a photoshoot of it being worn. Point it at a dress on a hanger, a flat-lay, or a catalog shot, and The Lookbook casts studio models, shoots the garment on them across poses, and stages each shot on a studio backdrop — no models, no studio, no photographer.

Built modest-wear-first: abayas, hijabs, kaftans, and every category of covered clothing are first-class, alongside western wear.

**Generation runs fully on-device.** No internet is required to create images; model packs are downloaded once and everything after that happens locally. An optional cloud tier (explicit opt-in) offers maximum quality when online.

> The internal module namespace remains `com.zakir.vestra` from the project's first iteration; the product name is **The Lookbook**.

> 📋 **Picking this up (new dev or AI)? Start with [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)** — the full handoff: every build phase, what's live, the architecture, known issues, and the roadmap.

## Generation engines

| Tier | Where it runs | Devices | Model pack | Status |
|---|---|---|---|---|
| **Lite** | On device — segmentation, warp, harmonization (*compositor*) | All phones (Android 8.0+) | ~270 MB | built; not hosted |
| **Pro** | On device — SD1.5 + ControlNet-Depth + IP-Adapter diffusion (NPU) | 12 GB+ RAM flagships | ~4.3 GB fp16 | ✅ **live** on Hugging Face |
| **Cloud** | Supabase Edge Function → Replicate IDM-VTON | Any, online only | none | ✅ **live** |

The engine is user-selectable in Settings; **Auto** picks the best installed on-device engine and **never touches the network**.

**A note on quality.** The Lite engine is a *compositor* (segment → warp → blend) — fast and offline, but it cannot synthesize a person and won't reach diffusion photorealism. For photoreal output use the **Cloud** engine (live — pennies/image, any phone) or the **Pro** on-device engine (live — free per image, offline, but ~4.3 GB and 12 GB+ RAM flagship only). The Pro pack's ONNX components were each converted and validated end-to-end; INT8 quantization to reach normal 8 GB phones is the top roadmap item. All engines share the same studio-model gallery, backdrops, and outfit layering.

## Project layout

- `composeApp/` — Android app (Jetpack Compose, cinematic dark UI)
- `shared/` — Kotlin Multiplatform core: domain models, engine routing, pack manager, cloud client. Pure Kotlin `commonMain`, ready for the iOS port (`docs/IOS_PORT.md`)
- `ml/` — Python export tooling that produces the downloadable model packs (not shipped in the app)
- `supabase/` — Edge Functions for the cloud tier and AI-content reports ([deploy runbook](supabase/README.md))
- `docs/` — [**project status & handoff**](docs/PROJECT_STATUS.md) · [architecture](docs/ARCHITECTURE.md) · [on-device pipeline](docs/PIPELINE.md) · [cloud setup](docs/CLOUD_SETUP.md) · [Hugging Face packs](docs/HUGGINGFACE_SETUP.md) · [Play compliance](docs/PLAY_COMPLIANCE.md) · [privacy policy](docs/PRIVACY_POLICY.md) · [iOS port plan](docs/IOS_PORT.md)
- `ml/MODEL_LICENSES.md` — license status of every shipped model (read before publishing packs)

## Status at a glance

- **Cloud tier:** live (Supabase `lookbook-cloud` + Replicate; add Replicate billing for live runs).
- **On-device Pro pack:** converted, CPU-validated, and hosted at `Iamzakirzr/vestra-packs` (~4.3 GB, 12 GB+ RAM phones).
- **Next up:** INT8-quantize the Pro pack for 8 GB phones; real-device test pass; modest-wear-tuned cloud model. See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) §8.

## Building

Requires JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew :composeApp:assembleDebug   # build the app
./gradlew :shared:testDebugUnitTest   # run core unit tests
```

## License

GPL-3.0 — see [LICENSE](LICENSE).
