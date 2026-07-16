// Cloud-tier try-on: receives garment + person images, runs a hosted try-on
// model on Replicate, returns the result image. Inputs are deleted immediately
// after the run; nothing is retained server-side (Play data-safety commitment).
//
// Secrets (set via `supabase secrets set`):
//   REPLICATE_API_TOKEN — Replicate account token
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
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return json({ error: "POST only" }, 405);
  }

  const token = Deno.env.get("REPLICATE_API_TOKEN");
  if (!token) return json({ error: "Server not configured" }, 500);

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
        garment_des: "garment",
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
