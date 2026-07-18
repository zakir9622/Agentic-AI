-- Vault-backed accessor so the tryon Edge Function can read the Replicate
-- token without a dashboard-set env secret (no management API/CLI step is
-- available in the provisioning environment).
--
-- The token value itself is NOT stored here — it is written once via
--   select vault.create_secret('<token>', 'replicate_api_token', '...');
-- so it never lands in migration history. This function only reads it, and
-- only the service_role (i.e. the Edge Functions) may execute it.
create extension if not exists supabase_vault with schema vault;

create or replace function public.get_replicate_token()
returns text
language sql
security definer
set search_path = ''
as $$
  select decrypted_secret
  from vault.decrypted_secrets
  where name = 'replicate_api_token'
  limit 1;
$$;

revoke all on function public.get_replicate_token() from public, anon, authenticated;
grant execute on function public.get_replicate_token() to service_role;
