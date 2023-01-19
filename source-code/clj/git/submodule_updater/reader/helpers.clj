
(ns git.submodule-updater.reader.helpers
    (:require [git.submodule-updater.core.helpers   :as core.helpers]
              [git.submodule-updater.detector.state :as detector.state]
              [git.submodule-updater.reader.state   :as reader.state]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn inner-dependency?
  ; @param (string) git-url
  ;
  ; @return (boolean)
  [git-url]
  ; Takes a git URL and iterates over the previously detected submodules.
  ; If one of the detected submodules has the same URL qualifies it as an inner
  ; dependency.
  (letfn [(f [[_ %]] (= (core.helpers/git-url->repository-name (:git-url %))
                        (core.helpers/git-url->repository-name   git-url)))]
         (some f @detector.state/DETECTED-SUBMODULES)))

(defn get-submodule-inner-dependencies
  ; @param (string) submodule-path
  ;
  ; @return (boolean)
  [submodule-path]
  (get @reader.state/INNER-DEPENDENCIES submodule-path))

(defn depends-on?
  ; @param (string) submodule-path
  ; @param (string) repository-name
  ;
  ; @usage
  ; (depends-on? "submodules/my-repository" "author/another-repository")
  ;
  ; @return (boolean)
  [submodule-path repository-name]
  ; Checks whether the given submodule-path depends on the given repository-name.
  ;
  ; Somehow the values have to be converted to strings, otherwise they are always different!
  (let [dependencies (get @reader.state/INNER-DEPENDENCIES submodule-path)]
       (letfn [(f [[% _ _]] (= (str %)
                               (str repository-name)))]
              (some f dependencies))))