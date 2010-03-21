(defpackage igo.char-category
  (:use :common-lisp)
  (:shadow load)
  (:export load
	   category
	   compatible?
	   category-trie-id
	   category-length
	   category-invoke?
	   category-group?))
(in-package :igo.char-category)

(igo::set-package-nickname :igo.varied-byte-stream :vbs)

(defstruct category
  (trie-id 0   :type fixnum)
  (length  0   :type fixnum)
  (invoke? nil :type boolean)
  (group?  nil :type boolean))
  
(defstruct (category-set (:conc-name ""))
  (categorys #() :type (simple-array category))
  (char->id  #() :type (simple-array (signed-byte 32)))
  (eql-masks #() :type (simple-array (signed-byte 32))))

(defun load-categorys (root-dir)
  (vbs:with-input-file (in (merge-pathnames "char.category" root-dir))
    (let ((data (vbs:read-sequence in 4 (/ (vbs:file-size in) 4))))
      (coerce
       (loop FOR i FROM 0 BELOW (length data) BY 4 COLLECT
         (make-category :trie-id (aref data (+ i 0))
			:length  (aref data (+ i 1))
			:invoke? (= 1 (aref data (+ i 2)))
			:group?  (= 1 (aref data (+ i 3)))))
       'vector))))

(defun load (root-dir)
  (vbs:with-input-file (in (merge-pathnames "code2category" root-dir))
    (make-category-set 
     :categorys (load-categorys root-dir)
     :char->id  (vbs:read-sequence in 4 (/ (vbs:file-size in) 2))
     :eql-masks (vbs:read-sequence in 4 (/ (vbs:file-size in) 2)))))

(defun category (code category-set)
  (with-slots (categorys char->id) category-set
    (aref categorys (aref char->id code))))

(defun compatible? (code1 code2 category-set)
  (with-slots (eql-masks) category-set
    (logtest (aref eql-masks code1) (aref eql-masks code2))))

(igo::delete-package-nickname :igo.varied-byte-stream)