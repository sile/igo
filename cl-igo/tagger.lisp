(in-package :igo)

;;;;;;;;;;;
;;; declaim
(declaim (inline coerce-to-simple-string set-mincost-node)
	 #.*optimize-fastest*)

;;;;;;;;;;
;;; struct 
(defstruct tagger
  (wdc nil :type dic:word-dic)
  (unk nil :type unk:unknown)
  (mtx nil :type mtx:matrix))

;;;;;;;;;;;;
;;; constant
(eval-when (:compile-toplevel :load-toplevel :execute)
  (igo::defconst-once-only +BOS-NODES+ (list (vn:make-bos/eos))))

;;;;;;;;;;;;;;;;;;;;;
;;; internal function
(defmacro nconcf (lst1 lst2)
  `(setf ,lst1 (nconc ,lst1 ,lst2)))

(defun set-mincost-node (vn prevs mtx wdc)
  (flet ((calc-cost (prev cur)
           (+ (vn:cost prev) 
	      (mtx:link-cost (vn:right-id prev) (vn:left-id cur) mtx))))
    (declare (inline calc-cost))
    (let ((fst (first prevs)))
      (setf (vn:prev vn) fst
	    (vn:cost vn) (calc-cost fst vn))
      
      (dolist (p (cdr prevs))
	(let ((cost (calc-cost p vn)))
	  (when (< cost (vn:cost vn))
	    (setf (vn:cost vn) cost
		  (vn:prev vn) p))))
      
      (incf (vn:cost vn) (dic:word-cost (vn:word-id vn) wdc))
      vn)))

(defun parse-impl (tagger cs len)
  (declare (fixnum len))
  (let ((nodes (make-sequence 'simple-vector (1+ len) :initial-element nil))
	(wdc   (tagger-wdc tagger))
	(unk   (tagger-unk tagger))
	(mtx   (tagger-mtx tagger))
	(per-rlt (make-array 16 :fill-pointer 0 :adjustable t :element-type 'vn::viterbi-node)))
    (setf (aref nodes 0) +BOS-NODES+)

    (loop FOR i FROM 0 BELOW len 
	  FOR prevs = (aref nodes i) DO
      (setf (code-stream:position cs) i)
      (setf (fill-pointer per-rlt) 0)
      (when prevs
	(dolist (vn (unk:search cs unk wdc (dic:search cs '() wdc)))
	  (if (vn:space? vn)
	      (nconcf (aref nodes (vn:end vn)) prevs)
	    (push (set-mincost-node vn prevs mtx wdc) (aref nodes (vn:end vn)))))))
    
    (vn:prev (set-mincost-node (vn:make-bos/eos) (aref nodes len) mtx wdc))))

(defun coerce-to-simple-string (s)
  (declare (string s))
  (the simple-string
       (if (simple-string-p s)
	   s
	 (copy-seq s))))

;;;;;;;;;;;;;;;;;;;;;
;;; external function
(defun load-tagger (data-dir &optional (feature-parser #'identity))
  (make-tagger :wdc (dic:load data-dir feature-parser)
	       :unk (unk:load data-dir)
	       :mtx (mtx:load data-dir)))

(defun parse (tagger text &aux rlt)
  (let ((wdc  (tagger-wdc tagger))
	(text (coerce-to-simple-string text)))
    (do ((vn (parse-impl tagger (code-stream:make text 0) (length text))
	     (vn:prev vn)))
	((null (vn:prev vn)) rlt)
      (push (igo::make-morpheme (subseq text (vn:start vn) (vn:end vn))
				(dic:word-data (vn:word-id vn) wdc)
				(vn:start vn))
	    rlt))))

(defun wakati (tagger text &aux rlt)
  (let ((text (coerce-to-simple-string text)))
    (do ((vn (parse-impl tagger (code-stream:make text 0) (length text))
	     (vn:prev vn)))
	((null (vn:prev vn)) rlt)
      (push (subseq text (vn:start vn) (vn:end vn)) rlt))))