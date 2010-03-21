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

(defun formalize-letargs (args)
  (mapcar (lambda (a) (if (atom a) (list a) a)) args))

(defmacro nlet (fn-name letargs &body body)
  (setf letargs (formalize-letargs letargs))
  `(labels ((,fn-name ,(mapcar #'car letargs)
              ,@body))
     (,fn-name ,@(mapcar #'cadr letargs))))

;; XXX:
(defmacro split-by-chars (delims str &optional count (remove-delim t))
  (assert (typep delims 'string) (delims) "DELIMS must be STRING (input is ~A)" (type-of delims))
  `(let (tokens (len (length ,str)) ,@(when count (list (list 'cnt count))))
     ,(when count '(declare (fixnum cnt)))
     (nlet self ((pos 0) (beg 0))
	   (declare (fixnum pos beg))
	   (if (= pos len)
	       (nreverse (if (= beg pos) tokens (cons (subseq ,str beg pos) tokens)))
	     (case (schar ,str pos)
		   (,(coerce delims 'list)
		    (push (subseq ,str beg pos) tokens)
		    (loop while (and (/= pos len)
				(case (schar ,str pos) 
				      (,(coerce delims 'list) 
				       ,(unless remove-delim
					  `(push (subseq ,str pos (1+ pos)) tokens))
				       (incf pos)))))
		    ,(when count 
		       `(when (zerop (decf cnt))
			  (return-from self (nreverse 
					     (if (>= pos len) tokens (cons (subseq ,str pos) tokens))))))
		    (self pos pos))
		   (otherwise
		    (self (1+ pos) beg)))))))