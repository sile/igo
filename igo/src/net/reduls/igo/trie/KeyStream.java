package net.reduls.igo.trie;

/**
 * 文字列を文字のストリームとして扱うためのクラス。
 * readメソッドで個々の文字を順に読み込み、文字列の終端に達した場合には{@code Node.Chck.TERMINATE_CODE}が返される。
 * XXX: クラス名は不適切
 */
final class KeyStream implements Comparable<KeyStream> {
    private final CharSequence s;
    private int cur;
    
    public KeyStream(CharSequence key) {
	s   = key;
	cur = 0;
    }
    
    public KeyStream(CharSequence key, int start) {
	s   = key;
	cur = start;
    }
    
    public int compareTo(KeyStream ks) {
	return rest().compareTo(ks.rest());
    }
    
    /**
     * このメソッドは動作的には、{@code rest().startsWith(prefix.substring(beg, len))}、と等価。
     * ほんの若干だが、パフォーマンスを改善するために導入。
     * 簡潔性のためになくしても良いかもしれない。
     */
    public boolean startsWith(CharSequence prefix, int beg, int len) {
	if(s.length()-cur < len)
	    return false;
	
	for(int i=0; i < len; i++)
	    if(s.charAt(cur+i) != prefix.charAt(beg+i))
		return false;
	return true;
    }
    
    public String rest() { return s.subSequence(cur,s.length()).toString(); }
    public char   read() { return eos() ? Node.Chck.TERMINATE_CODE : s.charAt(cur++); }
    public boolean eos() { return cur == s.length(); }
}