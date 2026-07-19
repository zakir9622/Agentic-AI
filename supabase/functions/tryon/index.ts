// Cloud-tier try-on: receives garment + person images, runs a hosted try-on
// model on Replicate, returns the result image. Inputs are deleted immediately
// after the run; nothing is retained server-side (Play data-safety commitment).
//
// Replicate token resolution (in order):
//   1. REPLICATE_API_TOKEN env secret (set via `supabase secrets set`), or
//   2. Supabase Vault, read through the service-role-only
//      public.get_replicate_token() accessor (see migration 0002). This lets
//      the token be provisioned entirely over the management API when no
//      dashboard/CLI step is available.
// Optional env:
//   REPLICATE_MODEL_VERSION — pinned version id of the try-on model

import { createClient } from "jsr:@supabase/supabase-js@2";

const REPLICATE_API = "https://api.replicate.com/v1";
// cuuupid/idm-vton — hosted IDM-VTON; pin a version in env for reproducibility.
const DEFAULT_MODEL_VERSION =
  "c871bb9b046607b680449ecbae55fd8c6d945e0a1948644bf2361b3d021d3ff4";

const BUCKET = "tryon-transient";
const MAX_IMAGE_BYTES = 8 * 1024 * 1024;
const POLL_INTERVAL_MS = 2000;
const TIMEOUT_MS = 120_000;

interface TryOnBody {
  garment_b64: string;
  person_b64: string;
  category?: "upper_body" | "lower_body" | "dresses";
  // Photorealism guidance from PromptStyle (on-device parity).
  positive?: string;
  negative?: string;
  cfg_scale?: number;
  steps?: number;
  // Backend selector for the free HF ZeroGPU Space (ignored by Replicate):
  //   "clean"  → commercially-clean SD+ControlNet+IP-Adapter (default)
  //   "idmvton"→ max-fidelity IDM-VTON (preview/personal; license caveat)
  model?: "clean" | "idmvton";
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return json({ error: "POST only" }, 405);
  }

  let body: TryOnBody;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }
  if (!body.garment_b64 || !body.person_b64) {
    return json({ error: "garment_b64 and person_b64 are required" }, 400);
  }

  const garment = decodeBase64(body.garment_b64);
  const person = decodeBase64(body.person_b64);
  if (!garment || !person) return json({ error: "Invalid base64 image" }, 400);
  if (garment.length > MAX_IMAGE_BYTES || person.length > MAX_IMAGE_BYTES) {
    return json({ error: "Image too large (8 MB max)" }, 413);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  // Free path first — a Modal serverless-GPU endpoint if configured (plain JSON
  // → JPEG). Falls through to HF / Replicate on any failure. Unset = unchanged.
  const modalUrl = Deno.env.get("MODAL_URL");
  if (modalUrl) {
    try {
      const image = await callModal(modalUrl, Deno.env.get("MODAL_KEY"), body);
      return new Response(image, {
        headers: { "Content-Type": "image/jpeg", "X-Backend": "modal" },
      });
    } catch (err) {
      console.error("Modal failed; falling back:", err);
    }
  }

  // Next: a Hugging Face ZeroGPU Space if configured (no per-image cost within
  // quota). Falls back to Replicate on any failure. When HF_SPACE_URL is unset
  // the behaviour is identical to before (Replicate only).
  const hfSpace = Deno.env.get("HF_SPACE_URL");
  if (hfSpace) {
    try {
      const image = await callHfSpace(hfSpace, Deno.env.get("HF_SPACE_TOKEN"), body);
      return new Response(image, {
        headers: { "Content-Type": "image/jpeg", "X-Backend": "hf-zerogpu" },
      });
    } catch (err) {
      console.error("HF Space failed; falling back to Replicate:", err);
    }
  }

  const token = await resolveReplicateToken(supabase);
  if (!token) return json({ error: "Server not configured" }, 500);

  const jobId = crypto.randomUUID();
  const garmentPath = `${jobId}/garment.jpg`;
  const personPath = `${jobId}/person.jpg`;

  try {
    for (const [path, bytes] of [[garmentPath, garment], [personPath, person]] as const) {
      const { error } = await supabase.storage
        .from(BUCKET)
        .upload(path, bytes, { contentType: "image/jpeg" });
      if (error) throw new Error(`upload failed: ${error.message}`);
    }

    const signed = async (path: string) => {
      const { data, error } = await supabase.storage
        .from(BUCKET)
        .createSignedUrl(path, 600);
      if (error || !data) throw new Error("sign failed");
      return data.signedUrl;
    };

    const prediction = await replicate(token, "POST", "/predictions", {
      version: Deno.env.get("REPLICATE_MODEL_VERSION") ?? DEFAULT_MODEL_VERSION,
      input: {
        garm_img: await signed(garmentPath),
        human_img: await signed(personPath),
        category: body.category ?? "upper_body",
        garment_des: body.positive ?? "garment",
        negative_prompt: body.negative,
        // IDM-VTON exposes these; other models ignore unknown keys.
        guidance_scale: body.cfg_scale ?? 2.0,
        num_inference_steps: body.steps ?? 30,
      },
    });

    const started = Date.now();
    let result = prediction;
    while (result.status === "starting" || result.status === "processing") {
      if (Date.now() - started > TIMEOUT_MS) throw new Error("Generation timed out");
      await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
      result = await replicate(token, "GET", `/predictions/${prediction.id}`);
    }
    if (result.status !== "succeeded") {
      throw new Error(`Generation failed: ${result.error ?? result.status}`);
    }

    const outputUrl: string = Array.isArray(result.output)
      ? result.output[0]
      : result.output;
    const image = await (await fetch(outputUrl)).arrayBuffer();

    return new Response(image, {
      headers: { "Content-Type": "image/jpeg", "X-Job-Id": jobId },
    });
  } catch (err) {
    return json({ error: String(err instanceof Error ? err.message : err) }, 502);
  } finally {
    // Transient by contract: inputs are removed even when generation fails.
    await supabase.storage.from(BUCKET).remove([garmentPath, personPath]);
  }
});

