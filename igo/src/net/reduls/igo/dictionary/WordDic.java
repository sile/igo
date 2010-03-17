package net.reduls.igo.dictionary;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import net.reduls.igo.trie.Builder;
import net.reduls.igo.trie.Searcher;
import net.reduls.igo.util.ReadLine;
import net.reduls.igo.util.FileMappedInputStream;
import net.reduls.igo.util.FileMappedOutputStream;

public final class WordDic {
    private final Searcher trie;
    private final String   data;
    private final Word     word;
    private final int[]    indices;

    public WordDic(String dataDir) throws IOException {
	trie    = new Searcher(dataDir+"/word2id");
	word    = new Word(dataDir+"/word.inf");
	data    = FileMappedInputStream.getString(dataDir+"/word.dat");
	indices = FileMappedInputStream.getIntArray(dataDir+"/word.ary.idx");
    }

    public short cost(int wordId) { return word.costs[wordId]; }
    public short leftId(int wordId) { return word.leftIds[wordId]; }
    public short rightId(int wordId) { return word.rightIds[wordId]; }
    public int dataOffset(int wordId) { return word.dataOffsets[wordId]; }

    public void search(CharSequence text, int start, List<ViterbiNode> result) {
	trie.eachCommonPrefix(text, start, new Collect(result));
    }

    public void searchFromTrieId(int trieId, int start, int wordLength, boolean isSpace, List<ViterbiNode> result) {
	final int end = indices[trieId+1];
	for(int i=indices[trieId]; i < end; i++)
	    result.add(new ViterbiNode(i, start, (short)wordLength, leftId(i), rightId(i), isSpace));
    }

    public String wordData(int wordId){
	return data.substring(dataOffset(wordId), dataOffset(wordId+1));
    }
    
    private class Collect implements Searcher.Callback {
	public final List<ViterbiNode> ms;
	public Collect(List<ViterbiNode> result) { ms = result; }
	
	public void call(int start, int offset, int trieId) {
	    final int end = indices[trieId+1];
	    for(int i=indices[trieId]; i < end; i++)
		ms.add(new ViterbiNode(i, start, (short)offset, leftId(i), rightId(i), false));
	}
    }

    // TODO: delimiterを指定可能にする
    private static void collectKey(ReadLine rl, List<String> keyList, String prefix) throws IOException {
	try {
	    for(String line=rl.read(); line!=null; line=rl.read()) {
		final String key = line.substring(0, line.indexOf(','));
		keyList.add(prefix+key);
	    }
	} finally {
	    rl.close();	
	}
    }

    public static void genWordIdMap(String inputDir, String outputDir, String encoding) throws IOException {
	final List<String> keyList = new ArrayList<String>();
	
	// 未知語定義からキーを集める
	collectKey(new ReadLine(inputDir+"/unk.def", encoding), keyList, CharCategory.KEY_PREFIX);
	
	// 単語辞書からキーを集める
	for(File csvFile : new File(inputDir).listFiles(new onlyCsv()))
	    collectKey(new ReadLine(csvFile.getPath(), encoding), keyList, "");
	
	final Builder bld = Builder.build(keyList);
	bld.save(outputDir+"/word2id");
    }

    // TODO: delimiter
    private static void collectWordInfo(String filepath, String encoding, Searcher wid, String prefix,
					ArrayList<ArrayList<WordInfo>> ws) throws IOException {
	final ReadLine rl = new ReadLine(filepath, encoding);
	try {
	    for(String s=rl.read(); s!=null; s=rl.read()) {
		final int p1 = s.indexOf(',');         // key
		final int p2 = s.indexOf(',',p1+1);    // left id
		final int p3 = s.indexOf(',',p2+1);    // right id
		final int p4 = s.indexOf(',',p3+1);    // cost
		final String data = s.substring(p4+1); // data

		final int id = wid.search(prefix+s.substring(0,p1));
		if(id < 0)
		    throw new IOException("Word '"+s.substring(0,p1)+"' is unregistered in trie"); // TODO: parse exception
		
		ws.get(id).add(new WordInfo(Short.valueOf(s.substring(p1+1,p2)),
					    Short.valueOf(s.substring(p2+1,p3)),
					    Short.valueOf(s.substring(p3+1,p4)),
					    data));
	    }
	} catch (Exception e) {
	    throw new IOException(filepath+": "+rl.lineNumber(), e); // XXX: for debug
	} finally {
	    rl.close();	
	}
    }

