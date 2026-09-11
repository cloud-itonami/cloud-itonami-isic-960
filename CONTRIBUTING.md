# Contributing to cloud-itonami-isic-960

Thank you for your interest in contributing to this actor!

## Development

### Running Tests

```bash
kbb -M:dev:test
```

### Running the Demo

```bash
kbb -M:dev:run
```

## Code Organization

- `src/personalcareops/store.cljk` - Data store (SSoT)
- `src/personalcareops/advisor.cljk` - Proposal advisor
- `src/personalcareops/governor.cljk` - Safety decision logic
- `src/personalcareops/operation.cljk` - Real compiled `langgraph.graph` StateGraph orchestration
- `src/personalcareops/phase.cljk` - Rollout phase control
- `src/personalcareops/sim.cljk` - Demo driver (runs the compiled StateGraph end-to-end)
- `test/personalcareops/*_test.clj` - `clojure.test` suite (advisor, governor, phase, store, operation)

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
