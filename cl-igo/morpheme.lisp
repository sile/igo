(in-package :igo)

(defstruct (morpheme (:constructor make-morpheme (surface feature start)))
  (surface ""  :type simple-string)  
  (feature nil :type t)
  (start   0   :type fixnum))