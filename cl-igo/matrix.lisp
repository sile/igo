(defpackage igo.matrix
  (:use :common-lisp)
  (:shadow load)
  (:export load
	   link-cost
	   matrix))
(in-package :igo.matrix)

(igo::set-package-nickname :igo.varied-byte-stream :vbs)

(deftype matrix () 'function)

(defun load (data-dir)
  (vbs:with-input-file (in (merge-pathnames "matrix.bin" data-dir))
    (let* ((left-size  (vbs:read-byte in 4))
	   (right-size (vbs:read-byte in 4))
	   (matrix (vbs:read-sequence in 2 (* left-size right-size))))
      (lambda (left-id right-id)
	(aref matrix (+ (* right-id right-size) left-id))))))

(defun link-cost (left-id right-id matrix)
  (funcall matrix left-id right-id))

(igo::delete-package-nickname :igo.varied-byte-stream)