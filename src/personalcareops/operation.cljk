(ns personalcareops.operation
  "OperationActor -- one personal-care salon/service coordination request
  = one supervised actor run, expressed as a REAL compiled `langgraph-clj`
  `StateGraph` (`langgraph.graph/state-graph` + `compile-graph`). The
  advisor (the five `personalcareops.advisor/advise-*` fns) is sealed
  into a single node (`:advise`); its proposal is ALWAYS routed through
  the independent `personalcareops.governor` (`:govern`) and the
  rollout-phase gate (`:decide`) before anything commits to the SSoT.

  This replaces the previous `run-proposal`, which was a plain
  `(let [after-intake (intake ..) after-advise (advise ..) ...] (case
  action ...))` threading pipeline that never required `langgraph.graph`
  and never touched `state-graph`/`add-node`/`compile-graph` at all --
  despite this namespace's own former docstring calling it \"the
  langgraph-clj StateGraph\". That claim was false; this is the real
  thing. Two concrete structural bugs the old pipeline had, now fixed
  (the exact same bug shape as `cloud-itonami-isic-932`'s
  `amusementfacilityops.operation` fix):

    1. `decide` never emitted `:action :commit` -- the `commit` step
       function existed but nothing in the old `decide` logic could ever
       route to it. Every clean, non-safety proposal fell through to
       `:request-approval` regardless of how low-risk the op was.
    2. `personalcareops.phase`'s auto-commit table (`phases`,
       `auto-commits-at-phase?`) was fully defined and unit-tested in
       isolation, but the old `decide` never called it -- phase rollout
       was inert decoration, not a real gate.
    3. (bonus, same family) the old `commit` step function called
       `store/commit-record!` but never `store/append-ledger!` -- so a
       committed proposal, unlike a held or escalated one, never actually
       left an audit-ledger trace. Fixed here: `:commit` now durably
       writes both the coordination-log AND the ledger, symmetric with
       `:hold`.

  Both (1) and (2) are fixed here by genuinely wiring
  `phase/auto-commits-at-phase?` into `:decide`: a clean, non-escalating
  proposal now actually reaches `:commit` when its op is in the current
  phase's auto-commit set, and is held (distinguished by
  `:reason :not-in-phase-auto-set`) otherwise.

  State machine:
  intake -> advise -> govern -> decide -+-> commit
                                         +-> request-approval -> commit
                                         +-> hold

  Everything the actor depends on is injected, so each is a swap, not a
  rewrite:
    - the Store   (`personalcareops.store/MemStore`, or any `Store` impl)
    - the Advisor (the SAME five per-op `personalcareops.advisor/advise-*`
                    fns the old pipeline called inline, unchanged --
                    dispatched from the `:advise` node by `:operation`,
                    see `advise-request` below)
    - the Phase   (0->3 rollout; passed per-request via `:phase-num`,
                    not frozen at `build` time)

  One graph run = one personal-care coordination request. No unbounded
  inner loop -- each run is auditable and checkpointed. Every
  commit/hold decision fact lands in `personalcareops.store`'s
  append-only ledger (`store/append-ledger!`).

  Human-in-the-loop = real approval workflow:
  `interrupt-before #{:request-approval}` pauses the actor at the
  `:request-approval` node until a human operator resumes it with a
  decision. `:flag-safety-concern` ALWAYS reaches this node -- see
  `escalate?` below, which agrees with `personalcareops.governor`'s
  scope-exclusion allowance for that op and with
  `personalcareops.phase/always-escalates?` at phase 3."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [personalcareops.store :as store]
            [personalcareops.advisor :as advisor]
            [personalcareops.governor :as governor]
            [personalcareops.phase :as phase]))

;; ---------------------- portable timestamp ----------------------

(defn- now []
  #?(:clj (java.util.Date.)
     :cljs (js/Date.)))

;; ---------------------- advisor dispatch (unchanged domain logic) --------
;; The SAME case dispatch the old hand-rolled `advise` step function used,
;; calling the SAME five `personalcareops.advisor/advise-*` fns, unchanged.

(defn advise-request
  "Dispatch `request` to the matching `personalcareops.advisor/advise-*`
  fn by `:operation`. Returns a proposal map, or
  `{:status :error :reason ...}` for an unrecognized op."
  [store request]
  (case (:operation request)
    :schedule-service-appointment
    (advisor/advise-appointment-proposal
     store (:client-id request) (:service-type request)
     (:scheduled-date request))

    :coordinate-service-status-update
    (advisor/advise-status-proposal
     store (:client-id request) (:status-type request)
     (:status-value request))

    :coordinate-supply-request
    (advisor/advise-supply-request
     store (:client-id request) (:supply-type request)
     (:quantity request) (:requested-delivery-date request))

    :schedule-staff-shift-proposal
    (advisor/advise-shift-proposal
     store (:staff-id request) (:shift-date request)
     (:shift-type request))

    :flag-safety-concern
    (advisor/advise-safety-concern
     store (:concern-type request)
     (:description request) (:severity request))

    {:status :error :reason "Unknown operation type" :operation (:operation request)}))

;; ---------------------- subject id (client-id, or staff-id for shifts) ---

(defn- subject-id
  [proposal]
  (or (:client-id proposal) (:staff-id proposal)))

