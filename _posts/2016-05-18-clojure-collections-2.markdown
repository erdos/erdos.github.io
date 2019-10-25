---
layout: post
title: "Clojure collection quirks - Maps"
date: 2016-05-18 13:00:02 +0200
tags:
 - clojure
 - programming
 - immutable
---

## Maps

Map data structures in Clojure are much like associative arrays in other languages. They, however contain many never seen possibilities.

### Creation

You can create maps with two syntaxes. Either by calling the `hash-map` function or by writing the `{}` map literal.

{% highlight clojure %}
(def m1 {:one :a, :two :b, :three :c, :four :d})
{% endhighlight %}

One interesting fact when working with maps is that all maps with *8 or less entries* maps will be created as a `PersistentArrayMap` an all larger maps will be a `PersistentHashMap`. The two differ only by the internal representation: small maps are better stored in an array.

### Sequences

Calling `seq` on a map returns a sequence of its key-value pairs, called entries. They are instances of `clojure.lang.MapEntry` but they also act like vectors.

{% highlight clojure %}
(seq m1)
  => ([:one :a] [:two :b] [:three :c] [:four :d])
(type (first m1))
  => clojure.lang.MapEntry
(vector? (first m1))
  => true
{% endhighlight %}

### Access

You can acces a value for a given key in a map with the `get` function.

{% highlight clojure %}
(get {:a 1 :b 2} :a)
  => 1
(get {:a 1 :b 2} :c)
  => nil
(get {:a 1 :b 2} :c :default-value)
  => :default-value
{% endhighlight %}

Maps also act like functions (they implement the `clojure.lang.IFn` interface). For example:

{% highlight clojure %}
(map {true :even false :odd}
     (map even?
          (range 100)))
 => (:even :odd :even :odd ...)
{% endhighlight %}

Please note that calling a map on a missing key returns `nil` or the default value given as an optional third argument.

### Modify

Use the `assoc` function to associate a new value with a given key. The result of the function call
is a new copy of the parameter with a value replaced. The original map is kept intact.
