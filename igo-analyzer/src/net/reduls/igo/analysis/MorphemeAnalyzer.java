package net.reduls.igo.analysis;

import java.io.Reader;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import net.reduls.igo.Tagger;

public class MorphemeAnalyzer extends Analyzer {
    private final Tagger tagger;

    public MorphemeAnalyzer(Tagger tagger) {
	this.tagger = tagger;
    }

    @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
	return new MorphemeTokenizer(tagger, reader);
    }

    @Override
    public final TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
	MorphemeTokenizer prev = (MorphemeTokenizer)getPreviousTokenStream();
	if(prev == null) {
	    prev = (MorphemeTokenizer)tokenStream(fieldName, reader);
	    setPreviousTokenStream(prev);
	} else {
	    prev.reset(reader);
	}
	return prev;
    }
}