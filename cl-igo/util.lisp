(in-package :igo)

(defmacro set-package-nickname (package nickname)
  `(eval-when (:compile-toplevel :load-toplevel :execute)
     (rename-package ,package ,package '(,nickname))))

(defmacro delete-package-nickname (package)
  `(eval-when (:compile-toplevel :load-toplevel :execute)
     (rename-package ,package ,package)))

(defmacro defconst-once-only (name value &optional documentation)
  `(unless (boundp ',name)
     (defconstant ,name ,value ,documentation)))