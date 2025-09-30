-- Table for org-scoped geofences (circles only)
create table if not exists geofence (
  id bigserial primary key,
  org_id bigint not null,

  name text not null,
  location_kind text not null check (location_kind in ('OFFICE','REMOTE')),
  site_code text,                       -- optional short code
  tz text,                              -- optional IANA TZ, nullable for now

  -- circle geometry
  center_lat double precision not null check (center_lat >= -90 and center_lat <= 90),
  center_lng double precision not null check (center_lng >= -180 and center_lng <= 180),
  radius_m integer not null check (radius_m >= 30),

  is_active boolean not null default true,

  created_by bigint,
  created_datetime timestamptz not null default now(),
  updated_by bigint,
  updated_datetime timestamptz
);

-- helpful indexes
create index if not exists idx_geofence_org_active on geofence(org_id, is_active);
create index if not exists idx_geofence_org_name on geofence(org_id, name);
