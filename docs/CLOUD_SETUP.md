# Cloud engine setup — studio-quality generation

The Cloud tier is the path to photoreal, diffusion-quality output (the kind of
finished model image you compare against). It runs a hosted try-on model via a
Supabase Edge Function, so it needs a one-time backend setup and the user opting
into it per generation. Everything on-device stays offline; Cloud is used only
when explicitly selected.

## Why Cloud (vs the on-device Lite engine)

The Lite engine is a **compositor** — it cuts out a garment and warps it over a
model image. It cannot synthesize a person wearing clothing, so it will never
reach diffusion quality. The Cloud engine runs a real diffusion try-on model
(IDM-VTON class) that **generates** the result. For a photoreal bar, use Cloud
now; the offline Pro engine reaches the same bar once its weights are trained
(`ml/train/`).

## Setup (~15 min)

Follow `supabase/README.md` for the backend, in short:

1. Create a Supabase project; `supabase link --project-ref <ref>`
2. `supabase db push` (creates the reports table + transient image bucket)
3. `supabase secrets set REPLICATE_API_TOKEN=r8_...` (from replicate.com/account)
4. `supabase functions deploy tryon && supabase functions deploy report`

Then point the app at it — in
`composeApp/src/main/kotlin/com/zakir/vestra/VestraApp.kt`:

```kotlin
const val SUPABASE_URL = "https://<your-ref>.supabase.co"
const val SUPABASE_ANON_KEY = "<anon / publishable key>"
```

Rebuild. Settings → **Cloud** stops showing "Coming soon"; selecting it routes
generation through the Edge Function.

## What Cloud needs as input

The hosted model needs a **person image + a garment image**. So Cloud quality
still depends on a real person:

- Install a **studio-models pack** (your model photos — `docs/HUGGINGFACE_SETUP.md`),
  **or**
- Use **your own photo** as the person in a shoot.

The garment should be a flat-lay / hanger / catalog shot — not a photo of
someone already wearing it (the in-app input guard warns you when it detects
the latter).

## Costs & privacy

- ~$0.02–0.05 per generated image on Replicate; re-verify the specific model's
  commercial terms before a paid launch.
- Inputs are uploaded to a private, short-TTL bucket and deleted immediately
  after each run (success or failure). Uploads are re-encoded with EXIF
  stripped. See `docs/PRIVACY_POLICY.md`.
- Add per-IP rate limiting before public launch.
