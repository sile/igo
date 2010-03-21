(in-package :asdf)

(defsystem igo
  :name    "igo"
  :version "0.0.1"
  :author  "Takeru Ohta"
  :description "Common Lisp morpheme analyzer"

  :serial t
  :components ((:file "package")
	       (:file "util")
	       (:file "varied-byte-stream")
	       (:file "code-stream")
	       (:file "trie")
	       (:file "matrix")
	       (:file "char-category")
	       (:file "viterbi-node")
	       (:file "word-dic")
	       (:file "unknown")
	       (:file "morpheme")
	       (:file "tagger")))