    private static void removeUnusedEntry(ArrayList<ArrayList<WordInfo>> ws) {
	for(ArrayList<WordInfo> wlist : ws) {
	    java.util.Collections.sort(wlist);
	    int last=0;
	    for(int i=1; i < wlist.size(); i++) {
		if(!(wlist.get(last).leftId  == wlist.get(i).leftId &&
		     wlist.get(last).rightId == wlist.get(i).rightId))
		    wlist.set(++last, wlist.get(i));
	    }
	    for(int i=wlist.size()-1; i > last; i--) 
		wlist.remove(i);
	}
    }
    
    public static void genWordInfo(String inputDir, String outputDir, String encoding) throws IOException {
	final Searcher wid = new Searcher(outputDir+"/word2id");
	final ArrayList<ArrayList<WordInfo>> ws = new ArrayList<ArrayList<WordInfo>>(wid.size());
	for(int i=0; i < wid.size(); i++)
	    ws.add(new ArrayList<WordInfo>());

	// 未知語定義からデータを集める
	collectWordInfo(inputDir+"/unk.def", encoding, wid, CharCategory.KEY_PREFIX, ws);
	
	// 単語辞書からデータを集める
	for(File csvFile : new File(inputDir).listFiles(new onlyCsv())) 
	    collectWordInfo(csvFile.getPath(), encoding, wid, "", ws);

	// 無駄な項目を削除する
	// NOTE:
	removeUnusedEntry(ws);

	// 単語情報を出力
	final StringBuilder wdat = new StringBuilder();
	
	int size=0;
	for(ArrayList<WordInfo> wlist : ws)
	    size += wlist.size();

	final FileMappedOutputStream fmosInf = 
	    new FileMappedOutputStream(outputDir+"/word.inf", (size+1)*(2+2+2+4));
	try {
	    for(ArrayList<WordInfo> wlist : ws) // dataOffset
		for(WordInfo w : wlist) {
		    fmosInf.putInt(wdat.length());
		    wdat.append(w.data);
		}
	    fmosInf.putInt(wdat.length());      

	    for(ArrayList<WordInfo> wlist : ws) // leftId
		for(WordInfo w : wlist) 
		    fmosInf.putShort(w.leftId);
	    fmosInf.putShort((short)0);
	    
	    for(ArrayList<WordInfo> wlist : ws) // rightId
		for(WordInfo w : wlist) 
		    fmosInf.putShort(w.rightId);
	    fmosInf.putShort((short)0);
	    
	    for(ArrayList<WordInfo> wlist : ws) // cost
		for(WordInfo w : wlist) 
		    fmosInf.putShort(w.cost);
	    fmosInf.putShort((short)0);
	} finally {
	    fmosInf.close();
	}

	// 単語データを出力
	final FileMappedOutputStream fmosDat = 
	    new FileMappedOutputStream(outputDir+"/word.dat", wdat.length()*2);
	try {
	    fmosDat.putString(wdat.toString());
	} finally {
	    fmosDat.close();
	}
	
	// 単語情報の配列へのインデックスを保存する
	{ 
	    final FileMappedOutputStream fmosIdx = 
		new FileMappedOutputStream(outputDir+"/word.ary.idx", (ws.size()+1)*4);
	    int begIndex=0;
	    try {
		for(ArrayList<WordInfo> wlist : ws) {
		    fmosIdx.putInt(begIndex);
		    begIndex += wlist.size();
		}
		fmosIdx.putInt(begIndex);
	    } finally {
		fmosIdx.close();
	    }
	}
    }

    private static class WordInfo implements Comparable<WordInfo> {
	public short  leftId;
	public short  rightId;
	public short  cost;
	public String data;

	public WordInfo(short lid, short rid, short c, String dat) {
	    leftId = lid;
	    rightId = rid;
	    cost = c;
	    data = dat;
	}

	public int compareTo(WordInfo wi) {
	    if(leftId != wi.leftId)
		return leftId - wi.leftId;
	    if(rightId != wi.rightId)
		return rightId - wi.rightId;
	    return cost - wi.cost;
	}
    }

    private static class onlyCsv implements FileFilter {
	public boolean accept(File file) {
	    return file.isFile() && file.toString().matches(".*\\.csv$");
	}
    }
}