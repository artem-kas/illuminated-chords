package kan.illuminated.chords.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author KAN
 */
public class DatabaseWrapper {

	private SQLiteDatabase db;

	DatabaseWrapper(SQLiteDatabase db) {
		this.db = db;
	}

	public long insert(String table, String nullColumnHack, ContentValues values) {
		return db.insert(table, nullColumnHack, values);
	}

	public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
		return db.update(table, values, whereClause, whereArgs);
	}

	public Cursor rawQuery(String sql, String[] selectionArgs) {
		return db.rawQuery(sql, selectionArgs);
	}

	public void execSQL(String sql, Object[] bindArgs) throws SQLException {
		db.execSQL(sql, bindArgs);
	}
}
