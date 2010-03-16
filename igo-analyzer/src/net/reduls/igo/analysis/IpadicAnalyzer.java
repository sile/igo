package net.reduls.igo.analysis;

import java.io.Reader;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import net.reduls.igo.Tagger;

public class IpadicAnalyzer extends Analyzer {
    private final Tagger tagger;

    public IpadicAnalyzer(Tagger tagger) {
	this.tagger = tagger;
    }

    @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
	return new IpadicTokenizer(tagger, reader);
    }

    @Override
    public final TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
	IpadicTokenizer prev = (IpadicTokenizer)getPreviousTokenStream();
	if(prev == null) {
	    prev = (IpadicTokenizer)tokenStream(fieldName, reader);
	    setPreviousTokenStream(prev);
	} else {
	    prev.reset(reader);
	}
	return prev;
    }
}