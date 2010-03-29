package net.reduls.igo.dictionary;

import java.io.IOException;
import net.reduls.igo.util.FileMappedInputStream;

public final class CharCategory {
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

    public static class Category {
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
    }

    private Category[] readCategorys(String dataDir) throws IOException {
	final int[] data = FileMappedInputStream.getIntArray(dataDir+"/char.category");
	final int size = data.length/4;

	final Category[] ary = new Category[size];
	for(int i=0; i < size; i++)
	    ary[i] = new Category(data[i*4],data[i*4+1],data[i*4+2]==1,data[i*4+3]==1);
	return ary;
    }
}