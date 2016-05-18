---
layout: post
title: "Clojure collection quirks - Vectors"
tags:
 - clojure
 - programming
 - immutable
---

Working with Clojure collections is a pleasure. Yet, sometimes one may run into unexpected behaviors. In this series, we take a look at the most simple Clojure data structures.

## Vectors

Vectors are sequential collections with constant time random access capability.

One can also view vectors as integer to element mappings. Examples:

{% highlight clojure %}
(def v1 [:zero :one :two :three])

(get v1 1) => :one
(find v1 1) => [1 :one]
{% endhighlight %}

Vectors also act like functions (they implement the `clojure.lang.IFn` interface):

{% highlight clojure %}
(v1 1) => :one
{% endhighlight %}

But be aware not to index out of bounds.

{% highlight clojure %}
(v1 16) => IndexOutOfBoundsException!!!
{% endhighlight %}

To avoid IOBE you should use the `(get)` function. You can also add a default value that is returned when the given index is not in the vector.

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
    (subvec v (inc idex)))))
{% endhighlight %}

