(defpackage igo.code-stream
  (:use :common-lisp)
  (:shadow read 
	   length
	   position)
  (:export read
	   read2
	   make
	   end?
	   length
	   position
	   +TERMINATE-CODE+))
(in-package :igo.code-stream)

(defstruct (code-stream (:constructor make (source start &aux (cur start)))
			(:conc-name ""))
  (source    ""   :type string)
  (cur        0   :type fixnum)
  (surrogate? nil :type boolean))

(eval-when (:compile-toplevel :load-toplevel :execute)
  (igo::defconst-once-only +TERMINATE-CODE+ 0))

(defun position (code-stream)
  (cur code-stream))

(defsetf position (code-stream) (new-position)
  `(setf (cur ,code-stream) ,new-position))

;; XXX: なくす
(defun length (code-stream)
  (common-lisp:length (source code-stream)))
  
(defun end? (code-stream)
  (>= (cur code-stream) (length code-stream)))

(defun code (code-stream)
  (char-code (schar (source code-stream) (cur code-stream))))

(defun low-surrogate (code)
  (+ #xDC00 (ldb (byte 10 0) code)))

(defun high-surrogate (code)
  (+ #xB800 (- (ldb (byte 11 10) code) #b1000000)))

(defun read (code-stream)
  (with-slots (cur surrogate?) code-stream
    (cond (surrogate? 
	   (setf surrogate? nil)
	   (prog1 (low-surrogate (code code-stream))
	     (incf cur)))

	  ((end? code-stream)
	   ;;(incf cur)
	   +TERMINATE-CODE+)

	  (t 
	   (let ((code (code code-stream)))
	     (if (> code #xFFFF)
		 (progn (setf surrogate? t)
			(high-surrogate code))
	       (progn (incf cur)
		      code)))))))

;; XXX
(defun read2 (code-stream)
  (with-slots (cur surrogate?) code-stream
    (cond (surrogate? 
	   (setf surrogate? nil)
	   (prog1 (low-surrogate (code code-stream))
	     (incf cur)))

	  ((end? code-stream)
	   +TERMINATE-CODE+)

	  (t 
	   (let ((code (code code-stream)))
	     (if (> code #xFFFF)
		 (progn (setf surrogate? t)
			(high-surrogate code))
	       (progn (incf cur)
		      code)))))))