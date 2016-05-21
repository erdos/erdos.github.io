---
layout: post
title: "Clojure collection quirks - A REFERENCE"
date: 2016-05-19 12:00:02 +0200
tags:
 - clojure
 - java
 - interop
 - programming
 - immutable
---


I've always wanted to create a reference on clojure collections for a long time now.

### Functions

This table contains the most common functions and collections.

<table class="features">
<thead>
<tr class="c"><td colspan="2" class="z"></td><td colspan="4">Seq</td><td colspan="2">Vector</td><td colspan="2">Map</td><td colspan="2">Set</td></tr>
<tr class="sm"><th class="z"></th><th>nil</th><th>'(1 2 3)</th><th>(range 5)</th><th>(range)<br/>(lazy-seq)<br>(cons 1 ())</th><th>Persistent<br>Queue/EMPTY</th><th>[1 2 3]</th><th>(last {1 1})</th><th>(hash-map)<br/>(array-map)</th><th>(sorted-map)</th><th>#{}</th><th>(sorted-set)</th></tr></thead>
<tbody>
  <tr class="sm"><td>type</td><td class="nil">nil</td><td>Persistent<br>List</td><td>LongRange</td><td>Iterate<br/>or LazySeq<br/>or Cons</td><td>Persistent<br>Queue</td><td>Persistent<br>Vector</td><td>MapEntry</td><td>Persistent<br>ArrayMap<br>or&nbsp;HashMap</td><td>Persistent<br>TreeMap</td><td>Persistent<br>HashSet</td><td>Persistent<br>TreeSet</td></tr>
  <tr><td>coll?</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td></tr>
  <tr><td>counted?</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td></tr>
  <tr><td>ifn?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td></tr>
  <tr><td>associative?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>map?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>sequential?</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>list?</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>seq?</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>reversible?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool t">true</td></tr>
  <tr><td>vector?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>map-entry?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td></tr>
 <tr><td>map?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool f">false</td></tr>
  <tr><td>set?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool t">true</td></tr>
  <tr><td>sorted?</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool f">false</td><td class="bool t">true</td><td class="bool f">false</td><td class="bool t">true</td></tr>
</tbody></table>


<br/>

### Interfaces

|interface| description|
|---------|------------|
|`IMeta`    | The object has meta information accessible via the `(meta x)` function. |
|`IObj`     | Indicates that meta information can be added to object (`with-meta` function) |
|`Counted`  | This collection supports fast counting (`(count x)` function call). |
|`IEditableCollection` | A transient version of the collection can be created with the `(transient x)` call. |
|`IFn`      | Collection can be called just like a function. |
|`IKeywordLookup`| Collection supports indexing by keywords.|
|`ILookup`  | Collection supports access by index (via `get` function with default value.|
|`IPending` | Supports `realized?` function.|
|`IPersistentCollection`| Persistent collections have support for `conj`, `empty`, `count`, `equals`.|
|`ISeq`     | Sequences have support for `first`, `next`, `rest`, `cons`|
|-----------|------------------------------------------|

etc...

