(ns personalcareops.sim
  "Demo driver for the personal-care salon/service coordination actor.

  Runs a self-contained simulation with demo data through the REAL
  compiled `langgraph.graph` StateGraph
  (`personalcareops.operation/build`) -- not a hand-rolled pipeline.
  Exercises the governor's hard checks, the phase auto-commit gate, and
  the human-in-the-loop escalation/approval `interrupt-before` pause +
  resume."
  (:require [personalcareops.store :as store]
            [personalcareops.operation :as operation]
            [langgraph.graph :as g]))

(defn- exec [actor tid request phase-num]
  (g/run* actor {:request request :phase-num phase-num} {:thread-id tid}))

;; ---------------------- demo scenarios ----------------------

(defn demo-happy-path
  "Scenario 1: Valid appointment request for a verified client, at a
  phase where :schedule-service-appointment auto-commits -> commits
  through the real compiled graph, straight through :decide -> :commit."
  []
  (let [s (store/make-store)
        actor (operation/build s)]
    (println "\n=== Demo 1: Happy Path (Appointment, auto-commits at phase 3) ===")
    (println "Client: Alice Johnson (verified)")
    (println "Request: Schedule a haircut appointment")
    (let [result (exec actor "demo-1"
                        {:operation :schedule-service-appointment
                         :client-id "client-1"
                         :service-type "hair-styling"
                         :scheduled-date "2026-07-30"}
                        3)]
      (println "Status:" (:status result))
      (println "Decision:" (:decision (:state result)))
      (println "Ledger entries:" (count (store/ledger s)))
      result)))

(defn demo-unverified-client
  "Scenario 2: Request targets an unverified client -- a HARD, permanent
  governor check fails. The real graph routes straight to :hold (never
  through human approval, regardless of phase)."
  []
  (let [s (store/make-store)
        actor (operation/build s)]
    (println "\n=== Demo 2: Unverified Client (Hard Check Fails) ===")
    (println "Client: Carol Lee (registered, NOT verified)")
    (println "Request: Schedule an appointment")
    (let [result (exec actor "demo-2"
                        {:operation :schedule-service-appointment
                         :client-id "client-3"
                         :service-type "hair-styling"
                         :scheduled-date "2026-07-30"}
                        3)]
      (println "Status:" (:status result))
      (println "Decision:" (:decision (:state result)))
      (println "Violations:" (:violations (:state result)))
      result)))

(defn demo-scope-exclusion
  "Scenario 3: Request touches excluded territory (scope check fails) --
  a HARD, permanent governor block regardless of phase or confidence."
  []
  (let [s (store/make-store)
        actor (operation/build s)]
    (println "\n=== Demo 3: Scope Exclusion (Treatment-Plan Content Rejected) ===")
    (println "Client: Alice Johnson")
    (println "Request: Attempting to propose a treatment-plan change")
    (let [result (exec actor "demo-3"
                        {:operation :coordinate-service-status-update
                         :client-id "client-1"
                         :status-type "service-status"
                         :status-value "treatment-plan change"}
                        3)]
      (println "Status:" (:status result))
      (println "Decision:" (:decision (:state result)))
      (println "Violations:" (:violations (:state result)))
      result)))

(defn demo-safety-escalation
  "Scenario 4: Safety concern flag -- ALWAYS escalates. The real graph
  genuinely interrupts (checkpointed) at :request-approval; a human
  operator's approval resumes the SAME compiled graph and commits via
  the graph's own :request-approval -> :commit edge."
  []
  (let [s (store/make-store)
        actor (operation/build s)]
    (println "\n=== Demo 4: Safety Concern (Always Escalates, then Approved) ===")
    (println "Request: Sanitation hazard flagged")
    (let [held (exec actor "demo-4"
                      {:operation :flag-safety-concern
                       :concern-type "sanitation-issue"
                       :description "Floor hazard detected"
                       :severity :high}
                      3)]
      (println "Status (pre-approval):" (:status held))
      (println "Ledger entries (pre-approval, must be 0):" (count (store/ledger s)))
      (let [approved (g/run* actor {:approval {:status :approved :by "ops-manager-01"}}
                              {:thread-id "demo-4" :resume? true})]
        (println "Status (post-approval):" (:status approved))
        (println "Decision (post-approval):" (:decision (:state approved)))
        (println "Ledger entries (post-approval, must be 1):" (count (store/ledger s)))
        approved))))

(defn demo-supply-coordination
  "Scenario 5: Non-safety supply request at phase 0 (nothing auto-commits
  yet) -- clean proposal, but held for review because it isn't in the
  current phase's auto-commit set (distinct from a governor violation)."
  []
  (let [s (store/make-store)
        actor (operation/build s)]
    (println "\n=== Demo 5: Supply Coordination (Phase 0, held for review) ===")
    (println "Client: Alice Johnson")
    (println "Request: Office supplies")
    (let [result (exec actor "demo-5"
                        {:operation :coordinate-supply-request
                         :client-id "client-1"
                         :supply-type "office-paper"
                         :quantity 10
                         :requested-delivery-date "2026-08-01"}
                        0)]
      (println "Status:" (:status result))
      (println "Decision:" (:decision (:state result)))
      result)))

;; ---------------------- run all demos ----------------------

(defn run-all-demos
  "Execute all demo scenarios."
  []
  (println "╔════════════════════════════════════════════════════════════╗")
  (println "║ ISIC-960 Personal-Care Coordination Actor Demo             ║")
  (println "╚════════════════════════════════════════════════════════════╝")

  (demo-happy-path)
  (demo-unverified-client)
  (demo-scope-exclusion)
  (demo-safety-escalation)
  (demo-supply-coordination)

  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║ All demo scenarios completed.                               ║")
  (println "╚════════════════════════════════════════════════════════════╝\n"))

;; ---------------------- entry point ----------------------

#?(:clj
   (defn -main [& args]
     (run-all-demos)))
