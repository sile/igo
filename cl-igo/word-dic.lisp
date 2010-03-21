(defpackage igo.word-dic
  (:use :common-lisp)
  (:shadow load
	   search)
  (:export load
	   *ipadic-feature-parser*
	   word-dic
	   word-data
	   cost
	   search
	   search-from-trie-id))
(in-package :igo.word-dic)

(igo::set-package-nickname :igo.varied-byte-stream :vbs)
(igo::set-package-nickname :igo.trie               :trie)
(igo::set-package-nickname :igo.code-stream        :code-stream)
(igo::set-package-nickname :igo.viterbi-node       :viterbi-node)

(defstruct word-dic
  (trie         nil :type trie:trie)
  (costs        #() :type (simple-array (signed-byte 16)))
  (left-ids     #() :type (simple-array (signed-byte 16)))
  (right-ids    #() :type (simple-array (signed-byte 16)))
  (data         #() :type simple-array) ;; XXX: surrogate? and TODO: features
  (indices      #() :type (simple-array (signed-byte 32))))

(defun read-indices (path)
  (vbs:with-input-file (in path)
    (vbs:read-sequence in 4 (/ (vbs:file-size in) 4))))

(defun read-data (path)
  (vbs:with-input-file (in path)
    (map 'string #'code-char (vbs:read-sequence in 2 (/ (vbs:file-size in) 2) :signed nil))))

(defun split-data (data offsets feature-parser)
  (let ((ary (make-array (1- (length offsets))))) 
    (dotimes (i (length ary) ary)
      (setf (aref ary i)
	    (funcall feature-parser (subseq data (aref offsets i) (aref offsets (1+ i))))))))

(defvar *ipadic-feature-parser*
  (lambda (feature)
    (flet ((kw (s) (intern s :keyword))
	   (kw-if-* (s) (if (string= s "*") (intern s :keyword) s)))
      (let ((fs (igo::split-by-chars "," feature)))
	(nconc (mapcar #'kw      (subseq fs 0 6))
	       (mapcar #'kw-if-* (subseq fs 6)))))))
 
(defun load (root-dir &optional (feature-parser #'identity))
  (flet ((fullpath (name) (merge-pathnames root-dir name)))
    (vbs:with-input-file (in (fullpath "word.inf"))
      (let* ((word-count (/ (vbs:file-size in) (+ 4 2 2 2)))
	     (data    (read-data    (fullpath "word.dat")))
	     (offsets (vbs:read-sequence in 4 word-count)))
	(make-word-dic
	 :trie    (trie:load    (fullpath "word2id"))
	 :indices (read-indices (fullpath "word.ary.idx"))
	 :data    (split-data data offsets feature-parser)
	 
	 :left-ids     (vbs:read-sequence in 2 word-count)
	 :right-ids    (vbs:read-sequence in 2 word-count)
	 :costs        (vbs:read-sequence in 2 word-count))))))

(defun cost (word-id wdic) (aref (word-dic-costs wdic) word-id))
(defun left-id (word-id wdic) (aref (word-dic-left-ids wdic) word-id))
(defun right-id (word-id wdic) (aref (word-dic-right-ids wdic) word-id))

(defun search (cs result wdic)
  (let ((start   (code-stream:position cs))
	(indices (word-dic-indices wdic)))
    (trie:each-common-prefix
     (lambda (end id)
       (loop FOR i FROM (aref indices id) BELOW (aref indices (1+ id)) DO
         (push (viterbi-node:new i start end (left-id i wdic) (right-id i wdic) nil)
	       result)))
     cs
     (word-dic-trie wdic)))
  result)

(defun search-from-trie-id (id start end space? result wdic)
  (let ((indices (word-dic-indices wdic)))
    (loop FOR i FROM (aref indices id) BELOW (aref indices (1+ id)) DO
      (push (viterbi-node:new i start end (left-id i wdic) (right-id i wdic) space?)
	    result)))
  result)

(defun word-data (word-id wdic)
  (aref (word-dic-data wdic) word-id))

(igo::delete-package-nickname :igo.varied-byte-stream)
(igo::delete-package-nickname :igo.trie)
(igo::delete-package-nickname :igo.code-stream)
(igo::delete-package-nickname :igo.viterbi-node)