;; ---------------------- audit-fact builders ----------------------
;; Same ledger-fact shape the pre-graph pipeline used for :hold/:escalate
;; ({:timestamp .. :status .. :violations ..}), now also written for
;; :commit (previously a gap -- see docstring bonus-fix #3 above).

(defn- hold-fact
  [request proposal violations reason]
  {:timestamp (now)
   :operation (:operation request)
   :client-id (subject-id proposal)
   :status :held
   :reason reason
   :violations violations})

(defn- commit-fact
  [request proposal approval]
  (cond-> {:timestamp (now)
           :operation (:operation request)
           :client-id (subject-id proposal)
           :status :committed
           :proposal proposal}
    approval (assoc :approved-by (:by approval))))

(defn- approval-requested-fact
  [request proposal phase-num reason]
  {:timestamp (now)
   :operation (:operation request)
   :client-id (subject-id proposal)
   :status :pending-approval
   :reason reason
   :phase phase-num
   :confidence (get-in proposal [:advisor :confidence])})

;; ---------------------- escalation predicate ----------------------

(defn escalate?
  "A clean proposal escalates to human approval (`:request-approval`, a
  real `interrupt-before` pause) when: the advisor itself flagged it
  (`:escalate?`, currently only ever set by
  `advisor/advise-safety-concern`), OR the op is `:flag-safety-concern`
  (belt-and-suspenders -- agrees with
  `personalcareops.phase/always-escalates?` at phase 3, and with
  `personalcareops.governor`'s allowance for that op alone in its
  scope-exclusion scan)."
  [proposal phase-num]
  (boolean (or (:escalate? proposal)
               (= :flag-safety-concern (:operation proposal))
               (phase/always-escalates? phase-num (:operation proposal)))))

;; ---------------------- compiled StateGraph ----------------------

(defn build
  "Compiles an OperationActor graph bound to `store`. opts:
    :checkpointer -- a `langgraph.checkpoint/Checkpointer`
                     (default: in-memory `cp/mem-checkpointer`)

  The compiled graph's input map: `{:request .. :phase-num ..}` (phase is
  per-request, not frozen at `build` time)."
  [store & [{:keys [checkpointer]
             :or {checkpointer (cp/mem-checkpointer)}}]]
  (-> (g/state-graph
       {:channels
        {:request {:default nil}
         :phase-num {:default 0}
         :proposal {:default nil}
         :violations {:default nil}
         :decision {:default nil}
         :approval {:default nil}
         :audit {:reducer into :default []}}})

      (g/add-node :intake (fn [s] s))

      (g/add-node :advise
        (fn [{:keys [request]}]
          {:proposal (advise-request store request)}))

      (g/add-node :govern
        (fn [{:keys [proposal]}]
          {:violations (:violations (governor/govern store proposal))}))

      (g/add-node :decide
        (fn [{:keys [request proposal violations phase-num]}]
          (let [clean? (empty? violations)
                escalating? (and clean? (escalate? proposal phase-num))
                auto-commit? (and clean? (not escalating?)
                                   (phase/auto-commits-at-phase?
                                    phase-num (:operation proposal)))]
            (cond
              ;; HARD governor violations are a permanent block -- NEVER
              ;; routed through human approval, straight to :hold.
              (not clean?)
              {:decision :hold
               :audit [(hold-fact request proposal violations :governor-violation)]}

              escalating?
              {:decision :escalate
               :audit [(approval-requested-fact
                        request proposal phase-num
                        (if (= :flag-safety-concern (:operation proposal))
                          :always-escalate
                          :advisor-escalation))]}

              auto-commit?
              {:decision :commit}

              :else
              {:decision :hold
               :audit [(hold-fact request proposal violations :not-in-phase-auto-set)]}))))

      (g/add-node :request-approval
        (fn [{:keys [request proposal approval violations]}]
          (if (= :approved (:status approval))
            {:decision :commit
             :audit [{:timestamp (now) :operation (:operation request)
                      :client-id (subject-id proposal)
                      :status :approval-granted :by (:by approval)}]}
            {:decision :hold
             :audit [(assoc (hold-fact request proposal violations :approver-rejected)
                            :status :approval-rejected)]})))

      (g/add-node :commit
        (fn [{:keys [request proposal approval]}]
          (let [record {:proposal proposal :timestamp (now) :status :committed}]
            (store/commit-record! store record)
            (let [f (commit-fact request proposal approval)]
              (store/append-ledger! store f)
              {:audit [f]}))))

      (g/add-node :hold
        (fn [{:keys [audit]}]
          (when-let [hf (last (filter #(#{:held :approval-rejected} (:status %)) audit))]
            (store/append-ledger! store hf))
          {}))

      (g/set-entry-point :intake)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)

      (g/add-conditional-edges :decide
        (fn [{:keys [decision]}]
          (case decision
            :commit :commit
            :escalate :request-approval
            :hold)))

      (g/add-conditional-edges :request-approval
        (fn [{:keys [decision]}]
          (if (= :commit decision) :commit :hold)))

      (g/set-finish-point :commit)
      (g/set-finish-point :hold)

      (g/compile-graph
       {:checkpointer checkpointer
        :interrupt-before #{:request-approval}})))
