(defpackage igo
  (:use :common-lisp)
  (:export morpheme
	   morpheme-surface
	   morpheme-feature
	   morpheme-start
	   
	   *ipadic-feature-parser*

	   load-tagger
	   parse
	   wakati))
(in-package :igo)

(eval-when (:compile-toplevel :load-toplevel)
 (defvar *optimize-fastest* '(optimize (speed 3) (debug 0) (safety 0) (compilation-speed 0)))
 (defvar *optimize-default* '(optimize)))

(defvar *ipadic-feature-parser*
  (lambda (feature)
    (declare #.igo::*optimize-fastest*
	     (simple-string feature))
    (flet ((kw (s) (intern s :keyword))
	   (kw-if-* (s) (if (string= s "*") (intern s :keyword) s)))
      (let ((fs (the list (igo::split "," feature))))
	(nconc (mapcar #'kw      (subseq fs 0 6))
	       (mapcar #'kw-if-* (subseq fs 6)))))))

