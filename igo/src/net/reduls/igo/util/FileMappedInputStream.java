package net.reduls.igo.util;

import java.io.IOException;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * ファイルにマッピングされた入力ストリーム<br />
 * net.reduls.igo以下のパッケージではファイルからバイナリデータを取得する場合、必ずこのクラスが使用される
 */
public final class FileMappedInputStream {
    private final FileChannel cnl;
    private int cur=0;

    /**
     * 入力ストリームを作成する
     * 
     * @param filepath マッピングするファイルのパス
     */
    public FileMappedInputStream(String filepath) throws IOException {
	cnl = new FileInputStream(filepath).getChannel();
    }

    public int getInt() throws IOException {
	return map(4).getInt();
    }
    
    public int[] getIntArray(int elementCount) throws IOException {
	final int[] ary = new int[elementCount];
	map(elementCount*4).asIntBuffer().get(ary);
	return ary;
    }
    
    public static int[] getIntArray(String filepath) throws IOException {
	final FileMappedInputStream fmis = new FileMappedInputStream(filepath);
	try {
	    return fmis.getIntArray(fmis.size()/4);
	} finally {
	    fmis.close();
	}
    }

    public short[] getShortArray(int elementCount) throws IOException {
	final short[] ary = new short[elementCount];
	map(elementCount*2).asShortBuffer().get(ary);
	return ary;
    }

    public char[] getCharArray(int elementCount) throws IOException {
	final char[] ary = new char[elementCount];
	map(elementCount*2).asCharBuffer().get(ary);
	return ary;
    }

    public String getString(int elementCount) throws IOException {
	return map(elementCount*2).asCharBuffer().toString();
    }

    public static String getString(String filepath) throws IOException {
	final FileMappedInputStream fmis = new FileMappedInputStream(filepath);
	try {
	    return fmis.getString(fmis.size()/2);
	} finally {
	    fmis.close();
	}
    }

    public int size() throws IOException {
	return (int)cnl.size();
    }

    public void close() {
	try {
	    cnl.close();
	} catch (IOException e) {}
    }

    private ByteBuffer map(int size) throws IOException {
	cur += size;
	return cnl.map(FileChannel.MapMode.READ_ONLY, cur-size, size).order(ByteOrder.nativeOrder());
    }
}