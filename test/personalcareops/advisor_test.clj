(ns personalcareops.advisor-test
  "Tests for the five deterministic per-op advisor functions and the
  `advisability` heuristic that `personalcareops.operation/advise-request`
  (the StateGraph's `:advise` node) dispatches to, unchanged from the
  pre-graph pipeline."
  (:require [clojure.test :refer [deftest is testing]]
            [personalcareops.store :as store]
            [personalcareops.advisor :as advisor]))

(deftest advise-appointment-proposal-happy-path
  (testing "an appointment proposal always :propose-s (never actuates)"
    (let [s (store/make-store)
          p (advisor/advise-appointment-proposal s "client-1" "hair-styling" "2026-08-01")]
      (is (= :schedule-service-appointment (:operation p)))
      (is (= :propose (:effect p)))
      (is (= "client-1" (:client-id p))))))

(deftest advise-status-proposal-is-administrative-only
  (testing "status-update proposals are :propose only, never a direct actuation"
    (let [s (store/make-store)
          p (advisor/advise-status-proposal s "client-1" "appointment-status" "completed")]
      (is (= :coordinate-service-status-update (:operation p)))
      (is (= :propose (:effect p))))))

(deftest advise-supply-request-happy-path
  (let [s (store/make-store)
        p (advisor/advise-supply-request s "client-1" "office-paper" 10 "2026-08-01")]
    (is (= :coordinate-supply-request (:operation p)))
    (is (= :propose (:effect p)))))

(deftest advise-shift-proposal-happy-path
  (let [s (store/make-store)
        p (advisor/advise-shift-proposal s "staff-1" "2026-08-01" "morning")]
    (is (= :schedule-staff-shift-proposal (:operation p)))
    (is (= :propose (:effect p)))
    (is (= "staff-1" (:staff-id p)))))

(deftest advise-safety-concern-always-flags-escalate
  (testing "a safety concern proposal ALWAYS carries :escalate? true --
            this is the only advisor fn that sets it"
    (let [s (store/make-store)
          p (advisor/advise-safety-concern s "sanitation-issue" "Floor hazard" :high)]
      (is (= :flag-safety-concern (:operation p)))
      (is (= :propose (:effect p)))
      (is (true? (:escalate? p))))))

;; ---------------------- advisability heuristic ----------------------

(deftest advisability-flags-forbidden-content-as-zero-score
  (testing "content mentioning clinical/health/allergy territory scores 0.0
            regardless of which op it's attached to"
    (let [result (advisor/advisability :coordinate-service-status-update "client-1"
                                        "treatment-plan change")]
      (is (= 0.0 (:score result)))
      (is (= :low (:confidence result))))))

(deftest advisability-safety-concern-scores-high
  (let [result (advisor/advisability :flag-safety-concern nil "safety-concern sanitation-issue severity=high")]
    (is (> (:score result) 0.8))
    (is (= :high (:confidence result)))))

(deftest advisability-unknown-op-scores-low-confidence
  (testing "an unrecognized op falls through to the :else branch (score
            0.5, which is NOT > 0.5, so :confidence is :low per the
            (if (> score 0.8) :high (if (> score 0.5) :medium :low))
            classification)"
    (let [result (advisor/advisability :not-a-real-op "client-1" "whatever")]
      (is (= 0.5 (:score result)))
      (is (= :low (:confidence result))))))
