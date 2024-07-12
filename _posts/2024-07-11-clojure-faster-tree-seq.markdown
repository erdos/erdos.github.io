---
layout: post
title: "Faster tree-seq in Clojure with transducers"
date: 2024-07-11 23:00:00 +0200
tags:
 - clojure
 - transducers
 - programming
---

# Faster tree-seq in Clojure with transducers

The [tree-seq](https://clojuredocs.org/clojure.core/tree-seq) is one of my favourite functions in Clojure. It can be used to lazily iterate in any tree-like data structure in a depth-first order. The definition of the function goes something like this:

```clojure
(defn tree-seq [branch? children root]
  ((fn walk [node]
     (lazy-seq
      (cons node
            (when (branch? node)
              (mapcat walk (children node)))))) root))
```

Let's create a big tree and see how it works. Quickly create an _n-ary_ tree of height _h_ where `n=4` and `h=10`.

```clojure
(defn ntree [n h]
  (nth (iterate (partial repeat n) :leaf) h))

(def deep-tree (ntree 4 10))
```

How does such a tree look like? Calling `(ntree 2 3)` gives `(((:leaf :leaf) (:leaf :leaf)) ((:leaf :leaf) (:leaf :leaf)))` and calling `(ntree 3 2)` gives `((:leaf :leaf :leaf) (:leaf :leaf :leaf) (:leaf :leaf :leaf))`.

Of course, `(ntree 4 10)` is much bigger. To see how much, run `tree-seq`, then `count` all nodes and also see how long it was running:

```clojure
(->> deep-tree
     (tree-seq coll? seq)
     (count)
     (time)
     (println "All node count"))
; user=> "Elapsed time: 1125.355003 msecs"
; All node count 1398101
```

The result sounds correct, given the number of nodes equals `N=(n^(h+1)-1)/(n-1)`. However, the performance could be improved, so lets do a rewrite with transducers. First of all, we need a function that will reduce a tree node with `rf` if it is a branch, and just skip it otherwise.

```clojure
#_ ;; pseudocode, will not compile
(defn reduce-node [rf a x]
  (if (branch? x)
    (reduce rf a (children x))
    a))
```

Then comes the actual transducer: for each element in the collection, try to recursively further reduce it.

```clojure
#_ ;; pseudocode, will not compile
(defn xf [rf]
  (fn inner
    ([]    (rf))
    ([x]   (rf x))
    ([a x] (reduce-node inner (rf a x) x))))
```

We are also going to utilize  the [preserving-reduced](https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L7612C1-L7617C14) trick from `clojure/core.clj`. It helps make sure that a reduced value stays reduced even when passed through a chain of reduce functions. So the actual `xf` function is closer to something like this:


```clojure
#_ ;; pseudocode, will not compile
(defn- preserve-reduced [rf]
  (fn [a b]
    (let [r (rf a b)]
      (if (reduced? r)
        (reduced r)
        r))))

#_ ;; pseudocode, will not compile
(defn xf [rf]
  (fn inner
    ([]    (rf))
    ([x]   (rf x))
    ([a x] (reduce-node (preserve-reduced inner) (rf a x) x))))
```

Of course it will not compile, because `branch?` and `children` are undefined. To resolve this, we assemble it into a single `letfn` form to give it some function and supply the missing functions as arguments:

```clojure
(defn tree-transducer [branch? children]
  (fn [rf]
    (letfn [(step [a x]
              (if (branch? x)
                (reduce preserve-reduced a (children x))
                a))
            (inner
              ([]    (rf))
              ([x]   (rf x))
              ([a x] (step (rf a x) x)))
            (preserve-reduced [a b]
              (let [result (inner a b)]
                (if (reduced? result)
                  (reduced result)
                  result)))]
      inner)))
```

We have a transducer now and we can use it to build an [eduction](https://clojuredocs.org/clojure.core/eduction):

```clojure
(defn tree-eduction [branch? children root]
  (eduction (tree-transducer branch? children) [root]))
```

Testing it yields:

```clojure
(->> deep-tree
     (tree-eduction coll? seq)
     (vec)
     (count)
     (println "All node count with tree-eduction:")
     (time))
; All node count with tree-eduction: 1398101
; "Elapsed time: 54.186769 msecs"
```

## Flatten

We can reuse the transducer to reimplement [flatten](https://clojuredocs.org/clojure.core/flatten) as a transducer. Test it with a small expression counting the number of leaves in the tree. First, with just `flatten` for a baseline:

```clojure

(->> deep-tree
     (flatten)
     (count)
     (println "Count of leaves with flatten:")
     (time))
; Count of leaves with flatten: 1048576
; "Elapsed time: 1128.76338 msecs"
```

The same calculation with transducers yields:

```clojure
(defn flatten-transducer []
  (comp (tree-transducer sequential? seq)
        (remove sequential?)))

(->> deep-tree
     (transduce (comp (flatten-transducer)
                      (map (constantly 1)))
                + 0)
     (println "Count of leaves with flatten-transducer:")
     (time))
; Count of leaves with flatten-transducer: 1048576
; "Elapsed time: 76.234496 msecs"
```

## Summary

We gained a bit of a speedup by getting rid of temporary object allocations. However, the new approach does not differ only by implementation. Like usually with engineering, a tradeoff was made: This new function returns an *eduction* instead of a *sequence*. To highlight the main differences:

- An eduction is not a collection. Calling [coll?](https://clojuredocs.org/clojure.core/coll_q) will return `false`.
- As shown in the code, we cannot call [count](https://clojuredocs.org/clojure.core/count) on it without first converting it to a collection. Calling [vec](https://clojuredocs.org/clojure.core/vec) on it will reduce it into a vector.
- The result of `eduction` is not caching. Calling `seq` on it (or reducing it, for example, into a vector) will evaluate the whole thing again and again every time and allocate a new sequence.

Keeping the differences in mind, I still sometimes optimize out parts of code to use reducers and eductions, which often proves to be a low-hanging fruit.
