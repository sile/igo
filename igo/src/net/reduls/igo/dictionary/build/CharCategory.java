package net.reduls.igo.dictionary.build;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.text.ParseException;
import net.reduls.igo.trie.Searcher;
import net.reduls.igo.util.ReadLine;
import net.reduls.igo.util.FileMappedOutputStream;

/**
 * 文字カテゴリ定義を保持したバイナリデータを作成するクラス
 */
public final class CharCategory {
    public static final String KEY_PREFIX="\002";

    private final String inputDir;
    private final String encoding;
    private final String outputDir;

    /**
     * コンストラクタ
     *
     * @param inputDir テキスト辞書が配置されているディレクトリのパス
     * @param encoding テキスト辞書の文字列エンコーディング
     * @param outputDir バイナリ辞書の保存先ディレクトリ
     */    
    public CharCategory(String inputDir, String encoding, String outputDir) {
	this.inputDir = inputDir;
	this.encoding = encoding;
	this.outputDir= outputDir;
    }

    /**
     * 文字カテゴリ定義のバイナリデータを作成する
     *
     * @throws ParseException テキスト辞書のパースに失敗した場合に送出される
     * @throws IOException 入出力エラーが発生した場合に送出される
     */
    public void build() throws ParseException, IOException {
	// 文字カテゴリの定義を取得する
	final Map<String,Category> ccmap = parseCharCategoryDef();

	// 文字カテゴリの定義を保存する
	List<Category> categorys = new ArrayList<Category>();
	for(Category e : ccmap.values())
	    categorys.add(e);
	saveCharCategoryMap(categorys);

	// 文字とカテゴリのマッピングを取得/保存する
	buildCodeCategoryMap(ccmap);
    }
    
    private Map<String,Category> parseCharCategoryDef() throws ParseException, IOException {
	final String path=inputDir+"/char.def";
	final ReadLine rl = new ReadLine(path,encoding);
	final Searcher srch = new Searcher(outputDir+"/word2id");
	final Map<String,Category> map = new HashMap<String,Category>();

	try {
	    for(String line=rl.read(); line != null; line=rl.read()) {
		if(line.isEmpty() || line.startsWith("#") || line.startsWith("0"))
		    continue;
		
		final String[] ss    = line.split("\\s+"); 
		if(ss.length < 4)
		    throw parseException("Invalid char category definition (too few fields).",path,rl);
		if(ss[1].matches("[01]")==false)
		    throw parseException("Invalid char category definition (INVOKE must be '0' or '1').",path,rl);
		if(ss[2].matches("[01]")==false)
		    throw parseException("Invalid char category definition (GROUP must be '0' or '1').",path,rl);
		
		final String  name   = ss[0];
		final boolean invoke = ss[1].equals("1");      // 0 or 1
		final boolean group  = ss[2].equals("1");      // 0 or 1
		final int length     = Integer.valueOf(ss[3]); // positive integer
		final int id         = srch.search(KEY_PREFIX+name);

		if(length < 0)
		    throw parseException("Invalid char category definition (LENGTH must be 0 or positive integer).",path,rl);
		if(id < 0)
		    throw parseException("Category '"+name+"' is unregistered in trie",path,rl);
		map.put(name, new Category(id,length,invoke,group));
	    }
	} catch (NumberFormatException e) {
	    throw parseException("Parse integer failed. "+e.getMessage(),path,rl);
	} finally {
	    rl.close();
	}

	// "DEFAULT"と"SPACE"は必須カテゴリ
	if(map.containsKey("DEFAULT")==false)
	    throw parseException("Missing mandatory category 'DEFAULT'.",path,rl);
	if(map.containsKey("SPACE")==false) 
	    throw parseException("Missing mandatory category 'SPACE'.",path,rl);
	
	return map;
    }

    private void saveCharCategoryMap(List<Category> categorys) throws IOException {
	final FileMappedOutputStream fmos = 
	    new FileMappedOutputStream(outputDir+"/char.category", categorys.size()*4*4);
	try {
	    java.util.Collections.sort(categorys);
	    for(Category e : categorys) {
		fmos.putInt(e.id);
		fmos.putInt(e.length);
		fmos.putInt(e.invoke ? 1 : 0);
		fmos.putInt(e.group ? 1 : 0);
	    }
	} finally {
	    fmos.close();
	}
    }

    private void buildCodeCategoryMap(Map<String,Category> map) throws ParseException, IOException {
	final CharId[] chars = new CharId[0x10000];
	{
	    final CharId dft = new CharId(map.get("DEFAULT").id);
	    for(int i=0; i < 0x10000; i++)
		chars[i] = dft;
	}

	final String path=inputDir+"/char.def";
	final ReadLine rl  = new ReadLine(path, encoding);
	try {
	    for(String line=rl.read(); line != null; line=rl.read()) {
		if(line.startsWith("0")==false)
		    continue;
		line = line.replaceFirst("#.*$","");

		final String[] ss = line.split("\\s+");
		int beg;
		int end;
		if(ss[0].indexOf("..") != -1) {
		    final String[] ss2 = ss[0].split("\\.\\.");
		    if(ss2.length != 2) 
			throw parseException("Invalid code to category mapping line.",path,rl);
		    beg = Integer.parseInt(ss2[0].substring(2),16);
		    end = Integer.parseInt(ss2[1].substring(2),16);
		} else {
		    beg = end = Integer.parseInt(ss[0].substring(2),16);
		}
		if(!(0 <= beg && beg <= 0xFFFF &&
		     0 <= end && end <= 0xFFFF && beg <= end))
		    throw parseException("Wrong UCS2 code specified.",path,rl);
		
		String categoryName="";
		try {
		    // 文字カテゴリ及び互換カテゴリの取得
		    final CharId ch = new CharId(map.get(categoryName=ss[1]).id);
		    for(int i=2; i < ss.length; i++)
			ch.add(map.get(categoryName=ss[i]).id);
		    
		    // カテゴリ登録
		    for(int i=beg; i <= end; i++)
			chars[i] = ch;
		} catch(NullPointerException e) {
		    throw parseException("Category '"+categoryName+"' is undefined.",path,rl);
		}
	    }

	    if(chars[' '].id != map.get("SPACE").id) 
		throw parseException("0x0020 is reserved for 'SPACE' category",path,rl);
	} catch(IndexOutOfBoundsException e) {
	    throw parseException("Invalid code to category mapping line.",path,rl);
	} catch (NumberFormatException e) {
	    throw parseException("Parse integer failed. "+e.getMessage(),path,rl);
	} finally {
	    rl.close();
	}
	
	final FileMappedOutputStream fmos 
	    = new FileMappedOutputStream(outputDir+"/code2category", chars.length*4*2);
	try {
	    for(CharId c : chars) fmos.putInt(c.id);
	    for(CharId c : chars) fmos.putInt(c.mask);
	} finally {
	    fmos.close();
	}
    }

    private static ParseException parseException(String msg, String path, ReadLine rl) {
	return new ParseException(msg+"\t{file: "+path+", line: "+rl.lineNumber()+"}", rl.lineNumber());
    }

    private static class Category extends net.reduls.igo.dictionary.CharCategory.Category implements Comparable<Category> {
	public Category(int i, int l, boolean iv, boolean g) {
	    super(i,l,iv,g);
	}
	
	public int compareTo(Category e) { return id - e.id; }
    }

    private static class CharId {
	public final int id;
	public       int mask;
	
	public CharId(int i) {
	    id = i;
	    add(id);
	}
	
	public void add(int i) {
	    mask |= 1<<i;
	}
    }
}