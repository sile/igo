package net.reduls.igo.dictionary;

/**
 * Viterbiアルゴリズムで使用されるノード
 */
public final class ViterbiNode {
    public int         cost;        // 始点からノードまでの総コスト
    public ViterbiNode prev = null; // コスト最小の前方のノードへのリンク

    public final int   wordId;      // 単語ID
    public final short leftId;      // 左文脈ID
    public final short rightId;     // 右文脈ID
    public final int   start;       // 入力テキスト内での形態素の開始位置
    public final short length;      // 形態素の表層形の長さ(文字数)

    public final boolean isSpace;   // 形態素の文字種(文字カテゴリ)が空白文字かどうか
    
    public ViterbiNode(int wid, int beg, short len, short wordCost, short l, short r, boolean space) {
	wordId = wid;
	leftId = l;
	rightId =r;
	length = len;
        cost = wordCost;
	isSpace = space;
	start = beg;
    }

    public static ViterbiNode makeBOSEOS() { 
	return new ViterbiNode(0,0,(short)0,(short)0,(short)0,(short)0,false); 
    }
}