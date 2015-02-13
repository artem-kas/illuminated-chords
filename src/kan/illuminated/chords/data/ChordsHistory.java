package kan.illuminated.chords.data;

import android.content.ContentValues;
import android.database.Cursor;
import kan.illuminated.chords.Chords;
import kan.illuminated.chords.Chords.ChordMark;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static kan.illuminated.chords.StringUtils.*;
import static kan.illuminated.chords.data.ChordsHistory.Field.*;

/**
 * @author KAN
 */
public class ChordsHistory {

	private static final int    MAX_CHORDS  = 128;

	public enum Field {
		AUTHOR,
		TITLE
	}

	private static final String CHORDS_FIELDS =
			"cid, author, title, url, type, rating, votes, text, marks, last_read, history, favourite";

	private static final Object lock = new Object();

	public void storeChords(Chords chords) {
		synchronized (lock) {
			Chords c = readChords(chords.url);
			if (c == null) {
				insertChords(chords);

				shrinkChords();
			}
		}
	}

	public void refreshChords(Chords chords) {

		updateChords(chords);
	}

	public void insertChords(Chords chords) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		if (chords.lastRead == null) {
			chords.lastRead = new Date();
		}

		ContentValues cv = chordsPublicContentValues(chords);
		cv.put("last_read", chords.lastRead.getTime());
		cv.put("history",   chords.history ? 1 : 0);

		long id = db.insert("chords", null, cv);

