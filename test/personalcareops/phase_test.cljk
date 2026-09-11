(ns personalcareops.phase-test
  "Tests for the 0->3 phase rollout auto-commit table. These fns are now
  genuinely consulted by `personalcareops.operation`'s `:decide` node
  (see `operation-test`) -- previously they were defined and tested in
  isolation but never wired into any decision."
  (:require [clojure.test :refer [deftest is testing]]
            [personalcareops.phase :as phase]))

(deftest phase-0-all-held
  (testing "Phase 0 (read-only): nothing auto-commits"
    (doseq [op [:schedule-service-appointment :coordinate-service-status-update
                :coordinate-supply-request :schedule-staff-shift-proposal
                :flag-safety-concern]]
      (is (false? (phase/auto-commits-at-phase? 0 op))))))

(deftest phase-1-appointment-and-status-only
  (testing "Phase 1: only appointment + status-update auto-commit"
    (is (true? (phase/auto-commits-at-phase? 1 :schedule-service-appointment)))
    (is (true? (phase/auto-commits-at-phase? 1 :coordinate-service-status-update)))
    (is (false? (phase/auto-commits-at-phase? 1 :coordinate-supply-request)))
    (is (false? (phase/auto-commits-at-phase? 1 :schedule-staff-shift-proposal)))
    (is (false? (phase/auto-commits-at-phase? 1 :flag-safety-concern)))))

(deftest phase-2-adds-supply-and-shift
  (testing "Phase 2: appointment/status + supply + shift-proposal auto-commit"
    (doseq [op [:schedule-service-appointment :coordinate-service-status-update
                :coordinate-supply-request :schedule-staff-shift-proposal]]
      (is (true? (phase/auto-commits-at-phase? 2 op))))
    (is (false? (phase/auto-commits-at-phase? 2 :flag-safety-concern)))))

(deftest phase-3-full-auto-commit-except-safety
  (testing "Phase 3: all non-safety ops auto-commit; safety never does"
    (doseq [op [:schedule-service-appointment :coordinate-service-status-update
                :coordinate-supply-request :schedule-staff-shift-proposal]]
      (is (true? (phase/auto-commits-at-phase? 3 op))))
    (is (false? (phase/auto-commits-at-phase? 3 :flag-safety-concern)))))

(deftest always-escalates-only-at-phase-3-for-safety
  (testing "always-escalates? is the phase-3-specific escalation table --
            :flag-safety-concern is the only member"
    (is (true? (phase/always-escalates? 3 :flag-safety-concern)))
    (is (false? (phase/always-escalates? 3 :schedule-service-appointment)))
    (is (false? (phase/always-escalates? 0 :flag-safety-concern))
        "phase 0 has no :always-escalate set at all")))

(deftest safety-concern-never-auto-commits-at-any-phase
  (doseq [ph [0 1 2 3]]
    (is (false? (phase/auto-commits-at-phase? ph :flag-safety-concern)))))
