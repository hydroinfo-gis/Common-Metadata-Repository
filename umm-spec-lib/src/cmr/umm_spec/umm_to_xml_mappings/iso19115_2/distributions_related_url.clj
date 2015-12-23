(ns cmr.umm-spec.umm-to-xml-mappings.iso19115-2.distributions-related-url
  "Functions for generating ISO19115-2 XML elements from UMM related-url records."
  (:require [clojure.string :as str]
            [cmr.umm-spec.xml.gen :refer :all]
            [cmr.umm-spec.iso19115-2-util :as iso]
            [cmr.umm-spec.url :as url]
            [cmr.common.util :as util]
            [cmr.umm-spec.umm-to-xml-mappings.iso19115-2.organizations-personnel :as org-per]
            [cmr.umm-spec.util :as su]))


(def type->name
  "Mapping of related url type to online resource name"
  {"GET DATA" "DATA ACCESS"
   "VIEW RELATED INFORMATION" "Guide"
   "GET RELATED VISUALIZATION" "Browse"})

(defn browse-url?
  "Returns true if the related-url is browse url"
  [related-url]
  (some #{"GET RELATED VISUALIZATION"} (:Relation related-url)))

(defn browse-urls
  "Returns the related-urls that are browse urls"
  [related-urls]
  (filter browse-url? related-urls))

(defn online-resource-urls
  "Returns all related-urls which are not browse urls"
  [related-urls]
  (remove browse-url? related-urls))

(defn generate-browse-urls
  "Returns content generator instructions for a browse url"
  [c]
  (for [{:keys [URLs Description] [rel] :Relation} (browse-urls (:RelatedUrls c))
        url URLs]
    [:gmd:graphicOverview
     [:gmd:MD_BrowseGraphic
      [:gmd:fileName
       [:gmx:FileName {:src url}]]
      [:gmd:fileDescription (char-string Description)]
      [:gmd:fileType (char-string (type->name rel))]]]))

(defn generate-online-resource-url
  "Returns content generator instructions for an online resource url or access url"
  [online-resource-url]
  (let [{:keys [URLs Description] [rel] :Relation} online-resource-url
        name (type->name rel)
        code (if (= "GET DATA" rel) "download" "information")]
    (for [url URLs]
      [:gmd:onLine
       [:gmd:CI_OnlineResource
        [:gmd:linkage
         [:gmd:URL url]]
        [:gmd:protocol
         (char-string (url/protocol url))]
        [:gmd:name
         (char-string name)]
        (if Description
          [:gmd:description
           (char-string Description)]
          [:gmd:description {:gco:nilReason "missing"}])
        [:gmd:function
         [:gmd:CI_OnLineFunctionCode
          {:codeList (str (:ngdc iso/code-lists) "#CI_OnLineFunctionCode")
           :codeListValue code}]]]])))

(defn generate-distributions
  "Returns content generator instructions for distributions in the given umm-c"
  [c]
  (let [distributions (:Distributions c)
        related-urls (online-resource-urls (:RelatedUrls c))
        responsibility (first (org-per/responsibility-by-role (:Organizations c) "DISTRIBUTOR"))
        contact-element [:gmd:distributorContact (when-not responsibility
                                                   {:gco:nilReason "missing"})
                         (when responsibility
                           (org-per/generate-responsible-party responsibility))]]
    (when (or distributions related-urls)
      [:gmd:distributionInfo
       [:gmd:MD_Distribution
        (for [[d idx] (map vector distributions (range (count distributions)))]
          [:gmd:distributor
           [:gmd:MD_Distributor
            contact-element
            [:gmd:distributionOrderProcess
             [:gmd:MD_StandardOrderProcess
              [:gmd:fees
               (char-string (or (:Fees d) ""))]]]
            [:gmd:distributorFormat
             [:gmd:MD_Format
              [:gmd:name
               (char-string (or (:DistributionFormat d) ""))]
              [:gmd:version {:gco:nilReason "unknown"}]
              [:gmd:specification
               (char-string (or (:DistributionMedia d) ""))]]]
            (for [size (:Sizes d)]
              [:gmd:distributorTransferOptions
               [:gmd:MD_DigitalTransferOptions
                [:gmd:unitsOfDistribution
                 (char-string (:Unit size))]
                [:gmd:transferSize
                 [:gco:Real (:Size size)]]]])
            (when (zero? idx))]])
        [:gmd:distributor
         [:gmd:MD_Distributor
          contact-element
          [:gmd:distributorTransferOptions
           [:gmd:MD_DigitalTransferOptions
            (for [related-url related-urls]
              (generate-online-resource-url related-url))]]]]]])))
