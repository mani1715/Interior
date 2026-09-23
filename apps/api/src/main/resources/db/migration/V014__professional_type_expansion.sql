-- ============================================================================
-- Phase Audit — Professional Type Constraint Alignment
-- Expands designer_studios professional_type check constraint to support
-- both canonical product spec names and existing descriptive aliases.
-- ============================================================================

ALTER TABLE designer_studios
    DROP CONSTRAINT IF EXISTS designer_studios_professional_type_check;

ALTER TABLE designer_studios
    ADD CONSTRAINT designer_studios_professional_type_check
    CHECK (professional_type IN (
        'INDIVIDUAL_DESIGNER',
        'INTERIOR_STUDIO',
        'ARCHITECT',
        'ARCHITECTURE_STUDIO',
        'CUSTOM_FURNITURE',
        'CUSTOM_FURNITURE_STUDIO',
        'WOODWORK_CABINETRY',
        'WOODWORK_CABINETRY_PROFESSIONAL',
        'TURNKEY_CONTRACTOR'
    ));
