package net.reduls.igo;

/**
 * 形態素クラス
 */
public final class Morpheme {
    /**
     * 形態素の表層形
     */
    public final String surface;
    /**
     * 形態素の素性
     */
    public final String feature;
    /**
     * テキスト内での形態素の出現開始位置
     */
    public final int start;
    
    public Morpheme(String surface, String feature, int start) {
	this.surface = surface;
	this.feature = feature;
        this.start   = start;
    }
}
