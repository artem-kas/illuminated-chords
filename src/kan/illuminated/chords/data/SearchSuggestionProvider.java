package kan.illuminated.chords.data;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import kan.illuminated.chords.data.SearchHistory.HistoryRecord;

import java.util.List;

/**
 * @author KAN
 */
public class SearchSuggestionProvider extends ContentProvider {

	public static final String AUTHORITY = "chords.SearchSuggestionProvider";

	private static final int    HISTORY_DISPLAY_COUNT   = 5;

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		System.out.println("SearchSuggestionProvider - searching for uri [" + uri + "], selection  [" + selection + "], selection args " + selectionArgs);

		System.out.println("querying suggestions from " + Thread.currentThread().getName());

		SearchHistory searchHistory = ChordsDb.chordsDb().searchHistory();
		List<HistoryRecord> history = searchHistory.getHistory(selectionArgs[0], HISTORY_DISPLAY_COUNT);

		MatrixCursor c = new MatrixCursor(new String[]{
				BaseColumns._ID,
				SearchManager.SUGGEST_COLUMN_TEXT_1,
				SearchManager.SUGGEST_COLUMN_INTENT_DATA});

		for (int i = 0; i < history.size(); i++) {

			HistoryRecord hr = history.get(i);

			c.addRow(new Object[]{
					i,
					hr.query,
					hr.query});
		}

		return c;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}
}
