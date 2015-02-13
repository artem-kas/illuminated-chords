package kan.illuminated.chords.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import kan.illuminated.chords.ChordsApplication;
import kan.illuminated.chords.data.SearchHistory.HistoryTable;

/**
 * @author KAN
 */
public class ChordsDb {

	public static final String  CHORDS_DB   = "chords.db";

	public static final int     DB_VERSION  = 4;

	private static volatile ChordsDb instance;


	public static ChordsDb chordsDb() {

		ChordsDb i = instance;
		if (i == null) {
			synchronized (ChordsDb.class) {
				if (instance == null) {
					i = instance = new ChordsDb(ChordsApplication.appContext);
				}
			}
		}

		return i;
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
					"shid integer primary key, " +
					"query text, " +
					"last_search date, " +
					"search_count int" +
					")");

			db.execSQL("drop table if exists chords");
			db.execSQL("create table chords (" +
					"cid integer primary key, " +
					"author text, " +
					"title text, " +
					"url text, " +
					"type text, " +
					"rating int, " +
					"votes int, " +
					"text text, " +
					"marks text, " +
					"last_read date, " +
					"history int, " +
					"favourite int " +
					")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			System.out.println("updating database " + oldVersion + " -> " + newVersion);

			onCreate(db);
		}
	}

	private ChordsDatabase chordsDatabase;

	public ChordsDb(Context context) {

		System.out.println("creating chords db with " + context);
		new Throwable().printStackTrace(System.out);

		this.chordsDatabase = new ChordsDatabase(context);
	}

	public DatabaseWrapper getWritableDatabase() {
		// TODO - cache it
		return new DatabaseWrapper(chordsDatabase.getWritableDatabase());
	}

	public DatabaseWrapper getReadableDatabase() {
		// TODO - cache it
		return new DatabaseWrapper(chordsDatabase.getReadableDatabase());
	}


	public SearchHistory searchHistory() {
		return new SearchHistory();
	}

	public ChordsHistory chordsHistory() {
		return new ChordsHistory();
	}
}
