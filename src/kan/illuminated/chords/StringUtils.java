package kan.illuminated.chords;

public class StringUtils {

	public static final char NO_BREAK_SPACE = 0x00A0;

	private static final String EMPTY_SRTING	= "";

	public static String trimLeft(String s) {

		if (s == null || s.isEmpty())
			return s;

		if (!Character.isWhitespace(s.charAt(0)))
			return s;

		int n = 0;
		while (Character.isWhitespace(s.charAt(n)) && n < s.length()) {
			n++;
		}

		if (n == s.length())
			return new String();

		return s.substring(n);
	}

	public static String trimHtml(String s) {

		if (s == null || s.isEmpty())
			return s;

		int n = 0, e = s.length() - 1;
		for ( ; n < s.length(); n++) {
			char c = s.charAt(n);
			if (!Character.isWhitespace(c) && c != NO_BREAK_SPACE)
				break;
		}

		if (n == s.length())
			return EMPTY_SRTING;

		while (e > 0 && e > n) {
			char c = s.charAt(e);
			if (!Character.isWhitespace(c) && c != NO_BREAK_SPACE)
				break;

			e--;
		}

		return s.substring(n, e + 1);
	}

	public static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	public static boolean isNotEmpty(String s) {
		return !isEmpty(s);
	}
}
