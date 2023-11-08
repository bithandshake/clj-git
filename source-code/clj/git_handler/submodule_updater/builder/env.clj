
(ns git-handler.submodule-updater.builder.env
    (:require [git-handler.submodule-updater.builder.state  :as submodule-updater.builder.state]
              [git-handler.submodule-updater.core.env       :as submodule-updater.core.env]
              [git-handler.submodule-updater.detector.state :as submodule-updater.detector.state]
              [git-handler.submodule-updater.reader.state   :as submodule-updater.reader.state]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn submodule-added-to-dependency-tree?
  ; @ignore
  ;
  ; @description
  ; Returns whether the submodule is added to the dependency tree.
  ;
  ; @param (string) submodule-path
  ;
  ; @return (boolean)
  [submodule-path]
  (letfn [(f [[% _]] (= % submodule-path))]
         (some f @submodule-updater.builder.state/DEPENDENCY-TREE)))

(defn dependency-tree-built?
  ; @ignore
  ;
  ; @description
  ; Returns whether the dependency tree is complete or some submodules are missing yet.
  ;
  ; @return (boolean)
  []
  (letfn [(f [[submodule-path _]]
             (submodule-added-to-dependency-tree? submodule-path))]
         (every? f @submodule-updater.detector.state/DETECTED-SUBMODULES)))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn submodule-has-inner-dependencies?
  ; @ignore
  ;
  ; @description
  ; - Returns whether the submodule is depend on other submodules.
  ; - It isn't necessary for a submodule to depend on other INNER submodules!
  ;
  ; @param (string) submodule-path
  ;
  ; @return (boolean)
  [submodule-path]
  (if-let [dependencies (get @submodule-updater.reader.state/INNER-DEPENDENCIES submodule-path)]
          (and (-> dependencies vector?)
               (-> dependencies empty? not))))

(defn submodule-non-depend?
  ; @ignore
  ;
  ; @description
  ; - Returns whether the submodule has INNER dependencies that are not added to the dependency tree yet.
  ; - A submodule can be non-depend if it has no known INNER dependencies,
  ;   or all of its inner dependencies are already added to the dependency tree.
  ;
  ; @param (string) submodule-path
  ;
  ; @return (boolean)
  [submodule-path]
  (if-let [dependencies (get @submodule-updater.reader.state/INNER-DEPENDENCIES submodule-path)]
          (letfn [(f [[dep-name url sha]]
                     (-> url submodule-updater.core.env/git-url->submodule-path submodule-added-to-dependency-tree?))]
                 (every? f dependencies))
          :submodule-has-no-inner-dependencies))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn get-unresolved-dependencies
  ; @ignore
  ;
  ; @description
  ; Returns the submodules' paths (in a vector) that have INNER dependencies
  ; but are not added to the dependency tree yet.
  ;
  ; @return (strings in vector)
  []
  (letfn [(f [result [submodule-path _]]
             (if (or (submodule-added-to-dependency-tree? submodule-path)
                     (submodule-non-depend?               submodule-path))
                 (->   result)
                 (conj result submodule-path)))]
         (reduce f [] @submodule-updater.detector.state/DETECTED-SUBMODULES)))
