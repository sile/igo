package net.reduls.igo.dictionary.build;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;
import net.reduls.igo.trie.Builder;
import net.reduls.igo.trie.Searcher;
import net.reduls.igo.util.ReadLine;
import net.reduls.igo.util.FileMappedOutputStream;

/** 
 * テキスト単語辞書をパースして、バイナリ単語辞書を構築するためのクラス
 */
public final class WordDic {
    private final String encoding;
    private final String inputDir;
    private final String outputDir;
    private final String delim;

    /**
     * コンストラクタ
     *
     * @param inputDir テキスト単語辞書が配置されているディレクトリのパス
     * @param encoding テキスト単語辞書の文字列エンコーディング
     * @param outputDir バイナリ単語辞書の保存先ディレクトリ
     * @param delim 単語辞書内の各項目の区切り文字
     */
    public WordDic(String inputDir, String encoding, String outputDir, String delim) {
	this.inputDir =inputDir;
	this.encoding =encoding;
	this.outputDir=outputDir;
	this.delim = delim;
    }
    
    /**
     * 単語の表層形をキーに、対応する一意なIDを値としたtrieを作成し、保存する
     *
     * @throws ParseException テキスト辞書のパースに失敗した場合に送出される
     * @throws IOException 入出力エラーが発生した場合に送出される
     */
    public void buildWordIdMap() throws ParseException, IOException {
	final List<String> keyList = new ArrayList<String>();
	
	// 未知語定義からキーを集める
	collectKey(inputDir+"/unk.def", keyList, CharCategory.KEY_PREFIX);
	
	// 単語辞書からキーを集める
	for(File csvFile : new File(inputDir).listFiles(new onlyCsv()))
	    collectKey(csvFile.getPath(), keyList, "");
	
	final Builder bld = Builder.build(keyList);
	bld.save(outputDir+"/word2id");
    }

    private void collectKey(String path, List<String> keyList, String prefix) throws IOException, ParseException {
	final ReadLine rl = new ReadLine(path,encoding);
	try {
	    for(String line=rl.read(); line!=null; line=rl.read()) {
		final String key = line.substring(0, line.indexOf(delim,1));
		keyList.add(prefix+key);
	    }
	} catch (IndexOutOfBoundsException e) {
	    throw parseException("Word surface must be terminated with '"+delim+"'.",path,rl);
	} finally {
	    rl.close();	
	}
    }
    
    private static ParseException parseException(String msg, String path, ReadLine rl) {
	return new ParseException(msg+"\t{file: "+path+", line: "+rl.lineNumber()+"}", rl.lineNumber());
    }

    /**
     * バイナリ単語辞書を作成し、保存する。
     *
     * @throws ParseException テキスト辞書のパースに失敗した場合に送出される
     * @throws IOException 入出力エラーが発生した場合に送出される
     */
    public void buildWordInfo() throws ParseException, IOException {
	final Searcher wid = new Searcher(outputDir+"/word2id");
	final ArrayList<ArrayList<WordInfo>> ws = new ArrayList<ArrayList<WordInfo>>(wid.size());
	for(int i=0; i < wid.size(); i++)
	    ws.add(new ArrayList<WordInfo>());

	// 未知語定義からデータを集める
	collectWordInfo(inputDir+"/unk.def", wid, CharCategory.KEY_PREFIX, ws);
	
	// 単語辞書からデータを集める
	for(File csvFile : new File(inputDir).listFiles(new onlyCsv())) 
	    collectWordInfo(csvFile.getPath(), wid, "", ws);

	// 無駄な項目を削除する
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

    private void collectWordInfo(String path, Searcher wid, String prefix,
				 ArrayList<ArrayList<WordInfo>> ws) throws ParseException, IOException {
	final ReadLine rl = new ReadLine(path, encoding);
	try {
	    for(String s=rl.read(); s!=null; s=rl.read()) {
		final int p1 = s.indexOf(delim,1);     // key
		final int p2 = s.indexOf(delim,p1+1);  // left id
		final int p3 = s.indexOf(delim,p2+1);  // right id
		final int p4 = s.indexOf(delim,p3+1);  // cost
		if(p1==-1) throw parseException("Word surface must be terminated with '"+delim+"'.",path,rl);
		if(p2==-1) throw parseException("Word left context id must be terminated with '"+delim+"'.",path,rl);
		if(p3==-1) throw parseException("Word right context id must be terminated with '"+delim+"'.",path,rl);
		if(p4==-1) throw parseException("Word cost must be terminated with '"+delim+"'.",path,rl);

		final String data = s.substring(p4+1); // data

		final int id = wid.search(prefix+s.substring(0,p1));
		if(id < 0)
		    throw parseException("Word '"+s.substring(0,p1)+"' is unregistered in trie",path,rl);
		
		ws.get(id).add(new WordInfo(Short.valueOf(s.substring(p1+1,p2)),
					    Short.valueOf(s.substring(p2+1,p3)),
					    Short.valueOf(s.substring(p3+1,p4)),
					    data));
	    }
	} catch (NumberFormatException e) {
	    throw parseException("Parse short integer failed. "+e.getMessage(),path,rl);
	} finally {
	    rl.close();	
	}
    }

    /**
     * 単語辞書から無駄な項目を除外する
     * 参照: http://d.hatena.ne.jp/sile/20100227/1267260585
     */
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
	    if(leftId != wi.leftId)  return leftId - wi.leftId;
	    if(rightId != wi.rightId) return rightId - wi.rightId;
	    return cost - wi.cost;
	}
    }

    private static class onlyCsv implements FileFilter {
	public boolean accept(File file) {
	    return file.isFile() && file.toString().matches(".*\\.csv$");
	}
    }
}