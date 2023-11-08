
(ns git-handler.submodule-updater.detector.side-effects
    (:require [git-handler.core.env                         :as core.env]
              [git-handler.core.utils                       :as core.utils]
              [git-handler.submodule-updater.detector.state :as submodule-updater.detector.state]
              [io.api                                       :as io]
              [string.api                                   :as string]
              [vector.api                                   :as vector]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn- store-detected-submodule!
  ; @ignore
  ;
  ; @description
  ; Stores the given 'submodule-props' in the 'DETECTED-SUBMODULES' atom.
  ;
  ; @param (string) submodule-path
  ; @param (map) submodule-props
  [submodule-path submodule-props]
  (swap! submodule-updater.detector.state/DETECTED-SUBMODULES assoc submodule-path submodule-props))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn- detect-submodule!
  ; @ignore
  ;
  ; @description
  ; - Stores the git URL and the repository name in the 'DETECTED-SUBMODULES' atom
  ; - Git submodules has a '.git' file and not a '.git' directory as normal git modules.
  ;   In that '.git' file there is a 'gitdir' path which points relatively to
  ;   the submodule's '.git' directory (which is placed in the root project's '.git' directory).
  ;
  ; @param (string) subdirectory-path
  [subdirectory-path]
  (if-let [git-url (core.env/get-submodule-git-url subdirectory-path)]
          (if-let [repository-name (core.utils/git-url->repository-name git-url)]
                  (store-detected-submodule! subdirectory-path {:git-url git-url :repository-name repository-name}))))

(defn detect-submodules!
  ; @ignore
  ;
  ; @description
  ; 1. Detects the subdirectories on the give paths.
  ; 2. Iterates over the subdirectory list and passes the found subdirectory paths to the 'detect-submodule!' function.
  ;
  ; @param (map) options
  ; {:source-paths (strings in vector)(opt)
  ;   Default: ["submodules"]}
  [{:keys [source-paths] :or {source-paths ["submodules"]}}]
  (reset! submodule-updater.detector.state/DETECTED-SUBMODULES nil)
  (letfn [; Cuts the "/.git" part from end of the '.git' file's path
          ; "my-submodule/.git" => "my-submodule"
          (cut-f [git-file] (-> git-file (string/not-ends-with! "/.git")))

          ; Cuts the "/.git" part from the end of all '.git' file's paths in the given vector.
          ; ["my-submodule/.git"] => ["my-submodule"]
          (go-up-f [git-files] (-> git-files (vector/->items cut-f)))

          ; Searches for .git files on the given source path.
          (search-f [source-path] (-> source-path (io/search-files #"\.git\b")))

          ; Collects all the submodule paths into the 'submodule-paths' vector.
          ; A submodule is a subdirectory where a '.git' file is found.
          (collect-f [submodule-paths source-path] (-> source-path search-f go-up-f (vector/concat-items submodule-paths)))]

         ; ...
         (let [submodule-paths (reduce collect-f [] source-paths)]
              (doseq [submodule-path submodule-paths]
                     (detect-submodule! submodule-path)))))
