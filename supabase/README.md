# Vestra cloud tier — Supabase backend

Two Edge Functions power the optional Cloud tier and the AI-content report
intake. Everything here deploys in ~10 minutes.

## One-time setup

```bash
# 1. Create a project (dashboard or CLI) and link it
supabase link --project-ref <PROJECT_REF>

# 2. Apply the migration (content_reports table + transient storage bucket)
supabase db push

# 3. Set the Replicate token (server-side only — never ships in the app)
supabase secrets set REPLICATE_API_TOKEN=r8_...

# 4. Deploy the functions
supabase functions deploy tryon
supabase functions deploy report
```

## Wire the app

Fill the two constants in
`composeApp/src/main/kotlin/com/zakir/vestra/VestraApp.kt`:

```kotlin
const val SUPABASE_URL = "https://<PROJECT_REF>.supabase.co"
const val SUPABASE_ANON_KEY = "<anon/publishable key>"
```

The anon key is publishable by design; the Replicate token and service-role
key exist only inside the functions. Until these constants are set, the app
shows the Cloud tier as "Coming soon" and never attempts a request.

## Privacy contract (mirrors docs/PLAY_COMPLIANCE.md)

- Inputs are uploaded to the private `tryon-transient` bucket, passed to
  Replicate via short-lived signed URLs, and deleted in a `finally` block —
  success or failure.
- The app re-encodes uploads (EXIF stripped, ≤1536 px) before sending.
- No account, no persistent user identifier; reports are anonymous.

## Costs & limits

- Model: `cuuupid/idm-vton` on Replicate (pin `REPLICATE_MODEL_VERSION` to
  taste) — roughly $0.02–0.05/run. Replicate's hosted-model terms cover
  commercial API use — but re-verify the specific model's terms before launch.
- Add rate limiting before public launch (e.g. Upstash Redis on the function,
  or Supabase's built-in per-IP function limits) — tracked in PLAY_COMPLIANCE.
