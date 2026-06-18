alter table public.remote_config
add column if not exists consumer_home_enabled boolean not null default true,
add column if not exists founder_war_room_enabled boolean not null default true,
add column if not exists app_store_experience_gate_enabled boolean not null default true,
add column if not exists public_privacy_policy_url text not null default 'https://topshelfnz.com/stickme/privacy',
add column if not exists public_terms_url text not null default 'https://topshelfnz.com/stickme/terms',
add column if not exists public_support_url text not null default 'https://topshelfnz.com/stickme/support',
add column if not exists public_account_deletion_url text not null default 'https://topshelfnz.com/stickme/delete-account';

create table if not exists public.app_store_experience_gate_events (
  id uuid primary key default gen_random_uuid(),
  status text not null check (status in ('passed', 'blocked')),
  decision text not null check (decision in ('ready', 'block')),
  blocker_count integer not null default 0,
  warning_count integer not null default 0,
  blockers jsonb not null default '[]',
  warnings jsonb not null default '[]',
  required_checks jsonb not null default '{}',
  raw_summary jsonb not null default '{}',
  created_at timestamptz not null default now()
);

alter table public.app_store_experience_gate_events enable row level security;

do $$
begin
  if not exists (
    select 1 from pg_policies
    where schemaname = 'public'
      and tablename = 'app_store_experience_gate_events'
      and policyname = 'app_store_experience_gate_events_select_admin'
  ) then
    create policy "app_store_experience_gate_events_select_admin"
    on public.app_store_experience_gate_events for select
    to authenticated
    using (public.current_user_is_admin());
  end if;

  if not exists (
    select 1 from pg_policies
    where schemaname = 'public'
      and tablename = 'app_store_experience_gate_events'
      and policyname = 'app_store_experience_gate_events_insert_admin'
  ) then
    create policy "app_store_experience_gate_events_insert_admin"
    on public.app_store_experience_gate_events for insert
    to authenticated
    with check (public.current_user_is_admin());
  end if;
end $$;

create index if not exists app_store_experience_gate_events_created_at_idx
on public.app_store_experience_gate_events(created_at desc);

create or replace function public.get_stickme_app_store_experience_gate()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  config jsonb := '{}'::jsonb;
  latest_lock jsonb := '{}'::jsonb;
  latest_clarity jsonb := '{}'::jsonb;
  latest_boardroom jsonb := '{}'::jsonb;
  blockers jsonb := '[]'::jsonb;
  warnings jsonb := '[]'::jsonb;
  required_checks jsonb := '{}'::jsonb;
  status text := 'passed';
  decision text := 'ready';
  next_action text := 'AAA app-store experience is ready for preview and submission.';
  blocker_count integer := 0;
  warning_count integer := 0;
  privacy_url text := '';
  terms_url text := '';
  support_url text := '';
  deletion_url text := '';
