package org.dita.dost.reader;

import static junit.framework.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.dita.dost.util.Constants.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.XMLUnit;
import org.dita.dost.util.Job;
import org.dita.dost.util.XMLUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.dita.dost.TestUtils;
import org.xml.sax.XMLReader;

public class MapLinksReaderTest {
    
    private static final File resourceDir = TestUtils.getResourceDir(MapLinksReaderTest.class);
    private static final File srcDir = new File(resourceDir, "src");
    private static MapLinksReader reader;

    @BeforeClass
    public static void setUp() throws Exception {
        final Job job = new Job(srcDir);
        job.setProperty(INPUT_DITAMAP, "foo.ditamap");

        reader = new MapLinksReader();
        reader.setLogger(new TestUtils.TestLogger());
        reader.setJob(job);

        final XMLReader parser = XMLUtils.getXMLReader();
        parser.setContentHandler(reader);
        parser.parse(new InputSource(new File(srcDir, "maplinks.unordered").getAbsoluteFile().toURI().toString()));
    }

    @Test
    public void testRead() throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        final Map<File, Document> expMap = readExpected(db);

        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreComments(true);

        final Map<File, Map<String, Element>> mapSet = reader.getMapping();
        
        for (final Map.Entry<File, Map<String, Element>> e: mapSet.entrySet()) {
            for (final Map.Entry<String, Element> ee: e.getValue().entrySet()) {
                assertEquals(SHARP, ee.getKey());
                final Document doc = XMLUtils.getDocumentBuilder().newDocument();
                final Node n = doc.importNode(ee.getValue(), true);
                doc.appendChild(n);
                assertXMLEqual(expMap.get(e.getKey()),
                               doc);
            }
        }
    }

    private Map<File, Document> readExpected(final DocumentBuilder db) throws SAXException, IOException {
        final Document expDoc = db.parse(new File(srcDir, "maplinks.unordered").toURI().toString());
        final Map<File, Document> expMap = new HashMap<File, Document>();
        final NodeList maplinks = expDoc.getElementsByTagName(ELEMENT_NAME_MAPLINKS);
        for (int i = 0; i < maplinks.getLength(); i++) {
            final Element m = (Element) maplinks.item(i);
            final Document d = XMLUtils.getDocumentBuilder().newDocument();
            final Element root = d.createElement("stub");
            d.appendChild(root);
            final NodeList cs = m.getChildNodes();
            for (int j = 0; j < cs.getLength(); j++) {
                final Node c = cs.item(j);
                if (c.getNodeType() == Node.ELEMENT_NODE) {
                    root.appendChild(d.importNode(c, true));
                }
            }
            expMap.put(new File(srcDir, m.getAttribute(ATTRIBUTE_NAME_HREF)), d);
        }
        return expMap;
    }

}
