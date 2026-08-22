# Changelog — The Lookbook

## 3.0.4
- **Quality pack integration:** Real-ESRGAN runner feeds FP16 `input` + `denoise_strength` (was silent no-op via single float32 OrtModel)
- BiRefNet matte applies **sigmoid** on logits before resize (was min–max normalize)
- Integrity verify smoke-runs Real-ESRGAN; catalog sizes corrected (~224 MB / ~5 MB)
- `realesrgan-v1` minRam gate lowered to 2 GB in export metadata; integration script smokes both quality packs
- **Local model crash hardening:** pack in-use refcount; block uninstall/update while generating; invalidate ORT session cache before pack file replace; rethrow cancel; soft-fail quality OOM; harden OrtModel output bounds; BackdropCompositor shares session cache
- **Stable release plan:** `docs/plans/stable-release/` — R0 (this cut) vs R1 perfect (offline Create Studio, pro-v2-int8 HF, live health UI)
- Pro unavailable copy prefers **pro-v1** (matches HF manifest); docs clarify **minSdk 35 / Android 15+**

## 3.0.3
- Published **birefnet-v1** (~224 MB) and **realesrgan-v1** (~5 MB) to `Iamzakirzr/vestra-packs` manifest
- Download from **Settings → Model packs**; matte refine + upscale activate when installed
- `scripts/build-and-publish-quality-packs.py` for future quality-pack releases

## 3.0.2
- **Generation stability M2–M6 (remaining):** global image deadline (120s) with remaining-time stage text; Gradio wakeRetries=1 + budget-derived maxPolls
- **M3:** `GradioSchemaClient` live `/info` payloads; removed guessing 1-arg Space fallbacks; HF discovery only for known Inference routes
- **M5:** `visual-verify.sh --compare`, `compare-screenshots.py`, `verify-all-models.sh`, `e2e-matrix.sh`
- **M6:** `EpochClock` replaces `System.currentTimeMillis` in commonMain; `DiagnosticsHook` per-run handles (no concurrent clobber); stop silent Space→Inference rewrite on token save
- Catalog: `local-sdturbo-v1` reserved; BiRefNet/Real-ESRGAN marked downloadable when packs ship; `ml/export_image_gen_pack.py` scaffold

## 3.0.1
- **Generation stability (Claude plan M1/M2):** `CloudFailure` typed errors; image fallback chain correctly advances models (fixes root-cause `continue` bug); per-candidate preflight inside loop; `ModelHealthTracker` with exponential cooldown; stronger `CloudOutputValidator` (1 KB min + dimension check); video no longer hard-requires HF Space; 402 skips remaining Inference candidates
- Removed duplicate `deepseek-r1-free-or` catalog entry (migration to `openrouter-free`)
- Unit tests: `CloudFailureTest`, updated `GenerativeCloudServiceTest` fixtures

## 3.0.0
- Image edit fallback no longer hits broken InstructPix2Pix HF Inference (nscale HTTP 400)
- Qwen Image Edit → InstructPix2Pix Space chain; migrate stale inference edit selection
- DNS / offline errors map to friendly "No internet" instead of raw host resolution text
- FLUX Space failures suggest HF Inference fallback when token is configured
- Usage ledger failures prefix selected model when fallback chain exhausts
- Google Gemma 3 local LLM documented as feasible via LiteRT-LM (catalog placeholder)

## 2.9.14
- Quality plan: `QualityRating` maps catalog scores to 1–5★ (5★ = READY + score ≥ 90)
- Cloud downloads validated (reject empty/corrupt images and videos; retry fallback chain)
- News chat uses the same LLM fallback chain as Code studio (Groq → OpenRouter → HF)
- Bypass filter assist on by default for Image/Video (fewer false safety blocks)
- Lite try-on applies BiRefNet matte refinement when `birefnet-v1` pack is installed
- Human parse uses declared 512×512 input; model picker shows star rating + sorts by quality
- Saving HF token migrates image gen to FLUX Inference when Space defaults were selected

## 2.9.3
- Model fallback chains for video, cloud try-on, and code (tries the next free model when one is busy or missing a key)
- LTX-Video payload aligned to live Space schema (null image fields, 704×512, 2s / CFG 1)
- InstructPix2Pix uses 8 steps to fit free ZeroGPU seconds
- OpenRouter free models: read `reasoning` when `content` is null
- Model picker lists Ready models first

## 2.9.2
- Fixed the biggest cause of failed cloud generation: once a Hugging Face
  account's daily ZeroGPU allowance is spent, HF rejects every Space call that
  carries the token instantly with an empty `event: error` / `data: null`, even
  though the same request still runs anonymously. Space calls now retry without
  the token, so image generation keeps working after the allowance runs out
- Explain empty Gradio errors as a likely spent ZeroGPU allowance rather than an
  unexplained failure
- Point Qwen Image Edit at a distilled mirror of the Space: the official one
  rejects every REST call outright, and 8 steps instead of 50 fits the free
  allowance
- Show the bundled Lite pack as installed as soon as it finishes seeding,
  instead of only after the next app launch

## 2.9.1
- Fixed image generation and editing against live Hugging Face Spaces: image
  arguments are now sent as Gradio `FileData` objects, so Qwen Image Edit and
  InstructPix2Pix no longer fail validation with an empty `event: error` /
  `data: null` response
- Support Spaces on Gradio 4 (`/call`) as well as Gradio 5 (`/gradio_api/call`)
- Read the result image from anywhere in a Space's output, fixing
  InstructPix2Pix (image is the 4th output) and OOTDiffusion (gallery)
- Retry Spaces that are waking or restarting, then fall back to another free
  Space when the selected one is out of ZeroGPU quota
- Report out-of-quota and rate-limited Spaces in plain language instead of raw
  Gradio errors
- Only Hugging Face Spaces can serve try-on, image and video; a stored HF
  Inference model is migrated to a curated Space and the correction is saved
- Default try-on is now OOTDiffusion (verified end-to-end); IDM-VTON, CatVTON
  and SDXL Lightning are marked degraded after live failures
- Settings names the Lite pack as the reason Pro try-on is unavailable

## 2.9.0
- Home: “What would you like to do” action list first; Core Try-on centered below
- Image / Video / Code studios: searchable in-composer model picker (name search)
- Local Lite/Pro always selectable; selecting a pack sets the matching engine tier
- HF: clearer Gradio empty-error messages; default image edit → Qwen; InstructPix2Pix marked degraded
- Stop listing warm HF Inference image models that cannot run via Spaces

## 2.8.0
- Looks gallery: tap opens look detail; delete confirmation; favorite a11y labels
- Video studio results ingest into Looks gallery
- In-app Privacy Policy screen (offline) + Settings About link
- Export local content reports from Settings → Storage & privacy
- Help: search semantics, email support CTA, privacy/report FAQ topics
- Cloud usage empty state → Open Image studio
- Try-on result: favorite + open gallery
- Atelier home respects reduced motion
- Deep-link visual verification (`lookbook://screen/*`, `scripts/visual-verify.sh`)

## 2.7.7
- Saffron FilterChips (no Material purple selected state)
- About + Privacy moved to top of Settings

## 2.7.3–2.7.6
- Cancel / Back recovery for try-on and cloud studios
- Gallery empty CTAs, Report/Share on cloud results
- Model chip → Settings; preflight Open Settings
- Composer/home a11y; deep-link screencap tooling
