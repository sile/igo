package net.reduls.igo.trie;

import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;

/**
 * DoubleArray構築時に使用可能なノードを割り当てるためのクラス
 */
final class Allocator {
    private ArrayList<LinkNode> lnk = new ArrayList<LinkNode>();
    private BitSet bset             = new BitSet();

    public Allocator() {
	lnk.add(new LinkNode(0,0));
	resizeLink(Node.Chck.CODE_LIMIT*10);
    }
   
    /**
     * 遷移に使用される文字のリストを受け取り、それらを割り当て可能なベースノードのインデックスを返す。
     *
     * @params codes 遷移文字リスト。昇順にソートされている必要がある
     * @return 引数の遷移文字群を割り当て可能なベースノードのインデックス
     */
    public int xCheck(List<Character> codes) {
	int cur = lnk.get(Node.Chck.CODE_LIMIT).next;
	for(;; cur=lnk.get(cur).next) {
	    final int x = cur-codes.get(0);
	    if(bset.get(x)==false && canAllocate(codes, x)) {
		bset.flip(x);  // このベースノードは使用中だというマークをつける
		
		for(int i=0; i < codes.size(); i++)
		    alloc(x+codes.get(i));
		return x;
	    }
	}
    }
    
    private boolean canAllocate(List<Character> codes, int x) {
	for(int i=1; i < codes.size(); i++)
	    if(x+codes.get(i) < lnk.size() && lnk.get(x+codes.get(i)).next==0)
		return false;
	return true;
    }
    
    private void alloc(int node) {
	while(node >= lnk.size()-1) resizeLink(0);
	
	lnk.get(lnk.get(node).prev).next = lnk.get(node).next;
	lnk.get(lnk.get(node).next).prev = lnk.get(node).prev;
	lnk.get(node).next = 0;
    }

    private void resizeLink(int hint) {
	final int newSize = Math.max(hint, lnk.size()*2);
	lnk.get(lnk.size()-1).next = lnk.size(); 
	
	for(int i=lnk.size(); i < newSize; i++)
	    lnk.add(new LinkNode(i-1, i+1)); 
	
	lnk.get(newSize-1).next = 0;
    }
    
    private static class LinkNode {
	public LinkNode(int p, int n) { prev=p; next=n; }
	public int next;
	public int prev;
    }
}