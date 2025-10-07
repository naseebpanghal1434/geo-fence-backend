-- Add punch request respond within minutes configuration fields to attendance_policy
ALTER TABLE attendance_policy
ADD COLUMN punch_respond_min_minutes INT NOT NULL DEFAULT 2,
ADD COLUMN punch_respond_max_minutes INT NOT NULL DEFAULT 20,
ADD COLUMN punch_respond_default_minutes INT NOT NULL DEFAULT 10;

-- Add check constraint to ensure min <= default <= max
ALTER TABLE attendance_policy
ADD CONSTRAINT check_punch_respond_minutes_order
CHECK (punch_respond_min_minutes <= punch_respond_default_minutes
       AND punch_respond_default_minutes <= punch_respond_max_minutes);

-- Add check constraint to ensure min is at least 1
ALTER TABLE attendance_policy
ADD CONSTRAINT check_punch_respond_min_positive
CHECK (punch_respond_min_minutes >= 1);
