(ns personalcareops.test
  "Test suite for the personal-care coordination actor.

  Tests cover the three HARD governor checks and the happy path proposal flow."
  (:require [personalcareops.store :as store]
            [personalcareops.governor :as governor]
            [personalcareops.operation :as operation]
            [personalcareops.phase :as phase]))

;; ---------------------- test utilities ----------------------

(defn run-test-group [name f]
  (println (str "\n" name))
  (f))

;; ---------------------- store tests ----------------------

(defn test-store []
  (run-test-group "=== Store Tests ===" (fn []
    (let [s (store/make-store)]
      (println "[1] Client lookup")
      (assert (= "Alice Johnson" (:name (store/client s "client-1"))))
      (println "    ✓ client-1 found and verified")

      (println "[2] All clients")
      (let [all (store/all-clients s)]
        (assert (= 3 (count all)))
        (println "    ✓ 3 clients in store"))

      (println "[3] Service lookup")
      (assert (= "Classic Haircut" (:name (store/service s "service-1"))))
      (println "    ✓ service-1 found")

      (println "[4] Ledger append")
      (store/append-ledger! s {:event "test-fact"})
      (assert (= 1 (count (store/ledger s))))
      (println "    ✓ ledger append works")))))

;; ---------------------- governor tests ----------------------

(defn test-governor []
  (run-test-group "=== Governor Tests ===" (fn []
    (let [s (store/make-store)]
      (println "[1] Client unverified check")
      (let [violations (governor/client-unverified-violations s :schedule-service-appointment "client-3")]
        (assert (= 1 (count violations)))
        (assert (= :client-unverified (get-in violations [0 :check/id])))
        (println "    ✓ unverified client blocked"))

      (println "[2] Effect not :propose check")
      (let [proposal {:operation :schedule-service-appointment :effect :commit :client-id "client-1"}
            violations (governor/effect-not-propose-violations proposal)]
        (assert (= 1 (count violations)))
        (println "    ✓ non-:propose effect blocked"))

      (println "[3] Scope exclusion (treatment-plan)")
      (let [proposal {:operation :coordinate-service-status-update
                      :client-id "client-1"
                      :status-value "treatment-plan change"
                      :effect :propose}
            violations (governor/scope-exclusion-violations proposal)]
        (assert (= 1 (count violations)))
        (println "    ✓ treatment-plan blocked"))

      (println "[4] Scope exclusion (allergy-risk)")
      (let [proposal {:operation :coordinate-supply-request
                      :client-id "client-1"
                      :description "evaluate allergy-risk"
                      :effect :propose}
            violations (governor/scope-exclusion-violations proposal)]
        (assert (= 1 (count violations)))
        (println "    ✓ allergy-risk blocked"))

      (println "[5] Flag-safety-concern allowed (legitimate use)")
      (let [proposal {:operation :flag-safety-concern
                      :concern-type "sanitation-issue"
                      :effect :propose}
            violations (governor/scope-exclusion-violations proposal)]
        (assert (= 0 (count violations)))
        (println "    ✓ flag-safety-concern not auto-blocked"))

      (println "[6] Full governor decision (pass)")
      (let [proposal {:operation :schedule-service-appointment
                      :client-id "client-1"
                      :effect :propose}
            result (governor/govern s proposal)]
        (assert (:passes? result))
        (assert (= :APPROVE (:decision result)))
        (println "    ✓ governance passes for verified client"))))))

;; ---------------------- operation tests ----------------------

(defn test-operations []
  (run-test-group "=== Operation Tests ===" (fn []
    (let [s (store/make-store)]
      (println "[1] Appointment proposal (happy path)")
      (let [result (operation/run-proposal s
                     {:operation :schedule-service-appointment
                      :client-id "client-1"
                      :service-type "hair-styling"
                      :scheduled-date "2026-07-30"})]
        (assert (or (= :pending-approval (:action result))
                    (= :request-approval (:action result))))
        (println "    ✓ appointment proposal completes"))

      (println "[2] Unverified client rejection")
      (let [result (operation/run-proposal s
                     {:operation :schedule-service-appointment
                      :client-id "client-3"
                      :service-type "hair-styling"
                      :scheduled-date "2026-07-30"})]
        (assert (= :held (:action result)))
        (println "    ✓ unverified client held"))

      (println "[3] Safety concern escalation")
      (let [result (operation/run-proposal s
                     {:operation :flag-safety-concern
                      :concern-type "sanitation-issue"
                      :description "Floor hazard"
                      :severity :high})]
        (assert (= :escalated (:action result)))
        (println "    ✓ safety concern escalates"))))))

;; ---------------------- phase tests ----------------------

(defn test-phases []
  (run-test-group "=== Phase Tests ===" (fn []
    (println "[1] Phase 0 (read-only)")
    (assert (not (phase/auto-commits-at-phase? 0 :schedule-service-appointment)))
    (println "    ✓ no auto-commit at phase 0")

    (println "[2] Phase 1 (appointment + status)")
    (assert (phase/auto-commits-at-phase? 1 :schedule-service-appointment))
    (assert (phase/auto-commits-at-phase? 1 :coordinate-service-status-update))
    (assert (not (phase/auto-commits-at-phase? 1 :coordinate-supply-request)))
    (println "    ✓ appointment and status auto-commit at phase 1")

    (println "[3] Phase 3 (full auto-commit)")
    (assert (phase/always-escalates? 3 :flag-safety-concern))
    (println "    ✓ safety concerns escalate at phase 3"))))

;; ---------------------- master test runner ----------------------

(defn run-all-tests []
  (println "╔════════════════════════════════════════════════════════════╗")
  (println "║ ISIC-960 Personal-Care Coordination Actor Tests           ║")
  (println "╚════════════════════════════════════════════════════════════╝")

  (test-store)
  (test-governor)
  (test-operations)
  (test-phases)

  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║ All tests passed!                                          ║")
  (println "╚════════════════════════════════════════════════════════════╝\n")
  0)

#?(:clj
   (defn -main [& args]
     (run-all-tests)))
