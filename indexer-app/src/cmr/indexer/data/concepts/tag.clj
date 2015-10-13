(ns cmr.indexer.data.concepts.tag
  "Contains functions to parse and convert tag concepts"
  (:require [clojure.string :as str]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.common.util :as util]
            [cmr.indexer.data.elasticsearch :as es]))

(defmethod es/concept->elastic-doc :tag
  [context concept parsed-concept]
  (let [{:keys [concept-id]} concept
        {:keys [namespace value category description originator-id associated-concept-ids]}
        parsed-concept]
    {:concept-id concept-id
     :namespace namespace
     :namespace.lowercase (str/lower-case namespace)
     :value value
     :value.lowercase (str/lower-case value)
     :category category
     :category.lowercase (when category (str/lower-case category))
     :description description
     :originator-id.lowercase  (str/lower-case originator-id)
     :associated-concept-ids-gzip-b64 (util/string->gzip-base64 (pr-str associated-concept-ids))
     :associated-concept-ids associated-concept-ids}))
