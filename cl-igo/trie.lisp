(defpackage igo.trie
  (:use :common-lisp)
  (:shadow load)
  (:export load
	   each-common-prefix))
(in-package :igo.trie)

(igo::set-package-nickname :igo.varied-byte-stream :vbs)
(igo::set-package-nickname :igo.code-stream        :code-stream)

(defstruct (trie (:conc-name ""))
  (element-count 0 :type fixnum)
  (begs #()        :type (simple-array (signed-byte 32)))
  (lens #()        :type (simple-array (signed-byte 16)))
  (base #()        :type (simple-array (signed-byte 32)))
  (chck #()        :type (simple-array (unsigned-byte 16)))
  (tail #()        :type (simple-array (unsigned-byte 16))))

(defmethod print-object ((o trie) stream)
  (print-unreadable-object (o stream :type t)
    (format stream ":element-count ~D" (trie-element-count o))))

(defun load (path)
  (vbs:with-input-file (in path)
    (let ((node-size (vbs:read-byte in 4))
	  (tind-size (vbs:read-byte in 4))
	  (tail-size (vbs:read-byte in 4)))
      (make-trie
       :element-count tind-size
       :begs (vbs:read-sequence in 4 tind-size)
       :base (vbs:read-sequence in 4 node-size)
       :lens (vbs:read-sequence in 2 tind-size)
       :chck (vbs:read-sequence in 2 node-size :signed nil)
       :tail (vbs:read-sequence in 2 tail-size :signed nil)))))

(defun id (node)
  (- 1 node))

(defun including-tail? (cs node trie &aux (id (id node)))
  (let ((tail (tail trie))
	(beg  (aref (begs trie) id))
	(len  (aref (lens trie) id)))
    (loop FOR i FROM beg BELOW (+ beg len) 
      ALWAYS (= (aref tail i) (code-stream:read cs)))))

(defun each-common-prefix (fn cs trie)
  (let* ((base  (base trie))
	 (chck  (chck trie))
	 (node  (aref base 0)))
    (loop FOR code = (code-stream:read cs) DO
      (let ((terminal-idx (+ node code-stream:+TERMINATE-CODE+)))
	(when (= (aref chck terminal-idx) code-stream:+TERMINATE-CODE+)
	  (funcall fn (1- (code-stream:position cs)) (id (aref base terminal-idx)))
	  (when (= code code-stream:+TERMINATE-CODE+)
	    (return-from each-common-prefix))))

      (prog ((idx (+ node code)))
	(setf node (aref base idx))
	(when (= (aref chck idx) code)
	  (if (plusp node) 
	      (go :continue)
	    (when (including-tail? cs node trie)
	      (funcall fn (code-stream:position cs) (id node)))))
	(return-from each-common-prefix)
	
	:continue))))
	
(igo::delete-package-nickname :igo.varied-byte-stream)
(igo::delete-package-nickname :igo.code-stream)