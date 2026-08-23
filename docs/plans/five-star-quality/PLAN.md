# Five-star quality — iterative release plan

**Status:** Active · baseline **v3.1.0-rc9** on `main`  
**Branch convention:** readable kebab names only (e.g. `cursor/five-star-quality-…`). Avoid random hex soup in the descriptive part.  
**Goal:** Ship a sideload APK that feels production-trustworthy — honest local/cloud, no cross-tab contamination, crash-hard native paths soft-failed, UI that earns a 5★ mental model.

---

## What is already on `main` (do not lose)

| Train | Tip | Contents |
|-------|-----|----------|
| True local | rc7–rc9 | SD-Turbo Create/Edit, still-clip Video, Gemma Code, system TTS, handshake |
| Cloud reliability | rc6 | ORT R8 keep, FLUX default, audio budget, credit copy |
| Release policy | PR #54 | APK **only** on `main` merge/push; old releases pruned |

Canonical download: GitHub Release tag **`latest`**.

---

## Audit snapshot (2026-08-23)

### P0 — must fix before 5★

| ID | Issue | Area |
|----|-------|------|
| U1 | Shared `GenerativeViewModel` wiped by adjacent pager tabs (`prepareStudio`) | UI |
| U2 | Wrong studio shows another tab’s result | UI |
| S1 | Local image/code/video never `markPackInUse` | Stability |
| S2 | `OrtGraph` lacks safe session + output size caps | Stability |
| S3 | Still-clip MediaCodec missing PTS → unplayable MP4s | Local video |
| S4 | Abrupt-exit logcat scrape no-op on main thread | Diagnostics |

### P1 — reliability / honesty

| ID | Issue |
|----|-------|
| U3 | ON-DEVICE picker rows not selectable |
| U4 | Help / product blurb still cloud-only |
| U5 | Handshake dumps `HANDSHAKE_OK` machine strings |
| U6 | Local always wins over explicit cloud model pick |
| S5 | Code/video abort whole chain on missing key (image skips) |
| S6 | MediaPipe Gemma no timeout |
| U7 | Gallery treats video paths as stills |

### P2 — polish (later cycles)

Glass-card density, hero height, sampler fields only when supported, hide non-runnable from studio picker, per-pack handshake busy, audio Cancel in ResultPane.

---

## Release trains

| Train | Version | Scope | Gate |
|-------|---------|-------|------|
| **Q1** | **3.1.0-rc10** | Studio isolation · pack-in-use · OrtGraph soft · MediaCodec PTS · honesty copy · human handshake · honor cloud pick when online | Unit + CI |
| **Q2** | 3.1.0-rc11 | Selectable local picker · MediaPipe timeout · gallery media types · CrashReporter scrape · code/video key skip | Unit + CI |
| **Q3** | 3.1.0-rc12 → **rc13** → **3.1.0** | Device matrix · Pro graph probe · sticky incompat · picker honesty | Unit + CI + manual matrix |
| **Q4** | 3.1.x | UI declutter · accesslint · visual baselines | Device evidence |

---

## Iterative cycle protocol

Each cycle:

1. **Pick** ≤6 P0/P1 items from the scorecard  
2. **Fix** with tests where JVM-possible  
3. **Matrix** (at least document; device when available):

| Surface | Offline pack ready | Offline pack missing | Online prefer cloud | Online local ready |
|---------|--------------------|----------------------|---------------------|--------------------|
| Image Create | local PNG | fail soft / CTA packs | selected cloud | local unless cloud selected |
| Image Edit | img2img | CTA | cloud | local if edit-ready |
| Video | still-clip | cloud or CTA | cloud | still-clip unless cloud |
| Code | Gemma | cloud | cloud | Gemma unless cloud |
| Audio | system TTS | — | TTS first | TTS first |
| Try-on | Lite/Pro | CTA | N/A | N/A |

4. **Score** 1–5 on: Stability · Honesty · Clarity · Offline · Cloud fallback  
5. **Stop** a cycle when average ≥4.5 or open a new cycle for remaining gaps  
6. **Merge to `main`** only when CI green → triggers Release APK `latest`

### Star rating rubric

| Stars | Meaning |
|-------|---------|
| 1 | Crashes / wrong studio results / lying copy |
| 2 | Works sometimes; confusing packs |
| 3 | Reliable online; offline half-broken |
| 4 | Honest local+cloud; rare soft fails |
| 5 | Feels intentional; failures teach next action; no cross-tab ghosts |

---

## Q1 checklist (shipped · rc10)

- [x] U1/U2 — per-studio generative session (no adjacent wipe)
- [x] S1 — `markPackInUse` on local image/code
- [x] S2 — `OrtGraph` safe session + output cap
- [x] S3 — MediaCodec PTS on still-clip
- [x] U4 — Help + product blurb true-local
- [x] U5 — Human handshake labels
- [x] U6 — Prefer local only when offline; honor cloud when online
- [x] Tests for handshake label + offline local routing
- [x] Version **3.1.0-rc10**

---

## Q2 checklist (shipped · rc11 · PR #56)

- [x] U3 — Selectable ON-DEVICE picker rows (`setLocalGenerator` / `prefersLocal`)
- [x] S6 — MediaPipe Gemma generate timeout (90s)
- [x] U7 — Gallery/wardrobe video frame thumbs (`MediaThumb`)
- [x] S4 — CrashReporter abrupt logcat scrape off main thread
- [x] S5 — Code/video fallback skips missing-key candidates (like Image)
- [x] Pro ORT FP16 / ControlNet soft-fail + AUTO→Lite (device diagnostics)
- [x] Version **3.1.0-rc11**

---

## Q3 checklist (active · rc13)

- [ ] Run [DEVICE_MATRIX.md](./DEVICE_MATRIX.md) on Pixel 8/9 with `latest` APK
- [x] Automated routing matrix tests (offline + online `prefersLocal`)
- [x] Hide non-runnable scaffolds from studio ON-DEVICE picker
- [x] Audio ResultPane Cancel during generation
- [x] Pro sticky graph-incompat (`markGraphIncompatible`) + handshake UNet probe
- [x] Skip legacy Pro after ControlNet ORT incompat (AUTO→Lite one shot)
- [ ] Scorecard ≥4.5 average → tag **3.1.0** (or open Q4 for polish gaps)
- [ ] Confirm Pro AUTO→Lite on device with installed `pro-v1`

**Interim builds:** rc12 polish · **rc13** Pro graph probe.

---

## Non-goals (Q1)

- Full accesslint device sweep  
- New visual design system (Loom Ink stays)  
- Diffusion video on-device  
- Larger than Gemma 1B on-device  

---

## Branch naming (going forward)

Use readable kebab-case:

```
cursor/five-star-quality-367c
cursor/studio-session-fix-367c
cursor/pack-handshake-polish-367c
```

Avoid opaque hashes in the *descriptive* segment. (Cursor cloud agents may append a short workspace suffix — keep the words clear.)
