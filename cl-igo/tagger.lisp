(in-package :igo)

(igo::set-package-nickname :igo.word-dic     :wdc) 
(igo::set-package-nickname :igo.unknown      :unk)
(igo::set-package-nickname :igo.matrix       :mtx)
(igo::set-package-nickname :igo.code-stream  :code-stream)
(igo::set-package-nickname :igo.viterbi-node :vn)

(defstruct tagger
  (wdc nil :type wdc:word-dic)
  (unk nil :type unk:unknown)
  (mtx nil :type mtx:matrix))

(defun tagger-new (data-dir &optional (feature-parser #'identity))
  (make-tagger :wdc (wdc:load data-dir feature-parser)
	       :unk (unk:load data-dir)
	       :mtx (mtx:load data-dir)))

(eval-when (:compile-toplevel :load-toplevel :execute)
  (igo::defconst-once-only +BOS-NODES+ (list (vn:new-bos/eos))))

(defmacro nconcf (lst1 lst2)
  `(setf ,lst1 (nconc ,lst1 ,lst2)))

(defun set-mincost-node (vn prevs mtx wdc)
  (flet ((calc-cost (prev cur)
           (+ (vn:cost prev) (mtx:link-cost (vn:right-id prev) (vn:left-id cur) mtx))))
  (let ((fst (first prevs)))
    (setf (vn:prev vn) fst
	  (vn:cost vn) (calc-cost fst vn))

    (dolist (p (cdr prevs))
      (let ((cost (calc-cost p vn)))
	(when (< cost (vn:cost vn))
	  (setf (vn:cost vn) cost
		(vn:prev vn) p))))
    
    (incf (vn:cost vn) (wdc:cost (vn:word-id vn) wdc))
    vn)))

(defun parse-impl (tagger cs len)
  (let ((nodes (make-sequence 'simple-vector (1+ len) :initial-element nil))
	(wdc   (tagger-wdc tagger))
	(unk   (tagger-unk tagger))
	(mtx   (tagger-mtx tagger)))
    (setf (aref nodes 0) +BOS-NODES+)

    (loop FOR i FROM 0 BELOW len 
	  FOR prevs = (aref nodes i) DO
      (setf (code-stream:position cs) i)
      (when prevs
	(dolist (vn (unk:search cs unk wdc (wdc:search cs '() wdc)))
	  (if (vn:space? vn)
	      (nconcf (aref nodes (vn:end vn)) prevs)
	    (push (set-mincost-node vn prevs mtx wdc) (aref nodes (vn:end vn)))))))
    
    (vn:prev (set-mincost-node (vn:new-bos/eos) (aref nodes len) mtx wdc))))

(defun parse (tagger text &aux (wdc (tagger-wdc tagger)) rlt)
  (do ((vn (parse-impl tagger (code-stream:make text 0) (length text))
	   (vn:prev vn)))
      ((null (vn:prev vn)) rlt)
    (push (igo::morpheme-new (subseq text (vn:start vn) (vn:end vn))
			     (wdc:word-data (vn:word-id vn) wdc)
			     (vn:start vn))
	   rlt)))

(defun wakati (tagger text &aux rlt)
  (do ((vn (parse-impl tagger (code-stream:make text 0) (length text))
	   (vn:prev vn)))
      ((null (vn:prev vn)) rlt)
    (push (subseq text (vn:start vn) (vn:end vn)) rlt)))

(igo::delete-package-nickname :igo.word-dic)
(igo::delete-package-nickname :igo.unknown)
(igo::delete-package-nickname :igo.matrix)
(igo::delete-package-nickname :igo.code-stream)
(igo::delete-package-nickname :igo.viterbi-node)