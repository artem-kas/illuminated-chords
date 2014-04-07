package kan.illuminated.chords.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import kan.illuminated.chords.data.SearchHistory.HistoryTable;

/**
 * @author KAN
 */
public class ChordsDb {

	public static final String  CHORDS_DB   = "chords.db";

	public static final int     DB_VERSION  = 1;

	private static ChordsDb instance;

	public static void init(Context context) {
		if (instance != null) {
			throw new RuntimeException("chords db is already inited");
		}

		instance = new ChordsDb(context);
	}

	public static class ChordsDatabase extends SQLiteOpenHelper {

		public ChordsDatabase(Context context) {
			super(context, ChordsDb.CHORDS_DB, null, ChordsDb.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			System.out.println("creating history table ...");

			db.execSQL("drop table if exists " + HistoryTable.HISTORY_TABLE);
			db.execSQL("create table " + HistoryTable.HISTORY_TABLE + " (" +
					"hid integer primary key, " +
					"query text, " +
					"last_search date, " +
					"search_count int" +
					")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onCreate(db);
		}
	}

	private ChordsDatabase chordsDatabase;

	public ChordsDb(Context context) {
		this.chordsDatabase = new ChordsDatabase(context);
	}
}
