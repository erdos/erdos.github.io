(ns  clj-reference)
(require 'boot.repl 'boot.core)

(boot.core/set-env! :dependencies '[[rhizome "0.2.5"]
                                    [cider/cider-nrepl "0.12.0"]
                                    [refactor-nrepl "2.2.0"]
                                    ])

(require '[rhizome.viz :refer [view-graph view-image dot->image]]
         '[rhizome.dot :refer [graph->dot]])

                                        ; (ifn? (range))
;;

(defn render-table
  "Given: seq of maps."
  [xs]
  (let [;ks (keys (first xs))
        ]
    (->
     ["<table>"
      ["<thead>" "<tr>" (for [k (first xs)] ["<th>" k "</th>"]) "</tr>" "</thead>"]
      ["<tbody>"
       (for [r (next xs)]
         ["<tr>"
          (for [c r]
            (cond
             (= ::ERROR c) "<td class=\"err\">-</td>"
             (true? c) "<td class=\"bool t\">true</td>"
             (false? c) "<td class=\"bool f\">false</td>"
             (nil? c) "<td class=\"nil\">nil</td>"
             :else ["<td>" c "</td>"]))
          "</tr>"])
       "</tbody>"]
      "</table>"]
     (flatten) (->> (apply str)))))

(defn table-fn-data []
  (let [xs '[nil
             '(1 2 3)    (range 5)
             [1 2 3]     clojure.lang.PersistentQueue/EMPTY
             (hash-map) (sorted-map)
             #{} (sorted-set)]
        fs '[type
                                        ;count
         ifn? associative? map? vector? seq? sequential? coll? counted? empty? seq list?
                                        ; map-entry?
         map? record? seq? set? sorted? vector?
                                        ;pop peek
             ]

        table (list* (cons 'fn (mapv str xs))
                     (for [f fs] (list* f
                                        (for [x xs]
                                          (try ((eval f) (eval x))
                                               (catch Exception e
                                                 (println (vec(.getStackTrace e)))
                                                 :ERROR)))
                                        )))]
    (render-table table)))

; (println (table-fn-data))

(defn fixpt [f x] (let [fx (f x)] (if (= fx x) fx (recur f fx))))
;(fixpt #(quot % 2) 1213)

(defn topsort [children roots]
  (loop [roots roots visited #{} output ()]
    (if-let [xs (seq (set (remove (set roots) (remove visited (mapcat children roots)))))]
      (recur xs (into visited roots) (into output roots))
      output)))

(defn graph-types []
  (let [roots '[[]
                ;() '(1 2 3) (cons nil nil) (sorted-set) (set nil) (sorted-map) (hash-map) (array-map)
                ;(range 1000)
                ]
                                        ;roots [{1 1 2 2 3 3 4 4 5 5 6 6 7 7 8 8 9 9 }]
        children (fn [x] (distinct (filter some? (list* (if (not= (.getSuperclass x) java.lang.Object)
                                                         (.getSuperclass x))
                                                       (.getInterfaces x)))))
        direct-children (fn [x] (set (remove (set (mapcat children (children x))) (children x))))
        ;direct-children children
        alles (topsort children (map (comp type eval) roots))
        clusters {"Map" #{clojure.lang.PersistentHashMap clojure.lang.PersistentArrayMap
                          clojure.lang.IPersistentMap    clojure.lang.APersistentMap
                          clojure.lang.PersistentTreeMap
                          clojure.lang.IMapIterable
                          clojure.lang.MapEquivalence}
                  "Set" #{clojure.lang.PersistentHashSet clojure.lang.PersistentTreeSet
                          clojure.lang.APersistentSet
                          clojure.lang.IPersistentSet }
                  "Vector" #{clojure.lang.PersistentVector
                             clojure.lang.IPersistentVector
                            clojure.lang.APersistentVector}
                  "List" #{clojure.lang.PersistentList
                           clojure.lang.IPersistentList
                           clojure.lang.ASeq
                           clojure.lang.PersistentList$EmptyList
                           ;clojure.lang.IPersistentStack
                           clojure.lang.ISeq
                           }
                  ;"Object" #{clojure.lang.Obj clojure.lang.IObj clojure.lang.IFn clojure.lang.Fn clojure.lang.IMeta clojure.lang.AFn}
                  "Java" (fn [x] (.startsWith (.getName (.getPackage x)) "java."))

                  }
        ]
    (->
     (graph->dot (vec alles)
                 direct-children
                 :cluster->descriptor (fn [c]
                                        (->
                                         ({"Java"
                                           {:label "Java" :color "#defddd"}
                                           "Object" {:label "Object and Function"}
                                           "List" {:label "List" :color "#dddefd"}
                                           "Vector" {:label "Vector" :color "#fdddde"}
                                           "Map"   {:label "Map" :color "#ddfdde"}
                                           "Set"   {:label "Set" :color "#deddfd"}
                                           } c)
                                         (merge {:style :filled
                                                 })
                                         )
                                        )
                 :node->descriptor (fn [node] {:label (str
                                                      ;(-> node .getPackage .getName)
                                                       ;   "<BR/>"
                                                          (.getSimpleName node)
                                                          )
                                              :fontsize 6 :style :filled
                                              :margin 0
                                              :height 0.2
                                              :fillcolor :white :color :black

                                              :shape (if (.isInterface node) :ellipse :rect)})
                 :node->cluster (fn [node] (some (fn [[k v]] (if (v node) k)) clusters))
                 :edge->descriptor (fn [a b] {:len 2
                                            ; :constraint (not= 1 (count (direct-children a)))


                                             #_(or (not (or (.isInterface a) (.isInterface b)))
                                                   (= 1 (count (direct-children a)))
                                                   (not (.isInterface b)))})
                 :options {:splines :line
                           :layout :dot
                           ;:concentrate :true
                           }
                 :vertical? false
                 )
     (-> dot->image view-image)
                                        ;(->> (spit "/home/jano/clojure-hier.dot"))
     ))
  )




