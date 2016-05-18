---
layout: post
title: "Indexed data structures in Clojure"
date: 2016-05-16 01:00:02 +0200
tags:
 - clojure
 - java
 - interop
 - programming
 - immutable
---

When rumbling in the <a href="https://github.com/clojure/clojure/tree/master/src/jvm/clojure/lang">Java source codes</a> of the Clojure language
I found the mysterious <a href="https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/IndexedSeq.java">IndexedSeq</a> interface. What is it good for?

## Details

It provides the `int index()` method for collections to return the current index of the sequence on them.
It extends `ISeq`, `Sequential`, `Counted` interfaces and thus `IPersistentCollection` and `Seqable` too.

## Usage

One can easily create a wrapper class that gives an indexed functionality to any collection. The following example does just that.

{% highlight clojure %}
(defn indexed
  "Creates an indexed data structure. You can access current index
  with the .index method call."
  ([s] (indexed s 0))
  ([s ^long init]
   (reify
     clojure.lang.IndexedSeq
     (index [_] init)
     clojure.lang.Counted
     (count [_] (count s))
     clojure.lang.ISeq
     (first [_] (first s))
     (next [_] (if-let [n (next s)]
                 (indexed n (inc init))))
     (more [_] (indexed (.more (seq s)) (inc init)))
     (cons [_ x] (indexed (cons x s) init))
     (empty [_] (indexed (empty s) 0))
     (equiv [this that] (= that s))
     (seq [this] (if (seq s) this))
     clojure.lang.Sequential
     )))
{% endhighlight %}

## Testing

{% highlight clojure %}
(def a1 (indexed (range 5 105)))

a1 ;=> (5 6 7 8 ... 104)

(.index a1) ;=> 0
(.index (next a1)) ;=> 1

(.index (take-last 2 a1)) ; => 98
;; ... etc.
{% endhighlight %}
