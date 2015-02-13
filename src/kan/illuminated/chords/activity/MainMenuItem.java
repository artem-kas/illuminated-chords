package kan.illuminated.chords.activity;

import android.app.Activity;
import kan.illuminated.chords.R;

/**
 * @author KAN
 */
public enum MainMenuItem {

	SEARCH(R.string.main_menu_search, SearchActivity.class),
	HISTORY(R.string.main_menu_history, HistoryActivity.class),
	FAVOURITES(R.string.main_menu_favourites, FavouritesActivity.class);

	public int  titleId;
	public Class<? extends Activity>  activity;

	MainMenuItem(int titleId, Class<? extends Activity> activity) {
		this.titleId = titleId;
		this.activity = activity;
	}
}
