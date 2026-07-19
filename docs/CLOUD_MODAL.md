# Free Cloud tier on Modal (serverless GPU, free credits)

Run the Cloud engine at **no fixed cost** on Modal. Modal's Starter tier includes
monthly credits (no card required to start), gives real GPUs, and scales to zero
when idle — so it fits "no subscription" better than HF PRO. The `tryon` Edge
Function uses Modal when `MODAL_URL` is set and falls back to HF/Replicate
otherwise, so wiring it up never breaks the live path.

## Why one step is yours
Modal's CLI/deploy talks to its control plane over **gRPC**, which the agent
sandbox's egress proxy cannot carry. So the single `modal deploy` command has to
run on **your** machine. Everything else (the app code, the Edge Function wiring,
validation) is done for you.

## Deploy (on your machine — ~2 min, free)

```bash
pip install modal
modal token new                      # opens a browser to log in (free signup)
#   …or reuse your token:
#   modal token set --token-id ak-… --token-secret as-…

# (recommended) lock the endpoint with a shared key:
modal secret create lookbook-api LOOKBOOK_API_KEY=$(openssl rand -hex 16)
#   note the key you set — you'll give it to the Edge Function as MODAL_KEY.

modal deploy deploy/modal_tryon.py
```

`modal deploy` prints an endpoint URL ending in `/generate`, e.g.
`https://zakir9622--lookbook-tryon-tryon-generate.modal.run`.

## Wire the backend
Give me that URL (and the key) and **I'll set the Edge Function secrets and
validate**, or do it yourself in Supabase → Edge Functions → Secrets:

```
MODAL_URL = https://<your-workspace>--lookbook-tryon-tryon-generate.modal.run
MODAL_KEY = <the LOOKBOOK_API_KEY you set>   # omit if you skipped the secret
```

That's it — the Cloud tier now runs on Modal (commercially-clean `clean` engine),
with Replicate as automatic fallback.

## Notes
- **First request is slow** (cold start downloads ~5 GB of weights into a cached
  Volume); subsequent requests are fast while warm, then scale to zero.
- **Engine:** Modal serves the commercially-safe `clean` pipeline (SD1.5 +
  ControlNet-Depth + IP-Adapter-Plus). For the max-fidelity `idmvton` engine use
  the HF ZeroGPU path (`docs/CLOUD_ZEROGPU.md`) — it's a heavier model.
- **Cost:** GPU time draws from Modal's free monthly credits, then pay-as-you-go.
  No fixed subscription. The on-device Pro pack remains the true-$0 path.
- **Security:** set the `lookbook-api` secret so the endpoint isn't open to the
  internet (it would otherwise burn your credits). The code enforces the key only
  when `LOOKBOOK_API_KEY` is present.
- **Validate:** I can't run the GPU code from here; the first real call may need a
  one-line tweak. The Replicate fallback keeps the tier working meanwhile.
