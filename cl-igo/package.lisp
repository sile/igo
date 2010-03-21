(defpackage igo
  (:use :common-lisp)
  (:export morpheme
	   morpheme-surface
	   morpheme-feature
	   morpheme-start

	   load-tagger
	   parse
	   wakati))
(in-package :igo)

(eval-when (:compile-toplevel :load-toplevel)
 (defvar *optimize-fastest* '(optimize (speed 3) (debug 0) (safety 0) (compilation-speed 0)))
 (defvar *optimize-default* '(optimize)))
