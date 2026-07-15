(ns personalcareops.phase
  "Rollout phases 0-3 for the personal-care salon/service coordination actor.

  Phase progression controls which operations auto-commit vs require human approval.

  - Phase 0 (read-only): All proposals held for review.
  - Phase 1: Appointment scheduling + service status updates auto-commit.
  - Phase 2: + supply coordination + staff shift proposals auto-commit.
  - Phase 3: All auto-commit with escalation (safety concerns always escalate).")

(def phases
  "Phase definitions: which operations auto-commit at each phase."
  {0 {:name "read-only"
      :auto-commit #{}
      :description "All proposals held for review, no auto-commit"}

   1 {:name "appointment + status-update"
      :auto-commit #{:schedule-service-appointment
                     :coordinate-service-status-update}
      :description "Appointment scheduling and status updates auto-commit"}

   2 {:name "+ supply-coordination + shift-proposals"
      :auto-commit #{:schedule-service-appointment
                     :coordinate-service-status-update
                     :coordinate-supply-request
                     :schedule-staff-shift-proposal}
      :description "All logistics (appointments, status, supply, shifts) auto-commit"}

   3 {:name "auto-commit with escalation"
      :auto-commit #{:schedule-service-appointment
                     :coordinate-service-status-update
                     :coordinate-supply-request
                     :schedule-staff-shift-proposal}
      :always-escalate #{:flag-safety-concern}
      :description "All non-safety ops auto-commit, safety concerns always escalate"}})

(defn phase-info
  "Get phase metadata."
  [phase-id]
  (get phases phase-id))

(defn auto-commits-at-phase?
  "Does this operation auto-commit at the given phase?"
  [phase-id op-id]
  (let [phase (phase-info phase-id)]
    (contains? (:auto-commit phase) op-id)))

(defn always-escalates?
  "Does this operation always escalate (even if governance passes)?"
  [phase-id op-id]
  (let [phase (phase-info phase-id)]
    (contains? (:always-escalate phase) op-id)))
