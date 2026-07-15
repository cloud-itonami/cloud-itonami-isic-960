(ns personalcareops.advisor
  "Proposal advisor for personal-care salon/service coordination.
   DETERMINISTIC DEMO ONLY: production requires real LLM with prompt injection safeguards.")

(defn advisability
  "Return advisability score (0–1) and reasoning for a proposal.
   DEMO: deterministic heuristics only.
   PRODUCTION: Replace with real LLM call + prompt-injection safeguards specific to personal-care domain."
  [op client-id content]
  (let [is-appointment? (= op :schedule-service-appointment)
        is-status? (= op :coordinate-service-status-update)
        is-supply? (= op :coordinate-supply-request)
        is-shift? (= op :schedule-staff-shift-proposal)
        is-safety? (= op :flag-safety-concern)

        has-forbidden-words?
        (some #(and (string? content)
                    (.toLowerCase (str content))
                    (or (.includes (.toLowerCase (str content)) %)
                        (.includes (str content) %)))
              ["allergy" "prescription" "medication" "treatment-plan"
               "health-condition" "medical-decision" "clinical-judgment"
               "アレルギー" "処方" "薬剤" "治療計画"
               "健康診断" "医学的判定" "臨床判断"])

        score (cond
                has-forbidden-words? 0.0
                is-safety? 0.95  ; Safety escalations are always high priority
                is-appointment? 0.9   ; Appointment scheduling is core logistics
                is-status? 0.85       ; Status updates are admin-level
                (or is-supply?) 0.85  ; Supply coordination is solid
                is-shift? 0.8    ; Shift proposals need review
                :else 0.5)

        reasoning (cond
                    has-forbidden-words?
                    "Content contains restricted territory (allergy/medication/clinical/health-condition)"
                    is-safety?
                    "Safety concern flagged for immediate escalation"
                    is-appointment?
                    "Appointment scheduling logistics (administrative only)"
                    is-status?
                    "Service status administrative update"
                    is-supply?
                    "Non-service-critical supply coordination"
                    is-shift?
                    "Staff shift proposal (administrative only)"
                    :else
                    "Unknown operation")]

    {:score score
     :reasoning reasoning
     :confidence (if (> score 0.8) :high (if (> score 0.5) :medium :low))}))

(defn advise-appointment-proposal
  "Generate appointment scheduling proposal."
  [store client-id service-type scheduled-date]
  {:operation :schedule-service-appointment
   :client-id client-id
   :service-type service-type
   :scheduled-date scheduled-date
   :effect :propose
   :advisor (advisability :schedule-service-appointment client-id
                          (str "appointment " service-type " on " scheduled-date))
   :escalate? false})

(defn advise-status-proposal
  "Generate service status update proposal."
  [store client-id status-type status-value]
  {:operation :coordinate-service-status-update
   :client-id client-id
   :status-type status-type
   :status-value status-value
   :effect :propose
   :advisor (advisability :coordinate-service-status-update client-id
                          (str "status " status-type " -> " status-value))
   :escalate? false})

(defn advise-supply-request
  "Generate supply request proposal (non-service-critical supplies only)."
  [store client-id supply-type quantity requested-delivery-date]
  {:operation :coordinate-supply-request
   :client-id client-id
   :supply-type supply-type
   :quantity quantity
   :requested-delivery-date requested-delivery-date
   :effect :propose
   :advisor (advisability :coordinate-supply-request client-id
                          (str "supply " supply-type " qty=" quantity))
   :escalate? false})

(defn advise-shift-proposal
  "Generate staff shift proposal (administrative proposal only)."
  [store staff-id shift-date shift-type]
  {:operation :schedule-staff-shift-proposal
   :staff-id staff-id
   :shift-date shift-date
   :shift-type shift-type
   :effect :propose
   :advisor (advisability :schedule-staff-shift-proposal staff-id
                          (str "shift " shift-type " on " shift-date))
   :escalate? false})

(defn advise-safety-concern
  "Generate safety concern escalation proposal.
  ALWAYS escalates to human review, never auto-commits."
  [store facility-concern-type description severity]
  {:operation :flag-safety-concern
   :concern-type concern-type
   :description description
   :severity severity
   :effect :propose
   :advisor (advisability :flag-safety-concern nil
                          (str "safety-concern " concern-type " severity=" severity))
   :escalate? true})
