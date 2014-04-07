package kan.illuminated.chords;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UltimateGuitarSearchRequest {

	public static class UltimateGuitarSearchResult {
		public List<Chords>     chordsList = new ArrayList<Chords>();

		public List<Integer>    availablePages = new ArrayList<Integer>();
	}

	private static final String UG_QUERY_BASE = "http://www.ultimate-guitar.com/search.php?search_type=title";

	private static final Pattern title_pattern = Pattern.compile("(.*)\\s+\\(ver \\d+\\)");

	public UltimateGuitarSearchResult search(String query) {
		return search(query, 1);
	}

	public UltimateGuitarSearchResult search(String query, Integer page) {

		System.out.println("requesting ultimate guitar for " + query + "/" + page);

		HttpQuery http = new HttpQuery();
		InputStream is = http.query(prepareQuery(query, page));

		final UltimateGuitarSearchResult ugsr = new UltimateGuitarSearchResult();

		Parser parser = new Parser();

		parser.setContentHandler(new ContentHandler() {

			private int	tableTresults = 0;

			private int	row = 0;
			private int	col = 0;

			private boolean aSong = false;

			private boolean ratdig = false;

			private int paging = 0;

			private StringBuilder sb;

			private StringBuilder   pagingSb;

			private Chords chords;


			@Override
			public void startPrefixMapping(String prefix, String uri)
					throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {

				if (tableTresults > 0)
					tableTresults++;

				if (qName.equals("table")) {
					String cls = attrs.getValue("class");
					if (cls != null && cls.equals("tresults")) {
						tableTresults = 1;
					}
				}

				if (tableTresults > 0) {
					if (qName.equals("tr")) {

						row++;

						chords = new Chords();

						col = 0;
					}
				}

				// first row is a header
				if (row > 1) {

					if (qName.equals("td")) {

						String id = attrs.getValue("id");
						if (id != null && id.equals("npd77")) {

							// <td id="npd77"><a>THIS APP DOESN'T HAVE RIGHTS TO DISPLAY TABS</a></td>
						}
						else {

							col++;
							sb = new StringBuilder();
						}
					}

					if (qName.equals("a")) {
						String cls = attrs.getValue("class");
						if (cls != null && cls.equals("song")) {

							String href = attrs.getValue("href");
							chords.url = href;

							aSong = true;
							sb = new StringBuilder();
						}
					}

					if (qName.equals("span")) {
						String cls = attrs.getValue("class");
						if (cls != null) {
							if (cls.equals("r_1")) {
								chords.rating   = 1;
							}
							else if (cls.equals("r_2")) {
								chords.rating   = 2;
							}
							else if (cls.equals("r_3")) {
								chords.rating   = 3;
							}
							else if (cls.equals("r_4")) {
								chords.rating   = 4;
							}
							else if (cls.equals("r_5")) {
								chords.rating   = 5;
							}
						}
					}

					if (qName.equals("b")) {
						String cls = attrs.getValue("class");
						if (cls != null && cls.equals("ratdig")) {
							ratdig = true;

							sb = new StringBuilder();
						}
					}
				}

				if (paging > 0)
					paging++;

				if (qName.equals("div")) {
					String cls = attrs.getValue("class");
					if (cls != null && cls.equals("paging")) {
						paging = 1;
					}
				}

				if (paging > 0) {
					if (qName.equals("a")) {
						pagingSb = new StringBuilder();
					}
				}
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

				if (tableTresults > 0) {
					if (row > 1) {

						if (aSong) {
							if (qName.equals("a")) {
								chords.title	= spotRawTitle(StringUtils.trimHtml(sb.toString()));
								aSong = false;
							}
						}

						if (qName.equals("td")) {
							if (col == 1)
								chords.author	= StringUtils.trimHtml(sb.toString());

							if (col == 4)
								chords.type		= StringUtils.trimHtml(sb.toString());
						}

						if (ratdig && qName.equals("b")) {
							try {
								chords.votes = Integer.valueOf(sb.toString());
							} catch (NumberFormatException e) {
								// this chord have rating with no votes for some reason
							}

							ratdig = false;
						}

						if (qName.equals("tr")) {

							// same author is not repeated in the source html
							if ((chords.author == null || chords.author.isEmpty()) && !ugsr.chordsList.isEmpty()) {
								chords.author = ugsr.chordsList.get(ugsr.chordsList.size() - 1).author;
							}

							ugsr.chordsList.add(chords);
						}
					}
				}

				if (tableTresults > 0)
					tableTresults--;

				if (paging > 0) {
					if (qName.equals("a")) {

						try {
							String pStr = pagingSb.toString();
							Integer pInt = Integer.valueOf(pStr);
							ugsr.availablePages.add(pInt);
						} catch (NumberFormatException e) {
							// probably something like 'next'
						}
						pagingSb = null;
					}
				}

				if (paging > 0)
					paging--;
			}

			@Override
			public void endDocument() throws SAXException {
				// TODO Auto-generated method stub

			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {

				if (row > 1 && col > 0) {
					sb.append(new String(ch, start, length));
				}

				if (paging > 0) {
					if (pagingSb != null) {
						pagingSb.append(new String(ch, start, length));
					}
				}

//				if (tableTresults > 0)
//					System.out.println("[" + row + "-" + col + "] " + new String(ch, start, length));
			}
		});

		try {
			parser.parse(new InputSource(is));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

		return ugsr;
	}

	private String prepareQuery(String query, Integer page) {
		try {
			String q = UG_QUERY_BASE;

			if (query != null) {
				q += "&value=" + URLEncoder.encode(query, "UTF-8");
			}

			if (page != null) {
				q += "&page=" + page;
			}

			return q;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String spotRawTitle(String title) {

		Matcher m = title_pattern.matcher(title);

		if (m.matches()) {
			return m.group(1);
		}
		else {
			return title;
		}
	}

	public static void main(String[] args) {

		UltimateGuitarSearchResult ugsr = new UltimateGuitarSearchRequest().search("ordinary");
//		for (Chords ch : ugsr.chordsList) {
//			System.out.println("[" + ch.author + "] [" + ch.title + "] [" + ch.url + "]");
//		}

		for (Integer p : ugsr.availablePages) {
			System.out.println(p);
		}

//		System.out.println(new UltimateGuitarSearchRequest().prepareQuery("this//is `my?qu=er:y"));
	}
}
