;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.test.check.clojure-test.assertions
  #?(:cljs (:require-macros [clojure.test.check.clojure-test.assertions.cljs]))
  (:require #?(:clj [clojure.test :as t]
               :clje [clojure.test :as t]
               :cljs [cljs.test :as t])))

#?(:clj
   (defn test-context-stacktrace [st]
     (drop-while
       #(let [class-name (.getClassName ^StackTraceElement %)]
          (or (.startsWith class-name "java.lang")
              (.startsWith class-name "clojure.test$")
              (.startsWith class-name "clojure.test.check.clojure_test$")
              (.startsWith class-name "clojure.test.check.clojure_test.assertions")))
       st)))

#?(:clj
   (defn file-and-line*
     [stacktrace]
     (if (seq stacktrace)
       (let [^StackTraceElement s (first stacktrace)]
         {:file (.getFileName s) :line (.getLineNumber s)})
       {:file nil :line nil})))

(defn check-results [m]
  (if (:pass? m)
    (t/do-report
      {:type :pass
       :message (dissoc m :result)})
    (t/do-report
      (merge {:type :fail
              :expected {:result true}
              :actual m}
             #?(:clj (file-and-line*
                       (test-context-stacktrace (.getStackTrace (Thread/currentThread))))
                ;; TODO: get the actual information
                :clje {:file nil :line nil}
                :cljs (t/file-and-line (js/Error.) 4))))))

(defn check?
  [_ form]
  `(let [m# ~(nth form 1)]
     (check-results m#)))

#?(:clj
   (defmethod t/assert-expr 'clojure.test.check.clojure-test/check?
     [_ form]
     (check? _ form))
   :clje
   (defmethod t/assert-expr 'clojure.test.check.clojure-test/check?
     [_ form]
     (check? _ form))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'clojure.test.check.clojure-test/check?
       [_ msg form]
       (clojure.test.check.clojure-test.assertions/check? msg form))))
