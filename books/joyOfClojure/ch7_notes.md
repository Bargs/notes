Chapter 7 Functional Programming
============================================

All About Functions
--------------------------------------------

### First Class Functions

Functions are first class in Clojure. What makes something first class?

* It can be created on demand.
* It can be stored in a data structure.
* It can be passed as an argument to a function.
* It can be returned as the value of a function.

#### Creating functions on demand

**Note**
> What does `apply` do? It calls the function passed as its first argument with the collection of arguments passed as its second argument. Essentially it allows you to pass a variable number of arguments if you don't know how many will be passed at compile time.

*Composition* is one way to create new functions on demand by smooshing together existing functions. Functions can be composed easily using the `comp` function which takes an arbitrary number of function arguments and returns a new function that applies each to the return value of the next function in line.

*Partial* functions can be created with the `partial` function. It takes a function and a list of arguments and returns a function that, when passed additional arguments, will apply the original function to the args passed to `partial` in addition to the new arguments.

```
((partial + 5) 100 200)
;=> 305
```

You can build the *complement* of a function using `complement`. This guy simply takes a function that returns a truthy value and builds a function that always returns the opposite truthy value for any given input.


#### Functions as arguments

Being able to pass functions as arguments is much better than having to rely on callback objects like you do in Java.

`sort-by` is a good example of the use of function args. In addition to a list to sort it can take a function argument that will be used to pre-process each list item. The sort will occur on the result of the application of this function to each list item. This can help when dealing with nested lists or heterogeneous lists of non-mutually comparable types.

The ability to pass functions as arguments allows you to build your logic on top of existing parts. This let's you focus on your application *instead of worrying about re-implementing core functionality*.


### Pure functions

What is a *pure function*?

* Always returns the same result, given the same arguments.
* Doesn't cause any observable side effects.

Why should you strive to use pure functions wenever possible?

1. [Referential transparency][1]. Pure functions are said to be referentially transparent, which means that the function could be replaced by its result without changing the behaviour of the program. This is only true because pure functions are unaffected by time, the same arguments will always produce the same results. Referential transparency helps the programmer and compiler reason about the code.

[1]: http://en.wikipedia.org/wiki/Referential_transparency_(computer_science)


### Named arguments

Named and default arguments can be created by using destructuring.


### Pre and post conditions

Clojure's `defn` allows you to assign pre and post conditions to a function. This is done by adding a map with `:pre` and `:post` keys in the function body. Each map entry value is a vector with a list of functions that all must return true, otherwise the function application will throw an `AssertionError`. You can also manually call `assert` to get similar functionality on an ad hoc basis.

These constraints can also be decoupled from the function body they are to be applied to. This is done by creating a higher order function that takes a function and its arguments as parameters and in its body simply calling that function. Then you add the constraints (the :pre and :post map) to this new function. Doing this allows for constraints specific to your use case without restricting how others use the main function. This can be a good way to remove business logic from functions that might otherwise be reusable.

These detached constraints can be thought of as *aspects*.


Closures
--------------------------------------------

What's a *closure*?

> In a sentence, a closure is a function that has access to locals from the context where it was created.

Basically a closure is a function that uses a local variable that was in scope at the time of the function's creation, but was defined outside of the function's body. The function is said to "close over" the variable.

Closures can be useful as arguments to higher-order functions. They can also get interesting when they close over objects with mutable state.

Closures can be used to build 'objects', bundling functions with the data they work on, by having multiple closures share the same environment. This could be done by creating a map with values that are both data and closures that work on that data. You can even implement a form of polymorphism by creating maps with the same keys but closures that do different things. However, Clojure has higher level concepts to handle some of this stuff so it's not necessary to create a bunch of ad hoc solutions.


Thinking Recursively
--------------------------------------------

Recursion is fun, but you can run into stack overflows when working with large values. Clojure provides some ways to deal with this.

Replacing *mundane* recursion with *tail recursion* that uses the `recur` form will help you avoid growing the stack. Usually a solution using regular recursion will be more clear and concise though. If you're working with seqs, being lazy might allow you to use regular recursion without running into stack overflows.

### Tail calls and recur

*Generalized tail-call optimization* works on any function call in the tail position. Clojure **does not provide generalized TCO** because java bytecode does not support it. The `recur` special form will only optimize self-calls in the tail position, not the general case.

You must explicitly use `recur` to benefit from tail call optimization. Even if a self-call is in tail position, it will not be optimized unless you use the `recur` special form. This is a design choice rather than a technical limitation. Why?

1. Using `recur` reminds the developer that Clojure does not provide general TCO.

2. Forcing the use of `recur` allows Clojure to detect errors when the developer thinks a call is in tail position, but is not. Without `recur` the call might be silently unoptimized.

3. `recur` allows `fn` and `loop` to act as anonymous recursion points instead of needing a name to reference.

### The trampoline

Mutually recursive calls can also be optimized to avoid stack overflows, though not with `recur`. You must follow a couple of rigid rules to gain this benefit:

1. Make all of the functions participating in the mutual recursion return a function instead of their normal result. Normally this is as simple as tacking a # onto the front of the outer level of the function body.

2. Invoke the first function in the mutual chain via the trampoline function.

The `trampoline` function optimizes mutual recursion by manually managing the stack instead of allowing mundane recursion to do the work. The book doesn't provide a ton of explanation, but my understanding is that this works because each function taking part in the recursion returns an anonymous function instead of immediately recurring. Allowing the function to return will pop it off the stack and then its resources can be deallocated. Then `trampoline` can go ahead and call the anonymous function, continuing the recursion. See `ch7_exercises.clj` for an example.

### Continuation-Passing Style (CPS)

The book gives a small overview of CPS, a style of programming common with functional languages. CPS is interesting because it can be used to create generic function builders. The same general computational structure can take different function arguments to produce new functions that do all sorts of different things, which allows for a great deal of reuse. See `ch7_exercises.clj` for an example.

CPS breaks down a problem into three parts:

1. An accept function that decides when a computation should terminate

2. A return continuation that’s used to wrap the return values

3. A continuation function used to provide the next step in the computation


However, CPS isn't often used in Clojure for reasons listed in the book.