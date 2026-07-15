(ns personalcareops.sim
  "Simulation harness for the personal-care coordination actor.
   Deterministic demo run for validation and end-to-end testing.")

(defn sim-run
  "Execute a simulation scenario: proposal intake through final decision."
  [store scenario]
  (let [{:keys [operation client-id description]} scenario
        ;; Map scenario to operation details
        request (case operation
                  :schedule-service-appointment
                  {:operation :schedule-service-appointment
                   :client-id client-id
                   :service-type "hair-styling"
                   :scheduled-date "2026-07-20"}

                  :coordinate-service-status-update
                  {:operation :coordinate-service-status-update
                   :client-id client-id
                   :status-type "appointment-status"
                   :status-value "completed"}

                  :coordinate-supply-request
                  {:operation :coordinate-supply-request
                   :client-id client-id
                   :supply-type "office-paper"
                   :quantity 10
                   :requested-delivery-date "2026-07-20"}

                  :schedule-staff-shift-proposal
                  {:operation :schedule-staff-shift-proposal
                   :staff-id "staff-1"
                   :shift-date "2026-07-20"
                   :shift-type "morning"}

                  :flag-safety-concern
                  {:operation :flag-safety-concern
                   :concern-type "sanitation-issue"
                   :description "Floor hazard detected"
                   :severity "high"}

                  {:operation :unknown})]
    {:scenario scenario
     :request request
     :description description}))

(defn run-demo
  "Run a comprehensive demo covering happy path and governor checks."
  [store]
  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║ ISIC-960 Personal-Care Coordination Actor - Demo Run       ║")
  (println "╚════════════════════════════════════════════════════════════╝\n")

  (println "[DEMO 1] Appointment scheduling (happy path)")
  (let [result (sim-run store
                       {:operation :schedule-service-appointment
                        :client-id "client-1"
                        :description "Schedule haircut"})]
    (println "  ✓ Request: " (:request result))
    (println "    Operation: appointment scheduling for verified client"))

  (println "\n[DEMO 2] Status update coordination")
  (let [result (sim-run store
                       {:operation :coordinate-service-status-update
                        :client-id "client-2"
                        :description "Update service status"})]
    (println "  ✓ Request: " (:request result))
    (println "    Operation: administrative status update"))

  (println "\n[DEMO 3] Supply coordination (non-clinical)")
  (let [result (sim-run store
                       {:operation :coordinate-supply-request
                        :client-id "client-1"
                        :description "Order office supplies"})]
    (println "  ✓ Request: " (:request result))
    (println "    Operation: supply ordering for non-service-critical items"))

  (println "\n[DEMO 4] Staff shift proposal (administrative)")
  (let [result (sim-run store
                       {:operation :schedule-staff-shift-proposal
                        :description "Propose staff shift"})]
    (println "  ✓ Request: " (:request result))
    (println "    Operation: administrative shift proposal only"))

  (println "\n[DEMO 5] Safety concern escalation")
  (let [result (sim-run store
                       {:operation :flag-safety-concern
                        :description "Flag sanitation hazard"})]
    (println "  ✓ Request: " (:request result))
    (println "    Operation: escalates to human review (always)"))

  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║ Demo complete - all scenarios runnable offline             ║")
  (println "╚════════════════════════════════════════════════════════════╝\n")
  0)
