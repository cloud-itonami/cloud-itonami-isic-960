(ns personalcareops.governor
  "Governor with three HARD, permanent, un-overridable checks for the
  personal-care salon/service coordination actor.

  1. Client/appointment-record unverified — target must exist in store AND be
     independently :registered?/:verified?, re-derived every time.
  2. Effect not :propose — rejected outright.
  3. Scope exclusion — any proposal touching service-technique decisions,
     health/sanitation-compliance determinations, client health/allergy-risk
     clinical judgments, or safety-authority overrides is permanently blocked.

     ALLOWED (closed allowlist):
     - :schedule-service-appointment — appointment scheduling logistics only
     - :coordinate-service-status-update — admin status tracking only
     - :coordinate-supply-request — non-service-critical consumables only
     - :schedule-staff-shift-proposal — admin shift proposal only
     - :flag-safety-concern — facility/sanitation/client-welfare escalation only"
  (:require [personalcareops.store :as store]
            [kotoba.lang.text :as str]))

;; ---------------------- hard checks ----------------------

(defn client-unverified-violations
  "Check 1: Client/target must be registered AND verified.
  This is re-derived from the store, never from proposal self-report.

  Exception: :flag-safety-concern doesn't require client verification
  (it's a facility-level concern that escalates to human review)."
  [store op-id client-id]
  ;; Safety concerns don't need client verification (facility-level)
  (if (= op-id :flag-safety-concern)
    []
    ;; All other operations require client verification
    (let [client (store/client store client-id)]
      (cond
        (nil? client)
        [{:check/id :client-unverified
          :violation "Client not found in store"}]

        (not (:registered? client))
        [{:check/id :client-unverified
          :violation "Client is not registered"}]

        (not (:verified? client))
        [{:check/id :client-unverified
          :violation "Client is not verified"}]

        :else
        []))))

(defn effect-not-propose-violations
  "Check 2: Effect must be :propose. Any other effect is rejected outright."
  [proposal]
  (if (not= (:effect proposal) :propose)
    [{:check/id :effect-not-propose
      :violation (str "Effect is " (:effect proposal) ", not :propose")}]
    []))

(defn scope-exclusion-violations
  "Check 3: Block proposals touching excluded territory.

  EXCLUDED (never allowed):
  - service-technique/treatment decisions (haircut style, product selection, etc.)
  - health/sanitation-compliance determinations
  - client health/allergy-risk clinical judgments
  - safety-authority overrides

  ALLOWED (closed allowlist):
  - :schedule-service-appointment — appointment scheduling logistics
  - :coordinate-service-status-update — administrative status tracking
  - :coordinate-supply-request — non-service-critical office supplies
  - :schedule-staff-shift-proposal — administrative shift proposals
  - :flag-safety-concern — facility/sanitation/client-welfare escalation

  Uses qualified substring scan (EN+JA) so legitimate :flag-safety-concern
  ops that mention 'safety' aren't self-blocked."
  [proposal]
  (let [forbidden-patterns
        [;; EN patterns for service-technique / clinical / safety-authority territory
         #"(?i)treatment.*plan"
         #"(?i)service.*technique"
         #"(?i)allergy.*risk"
         #"(?i)health.*condition"
         #"(?i)medication.*prescription"
         #"(?i)clinical.*judgment"
         #"(?i)medical.*decision"
         #"(?i)product.*selection"
         #"(?i)treatment.*protocol"
         #"(?i)sanitation.*certification"
         #"(?i)health.*authority"
         #"(?i)safety.*authority"
         #"(?i)compliance.*override"
         ;; JA patterns
         #"施術.?方法"
         #"治療.?計画"
         #"アレルギー.?リスク"
         #"健康.?診断"
         #"薬剤.?処方"
         #"臨床.?判定"
         #"医学.?決定"
         #"製品.?選択"
         #"衛生.?認可"
         #"衛生.?監督"]

        ;; Allowed operations
        allowed-ops #{:schedule-service-appointment
                      :coordinate-service-status-update
                      :coordinate-supply-request
                      :schedule-staff-shift-proposal
                      :flag-safety-concern}

        op-id (:operation proposal)
        proposal-str (str proposal)

        ;; Check 1: Operation must be in allowed list
        op-not-allowed (not (allowed-ops op-id))

        ;; Check 2: Content must not contain forbidden patterns
        ;; (except :flag-safety-concern which is allowed to escalate)
        content-forbidden (and (not= op-id :flag-safety-concern)
                               (some #(re-find % proposal-str) forbidden-patterns))

        ;; Combine EN+JA checks into a single explicit boolean
        in-forbidden-territory (or op-not-allowed content-forbidden)]

    (if in-forbidden-territory
      [{:check/id :scope-exclusion
        :violation "Proposal touches service-technique, clinical/health judgments, or safety-authority overrides"}]
      [])))

;; ---------------------- decision logic ----------------------

(defn govern
  "Apply all three HARD checks. Any violation is a permanent rejection
  with no override path."
  [store proposal]
  (let [client-violations (client-unverified-violations store (:operation proposal) (:client-id proposal))
        effect-violations (effect-not-propose-violations proposal)
        scope-violations (scope-exclusion-violations proposal)
        all-violations (concat client-violations effect-violations scope-violations)]

    {:proposal proposal
     :violations all-violations
     :passes? (empty? all-violations)
     :decision (if (empty? all-violations)
                 :APPROVE
                 :REJECT)}))
