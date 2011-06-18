package net.reduls.igo;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import net.reduls.igo.dictionary.Matrix;
import net.reduls.igo.dictionary.WordDic;
import net.reduls.igo.dictionary.Unknown;
import net.reduls.igo.dictionary.ViterbiNode;

/**
 * 形態素解析を行うクラス
 */
public final class Tagger {
    private static final ArrayList<ViterbiNode> BOS_NODES = new ArrayList<ViterbiNode>(1);
    static {
	BOS_NODES.add(ViterbiNode.makeBOSEOS());
    }
    
    private final WordDic wdc;
    private final Unknown unk;
    private final Matrix  mtx;
   
    /**
     * バイナリ辞書を読み込んで、形態素解析器のインスタンスを作成する
     *
     * @param dataDir バイナリ辞書があるディレクトリ
     * @throws FileNotFoundException 間違ったディレクトリが指定された場合に送出される
     * @throws IOException その他の入出力エラーが発生した場合に送出される
     */
    public Tagger(String dataDir) throws FileNotFoundException, IOException {
	wdc = new WordDic(dataDir);
	unk = new Unknown(dataDir);
	mtx = new Matrix(dataDir);
    }
    
    /**
     * 形態素解析を行う
     *
     * @param text 解析対象テキスト
     * @return 解析結果の形態素のリスト
     */
    public List<Morpheme> parse(CharSequence text) {
	return parse(text, new ArrayList<Morpheme>(text.length()/2));
    }

    /**
     * 形態素解析を行う
     *
     * @param text 解析対象テキスト
     * @param result 解析結果の形態素が追加されるリスト
     * @return 解析結果の形態素リスト. {@code parse(text,result)=result}
     */
    public List<Morpheme> parse(CharSequence text, List<Morpheme> result) {
	for(ViterbiNode vn=parseImpl(text); vn!=null; vn=vn.prev) {
	    final String surface = text.subSequence(vn.start, vn.start+vn.length).toString();
	    final String feature = wdc.wordData(vn.wordId);
	    result.add(new Morpheme(surface, feature, vn.start));
	}
	return result;
    }

    /**
     * 分かち書きを行う
     *
     * @param text 分かち書きされるテキスト
     * @return 分かち書きされた文字列のリスト
     */
    public List<String> wakati(CharSequence text) {
	return wakati(text, new ArrayList<String>(text.length()/2));
    }

    /**
     * 分かち書きを行う
     *
     * @param text 分かち書きされるテキスト
     * @param result 分かち書き結果の文字列が追加されるリスト
     * @return 分かち書きされた文字列のリスト. {@code wakati(text,result)=result}
     */
    public List<String> wakati(CharSequence text, List<String> result) {
	for(ViterbiNode vn=parseImpl(text); vn!=null; vn=vn.prev) 
	    result.add(text.subSequence(vn.start, vn.start+vn.length).toString());	
	return result;
    }
    
    private ViterbiNode parseImpl(CharSequence text) {
	final int len = text.length();
	final ArrayList<ArrayList<ViterbiNode>> nodesAry = new ArrayList<ArrayList<ViterbiNode>>(len+1);
	
	nodesAry.add(BOS_NODES);
	for(int i=1; i <= len; i++) 
	    nodesAry.add(new ArrayList<ViterbiNode>());
	
        MakeLattice fn = new MakeLattice(nodesAry);
        for(int i=0; i < len; i++) {
            if(nodesAry.get(i).isEmpty()==false) {
                fn.set(i);
                wdc.search(text, i, fn);      // 単語辞書から形態素を検索
                unk.search(text, i, wdc, fn); // 未知語辞書から形態素を検索
            }
        }

	ViterbiNode cur = setMincostNode(ViterbiNode.makeBOSEOS(), nodesAry.get(len)).prev;

        // reverse
        ViterbiNode head = null;
        while(cur.prev != null) {
          final ViterbiNode tmp = cur.prev;
          cur.prev = head;
          head = cur;
          cur = tmp;
        }
        return head;
    }

    private ViterbiNode setMincostNode(ViterbiNode vn, ArrayList<ViterbiNode> prevs) {
	final ViterbiNode f = vn.prev = prevs.get(0);
        int minCost = f.cost + mtx.linkCost(f.rightId, vn.leftId);

        for(int i=1; i < prevs.size(); i++) {
            final ViterbiNode p = prevs.get(i);
	    final int cost = p.cost + mtx.linkCost(p.rightId, vn.leftId);
	    if(cost < minCost) {
		minCost = cost;
		vn.prev = p;
	    }
	}
	vn.cost += minCost;
	return vn;
    }

    private final class MakeLattice implements WordDic.Callback {
        private final ArrayList<ArrayList<ViterbiNode>> nodesAry;
        private int i;
        private ArrayList<ViterbiNode> prevs;
        private boolean empty=true;

        public MakeLattice(ArrayList<ArrayList<ViterbiNode>> nodesAry) {
            this.nodesAry = nodesAry;
        }
        
        public void set(int i) {
            this.i = i;
            prevs = nodesAry.get(i);
            empty = true;
        }

        public void call(ViterbiNode vn) {
            empty=false;
            if(vn.isSpace)
                nodesAry.get(i+vn.length).addAll(prevs);
            else
                nodesAry.get(i+vn.length).add(setMincostNode(vn, prevs));
        }
        
        public boolean isEmpty() { return empty; }
    }
}