begin
  if not public.current_user_is_admin() then
    raise exception 'Admin access required.' using errcode = '42501';
  end if;

  select coalesce(to_jsonb(row), '{}'::jsonb)
  into config
  from (
    select
      maintenance_mode,
      consumer_home_enabled,
      founder_war_room_enabled,
      app_store_experience_gate_enabled,
      production_launch_lock_enabled,
      store_review_mode_enabled,
      public_privacy_policy_url,
      public_terms_url,
      public_support_url,
      public_account_deletion_url
    from public.remote_config
    where id = 'global'
  ) row;

  select coalesce(raw_summary, '{}'::jsonb)
  into latest_lock
  from public.production_launch_lock_events
  order by created_at desc
  limit 1;

  select coalesce(raw_summary, '{}'::jsonb)
  into latest_clarity
  from public.aaa_clarity_audit_snapshots
  order by created_at desc
  limit 1;

  select coalesce(raw_summary, '{}'::jsonb)
  into latest_boardroom
  from public.boardroom_mode_snapshots
  order by created_at desc
  limit 1;

  privacy_url := coalesce(config ->> 'public_privacy_policy_url', '');
  terms_url := coalesce(config ->> 'public_terms_url', '');
  support_url := coalesce(config ->> 'public_support_url', '');
  deletion_url := coalesce(config ->> 'public_account_deletion_url', '');

  if coalesce((config ->> 'app_store_experience_gate_enabled')::boolean, true) = false then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'experience_gate_disabled', 'title', 'App Store Experience Gate is disabled', 'detail', 'Re-enable this gate before submission.'));
  end if;

  if coalesce((config ->> 'consumer_home_enabled')::boolean, true) = false then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'consumer_home_disabled', 'title', 'Consumer Home is disabled', 'detail', 'Users must land on a clean sticker-first home screen.'));
  end if;

  if coalesce((config ->> 'founder_war_room_enabled')::boolean, true) = false then
    warnings := warnings || jsonb_build_array(jsonb_build_object('code', 'founder_war_room_disabled', 'title', 'Founder War Room is disabled', 'detail', 'Admin operations are available elsewhere, but the cockpit is not active.'));
  end if;

  if coalesce((config ->> 'maintenance_mode')::boolean, false) then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'maintenance_mode_active', 'title', 'Maintenance Mode is active', 'detail', 'The app must not be in maintenance mode for submission.'));
  end if;

  if coalesce((config ->> 'store_review_mode_enabled')::boolean, false) then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'remote_store_review_mode_enabled', 'title', 'Remote Store Review Mode is enabled', 'detail', 'Production submission must not ship with remote review mode enabled.'));
  end if;

  if coalesce((config ->> 'production_launch_lock_enabled')::boolean, true) = false then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'production_launch_lock_disabled', 'title', 'Production Launch Lock is disabled', 'detail', 'Production release must remain hard-gated.'));
  end if;

  if privacy_url !~ '^https://.+' then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'privacy_policy_url_missing', 'title', 'Privacy Policy URL is missing', 'detail', 'A public HTTPS privacy policy URL is required.'));
  end if;

  if terms_url !~ '^https://.+' then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'terms_url_missing', 'title', 'Terms URL is missing', 'detail', 'A public HTTPS terms URL should be available from Settings.'));
  end if;

  if support_url !~ '^https://.+' then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'support_url_missing', 'title', 'Support URL is missing', 'detail', 'A public HTTPS support URL should be available for review and users.'));
  end if;

  if deletion_url !~ '^https://.+' then
    blockers := blockers || jsonb_build_array(jsonb_build_object('code', 'account_deletion_url_missing', 'title', 'Account deletion web URL is missing', 'detail', 'Google Play account deletion requires a public web deletion resource.'));
  end if;

  required_checks := jsonb_build_object(
    'consumerHomeEnabled', coalesce((config ->> 'consumer_home_enabled')::boolean, true),
    'founderWarRoomEnabled', coalesce((config ->> 'founder_war_room_enabled')::boolean, true),
    'maintenanceModeOff', coalesce((config ->> 'maintenance_mode')::boolean, false) = false,
    'remoteStoreReviewModeOff', coalesce((config ->> 'store_review_mode_enabled')::boolean, false) = false,
    'productionLaunchLockEnabled', coalesce((config ->> 'production_launch_lock_enabled')::boolean, true),
    'privacyPolicyUrl', privacy_url,
    'termsUrl', terms_url,
    'supportUrl', support_url,
    'accountDeletionUrl', deletion_url
  );

  blocker_count := jsonb_array_length(blockers);
  warning_count := jsonb_array_length(warnings);

  if blocker_count > 0 then
    status := 'blocked';
    decision := 'block';
    next_action := concat('Fix first: ', coalesce(blockers -> 0 ->> 'title', 'app-store experience blocker'), '.');
  end if;

  return jsonb_build_object(
    'checkedAt', now(),
    'status', status,
    'decision', decision,
    'nextAction', next_action,
    'blockerCount', blocker_count,
    'warningCount', warning_count,
    'blockers', blockers,
    'warnings', warnings,
    'requiredChecks', required_checks,
    'latestProductionLaunchLock', latest_lock,
    'latestClarityAudit', latest_clarity,
    'latestBoardroomSnapshot', latest_boardroom
  );
end;
$$;

grant execute on function public.get_stickme_app_store_experience_gate()
to authenticated;

create or replace function public.capture_stickme_app_store_experience_gate()
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  summary jsonb;
  event_id uuid;
begin
  if not public.current_user_is_admin() then
    raise exception 'Admin access required.' using errcode = '42501';
  end if;

  summary := public.get_stickme_app_store_experience_gate();

  insert into public.app_store_experience_gate_events (
    status,
    decision,
    blocker_count,
    warning_count,
    blockers,
    warnings,
    required_checks,
    raw_summary
  )
  values (
    summary ->> 'status',
    summary ->> 'decision',
    coalesce((summary ->> 'blockerCount')::integer, 0),
    coalesce((summary ->> 'warningCount')::integer, 0),
    coalesce(summary -> 'blockers', '[]'::jsonb),
    coalesce(summary -> 'warnings', '[]'::jsonb),
    coalesce(summary -> 'requiredChecks', '{}'::jsonb),
    summary
  )
  returning id into event_id;

  return event_id;
end;
$$;

grant execute on function public.capture_stickme_app_store_experience_gate()
to authenticated;
