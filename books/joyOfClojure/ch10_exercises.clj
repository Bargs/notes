
;; Defining a utility that will let us throw a bunch of
;; threads at a given function
(ns joy.mutation
  (:import java.util.concurrent.Executors))

(def thread-pool
  (Executors/newFixedThreadPool
   (+ 2 (.availableProcessors (Runtime/getRuntime)))))

(defn dothreads!
  [f & {thread-count :threads
        exec-count :times
        :or {thread-count 1 exec-count 1}}]
  (dotimes
    [t thread-count]
    (.submit thread-pool #(dotimes [_ exec-count] (f)))))


;; Creating a mutable game board with refs

(def
  initial-board
  [[:- :k :-]
   [:- :- :-]
   [:- :K :-]])

(defn board-map [f board]
  (vec (map #(vec (for [s %] (f s)))
            board)))

(defn reset-board!
  "Resets the board state. Generally these types
  of functions are a bad idea, but matters of page
  count force our hand."
  []
  (def board (board-map ref initial-board))
  (def to-move (ref [[:K [2 1]] [:k [0 1]]]))
  (def num-moves (ref 0)))

;; Neighbors function from chapter 5.
(defn neighbors
  ([size yx]
   (neighbors [[-1 0] [1 0] [0 -1] [0 1]]
              size
              yx))
  ([deltas size yx]
   (filter (fn [new-yx]
             (every? #(< -1 % size) new-yx))
           (map #(vec (map + yx %))
                deltas))))


(def king-moves
  (partial neighbors
           [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] 3))

(defn good-move?
  [to enemy-sq]
  (when (not= to enemy-sq)
    to))

(defn choose-move
  "Randomly choose a legal move"
  [[[mover mpos] [_ enemy-pos]]]
  [mover (some #(good-move? % enemy-pos)
               (shuffle (king-moves mpos)))])


(defn place [from to] to)

(defn move-piece [[piece dest] [[_ src] _]]
  (alter (get-in board dest) place piece)
  (alter (get-in board src ) place :-_)
  (alter num-moves inc))

(defn update-to-move [move]
  (alter to-move #(vector (second %) move)))

;; Here there be dragons
;; With two separate `dosync` forms, our updates are spread across more
;; than one transaction. This will cause troubles, as we'll see below.
(defn make-move []
  (let [move (choose-move @to-move)]
    (dosync (move-piece move @to-move))
    (dosync (update-to-move move))))


(reset-board!)

(make-move)

(board-map deref board)

(make-move)

(board-map deref board)

;; Things look ok so far...
;; Let's see what happens when we run it in
;; a concurrent execution environment
(dothreads! make-move :threads 100 :times 100)

;; In all likelyhood, this shows us a broken board.
(board-map deref board)


;; An improved make-move with only one transaction.
(defn make-move-v2 []
  (dosync
   (let [move (choose-move @to-move)]
     (move-piece move @to-move)
     (update-to-move move))))

(reset-board!)

(make-move)

(board-map deref board)

@num-moves

(dothreads! make-move-v2 :threads 100 :times 100)
(board-map #(dosync (deref %)) board)

@to-move

@num-moves


;; Stressed refs

;; Runs a slow transaction in a separate thread while a loop with short transactions
;; quickly modifies the ref the slow transaction is trying to read
(defn stress-ref [r]
  (let [slow-tries (atom 0)]
    (future
      (dosync
       (swap! slow-tries inc)
       (Thread/sleep 200)
       @r)
      (println (format "r is: %s, history: %d, after: %d tries"
                       @r (.getHistoryCount r) @slow-tries)))
    (dotimes [i 500]
      (Thread/sleep 10)
      (dosync (alter r inc)))
    :done))

;; The long running transaction probably won't finish until the short
;; transactions are finished
(stress-ref (ref 0))

;; We can increase the max history for the ref if mixing long and short
;; transactions is inevitable in our application.
;; The long transaction manages to finish first here, but it still has
;; to retry a good number of times.
(stress-ref (ref 0 :max-history 30))

;; If we're doing real work in the long running transaction, retrying so many
;; times could get very wasteful. If we know the history is going to have to
;; be fairly long, we can start it out at a larger min length instead of increasing
;; it each try starting at 0
(stress-ref (ref 0 :min-history 15 :max-history 30))


;; Agents

;; Creating an agent and sending an action to it
(def joy (agent []))
(send joy conj "First edition")

@joy

;; Agent actions are asyncronous
(defn slow-conj [coll item]
  (Thread/sleep 1000)
  (conj coll item))

;; The return value will still show the old value of the agent since
;; the action is still running asyncronously.
(send joy slow-conj "Second Edition")

;; Execute this a little later, than the new value has taken over
@joy


;; send vs send-off

(defn exercise-agents [send-fn]
  (let [agents (map #(agent %) (range 10))]
    (doseq [a agents]
      (send-fn a (fn [_] (Thread/sleep 1000))))
    (doseq [a agents]
      (await a))))

;; send-off will complete in about a second since each
;; agent gets its own thread.
(time (exercise-agents send-off))

;; send will take longer because all the agents must run
;; their actions on a shared thread pool, and the sleeps
;; will clog up the pool.
(time (exercise-agents send))


;; Atomic memoization
(defn manipulable-memoize [function]
  (let [cache (atom {})]
    (with-meta
      (fn [& args]
        (or (second (find @cache args))
            (let [ret (apply function args)]
              (swap! cache assoc args ret)
              ret)))
      {:cache cache})))

(def slowly (fn [x] (Thread/sleep 1000) x))
(time [(slowly 9) (slowly 9)])

(def sometimes-slowly (manipulable-memoize slowly))

(time [(sometimes-slowly 108) (sometimes-slowly 108)])

(meta sometimes-slowly)

(let [cache (:cache (meta sometimes-slowly))]
  (swap! cache dissoc '(108)))

(meta sometimes-slowly)

(time [(sometimes-slowly 108) (sometimes-slowly 108)])


;; Using locks

(ns joy.locks
  (:refer-clojure :exclude [aget aset count seq])
  (:require [clojure.core :as clj])
  (:use [joy.mutation :only (dothreads!)]))

;; Creating a protocol for the safe concurrent modification of arrays
(defprotocol SafeArray
  (aset [this i f])
  (aget [this i])
  (count [this])
  (seq [this]))

;; This won't work very well
(defn make-dumb-array [t sz]
  (let [a (make-array t sz)]
    (reify
      SafeArray
      (count [_] (clj/count a))
      (seq [_] (clj/seq a))
      (aget [_ i] (clj/aget a i))
      (aset [this i f]
            (clj/aset a
                      i
                      (f (aget this i)))))))

;; Abuse our new protocol implementation a bit
(defn pummel [a]
  (dothreads! #(dotimes [i (count a)] (aset a i inc))
              :threads 100))

(def D (make-dumb-array Integer/TYPE 8))

;; Let 'er rip
(pummel D)

;; Running that function in 100 concurrent threads should result in
;; the value 100 being in each index of the array. As you can see, that
;; is not the case.
(seq D)


;; To ensure threadsafe modification of a mutable object, we need to
;; use the locking macro. Note that `locking` gets called twice
;; during `aset` (once in aset and again in aget). This is ok because
;; `locking` is *reentrant*.
(defn make-safe-array [t sz]
  (let [a (make-array t sz)]
    (reify
      SafeArray
      (count [_] (clj/count a))
      (seq [_] (clj/seq a))
      (aget [_ i] (locking a (clj/aget a i)))
      (aset [this i f]
            (locking a
              (clj/aset a
                        i
                        (f (aget this i))))))))

(def A (make-safe-array Integer/TYPE 8))

(pummel A)

(seq A)


;; Creating a finer grained locking system using java.util.concurrent

(defn lock-i [target-index num-locks]
  (mod target-index num-locks))

(import 'java.util.concurrent.locks.ReentrantLock)

;; We'll use a pool of locks equal to half the size of the array count.
;; This is called lock striping.
;; Note the release of locks in finally blocks to prevent leaking locks
;; in the case of errors.
(defn make-smart-array [t sz]
  (let [a (make-array t sz)
        Lsz (/ sz 2)
        L (into-array (take Lsz
                            (repeatedly #(ReentrantLock.))))]
    (reify
      SafeArray
      (count [_] (clj/count a))
      (seq [_] (clj/seq a))
      (aget [_ i]
            (let [lk (clj/aget L (lock-i (inc i) Lsz))]
              (.lock lk)
              (try
                (clj/aget a i)
                (finally (.unlock lk)))))
      (aset [this i f]
            (let [lk (clj/aget L (lock-i (inc i) Lsz))]
              (.lock lk)
              (try
                (clj/aset a
                          i
                          (f (aget this i)))
                (finally (.unlock lk))))))))


(def S (make-smart-array Integer/TYPE 8))

(pummel S)

(seq S)


;; Vars

;; Evaluating a var's name gives you its value
*read-eval*

;; To get the var object itself, you must use the `var` operator
(var *read-eval*)

;; #' is a reader feature. It is the same as the `var` operator.
#'*read-eval*

;; The binding macro
(defn print-read-eval []
  (println "*read-eval* is currently" *read-eval*))

;; `binding` can be used to bind a new value to a var in thread local
;; That value will remain in effect until the `binding` block is done
(defn binding-play []
  (print-read-eval)
  (binding [*read-eval* false]
    (print-read-eval))
  (print-read-eval))

(binding-play)

;; Vars are created with def and its other forms
(def foo 3)
foo
#'foo
(var foo)
'foo

;; Anonymous vars (bound to locals, no namespace or name) can be created
;; with `with-local-vars`. To get the value of these vars you must use
;; `deref` or `var-get`.
(def x 42)
{:outer-var-value x
 :with-locals (with-local-vars [x 9]
                {:local-var x
                 :local-var-value (var-get x)})}


;; Dynamic scope

;; `with-precision` uses `binding` to set `clojure.core/*math-context*`
;; Without it, dividing a BigDecimal might complain
(/ 1M 3)

;; By setting the *math-context* var we tell BigDecimal how to round
(with-precision 4 (/ 1M 3))

;; But here's where the dynamic binding of vars can get complext
;; This blows up because `map` is lazy. By the time the REPL tries to
;; realize the lazy seq returned by `map`, it has already left the
;; *dynamic scope* of `with-precision`. Because *math-context* is a var
;; it doesn't care that it's in the lexical scope of `with-precision`.
(with-precision 4
  (map (fn [x] (/ x 3)) (range 1M 4M)))

;; One solution is to force map not to be lazy with `do-all`
(with-precision 4
  (doall (map (fn [x] (/ x 3)) (range 1M 4M))))


;; But that's not really ideal. Instead, we can tell the function
;; passed to map to use the dynamic scope that it was created in
(with-precision 4
  (map (bound-fn [x] (/ x 3)) (range 1M 4M)))
