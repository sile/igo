package net.reduls.igo.bin;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import net.reduls.igo.dictionary.build.Matrix;
import net.reduls.igo.dictionary.build.WordDic;
import net.reduls.igo.dictionary.build.CharCategory;

/**
 * テキスト辞書からバイナリ辞書を作成するコマンド
 */
public final class BuildDic {
    public static void main(String[] args) {
	if(args.length != 3 && args.length != 4) {
	    System.err.println("Usage: java net.reduls.igo.bin.BuildDic <output directory> <input directory> <encoding> [delimiter]");
	    System.exit(1);
	}
	final String outputDir = args[0];
	final String inputDir  = args[1];
	final String encoding  = args[2];
	final String delim = args.length==4 ? args[3] : ",";

	new File(outputDir).mkdirs();

	final WordDic wd = new WordDic(inputDir, encoding, outputDir, delim);
	final CharCategory cc = new CharCategory(inputDir, encoding, outputDir);
	try {
	    System.err.println("### Build word trie");
	    wd.buildWordIdMap();
	    
	    System.err.println("### Build word dictionary");
	    wd.buildWordInfo();
	    
	    System.err.println("### Build matrix");
	    Matrix.build(inputDir, outputDir);
	    
	    System.err.println("### Build char-category dictionary");
	    cc.build();
	    
	    System.err.println("DONE");
	} catch (ParseException e) {
	    System.err.println("[PARSE ERROR] "+e.getMessage());
	    System.exit(1);
	} catch (IOException e) {
	    System.err.println("[ERROR] "+e.getMessage());
	    System.exit(1);
	}
    }   
}