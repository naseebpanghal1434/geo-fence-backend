create table if not exists fence_assignment (
  id bigserial primary key,
  org_id bigint not null,
  fence_id bigint not null,
  entity_type_id integer not null check (entity_type_id in (1,2,4,5)),
  entity_id bigint not null,
  is_default boolean not null default false,

  created_by bigint,
  created_datetime timestamptz not null default now(),
  updated_by bigint,
  updated_datetime timestamptz
);

-- one row per (entity, fence)
create unique index if not exists uq_fence_assignment_entity_fence
  on fence_assignment(org_id, entity_type_id, entity_id, fence_id);

-- allow only one default fence per entity within org
create unique index if not exists uq_fence_assignment_entity_default
  on fence_assignment(org_id, entity_type_id, entity_id)
  where is_default = true;

-- helpful queries
create index if not exists idx_fence_assignment_fence on fence_assignment(org_id, fence_id);
create index if not exists idx_fence_assignment_entity on fence_assignment(org_id, entity_type_id, entity_id);
