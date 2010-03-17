package net.reduls.igo.dictionary;

import java.io.IOException;
import java.util.List;

/**
 * 未知語の検索を行うクラス
 */
public final class Unknown {
    private final CharCategory category;  // 文字カテゴリ管理クラス
    private final int spaceId;            // 文字カテゴリがSPACEの文字のID

    public Unknown(String dataDir) throws IOException {
	category = new CharCategory(dataDir);
	spaceId = category.category(' ').id;  // NOTE: ' 'の文字カテゴリはSPACEに予約されている
    }
    
    public void search(CharSequence text, int start, WordDic wdic, List<ViterbiNode> result) {
	final char ch = text.charAt(start);
	final CharCategory.Category ct = category.category(ch);

	if(result.isEmpty()==false && ct.invoke==false)
	    return;
	
	final boolean isSpace = ct.id==spaceId;
	final int limit = Math.min(text.length(), ct.length+start);
	boolean finished = false;
	int i=start;
	for(; i < limit; i++) { 
	    wdic.searchFromTrieId(ct.id, start, (i-start)+1, isSpace, result);
	    if(i+1!=limit && category.isCompatible(ch, text.charAt(i+1)) == false) {
		finished = true;
		break;
	    }
	}
	
	if(ct.group && !finished && i < text.length()) {
	    for(; i < text.length(); i++)
		if(category.isCompatible(ch, text.charAt(i)) == false) {
		    wdic.searchFromTrieId(ct.id, start, i-start, isSpace, result);
		    return;
		}
	    wdic.searchFromTrieId(ct.id, start, text.length()-start, isSpace, result);
	}
    }
}