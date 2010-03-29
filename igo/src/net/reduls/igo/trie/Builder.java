package net.reduls.igo.trie;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import net.reduls.igo.util.FileMappedOutputStream;

/**
 * DoubleArrayの構築を行うクラス
 */
public final class Builder {
    private final ArrayList<KeyStream> ksList;
    private final AutoArray<Integer>   base = new AutoArray<Integer>();
    private final AutoArray<Character> chck = new AutoArray<Character>();
    private final ArrayList<Integer>   begs = new ArrayList<Integer>();
    private final ArrayList<Short>     lens = new ArrayList<Short>();
    private final StringBuilder        tail = new StringBuilder();
    
    private Builder(List<String> keyList) {
	ksList = new ArrayList<KeyStream>(keyList.size());
	
	// ソート and ユニーク
	java.util.Collections.sort(keyList);
	String prev=null;
	for(String k : keyList)
	    if(k.equals(prev)==false)
		ksList.add(new KeyStream(prev=k));
    }
    
    /**
     * キー文字列のリストから、DoubleArrayを構築する
     *
     * @param keyList DoubleArrayのキーとなる文字列のリスト. 破壊的に更新される
     * @return 構築済みDoubleArrayを有するBuilderインスタンス
     */
    public static Builder build(List<String> keyList) {
	Builder bld = new Builder(keyList);
	bld.buildImpl(new Allocator(), 0, bld.ksList.size(), 0);
	return bld;
    }

    /**
     * 構築したDoubleArrayをファイルに保存する
     *
     * @param filepath DoubleArrayを保存するファイルのパス
     * @throws IOException filepathで示されたファイルへの書き込みに失敗した場合に送出される
     */ 
    public void save (String filepath) throws IOException {
	new ShrinkTail(tail,begs,lens).shrink();
	
	int nodeSize = chck.size();

	// 末尾の未使用部分を取り除く
	for(; nodeSize > 0 && chck.get(nodeSize-1) == Node.Chck.VACANT_CODE; nodeSize--);
	nodeSize += Node.Chck.CODE_LIMIT;  // 検索時の範囲外アクセスを防ぐために、余白を設ける

	final int total = 4*3 + nodeSize*6 + begs.size()*6 + tail.length()*2;
	final FileMappedOutputStream fmos = new FileMappedOutputStream(filepath, total);
	try {
	    fmos.putInt(nodeSize);
	    fmos.putInt(begs.size());
	    fmos.putInt(tail.length());

	    // 4byte
	    for(Integer n : begs) fmos.putInt(n);
	    for(int i=0; i < nodeSize; i++)
		fmos.putInt(base.get(i, Node.Base.INIT_VALUE));

	    // 2byte
	    for(Short   n : lens) fmos.putShort(n);
	    for(int i=0; i < nodeSize; i++)
		fmos.putChar(chck.get(i, Node.Chck.VACANT_CODE));
	    
	    fmos.putString(tail.toString());
	} finally {
	    fmos.close();
	}
    }

    /**
     * 実際にDoubleArrayの構築を行うメソッド
     */
    private void buildImpl(Allocator alloca, int beg, int end, int rootIdx) {
	if(end-beg==1) {
	    // これ以降は単一の遷移パスしかないので、まとめてTAILに挿入してしまう
	    insertTail(ksList.get(beg), rootIdx);
	    return;  
	}

	final List<Integer> endList = new ArrayList<Integer>();
	final List<Character> codeList = new ArrayList<Character>();
	char prev=Node.Chck.VACANT_CODE;
	
	// rootIdxから遷移する文字を集める
	for(int i=beg; i < end; i++) {
	    char cur = ksList.get(i).read();

	    if(prev != cur) {
		codeList.add(prev=cur);
		endList.add(i);
	    }
	}
	endList.add(end);

	// rootIdxから派生(遷移)するノードを設定し、その各ノードに対して再帰的に処理を繰り返す
	final int x = alloca.xCheck(codeList);
	for(int i=0; i < codeList.size(); i++)
	    buildImpl(alloca, endList.get(i), endList.get(i+1), setNode(codeList.get(i),rootIdx,x));
    }

    private int setNode(char code, int prev, int xNode) {
	final int next = xNode+code;
	base.set(prev,xNode,Node.Base.INIT_VALUE);
	chck.set(next,code, Node.Chck.VACANT_CODE);
	return next;
    }

    private void insertTail(KeyStream in, int node) {
	base.set(node,Node.Base.ID(begs.size()),Node.Base.INIT_VALUE);
	
	begs.add(tail.length());
	tail.append(in.rest());
	lens.add((short)in.rest().length());
    }
}