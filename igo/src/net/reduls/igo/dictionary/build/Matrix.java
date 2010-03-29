package net.reduls.igo.dictionary.build;

import java.io.IOException;
import java.io.EOFException;
import java.text.ParseException;
import net.reduls.igo.util.ReadLine;
import net.reduls.igo.util.FileMappedOutputStream;

/**
 * 形態素の連接コスト表のバイナリデータを作成するためのクラス
 */
public final class Matrix {
    /**
     * 連接コスト表のバイナリデータを作成する
     *
     * @param inputDir ソース辞書があるディレクトリ。{@code inputDir+"/matrix.def"}ファイルが使用される
     * @param outputDir バイナリデータが保存されるディレクトリ。{@code outputDir+"/matrix.bin"}ファイルが作成される
     * @throws ParseException 入力ファイルのパースに失敗した場合に送出される
     * @throws IOException 入出力エラーが発生した場合に送出される
     */
    public static void build(String inputDir, String outputDir) throws ParseException, IOException {
	final ReadLine rl = new ReadLine(inputDir+"/matrix.def", "UTF-8"); 
	try {
	    // 一行目はサイズ: [左文脈IDの数] [右文脈IDの数]
	    String s = rl.readEof();
	    final int leftNum = Integer.valueOf(s.substring(0,s.indexOf(' ')));
	    final int rightNum= Integer.valueOf(s.substring(s.indexOf(' ')+1));
	    final FileMappedOutputStream fmos =
		new FileMappedOutputStream(outputDir+"/matrix.bin", 4*2+leftNum*rightNum*2);
	    try {
		fmos.putInt(leftNum);
		fmos.putInt(rightNum);
		
		// 二行目以降はデータ: [左文脈ID] [右文脈ID] [連接コスト]
		final short[] tmpMatrix = new short[leftNum*rightNum];
		for(int i=0; i < leftNum; i++)
		    for(int j=0; j < rightNum; j++) {
			s = rl.readEof();
			final int p1 = s.indexOf(' ');
			final int p2 = s.indexOf(' ',p1+1);
			
			final int   lftID = Integer.valueOf(s.substring(0,    p1));
			final int   rgtID = Integer.valueOf(s.substring(p1+1, p2));
			final short cost  =   Short.valueOf(s.substring(p2+1));
			
			if(i != lftID) throw new ParseException
					   ("Unexpected left context ID. ID="+lftID+", expedted="+i+"\t"+
					    "{file: matrix.def, line: "+rl.lineNumber()+"}", rl.lineNumber());
			if(j != rgtID) throw new ParseException
					   ("Unexpected right context ID. ID="+rgtID+", expedted="+j+"\t"+
					    "{file: matrix.def, line: "+rl.lineNumber()+"}", rl.lineNumber());
			
			// NOTE: tmpMatrixという一時配列を用いている理由
			// 
			// この段階で、fmos.putShort(cost)、などとしてファイルに書き出した場合、
			// matrix[leftId][rightId]=cost、といった配列ができる。
			//
			// それでも特に問題はないのだが、今回のケースでは、
			// 「rightIdが固定で、leftIdのみが変動する」といったようなメモリアクセスパターンが多い。
			//
			// そのためtmpMatrix配列を用いて、コスト値の並び順を変更し、
			// matrix[rightId][leftId]とったように、rightIdが第一添字になるようにした方が
			// メモリアクセスの局所性が高まり(多分)、若干だが処理速度が向上する。
			tmpMatrix[j*rightNum + i] = cost;  
		    }
		for(short cost : tmpMatrix)
		    fmos.putShort(cost);
	    } finally {
		fmos.close();
	    }
	} catch (NumberFormatException e) {
	    throw new ParseException("Parse short integer failed. "+e.getMessage()+"\t"+
				     "{file: matrix.def, line: "+rl.lineNumber()+"}", rl.lineNumber());
	} catch (EOFException e) {
	    throw new ParseException("End of file reached\t"+
				     "{file: matrix.def, line: "+rl.lineNumber()+"}", rl.lineNumber());
	} finally {
	    rl.close();
	}
    }
}