// Resolve the Replicate token from the env secret, falling back to the
// Vault-backed accessor (service-role only). Returns null if neither is set.
async function resolveReplicateToken(
  supabase: ReturnType<typeof createClient>,
): Promise<string | null> {
  const fromEnv = Deno.env.get("REPLICATE_API_TOKEN");
  if (fromEnv) return fromEnv;
  const { data, error } = await supabase.rpc("get_replicate_token");
  if (error || typeof data !== "string" || data.length === 0) return null;
  return data;
}

// Call a Modal serverless-GPU endpoint: POST JSON, receive JPEG bytes.
async function callModal(
  url: string,
  key: string | undefined,
  body: TryOnBody,
): Promise<Uint8Array> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      person_b64: body.person_b64,
      garment_b64: body.garment_b64,
      model: body.model ?? "clean",
      seed: 42,
      ...(key ? { api_key: key } : {}),
    }),
  });
  if (!res.ok) throw new Error(`Modal ${res.status}: ${(await res.text()).slice(0, 120)}`);
  return new Uint8Array(await res.arrayBuffer());
}

// Call a Hugging Face Gradio (ZeroGPU) Space's /tryon endpoint. Images are sent
// as base64 data-URI FileData; the SSE result carries the output image URL.
// NOTE: validate against your Space on first deploy — Gradio's FileData shape can
// vary by version. See docs/CLOUD_ZEROGPU.md.
async function callHfSpace(
  base: string,
  hfToken: string | undefined,
  body: TryOnBody,
): Promise<Uint8Array> {
  const root = base.replace(/\/$/, "");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (hfToken) headers["Authorization"] = `Bearer ${hfToken}`;

  const fileData = (b64: string) => ({
    url: `data:image/jpeg;base64,${b64}`,
    meta: { _type: "gradio.FileData" },
  });
  const payload = {
    data: [
      fileData(body.person_b64),
      fileData(body.garment_b64),
      body.model === "idmvton" ? "idmvton" : "clean",
      true, // upscale 2×
      42, // seed
    ],
  };

  const post = await fetch(`${root}/gradio_api/call/tryon`, {
    method: "POST", headers, body: JSON.stringify(payload),
  });
  if (!post.ok) throw new Error(`HF call POST ${post.status}`);
  const { event_id } = await post.json();
  if (!event_id) throw new Error("HF call: no event_id");

  const stream = await fetch(`${root}/gradio_api/call/tryon/${event_id}`, { headers });
  if (!stream.ok) throw new Error(`HF stream ${stream.status}`);
  const text = await stream.text();

  // SSE: find the "data:" line following an "event: complete" marker.
  const complete = text.split("\n").find((l) => l.startsWith("data:") && l.includes("url"));
  if (!complete) throw new Error(`HF: no result in stream: ${text.slice(0, 200)}`);
  const parsed = JSON.parse(complete.slice(5).trim());
  const out = Array.isArray(parsed) ? parsed[0] : parsed;
  const imageUrl: string = typeof out === "string" ? out : out?.url;
  if (!imageUrl) throw new Error("HF: result had no image url");

  const imgRes = await fetch(imageUrl, { headers });
  if (!imgRes.ok) throw new Error(`HF image fetch ${imgRes.status}`);
  return new Uint8Array(await imgRes.arrayBuffer());
}

async function replicate(
  token: string,
  method: "GET" | "POST",
  path: string,
  body?: unknown,
) {
  const res = await fetch(`${REPLICATE_API}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`Replicate ${path}: HTTP ${res.status}`);
  return await res.json();
}

function decodeBase64(value: string): Uint8Array | null {
  try {
    return Uint8Array.from(atob(value), (c) => c.charCodeAt(0));
  } catch {
    return null;
  }
}

function json(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
