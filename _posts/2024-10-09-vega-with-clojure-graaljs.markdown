---
layout: post
title: "Vega-Lite charts with Clojure and GraalJs"
date: 2024-10-09 09:00:00 +0200
tags:
 - clojure
 - graal
 - programming
 - javascript
---

# Vega-Lite charts with Clojure and GraalJS

For a recent side project I needed a charting tool I could operate on the server-side.
I initially used [gnuplot](http://www.gnuplot.info/) which was really nice but had some limitations (for example, making plots clickable)
and found the DSL somewhat complex. Then I discovered [Vega](https://vega.github.io/) and [Vega-Lite](https://vega.github.io/vega-lite/) and I really liked the elegant style and flexibility. But since these libraries are written in Javascript to be used in the frontend, I had to find a way to be able to integrate them in my backend-heavy Clojure application. This limitation led me to use a `Clojure -> GraalJS -> Javascript` interop approach to generate charts from the backend.

## Setup

I got a hand on the Javascript files by just copying them from the node packages directly into the `resources` folder:

```sh
$ npm install vega vega-lite
$ cp node_modules/vega-lite/build/vega-lite.js resources/vega-lite.js
$ cp node_modules/vega/build/vega.js resources/vega.js
```

Next, here is a snipped of the contents of what [deps.edn](https://clojure.org/guides/deps_and_cli) looked like. The configuration should be similar with other build systems also.

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.0-alpha10"}
        org.graalvm.js/js   {:mvn/version "23.0.4"}}
 :paths ["src" "resources"]
 :aliases {:run {:exec-fn core/-main}}}
```

For simplicity, all the code in the next steps should go to just a `core.clj` source file.

## Calling GraalJs

To create the environment to run Javascript, we need a `Context` object that is going to hold the active value bindings:

```clojure
(defonce context
  (-> (org.graalvm.polyglot.Context/newBuilder (into-array String []))
      (.allowAllAccess true)
      (.build)))
```

With the context established, we can define a function to evaluate Javascript code:

```clojure
(defn- js-eval [source] (.eval context "js" (str source)))
```

## Utilities

I found that the Javascript runtime provided by GraalJs lacks some features, but we can easily implement what we need:

1. **Structured clone:** The [window.structuredClone](https://developer.mozilla.org/en-US/docs/Web/API/Window/structuredClone) is missing but we can define a simple implementation:
```clojure
(js-eval "function structuredClone(a) { return JSON.parse(JSON.stringify(a)) }")
```

2. **Converting Clojure data:** And we need an utility to recursively convert Clojure data structures to Javascript-compatible types:

```clojure
(defn- javascriptify1 [x]
  (cond (map? x) (org.graalvm.polyglot.proxy.ProxyObject/fromMap x)
        (map-entry? x) x
        (keyword? x) (name x)
        (sequential? x) (org.graalvm.polyglot.proxy.ProxyArray/fromList (vec x))
        :else x))

(defn edn->js [data]
  (clojure.walk/postwalk javascriptify1 data))
```
3. **Callbacks:** We need a way to handle Clojure functions as Javasript callbacks. It can be done by implementing the `Consumer` interface.
```clojure
(defn- callback [f]
  (reify java.util.function.Consumer
    (accept [_ x] (f x))))
```

4. **Timeouts:** The `window.setTimeout` function is currently missing in GraalJs, so we will have to implement it ourserves.

## Integrating Vega and Vega-Lite

We can go ahead with loading the Vega and Vega-Lite libraries into the context.

```clojure
(js-eval (slurp (clojure.java.io/resource "vega.min.js")))
(js-eval (slurp (clojure.java.io/resource "vega-lite.min.js")))
```

The `.toSVG()` call return a `Promise`, which can be tricky to implement in Clojure. Instead, I defined a wrapper function to handle callbacks. This function takes three arguments: it parses and renders the Vega-Lite definition provided in the first parameter and calls the function passed in the `success` parameter with the rendered SVG image, while calling the `reject` parameter on error.

```clojure
(def js-render-fn
  (js-eval "(function(input, success, reject) {
               const vegaSpec = vegaLite.compile(input, {}).spec;
               new vega.View(vega.parse(vegaSpec)).toSVG().then(success, reject);           
             })")
```

Additionally, Vega relies on `.setTimeout` calls in its [resource loader](https://github.com/search?q=repo%3Avega%2Fvega%20setTimeout&type=code). This mechanism is also invoked when using [hyperlinks](https://vega.github.io/vega-lite/docs/mark.html#hyperlink) in the marks, a feature I intend to use.

To work around this, let's simulate `setTimeout()` calls. Since actual timing is not required for this use case, the new implementation just registers the callback functions to a list that then can be executed sequentially. This is done in a helper something function:

```clojure
(defn with-set-timeouts [f]
  (js-eval "globalThis.tasks = []; function setTimeout(f) { globalThis.tasks.push(f); }")
  (let [result (f)]
    (js-eval "while(globalThis.tasks.length > 0) { globalThis.tasks.pop()() }"))
    result))
```

This approach is taining the previously defined `context`, and is therefore **not therad safe**. Ideally, every thread should operate within its own context. For now, synchronizing the calls on the same monitor ensures the calls remain _serializable_.

```clojure
(defn render-fn [data]
  (let [data       (edn->js data)
        result     (promise)
        on-success (callback result)
        on-error   (callback (fn [e]
                               (println :error (pr-str e))
                               (result (format "<H1>ERROR</H1><BR/><PRE>%s</PRE>" (pr-str e)))))]
    (locking ::render-plot
      (with-set-timeouts (fn [] (.execute js-render-fn (object-array [data on-success on-error]))))
      (deref result))))
```

## Testing the implementation

To test the above code, the following example Vega-Lite definition can be used:

```clojure
(def data
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json",
   :description "A simple bar chart with embedded data.",
   :data {:values [{"a" "A", "b" 28}, {"a" "B", "b" 55}, {"a" "C", "b" 43},
                   {"a" "D", "b" 91}, {"a" "E", "b" 81}, {"a" "F", "b" 53},
                   {"a" "G", "b" 19}, {"a" "H", "b" 87}, {"a" "I", "b" 52}]},
   :mark "bar",
   :encoding {:x {:field "a", :type "nominal", :axis {:labelAngle 0}},
              :y {:field "b", :type "quantitative"}
              :href {:field "b"}}})

(println (render-fn data))
```

This will print the SVG chart to the standard output.