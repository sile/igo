(defpackage igo.word-dic
  (:use :common-lisp)
  (:shadow load
	   search)
  (:export load
	   search
	   search-from-trie-id))
(in-package :igo.word-dic)