(comment

  (def c1 (cons 1 (lazy-seq)))
  (ifn? c1)
  (list? c1)
  (seq? c1) (type (cons 1 (range))) (type (conj (range ) 2))s


  (realized?) (type (cons 1 nil)) (type (cons 1 (lazy-seq)))
  (set
   (mapcat
    (fn [c] (take-while some? (iterate #(.getSuperclass %) c)))
    (tree-seq some? #(.getInterfaces %) (class []))))

  (set (tree-seq (partial not= java.lang.Object)

                 #(remove nil? (cons (.getSuperclass %) (.getInterfaces %)))
             (type [])))

  (vec (.getInterfaces java.lang.Iterable))

  (vec (.getSuperclass clojure.lang.IObj))

  )




(defn graph-type-hier-for [x]
  (let [children (fn [x] (set (cond-> (seq (.getInterfaces x))
                                 (.getSuperclass x) (conj (.getSuperclass x)))))
        ]
    (->
     (graph->dot (set (tree-seq children children x))
                 children ;#(filter some? (flatten [(.getSuperclass %) (.getInterfaces %)]))

                 :node->descriptor (fn [node] {:label (str
                                        ;(-> node .getPackage .getName)
                                        ;   "<BR/>"
                                                      (.getSimpleName node)
                                                      )
                                              :fontsize 6 :style :filled
                                              :margin 0
                                              :height 0.2
                                              :fillcolor :white :color :black
                                              :shape (if (.isInterface node) :ellipse :rect)})
                                        ;:node->cluster (fn [node] (some (fn [[k v]] (if (v node) k)) clusters))
                 :node->cluster (fn [n] (if (-> n .getPackage .getName (.startsWith "java."))
                                         "Java"))
                 :edge->descriptor (fn [a b] {:len 2})
                 :options {:splines :line
                           :layout :dot
                           ;:concentrate :true
                           }
                 ;:vertical? false
                 :cluster->descriptor {"Java" {:label "Java" "color" "#feddff"}}
                 )
     (-> dot->image view-image)
                                        ;(->> (spit "/home/jano/clojure-hier.dot"))
     ))
  )
; (graph-type-hier-for (type []))
