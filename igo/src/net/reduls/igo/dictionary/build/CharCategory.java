package net.reduls.igo.dictionary.build;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import net.reduls.igo.trie.Searcher;
import net.reduls.igo.util.ReadLine;
import net.reduls.igo.util.FileMappedInputStream;
import net.reduls.igo.util.FileMappedOutputStream;

public final class CharCategory {
    public static final String KEY_PREFIX="\002";

    private final Category[] categorys;
    private final int[]      char2id;
    private final int[]      eqlMasks;

    public CharCategory (String dataDir) throws IOException {
	categorys = readCategorys(dataDir);

	final FileMappedInputStream fmis = new FileMappedInputStream(dataDir+"/code2category");
	try {
	    char2id = fmis.getIntArray(fmis.size()/4/2);
	    eqlMasks= fmis.getIntArray(fmis.size()/4/2);
	} finally {
	    fmis.close();
	}
    }
    
    public Category category(char code) {
	return categorys[char2id[code]]; 
    }
    
    public boolean isCompatible(char code1, char code2) {
	return (eqlMasks[code1] & eqlMasks[code2]) != 0;
    }

    public static void build(String inputDir, String outputDir, String encoding) throws IOException {
	// 文字カテゴリの定義を取得する
	final Map<String,Category> ccmap = parseCharCategoryDef(inputDir, outputDir, encoding);

	// 文字カテゴリの定義を保存する
	List<Category> categorys = new ArrayList<Category>();
	for(Category e : ccmap.values())
	    categorys.add(e);
	genCharCategoryMap(categorys, outputDir);

	// 文字とカテゴリのマッピングを取得/保存する
	genCodeCategoryMap(ccmap, inputDir, outputDir, encoding);
    }
    
    private static void genCharCategoryMap(List<Category> categorys, String outputDir) throws IOException {
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

    private static Map<String,Category> parseCharCategoryDef(String inputDir,String outputDir,String encoding) 
	throws IOException {
	final ReadLine rl = new ReadLine(inputDir+"/char.def",encoding);
	final Searcher srch = new Searcher(outputDir+"/word2id");
	final Map<String,Category> map = new HashMap<String,Category>();
	String line;

	try {
	    while((line=rl.read()) != null) {
		if(line.isEmpty() || line.startsWith("#") || line.startsWith("0"))
		    continue;
		
		// TODO: 不正な入力をチェックする
		final String[] ss    = line.split("\\s+"); 
		final String  name   = ss[0];
		final boolean invoke = ss[1].equals("1");    // 0 or 1
		final boolean group  = ss[2].equals("1");    // 0 or 1
		final int length     = Short.valueOf(ss[3]); // positive integer
		final int id         = srch.search(KEY_PREFIX+name);
		if(id < 0)
		    throw new IOException("Category '"+name+"' is unregistered in trie"); // TODO: parse exception
		map.put(name, new Category(id,length,invoke,group));
	    }
	} finally {	    
	    rl.close();
	}
	// TODO: 'DEFAULT' and 'SPACE'(?) カテゴリがあるかどうかをチェックする
	return map;
    }

    public static class Category  implements Comparable<Category> {
	public final int     id;
	public final int     length;
	public final boolean invoke;
	public final boolean group;
	
	public Category(int i, int l, boolean iv, boolean g) {
	    id     = i;
	    length = l;
	    invoke = iv;
	    group  = g;
	}
	
	public int compareTo(Category e) { return id - e.id; }
    }

    private Category[] readCategorys(String dataDir) throws IOException {
	final int[] data = FileMappedInputStream.getIntArray(dataDir+"/char.category");
	final int size = data.length/4;

	final Category[] ary = new Category[size];
	for(int i=0; i < size; i++)
	    ary[i] = new Category(data[i*4],data[i*4+1],data[i*4+2]==1,data[i*4+3]==1);
	return ary;
    }
    
    private static class CharId {
	public final int id;
	public       int mask;
	
	public CharId(int i) {
	    id = i;
	    add(id);
	}
	public CharId(int i, int m) {
	    id=i;
	    mask=m;
	}
	public void add(int i) {
	    mask |= 1<<i;
	}
    }

    private static void genCodeCategoryMap
	(Map<String,Category> map, String inputDir, String outputDir, String encoding) throws IOException {
	final CharId[] chars = new CharId[0x10000];
	{
	    final CharId dft = new CharId(map.get("DEFAULT").id);
	    for(int i=0; i < 0x10000; i++)
		chars[i] = dft;
	}

	final ReadLine rl  = new ReadLine(inputDir+"/char.def", encoding);
	try {
	    String line;
	    while((line=rl.read()) != null) {
		if(line.startsWith("0")==false)
		    continue;
		
		// TODO: 不正な入力をチェックする
		final String[] ss = line.split("\\s+");
		int beg;
		int end;
		if(ss[0].indexOf("..") != -1) {
		    final String[] ss2 = ss[0].split("\\.\\.");
		    beg = Integer.parseInt(ss2[0].substring(2),16);
		    end = Integer.parseInt(ss2[1].substring(2),16);
		} else {
		    beg = end = Integer.parseInt(ss[0].substring(2),16);
		}
		
		CharId ch = new CharId(map.get(ss[1]).id);
		for(int i=2; i < ss.length; i++) {
		    if(ss[i].startsWith("#"))
			break;
		    ch.add(map.get(ss[i]).id);
		}
		
		for(int i=beg; i <= end; i++)
		    chars[i] = ch;
	    }
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
}