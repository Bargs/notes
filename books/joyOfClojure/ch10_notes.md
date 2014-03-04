Mutation and Concurrency
===============================

Clojure takes a different approach than Java to mutability and concurrent programming. Instead of relying on shared state with fine grained locks, Clojure tries to reduce mutability as much as possible.

When mutation is needed, Clojure provides 4 major mutable references: refs, agents, atoms and vars.


Refs and Clojure's Software Transactional Memory (STM)
------------------------------------------------------

> STM is a non-blocking way to coordinate concurrent updates between related mutable value cells.

Three important terms that form the foundation for Clojure's model of state management and mutation:

* Time - The relative moments when events occur
* State - A snapshot of an entity's properties at a moment in time
* Identity - The logical entity identified by a common stream of states occuring over time.

When dealing with identities in Clojure's model, you're receiving a snapshot of its properties at a moment in time, not necessarily the most recent.

Clojure's STM works with transactions demarked by the `dosync` form. A transaction builds a set of changeable data cells that should all change together. Like a database transaction, a Clojure transaction is all or nothing.

Clojure's four reference types are good at different things. The following table describes their features:


              Ref  Agent  Atom  Var
Coordinated    x
Asynchronous         x
Retriable      x           x
Thread-local                     x


* Coordinated - Reads and writes to multiple refs can be made in a way that guarantees no race conditions
* Asynchronous - The request to update is queued to happen in another thread some time later, while the thread that made the request continues immediately
* Retriable - Indicates that the work done to update a reference's value is speculative and may have to be repeated
* Thread-local - Thread safety is achieved by isolating changes to state to a single thread


The value for each reference type is accessed in the same way, using the `@` reader feature or the `deref` function. The write mechanism for each type is unique. All reference types provide *consistency* by allowing the association of a validator function via `setvalidator`.

`ch10_exercises.clj` delves into the details of refs with a mutable game board example.

### Transactions

Clojure's STM doesn't rely on locking mechanisms like Java's `synchronized` so it can't cause deadlocks.

Behind the scenes it uses *multiversion concurrency control (MVCC)* to ensure *snapshot isolation*.

Snapshot isolation means each transaction gets its own view of the data. The snapshot is made up of reference values that only that transaction has access to. Once the transaction completes, the values of the in transaction refs are compared with the target refs. If no conflicts are found, the changes are committed. If there are conflicts the transaction may be retried.

### Embedded Transactions

Unlike some systems, Clojure doesn't provide allow nested transactions to limit the scope of a restart. If a nested transaction encounters a conflict, the entire enclosing transaction must be restarted. Clojure only has one transaction at a time per thread.


### Good things about the STM

1. STM provides your application with a consistent view of its data
2. No need for locks
3. Give you the ACI part of ACID

### Bad things about the STM

1. Write skew. Only updated refs are checked for conflicts when a transaction commits. So if two transactions run concurrently, one reading a value and the other writing to it, both can commit without conflict. This is undesirable because the behavior of the first transaction is probably affected by the value of the ref, even though it doesn't write to it. This anomaly is called *write skew*.

2. Live lock - a set of transactions that repeatedly restart one another. Clojure combats live lock in a couple ways. One, there are transaction restart limits that will raise an error. Two, Clojure implements *barging* in the STM, which means older transactions are allowed to run while younger transactions have to retry.


### Things you shouldn't do in a transaction

1. IO. Transaction retrys could execute the IO operation over and over. When performing IO, it's useful to use the `io!` macro which will throw an error if accidentally used in a transaction.

2. Object mutation. It often isn't idempotent, which will be a problem if the transaction must retry.

3. Large units of work. Get in and get out as quickly as possible.


### Commutative change

If you have an update function that's commutative (order of operands is unimportant) then you can reduce the number of transaction retrys by using the `commute` function instead of `alter`. `commute` will run the update function once on the in-transaction value of the ref and once again on the value of the ref at commit time to determine what value should get committed. This makes your app more concurrent if you can put up with two conditions:

1. The value you see in-transaction may not be the value that gets committed at commit time.
2. The function you give to `commute` will be run at least twice - once to compute the in-tranaction value, and again to compute the commit value. It might be run any number of times.

### Vulgar change

The `ref-set` function can be used to set a ref to a provided value, unlike `alter` and `commute` which take an update function instead of an explicit value.