;; Minimal demo harness for testing without nbb/clj machinery
(ns personalcareops.demo
  (:require [personalcareops.store :as store]
            [personalcareops.governor :as governor]
            [personalcareops.operation :as op]
            [personalcareops.sim :as sim]
            [personalcareops.phase :as phase]))

(defn test-store []
  (let [s (store/make-store)]
    (and (= "Alice Johnson" (:name (store/client s "client-1")))
         (nil? (store/client s "unknown-client")))))

(defn test-governor-basics []
  (let [s (store/make-store)
        good {:operation :schedule-service-appointment :client-id "client-1" :effect :propose :content "test"}
        bad-scope {:operation :coordinate-service-status-update :client-id "client-1" :effect :propose :content "allergy-risk"}]
    (and (governor/govern s good)
         (not (:passes? (governor/govern s bad-scope))))))

(defn -main []
  (println "=== Quick Sanity Checks ===")
  (let [s (store/make-store)]
    (println (if (test-store) "✓ Store tests pass" "✗ Store tests fail"))
    (println (if (test-governor-basics) "✓ Governor tests pass" "✗ Governor tests fail"))
    (println "")
    (sim/run-demo s)))
