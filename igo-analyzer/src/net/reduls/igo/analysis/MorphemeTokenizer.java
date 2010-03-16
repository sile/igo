package net.reduls.igo.analysis;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import net.reduls.igo.Tagger;
import net.reduls.igo.Morpheme;

public class MorphemeTokenizer extends Tokenizer {
    private final Tagger tagger;
    private Iterator<Morpheme> curToken = new ArrayList<Morpheme>().iterator();
    private int offset = 0;
    
    BufferedReader br;
    
    private TermAttribute termAtt;
    private OffsetAttribute offsetAtt;
    private TypeAttribute typeAtt;

    public MorphemeTokenizer(Tagger tagger, Reader in) {
	super(in);
	this.tagger = tagger;
	br = new BufferedReader(in);

	termAtt = addAttribute(TermAttribute.class);
	offsetAtt = addAttribute(OffsetAttribute.class);
	typeAtt = addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
	clearAttributes();

	final Morpheme m = readMorpheme();
	if(m==null)
	    return false;

	offset = m.start+m.surface.length();

	termAtt.setTermBuffer(m.surface);
	offsetAtt.setOffset(correctOffset(m.start), correctOffset(offset));
	typeAtt.setType(m.feature);
	
	return true;
    }

    @Override
    public final void end() {
	final int finalOffset = correctOffset(offset);
	offsetAtt.setOffset(finalOffset, finalOffset);
    }

    @Override
    public void reset() throws IOException {
	super.reset();
	offset = 0;
	curToken = new ArrayList<Morpheme>().iterator();
    }

    @Override
    public void reset(Reader reader) throws IOException {
	super.reset(reader);
	reset();
	br = new BufferedReader(reader);
    }

    private Morpheme readMorpheme() throws IOException {
	if(curToken.hasNext()==false) {
	    final String line = br.readLine();
	    if(line==null)
		return null;
	    
	    curToken = tagger.parse(line).iterator();
	    return readMorpheme();
	}
	return curToken.next();
    }
}

