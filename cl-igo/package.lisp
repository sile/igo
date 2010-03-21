(defpackage igo
  (:use :common-lisp)
  (:export morpheme
	   morpheme-surface
	   morpheme-feature
	   morpheme-start

	   tagger-new
	   parse
	   wakati))