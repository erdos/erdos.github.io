---
layout: post
title: "Better macroexpand-all for Clojure"
date: 2026-11-20 14:00:00 +0100
tags:
 - clojure
 - programming
---

# Better macroexpand-all for Clojure

This is a small note about an aspect of macroexpansion I find keep returning to.

According to the docs, what `clojure.walk/macroexpand-all` does is that it `Recursively performs all possible macroexpansions in form.`.
It is a bit tricky though, because calling this macro does not always give the same result as the macroexpansion step of the Clojure compiler would. Take the following simple expression:

```clojure
(let [] '(-> a b c)))
```

When evaluated as Clojure code, it expands to `(let* [] '(-> a b c))` which evaluates to just `(-> a b c)`. That is, a list with four symbols.

However, when called with `macroexpand-all`, the expression in the quoted form also gets expanded, so the result will be `(let* [] (quote (c (b a))))`. Notice that the `->` (also known as `thread-first`) macro call disappeared from within the quoted form. But quoted forms should not be macroexpanded.

## An improvement

So it seems like the macro is a bit just too eager to expand everything. Looking at the source code gives us the reason too:

```clojure
(defn macroexpand-all [form]
  (prewalk (fn [x] (if (seq? x) (macroexpand x) x)) form))
```

That is, it tries to go through all forms in pre-order, depth-first, and expand all seq (list) forms. Since we do not want to expand quote forms, an improvement with the additional check would look something like the following:

```clojure
(defn- quoted? [x]
  (and (seq? x) (= 'quote (first x))))

(defn macroexpand-all [form]
  (prewalk
   (fn [x]
     (if (quoted? x)
       x
       (macroexpand x)))
   form))
```

Much better.