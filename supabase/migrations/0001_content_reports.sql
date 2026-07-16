-- AI-content report intake (see functions/report/index.ts).
create table if not exists public.content_reports (
    id uuid primary key default gen_random_uuid(),
    created_at timestamptz not null default now(),
    reason text not null,
    details text,
    engine_tier text,
    app_version text
);

-- Only the service role (Edge Functions) writes or reads reports.
alter table public.content_reports enable row level security;

-- Transient storage for cloud try-on inputs; objects are deleted by the
-- function after each run, and this policy-free bucket is service-role only.
insert into storage.buckets (id, name, public)
values ('tryon-transient', 'tryon-transient', false)
on conflict (id) do nothing;
