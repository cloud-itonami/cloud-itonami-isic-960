(ns personalcareops.governor-test
  "Tests for the three HARD, permanent, un-overridable governor checks."
  (:require [clojure.test :refer [deftest is testing]]
            [personalcareops.store :as store]
            [personalcareops.governor :as governor]))

(deftest hard-check-1-client-unverified
  (testing "HARD-check 1: client must be registered AND verified"
    (let [s (store/make-store)]
      ;; client-1 is registered and verified
      (is (empty? (governor/client-unverified-violations s :schedule-service-appointment "client-1")))
      ;; client-3 is registered but NOT verified
      (is (= 1 (count (governor/client-unverified-violations s :schedule-service-appointment "client-3"))))
      ;; unknown client is not registered at all
      (is (= 1 (count (governor/client-unverified-violations s :schedule-service-appointment "no-such-client")))))))

(deftest hard-check-1-exempts-flag-safety-concern
  (testing ":flag-safety-concern is a facility-level concern -- it never
            requires client verification, even for an unverified/unknown
            client-id"
    (let [s (store/make-store)]
      (is (empty? (governor/client-unverified-violations s :flag-safety-concern "no-such-client"))))))

(deftest hard-check-2-effect-not-propose
  (testing "HARD-check 2: :effect must be :propose"
    (is (empty? (governor/effect-not-propose-violations
                 {:operation :schedule-service-appointment :effect :propose})))
    (is (= 1 (count (governor/effect-not-propose-violations
                      {:operation :schedule-service-appointment :effect :commit}))))
    (is (= 1 (count (governor/effect-not-propose-violations
                      {:operation :schedule-service-appointment :effect :execute}))))))

(deftest hard-check-3-scope-exclusion
  (testing "HARD-check 3: treatment-plan / allergy-risk / sanitation-authority
            content is blocked"
    (is (= 1 (count (governor/scope-exclusion-violations
                      {:operation :coordinate-service-status-update
                       :status-value "treatment-plan change"}))))
    (is (= 1 (count (governor/scope-exclusion-violations
                      {:operation :coordinate-supply-request
                       :description "evaluate allergy-risk"}))))
    (is (= 1 (count (governor/scope-exclusion-violations
                      {:operation :coordinate-service-status-update
                       :status-value "safety authority compliance override"}))))))

(deftest hard-check-3-blocks-operations-outside-allowlist
  (testing "an :operation not in the closed allowlist is blocked, even
            with clean content"
    (is (= 1 (count (governor/scope-exclusion-violations
                      {:operation :not-a-real-operation}))))))

(deftest flag-safety-concern-not-self-blocked
  (testing ":flag-safety-concern legitimately mentions safety and must NOT
            trip the scope-exclusion check on itself"
    (is (empty? (governor/scope-exclusion-violations
                 {:operation :flag-safety-concern
                  :concern-type "sanitation-issue"
                  :description "guardrail is loose"})))))

(deftest clean-proposal-has-no-violations
  (testing "a routine, in-scope, :propose proposal has zero violations
            from the scope-exclusion check"
    (is (empty? (governor/scope-exclusion-violations
                 {:operation :schedule-service-appointment
                  :service-type "hair-styling"})))))

(deftest govern-passes-for-clean-verified-proposal
  (let [s (store/make-store)
        proposal {:operation :schedule-service-appointment
                  :client-id "client-1" :effect :propose}
        result (governor/govern s proposal)]
    (is (true? (:passes? result)))
    (is (= :APPROVE (:decision result)))
    (is (empty? (:violations result)))))

(deftest govern-rejects-unverified-client
  (testing "governor rejection: an unverified client is a hard reject,
            regardless of how clean the rest of the proposal is -- this
            is what blocks commit downstream in operation.cljc"
    (let [s (store/make-store)
          proposal {:operation :schedule-service-appointment
                    :client-id "client-3" :effect :propose}
          result (governor/govern s proposal)]
      (is (false? (:passes? result)))
      (is (= :REJECT (:decision result)))
      (is (some #{:client-unverified} (map :check/id (:violations result)))))))

(deftest govern-rejects-non-propose-effect
  (let [s (store/make-store)
        proposal {:operation :schedule-service-appointment
                  :client-id "client-1" :effect :commit}
        result (governor/govern s proposal)]
    (is (false? (:passes? result)))
    (is (= :REJECT (:decision result)))))

(deftest govern-accumulates-multiple-violations
  (testing "multiple independent violations on the SAME proposal all
            surface -- the governor doesn't short-circuit on the first"
    (let [s (store/make-store)
          proposal {:operation :coordinate-service-status-update
                    :client-id "no-such-client" :effect :commit
                    :status-value "treatment-plan change"}
          result (governor/govern s proposal)]
      (is (= 3 (count (:violations result)))
          "client-unverified + effect-not-propose + scope-exclusion, all three"))))
