package kan.illuminated.chords;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class UltimateGuitarTest {

	public static void goHttp() {

		try {
			URL url = new URL("http://tabs.ultimate-guitar.com/d/depeche_mode/goodnight_lovers_crd.htm");

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();

			byte resp[] = new byte[1024 * 1024];

			int read = 0;
			int r = 0;
			while ((r = is.read(resp, read, resp.length - read)) != -1) {
				read += r;
			}

			System.out.println(read);

			System.out.println(new String(resp, 0, read));

			InputStream pis = new ByteArrayInputStream(resp, 0, read);

			Parser parser = new Parser();

			parser.setContentHandler(new ContentHandler() {

				private boolean listen = false;

				private int nest = 0;

				@Override
				public void startPrefixMapping(String prefix, String uri)
						throws SAXException {
					// TODO Auto-generated method stub

				}

				@Override
				public void startElement(String uri, String localName, String qName,
						Attributes atts) throws SAXException {

					if (qName.equals("div")) {
						String id = atts.getValue("id");
						if (id != null && id.equals("cont")) {
							listen = true;
						}
					}

					if (!listen)
						return;

					nest++;

					int l = atts.getLength();
					String sa = "";
					for (int i = 0; i < l; i++) {
						sa += atts.getLocalName(i) + " " + atts.getValue(i) + "[" + atts.getType(i) + "]; ";
					}

					System.out.println(">>> " + qName + " " + sa + " (" + nest + ")");

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

					if (listen)
						nest--;

					if (nest <= 0)
						listen = false;

					if (listen)
						System.out.println("<<<" + qName + " (" + nest + ")");
				}

				@Override
				public void endDocument() throws SAXException {
					// TODO Auto-generated method stub

				}

				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {

					if (listen) {
						System.out.println("[" + new String(ch, start, length) + "]");
					}

				}
			});

			parser.parse(new InputSource(pis));

//			parser.

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		goHttp();
	}
}
