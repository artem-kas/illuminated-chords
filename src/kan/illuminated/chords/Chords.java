package kan.illuminated.chords;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Chords {

	public static class ChordMark {
		public int	from;
		public int	length;
	}

	public Integer  chordId;
	public String	author;
	public String	title;
	public String	url;
	public String	type;
	public Integer  rating;
	public Integer  votes;

	public String	text;

	public List<ChordMark> chordMarks = new ArrayList<Chords.ChordMark>();

	public Date     lastRead;
	public Date     previousRead;

	private int longestLine = -1;

	public int getLongestLine() {
		if (text == null || text.isEmpty())
			return 0;

		// hopefully text will not change...
		if (longestLine != -1)
			return longestLine;

		String[] lines = text.split("\n");

		int max = 0;
		for (String l : lines) {
			if (l.length() > max)
				max = l.length();
		}

		longestLine = max;
		return max;
	}
}
