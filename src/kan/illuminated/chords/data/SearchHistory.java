package kan.illuminated.chords.data;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author KAN
 */
public class SearchHistory {

	public static class HistoryRecord {
		public int      hid;
		public String   query;
		public Date     lastSearch;
		public int      searchCount;
	}

	public static class HistoryTable {
		public static final String  HISTORY_TABLE   =   "search_history";
	}



	private static final int    MAX_RECORDS = 1024;


	SearchHistory() {
		// accessible from chords db
	}

	public void rememberQuery(String query) {

		// should be called from ui thread - all invocations should be sequential

		HistoryRecord hr = getHistoryRecord(query);
		if (hr == null) {
			hr = new HistoryRecord();
			hr.query        = query;
			hr.lastSearch   = new Date();
			hr.searchCount  = 1;
			addRecord(hr);
		} else {
			hr.query        = query;
			hr.lastSearch   = new Date();
			hr.searchCount++;
			updateRecord(hr);
		}
	}

	public void addRecord(HistoryRecord record) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		db.insert(HistoryTable.HISTORY_TABLE, null, recordContentValues(record));

		shrinkHistory();
	}

	public void updateRecord(HistoryRecord record) {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		// update query string as it may change case

		db.update(HistoryTable.HISTORY_TABLE, recordContentValues(record),
				"shid = ?",
				new String[]{Integer.toString(record.hid)});
	}

	public HistoryRecord getHistoryRecord(String query) {

		DatabaseWrapper db = ChordsDb.chordsDb().getReadableDatabase();

		String sql = "select shid, query, last_search, search_count from " + HistoryTable.HISTORY_TABLE + " " +
			"where lower(query) = ? ";

		Cursor c = db.rawQuery(sql, new String[]{query.toLowerCase()});

		if (c.moveToNext()) {
			return fromCursor(c);
		} else {
			return null;
		}
	}

	public List<HistoryRecord> getHistory(String query, int count) {

		// TODO - show queries with larger search count first

		DatabaseWrapper db = ChordsDb.chordsDb().getReadableDatabase();

		String[] params = {};

		String sql = "select shid, query, last_search, search_count from " + HistoryTable.HISTORY_TABLE + " ";
		if (query != null && !query.trim().isEmpty()) {
			sql += "where lower(query) like ? ";
			params = new String[]{"%" + query.toLowerCase() + "%"};
		}
		sql += "order by last_search desc";

		Cursor c = db.rawQuery(sql, params);

		List<HistoryRecord> history = new ArrayList<HistoryRecord>();
		while (c.moveToNext() && history.size() < count) {
			history.add(fromCursor(c));
		}

		c.close();

		return history;
	}

	private void shrinkHistory() {

		DatabaseWrapper db = ChordsDb.chordsDb().getWritableDatabase();

		String sql = "delete from " + HistoryTable.HISTORY_TABLE + " " +
				"where shid in (" +
					"select shid from " + HistoryTable.HISTORY_TABLE + " order by last_search desc " +
					"limit -1 offset ?)";

		db.execSQL(sql, new Object[]{MAX_RECORDS});
	}

	private ContentValues recordContentValues(HistoryRecord record) {
		ContentValues cv = new ContentValues();

		if (record.hid != 0) {
			// need null for insert
			cv.put("shid",      record.hid);
		}
		cv.put("query",         record.query);
		cv.put("last_search",   record.lastSearch != null ? record.lastSearch.getTime() : null);
		cv.put("search_count",  record.searchCount);

		return cv;
	}

	private HistoryRecord fromCursor(Cursor c) {

		HistoryRecord hr = new HistoryRecord();
		hr.hid          = c.getInt(0);
		hr.query        = c.getString(1);
		hr.lastSearch   = new Date(c.getLong(2));
		hr.searchCount  = c.getInt(3);

		return hr;
	}
}
