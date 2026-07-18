# Vestra cloud tier — Supabase backend

Two Edge Functions power the optional Cloud tier and the AI-content report
intake. Everything here deploys in ~10 minutes.

> **Provisioned project:** `lookbook-cloud` (ref `todzunpexvvmbxpvdyap`,
> region ap-south-1) is already live with both functions deployed, the
> migrations applied, and the Replicate token stored in Vault. `VestraApp.kt`
> is wired to it — the Cloud tier works out of the box. The steps below
> document how to reproduce or re-provision it.

## One-time setup

```bash
# 1. Create a project (dashboard or CLI) and link it
supabase link --project-ref <PROJECT_REF>

# 2. Apply the migrations (content_reports table + transient bucket +
#    Vault accessor for the Replicate token)
supabase db push

# 3a. Provide the Replicate token — preferred: an Edge Function env secret
supabase secrets set REPLICATE_API_TOKEN=r8_...

# 3b. …or, when no CLI/dashboard step is available, store it in Vault (the
#     tryon function falls back to reading it through the service-role-only
#     public.get_replicate_token() accessor from migration 0002):
#       select vault.create_secret('r8_...', 'replicate_api_token', 'Replicate token');

# 4. Deploy the functions
supabase functions deploy tryon
supabase functions deploy report
```

The `tryon` function resolves the token env-first, then Vault — so either
path in step 3 works with no code change.

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
