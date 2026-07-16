// AI-generated-content report intake (Google Play AI-Generated Content policy
// requires an in-app way to report offensive generations). The app queues
// reports offline and posts them here when online.
//
// Reports are stored in the `content_reports` table:
//   id uuid primary key default gen_random_uuid()
//   created_at timestamptz default now()
//   reason text not null
//   details text
//   engine_tier text
//   app_version text

import { createClient } from "jsr:@supabase/supabase-js@2";

const VALID_REASONS = new Set([
  "sexual_content",
  "violence",
  "likeness_misuse",
  "other",
]);

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return json({ error: "POST only" }, 405);
  }

  let body: {
    reason?: string;
    details?: string;
    engine_tier?: string;
    app_version?: string;
  };
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  if (!body.reason || !VALID_REASONS.has(body.reason)) {
    return json({ error: "reason must be one of " + [...VALID_REASONS].join(", ") }, 400);
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { error } = await supabase.from("content_reports").insert({
    reason: body.reason,
    details: (body.details ?? "").slice(0, 2000),
    engine_tier: body.engine_tier ?? null,
    app_version: body.app_version ?? null,
  });
  if (error) return json({ error: "Storage failed" }, 500);

  return json({ ok: true }, 201);
});

function json(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
