# Governance - ISIC-960 Personal-Care Coordination Actor

## Decision Authority

The Governor (`personalcareops.governor`) is the ultimate decision authority.
Its three HARD checks are permanent and un-overridable:

1. **Client Verification Check**
   - Target client must exist in store
   - Client must be `:registered?` AND `:verified?`
   - This is re-derived from the client's own fields every proposal

2. **Effect Validation Check**
   - Effect must be `:propose`
   - No other effect values (`:commit`, `:execute`, etc.) are accepted
   - Rejected outright if violated

3. **Scope Exclusion Check**
   - Content scanned against forbidden territory (service-technique, clinical, safety-authority)
   - Uses combined EN+JA pattern matching
   - Legitimate `:flag-safety-concern` operations are allowed (always escalate)

## Operation Allowlist

Only these operations may be proposed:

- `:schedule-service-appointment` — appointment logistics (administrative only)
- `:coordinate-service-status-update` — status tracking (administrative only)
- `:coordinate-supply-request` — non-service-critical supplies
- `:schedule-staff-shift-proposal` — shift proposals (administrative proposal only)
- `:flag-safety-concern` — facility/sanitation/client-welfare escalation

All other operation types are rejected.

## Escalation Rules

- **Safety concerns** (`:flag-safety-concern`) ALWAYS escalate to human review
- Escalation bypasses auto-commit — human verification is required
- Escalation is never overridable by the governor

## Phase Progression (0→3)

- **Phase 0** (read-only): All proposals held for human review
- **Phase 1**: Appointment scheduling + status updates auto-commit
- **Phase 2**: + supply + shift proposals auto-commit
- **Phase 3**: All auto-commit with escalation (safety concerns always escalate)

## Audit Trail

All decisions are recorded in an append-only ledger:
- Committed proposals go to `coordination-log`
- Governance violations go to `ledger`
- Decision facts (escalated, held, etc.) are immutable

## No Clinical Authority

This actor has **zero clinical judgment authority**. Health, allergy, treatment,
and safety-compliance determinations require qualified human review.

The Governor enforces this as a **permanent, un-overridable restriction**.
