(ns personalcareops.store-test
  "Tests for the ISIC-960 MemStore SSoT: client/service directories, the
  append-only ledger, and the coordination-log."
  (:require [clojure.test :refer [deftest is testing]]
            [personalcareops.store :as store]))

(deftest client-lookup
  (testing "a seeded, verified client is found by :client-id"
    (let [s (store/make-store)]
      (is (= "Alice Johnson" (:name (store/client s "client-1"))))
      (is (true? (:registered? (store/client s "client-1"))))
      (is (true? (:verified? (store/client s "client-1")))))))

(deftest client-lookup-miss
  (testing "an unknown client-id returns nil, not an exception"
    (let [s (store/make-store)]
      (is (nil? (store/client s "does-not-exist"))))))

(deftest all-clients-count
  (testing "demo-data seeds exactly 3 clients"
    (let [s (store/make-store)]
      (is (= 3 (count (store/all-clients s)))))))

(deftest service-lookup
  (testing "a seeded service is found by :service-id"
    (let [s (store/make-store)]
      (is (= "Classic Haircut" (:name (store/service s "service-1")))))))

(deftest service-lookup-miss
  (testing "an unknown service-id returns nil"
    (let [s (store/make-store)]
      (is (nil? (store/service s "does-not-exist"))))))

(deftest ledger-starts-empty-and-is-append-only
  (testing "a fresh store's ledger is empty; append-ledger! grows it by
            exactly one entry per call, never mutating prior entries"
    (let [s (store/make-store)]
      (is (empty? (store/ledger s)))
      (store/append-ledger! s {:event "fact-1"})
      (is (= 1 (count (store/ledger s))))
      (store/append-ledger! s {:event "fact-2"})
      (is (= 2 (count (store/ledger s))))
      (is (= [{:event "fact-1"} {:event "fact-2"}] (store/ledger s))
          "insertion order is preserved"))))

(deftest coordination-log-starts-empty-and-records-commits
  (testing "a fresh store's coordination-log is empty; commit-record!
            grows it by exactly one entry per call"
    (let [s (store/make-store)]
      (is (empty? (store/coordination-log s)))
      (store/commit-record! s {:operation :schedule-service-appointment})
      (is (= 1 (count (store/coordination-log s)))))))

(deftest with-clients-replaces-directory
  (testing "with-clients replaces the seeded client directory wholesale"
    (let [s (store/make-store)]
      (store/with-clients s {"only-one" {:client-id "only-one"
                                          :registered? true :verified? true}})
      (is (= 1 (count (store/all-clients s))))
      (is (nil? (store/client s "client-1")) "the old seed data is gone"))))
