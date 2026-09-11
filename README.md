# cloud-itonami-isic-960 — Personal-Care Salon/Service Coordination Actor

An autonomous coordination actor for personal-care salon and service operations
(hairdressing, beauty treatments, laundry, funeral services, etc.). Orchestrates
**administrative logistics only**: appointment scheduling, service-status updates,
supply coordination, staff shifts, and safety escalation.

## SCOPE (In)

### Allowed Operations

- `:schedule-service-appointment` — appointment scheduling logistics
- `:coordinate-service-status-update` — administrative status tracking
- `:coordinate-supply-request` — non-service-critical supplies (office paper, etc.)
- `:schedule-staff-shift-proposal` — administrative shift proposals
- `:flag-safety-concern` — facility/sanitation/client-welfare escalation (always to human)

## SCOPE (Out) — Permanently Blocked

- Service-technique decisions (haircut style, product selection, treatment method)
- Health/sanitation-compliance determinations
- Client health/allergy-risk clinical judgments
- Safety-authority overrides

The **Governor** enforces these as HARD, permanent, un-overridable blocks.
No proposal may touch excluded territory.

## Three HARD Governor Checks

Every proposal must pass all three or be rejected outright:

1. **Client Verification**
   - Client must exist in store
   - Client must be `:registered?` AND `:verified?`
   - Re-derived from store every proposal (never self-reported)

2. **Effect Validation**
   - Effect must be `:propose`
   - No other effect values accepted

3. **Scope Exclusion**
   - Content scanned for service-technique/clinical/safety-authority keywords (EN+JA)
   - Legitimate `:flag-safety-concern` ops are allowed
   - Any other op touching excluded territory is rejected

## Architecture

- **Store** (`personalcareops.store`) — SSoT with clients, services, ledgers
- **Advisor** (`personalcareops.advisor`) — Proposal generation & confidence
- **Governor** (`personalcareops.governor`) — Safety decision logic (HARD checks)
- **Operation** (`personalcareops.operation`) — langgraph-clj StateGraph orchestration
- **Phase** (`personalcareops.phase`) — Rollout control (0→3, auto-commit gates)
- **Sim** (`personalcareops.sim`) — Deterministic demo runner

## Running

### Tests

```bash
kbb -M:dev:test
```

### Demo

```bash
kbb -M:dev:run
```

## Audit Trail

- Committed proposals → `coordination-log` (append-only)
- Governance violations → `ledger` (immutable decision facts)
- No retroactive modification of records

## No Clinical Authority

**This actor has zero authority over clinical or health judgments.**
The Governor enforces this permanently and unconditionally.

All health/allergy/medication/treatment decisions require qualified human review.

## Phase Progression (0→3)

- **Phase 0** (read-only): All proposals held, no auto-commit
- **Phase 1**: Appointments + status updates auto-commit
- **Phase 2**: + supply + shifts auto-commit
- **Phase 3**: All auto-commit with escalation (safety → human always)

## References

- ADR-2607121000 (wave definition)
- ADR-2607152500 (Wave 4 amendment)
- Skill `build-actor` (actor pattern reference)
- Sibling actors (isic-920, isic-932, isic-873)

## License

AGPL-3.0 — see LICENSE

## Authors

cloud-itonami contributors
