---
layout: post
title: "Clojure collection quirks - Vectors"
date: 2016-05-17 08:00:02 +0200
tags:
 - clojure
 - programming
 - immutable
---

Working with Clojure collections is a pleasure. Yet, sometimes one may run into unexpected behaviors. In this series, we take a look at the most simple Clojure data structures.

## Vectors

Vectors are sequential collections with constant time random access capability. One can also view vectors as integer to element mappings. Examples:

{% highlight clojure %}
(def v1 [:zero :one :two :three])

(get v1 1) => :one
(find v1 1) => [1 :one]
{% endhighlight %}

### Access

You can acces an element on the nth index with the `get` function or the `nth` function. Be aware that when a given index is not found the `get` function returns `nil` while the `nth` function throws an `IndexOutOfBoundsException`. You can specify a default value as a third argument that is returned when the index is not found.

{% highlight clojure %}
(nth v1 1) => :one
(get v1 1) => :one
(get v1 -3) => nil
{% endhighlight %}

Vectors also act like functions (they implement the `clojure.lang.IFn` interface). An IOOBE exception is thrown when called on an uknown index.

{% highlight clojure %}
(v1 1) => :one
(v1 9) => IndexOutOfBoundsException!!
{% endhighlight %}

### Modify

We can also modify an item on an index of a vector:

{% highlight clojure %}
(assoc v1 1 :One) => [:zero :One :two :three]
{% endhighlight %}

That's right, one can change an item on an index of a vector just like with a map.

How is about removing an item from a vector?

{% highlight clojure %}
(dissoc v1 1) => ClassCastException!!
{% endhighlight %}

You can not call `dissoc` on a vector because that could not remove an item without affecting the indices of all other items after the given index. To remove an item, first you need to split the vector into two parts (calling `subvec`) and then create a vector from the concatenation of the result.

{% highlight clojure %}
(defn without [v index]
  (vec (concat 
    (subvec v 0 index) 
    (subvec v (inc index)))))
{% endhighlight %}

You can use the `subvec` function to return part of a vector.

{% highlight clojure %}
(subvec [0 1 2 3 4] 3) => [3 4]
(subvec [0 1 2 3 4] 1 3) => [1 2]
{% endhighlight %}

### Usage as queues

Vectors are often used as queues.

- The `conj` function appends a value to the end of the vector. 
- The `peek` function returs the last item of the vector in constant time. (The `last` function is linear time thus much slower usually.) 
- The `pop` function drops the last item from a vector.

### Sequences

You can turn a vector to a sequence with the `seq` function (note that `(seq [])` is `nil`). The `rseq` function returns a lazy sequence of items in the vector in reverse order. It is advised to favor `rseq` over `reverse` because of the constant time complexity and lazyness.


