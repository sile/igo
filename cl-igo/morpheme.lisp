(in-package :igo)

(defstruct (morpheme (:constructor morpheme-new (surface feature start)))
  (surface ""  :type string)  ; TODO: 共有した方が良いかどうかは後で検討する
  (feature nil :type t)
  (start   0   :type fixnum))