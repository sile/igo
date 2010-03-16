package net.reduls.igo.trie;

import java.util.ArrayList;

/**
 * TAIL配列(文字列)の圧縮を行うクラス
 * TAILに格納されている文字列群の内で、末尾部分が重複するものは、同じ領域を使用するようにする。
 */
final class ShrinkTail {
    private final StringBuilder      tail;
    private final ArrayList<Integer> begs;
    private final ArrayList<Short>   lens;

    /**
     * 圧縮対象となるTAIL配列および、TAIL配列へのインデックスを渡してインスタンスを初期化する。
     * 引数に渡した各オブジェクトはshrinkメソッドの呼び出しに伴い、破壊的に更新される。
     */
    public ShrinkTail(StringBuilder tail, ArrayList<Integer> begs, ArrayList<Short> lens) {
	this.tail = tail;
	this.begs = begs;
	this.lens = lens;
    }

    public void shrink() {
	// TAILに格納されている文字列群を、その末尾から比較してソートする
	final ArrayList<TailString> sorted = new ArrayList<TailString>(begs.size());
	for(int i=0; i < begs.size(); i++)
	    sorted.add(new TailString(i));
	java.util.Collections.sort(sorted);
	
	// 新しいTAILを用意する
	// その際に、末尾部分が重複する文字列同士は、領域を共有するようにする
	final StringBuilder newTail = new StringBuilder();
	for(int i=0; i < sorted.size(); i++) {
	    final TailString ts = sorted.get(i);
	    
	    int begIndex = newTail.length();
	    if(i > 0 && sorted.get(i-1).s.endsWith(ts.s))
		begIndex -= ts.s.length();  // 末尾文字列を共有する
	    else
		newTail.append(ts.s);       // 新しく追加する
	    
	    // TAIL配列へのポインタを更新する
	    begs.set(ts.id, begIndex);
	    lens.set(ts.id, (short)ts.s.length());
	}
	
	tail.replace(0, tail.length(), newTail.toString());
    }
    
    private class TailString implements Comparable<TailString> {
	public final int    id;
	public final String s;

	public TailString(int id) {
	    this.id = id;
	    this.s  = tail.substring(begs.get(id), begs.get(id)+lens.get(id));
	}
	
	// TailStringを文字列の末尾から順に比較する
	public int compareTo(TailString ts) {
	    for(int i=s.length()-1, j=ts.s.length()-1;; i--, j--)
		if(i < 0)                             return j < 0 ? 0 : 1;
		else if(j < 0)                        return -1;
		else if(s.charAt(i) > ts.s.charAt(j)) return -1;
		else if(s.charAt(i) < ts.s.charAt(j)) return 1;
	}
    }
}