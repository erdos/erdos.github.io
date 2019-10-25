---
layout: post
title: "Clojure - testing private vars"
date: 2019-10-25 23:00:00 +0200
tags:
 - clojure
 - programming
---

# How do we test private vars in Clojre?

How do we write unit tests for vars that are defined private?

## 1. Namespace for implementation

One approach is just not to use private vars. Private vars are hidden, because
they are part of the implementation of a functionality. As such, they are not
part of the public API of the software, and should not be accessed from the
outside world.

You could introduce a new namespace containing the implementation details. Lets
call it `yournamespace.impl`. The name indicates that this namespace contains
implementation details and should not be relied on directly. You can unit test
the vars if you define all vars publicly in this namespace.


## 2. Make private vars visible for testing

An other approach is to use Clojure's dynamic nature. You can access the namespaces
and define vars on the fly for testing:

{% highlight clojure %}
(let [target (the-ns 'testable-namespace)]
  (doseq [[k v] (ns-map target)
          :when (and (var? v) (= target (.ns v)))]
    (eval `(defn ~(symbol (str "-" k)) [~'& args#] (apply (deref ~v) args#)))))
{% endhighlight %}

This code snippet checks all functions in the `testable-namespace` namespace and
creates an alias for them in the current namespace. The aliases will have a `-`
prefix and you can use these names from your unit tests.

For example, if you have the following in `testable-namespace` ns:

{% highlight clojure %}
(defn- add [a b] (+ a b))
{% endhighlight %}

They you can test it like this in the unit test namespace.

{% highlight clojure %}
(deftest test-add
  (is (= 5 (-add 2 3))))
{% endhighlight %}

Note that we needed to write `-add` instead of `-add` in the test.
