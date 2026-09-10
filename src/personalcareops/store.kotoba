(ns personalcareops.store
  "SSoT for the ISIC-960 personal-care salon/service administrative coordination actor.

  This actor coordinates the back-office operations of personal-care salons and
  services (hairdressing, beauty treatments, laundry, funeral services, etc.):
  appointment scheduling, service-status logistics, supply coordination, staff
  shift proposals, and safety-concern flagging (facility hazards, sanitation
  issues, client-welfare concerns).

  It NEVER touches service-technique decisions, health/sanitation-compliance
  determinations, client health/allergy-risk clinical judgments, or safety-
  authority overrides -- see `personalcareops.governor`'s `scope-exclusion-violations`,
  a HARD, permanent, un-overridable block.

  `MemStore` -- atom of EDN. The deterministic default for dev/tests/demo
  (no deps). A `clients` directory keyed by `:client-id` STRING and a
  `services` directory keyed by `:service-id` STRING.

  A registered/verified client record must exist before ANY proposal for that
  client may ever commit or escalate.")

(defprotocol Store
  (client [s client-id] "Registered client record, or nil.
    Client map: {:client-id .. :name .. :registered? bool :verified? bool}.")
  (all-clients [s])
  (service [s service-id] "Service record, or nil.")
  (all-services [s])
  (ledger [s] "the append-only immutable decision-fact log")
  (coordination-log [s] "the append-only committed coordination-proposal history")
  (commit-record! [s record] "apply a committed proposal's record to the SSoT")
  (append-ledger! [s fact] "append one immutable decision fact")
  (with-clients [s clients] "replace/seed the client directory")
  (with-services [s services] "replace/seed the service directory"))

;; ----------------------------- demo data --------------------------------------

(defn demo-data
  "A small, self-contained client and service directory covering both the
  happy path and the governor's own hard checks, so the actor + tests run offline."
  []
  {:clients
   {"client-1" {:client-id "client-1" :name "Alice Johnson"
                :registered? true :verified? true
                :address "123 Main St"}
    "client-2" {:client-id "client-2" :name "Bob Smith"
                :registered? true :verified? true
                :address "456 Oak Ave"}
    "client-3" {:client-id "client-3" :name "Carol Lee (intake)"
                :registered? true :verified? false
                :address "789 Elm Blvd"}}
   :services
   {"service-1" {:service-id "service-1" :service-type "hair-styling"
                 :name "Classic Haircut" :duration-minutes 30}
    "service-2" {:service-id "service-2" :service-type "beauty-treatment"
                 :name "Facial Treatment" :duration-minutes 60}
    "service-3" {:service-id "service-3" :service-type "laundry"
                 :name "Laundry Service" :duration-minutes 120}}
   :ledger []
   :coordination-log []})

;; ----------------------------- MemStore implementation ----------------------

(deftype MemStore [atom-data]
  Store
  (client [_s client-id]
    (get-in @atom-data [:clients client-id]))
  (all-clients [_s]
    (vals (get @atom-data :clients {})))
  (service [_s service-id]
    (get-in @atom-data [:services service-id]))
  (all-services [_s]
    (vals (get @atom-data :services {})))
  (ledger [_s]
    (get @atom-data :ledger []))
  (coordination-log [_s]
    (get @atom-data :coordination-log []))
  (commit-record! [_s record]
    (swap! atom-data update :coordination-log conj record))
  (append-ledger! [_s fact]
    (swap! atom-data update :ledger conj fact))
  (with-clients [_s clients]
    (swap! atom-data assoc :clients clients)
    _s)
  (with-services [_s services]
    (swap! atom-data assoc :services services)
    _s))

(defn make-store
  "Create a fresh MemStore from demo data (or seeded with custom data)."
  ([]
   (make-store (demo-data)))
  ([data]
   (MemStore. (atom data))))
