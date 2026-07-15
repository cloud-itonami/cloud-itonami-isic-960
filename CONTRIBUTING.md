# Contributing to cloud-itonami-isic-960

Thank you for your interest in contributing to this actor!

## Development

### Running Tests

```bash
nbb run-tests.cljs
```

### Running the Demo

```bash
nbb -e "(require '[personalcareops.demo]) (personalcareops.demo/-main)"
```

## Code Organization

- `src/personalcareops/store.cljc` - Data store (SSoT)
- `src/personalcareops/advisor.cljc` - Proposal advisor
- `src/personalcareops/governor.cljc` - Safety decision logic
- `src/personalcareops/operation.cljc` - State machine orchestration
- `src/personalcareops/phase.cljc` - Rollout phase control
- `src/personalcareops/sim.cljc` - Simulation harness
- `test/personalcareops/test.cljc` - Test suite

## Scope Guidelines

This actor coordinates **administrative** operations only:

### In Scope (allowed):
- Appointment scheduling logistics
- Service status administrative updates
- Non-service-critical supply ordering
- Staff shift proposal (administrative only)
- Safety concern escalation (always to human review)

### Out of Scope (permanently blocked by governor):
- Service-technique/treatment decisions
- Health/sanitation-compliance determinations
- Client health/allergy-risk clinical judgments
- Safety-authority overrides

## Three HARD Governor Checks

All proposals must pass:

1. **Client verification** - Target client must be registered AND verified
2. **Effect validation** - Effect must be `:propose` (no other values accepted)
3. **Scope exclusion** - No service-technique, clinical, or safety-authority content

## Proposals Outside Scope

If a proposal touches excluded territory, the governor will permanently reject it.
This is by design and cannot be overridden.
