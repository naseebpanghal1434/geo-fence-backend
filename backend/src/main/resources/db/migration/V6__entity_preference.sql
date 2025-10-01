-- Migration: Create entity_preference table for geo-fencing feature access control
-- Description: Stores feature preferences for entities (user, org, team, project)
--              to control access to geo-fencing features

CREATE TABLE entity_preference (
    entity_preference_id BIGSERIAL PRIMARY KEY,
    entity_type_id INTEGER NOT NULL,
    entity_id BIGINT NOT NULL,
    is_geofencing_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    is_geofencing_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    created_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_datetime TIMESTAMP,
    CONSTRAINT uq_entity_preference_entity UNIQUE (entity_type_id, entity_id)
);

-- Create index for faster lookups by entity
CREATE INDEX idx_entity_preference_entity ON entity_preference(entity_type_id, entity_id);

-- Create index for geofencing enabled checks
CREATE INDEX idx_entity_preference_geofencing ON entity_preference(entity_type_id, entity_id, is_geofencing_allowed, is_geofencing_active)
    WHERE is_geofencing_allowed = TRUE AND is_geofencing_active = TRUE;

-- Comments
COMMENT ON TABLE entity_preference IS 'Stores feature preferences for entities to control access to geo-fencing features';
COMMENT ON COLUMN entity_preference.entity_type_id IS 'Type of entity: 1=USER, 2=ORG, 4=PROJECT, 5=TEAM';
COMMENT ON COLUMN entity_preference.entity_id IS 'ID of the entity (user_id, org_id, project_id, or team_id)';
COMMENT ON COLUMN entity_preference.is_geofencing_allowed IS 'Indicates if geo-fencing feature is provisioned for this entity';
COMMENT ON COLUMN entity_preference.is_geofencing_active IS 'Indicates if admin has activated geo-fencing for this entity';
