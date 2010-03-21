(defpackage :igo.unknown
  (:use :common-lisp)
  (:shadow load
	   search)
  (:export load
	   unknown
	   search))
(in-package :igo.unknown)

(igo::set-package-nickname :igo.char-category :cc)
(igo::set-package-nickname :igo.code-stream   :code-stream)
(igo::set-package-nickname :igo.word-dic      :dic) 

(defstruct (unknown (:conc-name ""))
  (categorys nil :type cc::category-set)
  (space-id  0   :type fixnum))

(defun load (root-dir)
  (let* ((cts (cc:load root-dir))
	 (unk (make-unknown :categorys cts)))
    (setf (space-id unk) 
	  (cc:category-trie-id (cc:category (char-code #\Space) cts)))
    unk))

(defun search (cs unk wdic result)
  (prog* ((start     (code-stream:position cs))
	  (code      (code-stream:read2 cs))
	  (categorys (categorys unk))
	  (ct        (cc:category code categorys)))
    (when (and result
	       (not (cc:category-invoke? ct)))
      (go :end))

    (let* ((trie-id (cc:category-trie-id ct))
	   (space?  (= trie-id (space-id unk)))
	   (limit   (cc:category-length ct)))
      (loop FOR len FROM 1 TO limit DO
        (setf result 
	      ;; XXX: (+ start len) => surrogate?
	      (dic:search-from-trie-id trie-id start (+ start len) space? result wdic))
	(when (or (code-stream:end? cs)
		  (not (cc:compatible? code (code-stream:read2 cs) categorys)))
	  (go :end)))
      
      (when (and (cc:category-group? ct))
	(if (code-stream:end? cs)
	    (setf result (dic:search-from-trie-id trie-id start (code-stream:length cs)
				     space? result wdic))
	  (progn (loop WHILE (and (not (code-stream:end? cs))
			 (cc:compatible? code (code-stream:read2 cs) categorys)))
			 
	(setf result 
	      (dic:search-from-trie-id trie-id start 
				       (1- (code-stream:position cs))
				       space? result wdic))))))
    :end
    (setf (code-stream:position cs) start))
  result)

(igo::delete-package-nickname :igo.char-category)
(igo::delete-package-nickname :igo.code-stream)
(igo::delete-package-nickname :igo.word-dic)