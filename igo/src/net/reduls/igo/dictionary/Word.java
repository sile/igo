package net.reduls.igo.dictionary;

import java.io.IOException;
import net.reduls.igo.util.FileMappedInputStream;

/**
 * 辞書内の単語を扱うクラス
 * クラス名は単数形だが、個々の単語というよりは、単語セット全体を扱っている
 */
final class Word {
    public final int     count;       // 単語数
    public final short[] costs;       // consts[単語ID] = 単語のコスト
    public final short[] leftIds;     // leftIds[単語ID] = 単語の左文脈ID
    public final short[] rightIds;    // rightIds[単語ID] = 単語の右文脈ID
    public final int[]   dataOffsets; // dataOffsets[単語ID] = 単語の素性データの開始位置
    
    public Word(String filepath) throws IOException {
	final FileMappedInputStream fmis = new FileMappedInputStream(filepath);
	count = fmis.size()/(2+2+2+4);
	
	try {
	    leftIds    = fmis.getShortArray(count);
	    rightIds   = fmis.getShortArray(count);
	    costs      = fmis.getShortArray(count);
	    dataOffsets= fmis.getIntArray(count);
	} finally {
	    fmis.close();
	}
    }
}