		chords.chordId = (int)id;
	}

	public void updateChords(Chords chords) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		db.update("chords", chordsPublicContentValues(chords), "url = ?", new String[]{chords.url});
	}

	public void updateLastRead(Chords chords) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		chords.previousRead = chords.lastRead;
		chords.lastRead = new Date();
		chords.history = true;

		ContentValues cv = new ContentValues();

		cv.put("last_read", chords.lastRead.getTime());
		cv.put("history",   chords.history ? 1 : 0);

		db.update("chords", cv, "cid = ?", new String[]{chords.chordId.toString()});

	}

	public Chords readChords(String url) {

		DatabaseWrapper db = ChordsDb.chordsDb().getReadableDatabase();

		Cursor c = db.rawQuery(
				"select " + CHORDS_FIELDS + " " +
				"from chords where url = ?", new String[]{url});

		if (c.moveToNext()) {
			Chords chords = fromCursor(c);

			updateLastRead(chords);

			return chords;
		} else {
			return null;
		}
	}

	public List<Chords> readFullHistory() {

		DatabaseWrapper db = ChordsDb.chordsDb().getReadableDatabase();

		Cursor c = db.rawQuery(
				"select " + CHORDS_FIELDS + " " +
				"from chords " +
				"where history = 1 " +
				"order by last_read desc", null);

		List<Chords> chords = new ArrayList<Chords>();
		while (c.moveToNext()) {
			chords.add(fromCursor(c));
		}

		return chords;
	}

	public void deleteFromHistory(Integer chordId) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put("history",   0);

		db.update("chords", cv, "cid = ?", new String[]{chordId.toString()});
	}

	public void clearHistory() {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put("history",   0);

		db.update("chords", cv, null, null);
	}

	public void markFavourite(Chords chords) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		chords.favourite = true;

		ContentValues cv = new ContentValues();
		cv.put("favourite", 1);

		int r = db.update("chords", cv, "cid = ?", new String[]{chords.chordId.toString()});
		if (r != 1) {
			throw new RuntimeException("expected 1 row update for marking it favourite, instead got " + r + " rows updated");
		}
	}

	public void unmarkFavourite(Chords chords) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		chords.favourite = false;

		ContentValues cv = new ContentValues();
		cv.put("favourite", 0);

		int r = db.update("chords", cv, "cid = ?", new String[]{chords.chordId.toString()});
		if (r != 1) {
			throw new RuntimeException("expected 1 row update for unmarking it favourite, instead got " + r + " rows updated");
		}
	}

	public boolean isFavourite(Chords chords) {

		DatabaseWrapper db = ChordsDb.chordsDb().getReadableDatabase();

		Cursor c = db.rawQuery(
				"select " + CHORDS_FIELDS + " " +
						"from chords where url = ?", new String[]{chords.url});

		if (c.moveToNext()) {
			Chords res = fromCursor(c);
			return res.favourite;
		}

		return false;
	}

	public List<Chords> readFavourites(String filter, Field sort) {

		DatabaseWrapper db = ChordsDb.chordsDb().getReadableDatabase();

		String sql = "select " + CHORDS_FIELDS + " " +
				"from chords " +
				"where favourite = 1 ";

		String[] params = null;
		if (isNotEmpty(filter)) {
			sql += " and (" + AUTHOR + " like ? or " + TITLE + " like ?) ";
			String ft = filter.trim() + "%";
			params = new String[]{ft, ft};
		}

		switch (sort) {
			case AUTHOR:
				sql += "order by " + AUTHOR + ", " + TITLE;
				break;
			case TITLE:
				sql += "order by " + TITLE + ", " + AUTHOR;
				break;
			default:
				sql += "order by " + sort;
		}

		Cursor c = db.rawQuery(
				sql, params);

		List<Chords> chords = new ArrayList<Chords>();
		while (c.moveToNext()) {
			chords.add(fromCursor(c));
		}

		return chords;
	}


	public void shrinkChords() {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		db.execSQL("delete from chords where cid in (" +
				"select cid from chords order by last_read desc " +
				"limit -1 offset ?)",
				new Object[]{MAX_CHORDS});
	}

	private ContentValues chordsPublicContentValues(Chords chords) {

		ContentValues cv = new ContentValues();

		if (chords.chordId != null) {
			cv.put("cid", chords.chordId);
		}

		cv.put("author",    chords.author);
		cv.put("title",     chords.title);
		cv.put("url",       chords.url);
		cv.put("type",      chords.type);
		cv.put("rating",    chords.rating);
		cv.put("votes",     chords.votes);
		cv.put("text",      chords.text);

		cv.put("marks",     marksToString(chords.chordMarks));

		return cv;
	}

	private Chords fromCursor(Cursor c) {

		Chords chords = new Chords();

		chords.chordId      = c.getInt(0);
		chords.author       = c.getString(1);
		chords.title        = c.getString(2);
		chords.url          = c.getString(3);
		chords.type         = c.getString(4);
		chords.rating       = c.getInt(5);
		chords.votes        = c.getInt(6);
		chords.text         = c.getString(7);
		chords.chordMarks   = stringToMarks(c.getString(8));

		chords.lastRead     = new Date(c.getLong(9));
		chords.history      = c.getInt(10) == 1;
		chords.favourite    = c.getInt(11) == 1;

		return chords;
	}

	private String marksToString(List<ChordMark> marks) {

		try {

			JSONArray ja = new JSONArray();
			for (ChordMark m : marks) {
				JSONObject jo = new JSONObject();
				jo.put("f", m.from);
				jo.put("l", m.length);

				ja.put(jo);
			}

			String s = ja.toString();
			System.out.println("chords marks are " + s);

			return s;

		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<ChordMark> stringToMarks(String s) {

		try {

			List<ChordMark> chordMarks = new ArrayList<ChordMark>();

			JSONArray ja = new JSONArray(s);
			for (int i = 0; i < ja.length(); i++) {
				JSONObject jo = ja.getJSONObject(i);
				ChordMark cm = new ChordMark();
				cm.from     = jo.getInt("f");
				cm.length   = jo.getInt("l");

				chordMarks.add(cm);
			}

			return chordMarks;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		try {
			JSONArray ja = new JSONArray();

			JSONObject jo = new JSONObject();
			jo.put("p", 7);
			jo.put("l", 5);

			ja.put(jo);

			System.out.println(ja.toString());

		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
}
