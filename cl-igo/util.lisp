(in-package :igo)

(defmacro delete-package-nickname (package) 
  (declare (ignore package)))
#|
  `(eval-when (:compile-toplevel :load-toplevel :execute)
     (rename-package ,package ,package)))
|#

(defmacro defconst-once-only (name value &optional documentation)
  `(unless (boundp ',name)
     (defconstant ,name ,value ,documentation)))

(defun split (delim seq &aux (len (length delim)))
  (when (zerop len)
    (return-from split (list seq)))

  (loop FOR beg = 0 THEN (+ end len)
        FOR end = (search delim seq :start2 beg)
        COLLECT (subseq seq beg end)
        WHILE end))