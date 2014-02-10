Chapter 9 Combining Data and Code
============================================


Namespaces
--------------------------------------------

Namespaces can be thought of as two level look ups. The first part looks up a namespace map, the second is a symbol in that namespace that refers to a var.

### Creating namespaces

There are multiple ways to create a namespace:

#### ns

Using `ns` creates a namespace (if it doesn't exist) and switches to it. It includes all classes in `java.lang` and functions, macros, and special forms in `clojure.core`.

#### in-ns

Includes `java.lang` but not `clojure.core`. Takes a quoted symbol as the namespace qualifier. Switches to the namespace in addition to creating it.

```
(in-ns 'gibbon)

(reduce + [1 2 (Integer. 3)])
; java.lang.Exception: Unable to resolve symbol: reduce in this context

(clojure.core/refer 'clojure.core)
(reduce + [1 2 (Integer. 3)])
;=> 6
```

#### create-ns

Gives the programmer the most control over the namespace they're creating. Doesn't automatically switch to the new namespace, instead returns it. Imports Java class mappings, but not `clojure.core`.

`create-ns` is meant for advanced techniques. When you have a namespace object you can programmtically modify its map of symbols using `intern` (adds a binding), `ns-unmap` (removes a binding), and `remove-ns` (removes the entire namespace). See `ch9_exercises.clj` for example usages. If you need `clojure.core` functionality, you'll have to `intern` it. Because this manual importing can cause subtle bugs, `create-ns` is best used sparingly.


### Hiding your privates

Members of a namespace can be made private by attaching metadata to their var. That would look something like this:

```
(ns hider.ns)
(defn ^{:private true} answer [] 42)
```

For functions, the same can be accomplished with the shorthand macro `defn-`. For `def` and `defmacro`, however, the private metadata must be added manually.

### Inclusions and exclusions

Clojure gives you fine grained declarative control over imports in your namespace. These are provided as directives to the `ns` macro.

A namespace definition might look like this:

```
(ns joy.ns-ex
  (:refer-clojure :exclude [defstruct])
  (:use (clojure set xml))
  (:use [clojure.test :only (are is)])
  (:require (clojure [zip :as z]))
  (:import (java.util Date)
         (java.io File)))
```

`use` will load everything in the specified namespace so that its contents can be used without namespace qualification. This can lead to name conflicts, so it should almost always be used with `only` to only pull in what you need.

`require` will also load everything in a namespace, but its contents will need to be qualified when used. The qualification can be shortened by aliasing the namespace with `as`.

`import` is used to import Java classes and interfaces

`refer-clojure` is the same as `(refer 'clojure.core)`. It can be used in conjunction with the `exclude` filter to remove bindings to vars in clojure.core in your namespace to avoid conflicts if your lib needs to use those names.

`load` will load clojure code on the classpath, given a directory path.


Multimethods and the UDP
--------------------------------------------

Multimethods provide function polymorphism based on the result of an arbitrary dispatch function.