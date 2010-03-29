package net.reduls.igo.dictionary;

import java.io.IOException;
import net.reduls.igo.util.FileMappedInputStream;

/**
 * 形態素の連接コスト表を扱うクラス
 */
public final class Matrix {
    private final int     leftSize;
    private final int     rightSize;
    private final short[] matrix;

    public Matrix(String dataDir) throws IOException {
	final FileMappedInputStream fmis = new FileMappedInputStream(dataDir+"/matrix.bin");
	try {
	    leftSize = fmis.getInt();
	    rightSize= fmis.getInt();
	    matrix   = fmis.getShortArray(leftSize*rightSize);
	} finally {
	    fmis.close();
	}
    }

    /**
     * 形態素同士の連接コストを求める
     */
    public short linkCost(int leftId, int rightId) {
	return matrix[rightId*rightSize + leftId];
    }
}