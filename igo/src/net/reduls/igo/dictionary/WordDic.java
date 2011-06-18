package net.reduls.igo.dictionary;

import java.io.IOException;
import java.util.List;
import net.reduls.igo.trie.Searcher;
import net.reduls.igo.util.FileMappedInputStream;

public final class WordDic {
    private final Searcher trie;
    private final String   data;
    private final int[]    indices;

    public final short[] costs;       // consts[単語ID] = 単語のコスト
    public final short[] leftIds;     // leftIds[単語ID] = 単語の左文脈ID
    public final short[] rightIds;    // rightIds[単語ID] = 単語の右文脈ID
    public final int[]   dataOffsets; // dataOffsets[単語ID] = 単語の素性データの開始位置

    public WordDic(String dataDir) throws IOException {
	trie    = new Searcher(dataDir+"/word2id");
	data    = FileMappedInputStream.getString(dataDir+"/word.dat");
	indices = FileMappedInputStream.getIntArray(dataDir+"/word.ary.idx");
	
	{
	    final FileMappedInputStream fmis = new FileMappedInputStream(dataDir+"/word.inf");
	    final int wordCount = fmis.size()/(4+2+2+2);
	    
	    try {
		dataOffsets= fmis.getIntArray(wordCount);
		leftIds    = fmis.getShortArray(wordCount);
		rightIds   = fmis.getShortArray(wordCount);
		costs      = fmis.getShortArray(wordCount);
	    } finally {
		fmis.close();
	    }	
	}
    }

    public void search(CharSequence text, int start, List<ViterbiNode> result) {
	trie.eachCommonPrefix(text, start, new Collect(result));
    }

    public void searchFromTrieId(int trieId, int start, int wordLength, boolean isSpace, List<ViterbiNode> result) {
	final int end = indices[trieId+1];
	for(int i=indices[trieId]; i < end; i++)
	    result.add(new ViterbiNode(i, start, (short)wordLength, costs[i], leftIds[i], rightIds[i], isSpace));
    }

    public String wordData(int wordId){
	return data.substring(dataOffsets[wordId], dataOffsets[wordId+1]);
    }
    
    private class Collect implements Searcher.Callback {
	public final List<ViterbiNode> ms;
	public Collect(List<ViterbiNode> result) { ms = result; }
	
	public void call(int start, int offset, int trieId) {
	    final int end = indices[trieId+1];
	    for(int i=indices[trieId]; i < end; i++)
		ms.add(new ViterbiNode(i, start, (short)offset, costs[i], leftIds[i], rightIds[i], false));
	}
    }

    public static interface Callback {
        public void call(ViterbiNode vn);
        public boolean isEmpty();
    }
}