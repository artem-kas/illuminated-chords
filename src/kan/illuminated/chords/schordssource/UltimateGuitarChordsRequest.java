package kan.illuminated.chords.schordssource;

import kan.illuminated.chords.Chords;
import kan.illuminated.chords.Chords.ChordMark;
import kan.illuminated.chords.HttpQuery;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author KAN
 */
public class UltimateGuitarChordsRequest {

	public Chords getChords(String url) {

		HttpQuery http = new HttpQuery();
		InputStream qis = http.query(url);

		final Chords chords = new Chords();
		chords.url = url;

		Parser parser = new Parser();

		final StringBuilder sb = new StringBuilder();

		parser.setContentHandler(new ContentHandler() {

			private int		title	= -1;
			private	boolean	titleH	= false;
			private	boolean	titleA	= false;
			private StringBuilder	titleSb		= new StringBuilder();
			private StringBuilder	authorSb	= new StringBuilder();

			private int	divCont	= -1;

			private int	listen	= -1;

			private boolean skip	= false;
			private int		skipNest = 0;

			private ChordMark cm;


			@Override
			public void startPrefixMapping(String prefix, String uri)
					throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void startElement(String uri, String localName, String qName,
			                         Attributes atts) throws SAXException {

				if (title >= 0)
					title++;

				if (qName.equals("div")) {
					String clas = atts.getValue("class");
					if (clas != null && clas.contains("t_title")) {
						title = 0;
						System.out.println("have title");
					}
				}

				if (title >= 0) {
					if (qName.equals("h1")) {
						titleH = true;
					}

					if (qName.equals("a")) {
						titleA = true;
					}
				}

				if (qName.equals("div")) {
					String id = atts.getValue("id");
					if (id != null && id.equals("cont")) {
						divCont = 0;
					}
				}

				if (divCont < 0)
					return;

				divCont++;


				if (qName.equals("div")) {
					String clas = atts.getValue("class");
					if (clas != null && clas.equals("dn")) {
						if (!skip)
							skipNest = 0;

						skip = true;
					}
				}

				if (qName.equals("pre")) {
					String clas = atts.getValue("class");
					if (clas != null && clas.equals("print-visible")) {
						if (!skip)
							skipNest = 0;

						skip = true;
					}
				}

				if (skip)
					skipNest++;

				if (!skip && qName.equals("pre")) {
					listen = 0;
				}

				if (listen >= 0)
					listen++;

				if (listen > 0 && qName.equals("span")) {
					cm = new ChordMark();
					cm.from	= sb.length();
				}


				int l = atts.getLength();
				String sa = "";
				for (int i = 0; i < l; i++) {
					sa += atts.getLocalName(i) + " " + atts.getValue(i) + "[" + atts.getType(i) + "]; ";
				}

//				System.out.println(">>> " + qName + " - " + sa + " (" + listen + ")");

			}

			@Override
			public void startDocument() throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void skippedEntity(String name) throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void setDocumentLocator(Locator locator) {
				// TODO Auto-generated method stub

			}

			@Override
			public void processingInstruction(String target, String data)
					throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void ignorableWhitespace(char[] ch, int start, int length)
					throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void endPrefixMapping(String prefix) throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {

				if (title >= 0) {
					if (qName.equals("h1")) {
						chords.title = titleSb.toString().trim();
						titleH = false;
					}

					if (qName.equals("a")) {
						chords.author = authorSb.toString().trim();
						titleA = false;
					}
				}

				if (title >= 0) {
					title--;
				}

				if (listen > 0 && qName.equals("span")) {
					cm.length	= sb.length() - cm.from;
					chords.chordMarks.add(cm);
				}

				if (divCont > 0)
					divCont--;
				if (divCont == 0)
					divCont = -1;

				if (skip) {
					skipNest--;

					if (skipNest <= 0)
						skip = false;
				}

//				if (listen > 0)
//					System.out.println("<<<" + qName + " (" + listen + ")");

				if (listen > 0)
					listen--;
				if (listen == 0)
					listen = -1;
			}

			@Override
			public void endDocument() throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {

				if (titleH) {
					titleSb.append(new String(ch, start, length));
				}

				if (titleA) {
					authorSb.append(new String(ch, start, length));
				}

				if (listen > 0) {
					String chars = new String(ch, start, length);
//					System.out.println("[" + chars + "]");

					sb.append(chars);
				}

			}
		});

		try {
			parser.parse(new InputSource(qis));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

		chords.text	= sb.toString();

		return chords;
	}

}
