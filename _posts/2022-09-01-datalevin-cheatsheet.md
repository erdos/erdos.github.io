---
layout: post
title: "Datalevin Cheatsheet"
date: 2022-09-01 12:00:00 +0200
tags:
 - clojure
 - programming
 - immutable
 - datalog
---

## Remove an entity

Remove an entity by id (with all attributes).

```
(transact! connection [[:db.fn/retractEntity id]])
```

Remove an attribute:

```
(transact! connection [[:db/retract id :my-own-attribute-name]])
```

Remove an attribute with a given value:

```
(transact! connection [[:db/retract id :my-own-attribute-name my-attribute-value]])
```
