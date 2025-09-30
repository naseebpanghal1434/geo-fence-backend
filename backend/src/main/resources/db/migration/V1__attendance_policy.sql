-- Table for org-scoped geo-fencing/attendance policy
create table if not exists attendance_policy (
  id bigserial primary key,
  org_id bigint not null,

  -- activation & posture
  is_active boolean not null default false,
  outside_fence_policy text not null check (outside_fence_policy in ('BLOCK','WARN')),
  integrity_posture    text not null check (integrity_posture    in ('WARN','BLOCK')),

  -- windows (minutes; relative to office start/end)
  allow_checkin_before_start_min integer not null default 20  check (allow_checkin_before_start_min >= 0),
  late_checkin_after_start_min   integer not null default 30  check (late_checkin_after_start_min   >= 0),
  allow_checkout_before_end_min  integer not null default 15  check (allow_checkout_before_end_min  >= 0),
  max_checkout_after_end_min     integer not null default 60  check (max_checkout_after_end_min     >= 0),
  notify_before_shift_start_min  integer not null default 10  check (notify_before_shift_start_min  >= 0),

  -- geofence & accuracy
  fence_radius_m integer not null default 150 check (fence_radius_m >= 30),
  accuracy_gate_m integer not null default 80 check (accuracy_gate_m >= 10),

  -- punch limits & cooldowns
  cooldown_seconds integer not null default 120 check (cooldown_seconds >= 0),
  max_successful_punches_per_day integer not null default 6 check (max_successful_punches_per_day >= 0),
  max_failed_punches_per_day     integer not null default 3 check (max_failed_punches_per_day     >= 0),

  -- working hours (soft guard)
  max_working_hours_per_day integer not null default 10 check (max_working_hours_per_day >= 0),

  -- legacy placeholders (persist, unused now)
  dwell_in_min    integer not null default 3 check (dwell_in_min    >= 0),
  dwell_out_min   integer not null default 5 check (dwell_out_min   >= 0),
  auto_out_enabled boolean not null default false,
  auto_out_delay_min integer not null default 5 check (auto_out_delay_min >= 0),
  undo_window_min    integer not null default 5 check (undo_window_min    >= 0),

  -- audit
  created_by bigint,
  created_datetime timestamptz not null default now(),
  updated_by bigint,
  updated_datetime timestamptz
);

-- one policy per org; supports idempotent create
create unique index if not exists uq_attendance_policy_org on attendance_policy(org_id);
