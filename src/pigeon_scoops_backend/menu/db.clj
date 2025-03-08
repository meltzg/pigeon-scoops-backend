(ns pigeon-scoops-backend.menu.db)

(defn find-all-menus [db])

(defn insert-menu! [db menu])

(defn find-menu-by-id [db menu-id])

(defn update-menu! [db menu])

(defn delete-menu! [db menu-id])

(defn insert-menu-item! [db menu-item])

(defn update-menu-item! [db menu-item])

(defn delete-menu-item! [db menu-item-id])

(defn insert-menu-item-size! [db menu-item-size])

(defn update-menu-item-size! [db menu-item-size])

(defn delete-menu-item-size! [db menu-item-size-id])
