package net.reduls.igo.trie;

/**
 * java.util.ArrayListの拡張。
 * 非負の範囲外アクセスがあった場合に自動的にリストの拡張が行われる。
 */
final class AutoArray<E> extends java.util.ArrayList<E> {
    /**
     * 基本的な動作はjava.util.ArrayList.getに準ずる。
     *
     * 非負の範囲外アクセスがあった場合は、自動的にリストが拡張され、デフォルト値が返される。
     * 拡張された領域にはデフォルト値が格納される。
     * 添字として負数が指定された場合の動作は未定義。
     *
     * @params index リストの添字
     * @params defaultValue リスト要素のデフォルト値
     * @return 添字に対応する要素
     */
    public E get(int index, E defaultValue) {
	try {
	    return get(index);
	} catch(IndexOutOfBoundsException e) {
	    for(int i=size(); i <= index*2; i++)
		add(defaultValue);
	    return get(index);
	}
    }
    
    /**
     * 基本的な動作はjava.util.ArrayList.setに準ずる。
     *
     * 非負の範囲外アクセスがあった場合は、十分なサイズにまで自動的にリストが拡張される。
     * 拡張された領域にはデフォルト値が格納される。
     * 添字として負数が指定された場合の動作は未定義。
     *
     * @params index リストの添字
     * @params element 添字の位置に格納する値
     * @params defaultValue リスト要素のデフォルト値
     * @return 添字の位置に以前格納されていた値
     */
    public E set(int index, E element, E defaultValue) {
	try {
	    return set(index,element);
	} catch(IndexOutOfBoundsException e) {
	    for(int i=size(); i <= index*2; i++)
		add(defaultValue);
	    return set(index,element);
	}
    }
}