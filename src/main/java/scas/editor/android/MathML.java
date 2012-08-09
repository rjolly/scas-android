package scas.editor.android;

import java.io.Reader;
import java.io.Writer;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class MathML {

    static final Map<String, Transformer> cache = new HashMap<String, Transformer>();

    public static String get(String document) throws Exception {
        return transform(document, "/mmltxt.xsl");
    }

    static String transform(String document, String stylesheet) throws TransformerException {
        Reader r = new StringReader(document);
        Writer w = new StringWriter();
        transformer(stylesheet).transform(new StreamSource(r), new StreamResult(w));
        return w.toString();
    }

    static Transformer transformer(String stylesheet) throws TransformerException {
        if (!cache.containsKey(stylesheet)) cache.put(stylesheet, TransformerFactory.newInstance().newTransformer(new StreamSource(MathML.class.getResource(stylesheet).toString())));
        return cache.get(stylesheet);
    }
}
