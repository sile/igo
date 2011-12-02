package net.reduls.igo.bin;

import java.io.IOException;
import java.util.ArrayList;
import net.reduls.igo.Tagger;
import net.reduls.igo.Morpheme;
import net.reduls.igo.util.ReadLine;

/**
 * 簡易ベンチマークコマンド
 */
public final class IgoBench {
    public static void main(String[] args) throws IOException {
	if(args.length!=1) { 
	    System.err.println("Usage: java net.reduls.igo.bin.IgoBench <dictionary directory>");
	    System.exit(1);
	}
	final String  dicDir   = args[0];
	final Tagger  tagger   = new Tagger(dicDir);
	final ReadLine rl = new ReadLine(System.in);

        ArrayList<String> result = new ArrayList<String>();
        int morphemeCount = 0;
        long beg_t = System.currentTimeMillis();
        for(String s=rl.read(); s != null; s=rl.read()) {
            result.clear();
            morphemeCount += tagger.wakati(s, result).size();
        }
        
        System.out.println("elapsed: "+(System.currentTimeMillis()-beg_t)+" ms");
        System.out.println("morpheme count: "+morphemeCount);
    }   
}
