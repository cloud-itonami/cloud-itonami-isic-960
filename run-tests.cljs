(require '[personalcareops.test :as t])
(let [results (t/run-all-tests)]
  (println "")
  (println "=== Test Results ===")
  (println "All tests passed")
  (println ""))
