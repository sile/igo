package net.reduls.igo.trie;

import java.io.IOException;
import net.reduls.igo.util.FileMappedInputStream;

/**
 * DoubleArray検索用のクラス
 */
public final class Searcher {
    private final int     keySetSize;
    private final int[]   base;
    private final char[]  chck;
    private final int[]   begs;
    private final short[] lens;
    private final String  tail;

    /**
     * 保存されているDoubleArrayを読み込んで、このクラスのインスタンスを作成する
     *
     * @param filepath DoubleArrayが保存されているファイルのパス
     * @throws IOException filepathで示されるファイルの読み込みに失敗した場合に送出される
     */
    public Searcher(String filepath) throws IOException {
	final FileMappedInputStream fmis = new FileMappedInputStream(filepath);
	try {
	    final int nodeSz = fmis.getInt();
	    final int tindSz = fmis.getInt();
	    final int tailSz = fmis.getInt();
	    keySetSize = tindSz;
	    begs = fmis.getIntArray(tindSz);
	    base = fmis.getIntArray(nodeSz);
	    lens = fmis.getShortArray(tindSz);
	    chck = fmis.getCharArray(nodeSz);
	    tail = fmis.getString(tailSz);
	} finally {
	    fmis.close();
	}
    }

    /**
     * DoubleArrayに格納されているキーの数を返す
     * @return DoubleArrayに格納されているキー数
     */
    public int size() { return keySetSize; }
    
    /**
     * キーを検索する
     *
     * @param key 検索対象のキー文字列
     * @return キーが見つかった場合はそのIDを、見つからなかった場合は-1を返す
     */
    public int search(CharSequence key) {
	int node = base[0];
	KeyStream in = new KeyStream(key);
	
	for(char code=in.read();; code=in.read()) {
	    final int idx = node+code;
	    node = base[idx];
	    
	    if(chck[idx]==code)
		if(node >= 0)                           continue;
		else if(in.eos() || keyExists(in,node)) return Node.Base.ID(node);
	    return -1;
	}
    }
    
    /**
     * common-prefix検索でキーが見つかった場合に呼び出されるコールバッククラスのインターフェース
     */
    public interface Callback {
	/**
	 * eachCommonPrefixメソッドで該当するキーの部分文字列が見つかった都度に呼び出されるメソッド
	 *
	 * @param start 入力テキストの検索開始位置
	 * @param offset 一致した部分文字列の終端位置
	 * @param id 一致した部分文字列のID
	 */
	public void call(int start, int offset, int id);
    }

    /**
     * common-prefix検索を行う
     * 条件に一致するキーが見つかる度に、fn.call(...)メソッドが呼び出される
     *
     * @param key 検索対象のキー文字列
     * @param start 検索対象となるキー文字列の最初の添字
     * @param fn 一致を検出した場合に呼び出されるメソッドを定義したコールバッククラス
     */
    public void eachCommonPrefix(CharSequence key, int start, Callback fn) {
	int node = base[0];
	int offset=0;
	KeyStream in = new KeyStream(key, start);
	
	for(char code=in.read();; code=in.read(),offset++) {
	    final int terminalIdx = node + Node.Chck.TERMINATE_CODE;

	    if(chck[terminalIdx] == Node.Chck.TERMINATE_CODE) {
		fn.call(start, offset, Node.Base.ID(base[terminalIdx]));
		if(code==Node.Chck.TERMINATE_CODE)
		    return;
	    }
	    
	    final int idx = node + code;
	    node = base[idx];
	    if(chck[idx] == code)
		if(node >= 0) continue;
		else          call_if_keyIncluding(in, node, start, offset, fn);
	    return;
	}
    }
    
    private void call_if_keyIncluding(KeyStream in, int node, int start, int offset, Callback fn) {
	final int id  = Node.Base.ID(node);
	if(in.startsWith(tail, begs[id], lens[id]))
	    fn.call(start, offset+lens[id]+1, id);
    }
    
    private boolean keyExists(KeyStream in, int node) {
	final int id  = Node.Base.ID(node);
	final String s = tail.substring(begs[id], begs[id]+lens[id]);
	return in.rest().equals(s);
    }
}