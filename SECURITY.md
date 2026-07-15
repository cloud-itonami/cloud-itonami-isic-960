# Security Policy - ISIC-960

## Reporting Vulnerabilities

If you discover a security vulnerability, please email jun784@gmail.com
with details. Do not open a public issue.

## Security Guarantees

### Permanent Restrictions (Non-Negotiable)

1. **No clinical authority**: Health, allergy, treatment, and medication decisions
   require qualified human review. The Governor enforces this rejection.

2. **No service-technique decisions**: Treatment protocols, product selections,
   and service-specific methods are outside this actor's authority.

3. **No safety-authority overrides**: Facility safety certifications and
   compliance determinations require human review and authority.

4. **No unverified client proposals**: All proposals require the client to be
   both registered AND verified in the store. Unverified clients are always blocked.

5. **Effect restriction**: All proposals must have effect `:propose`. No other
   effects (`:commit`, `:execute`, etc.) are accepted.

### Immutable Audit Trail

- All governance decisions are recorded in append-only ledgers
- Commitment history cannot be tampered with after recording
- Decision violations are permanently logged

### Phase-Gated Rollout

- Phase 0 (read-only): All proposals held for human review
- Automatic commitment only at phase 2+ and only for whitelisted ops
- Safety concerns always escalate, regardless of phase

## Implementation Notes

The Governor (`personalcareops.governor`) is the trusted decision enforcer.
Its three checks cannot be bypassed or overridden:

1. Client verification (store-derived, re-checked every proposal)
2. Effect validation (must be `:propose`)
3. Scope exclusion (combined EN+JA pattern scan, qualified for legitimate ops)

Proposals that fail any check are permanently rejected with no override path.

## Threat Model

This actor is designed to resist:

- **Prompt injection**: Scans content for service-technique/clinical keywords
- **Scope creep**: Whitelisted operations only, no new ops without explicit governance change
- **Unverified client access**: Governor re-validates client status every proposal
- **Unauthorized escalation bypass**: Safety concerns always escalate
- **Effect spoofing**: All proposals normalized to `:propose` before governance

## No Liability

This is a reference implementation. Deployment requires your own compliance
and security review. The tool is provided as-is for coordination logistics only.
