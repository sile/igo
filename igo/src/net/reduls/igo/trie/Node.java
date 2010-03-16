package net.reduls.igo.trie;

/**
 * DoubleArrayのノード用の定数などが定義されているクラス
 */
final class Node {
    /**
     * BASEノード用の定数およびメソッドが定義されているクラス
     */
    public static class Base {
	/**
	 * BASEノードの初期値
	 */
	public static final int INIT_VALUE = Integer.MIN_VALUE;
	/**
	 * BASEノードに格納するID値をエンコードするためのメソッド
	 * BASEノードに格納されているID値をデコードするためにも用いられる
	 */
	public static int ID(int id) { return id*-1-1; }
    }
    /**
     * CHECKノード用の定数が定義されているクラス
     */
    public static class Chck {
	/**
	 * 文字列の終端を表す文字定数
	 * 
	 * この文字はシステムにより予約されており、辞書内の形態素の表層形および解析対象テキストに含まれていた場合の動作は未定義
	 */ 
	public static final char TERMINATE_CODE=0;
	/**
	 * CHECKノードが未使用だということを示すための文字定数
	 *
	 * この文字はシステムにより予約されており、辞書内の形態素の表層形および解析対象テキストに含まれていた場合の動作は未定義
	 */
	public static final char VACANT_CODE=1;
	/**
	 * 使用可能な文字の最大値
	 */
	public static final char CODE_LIMIT=0xFFFF;
    }
}