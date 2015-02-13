package kan.illuminated.chords.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import kan.illuminated.chords.ActivityUtils;
import kan.illuminated.chords.Chords;
import kan.illuminated.chords.R;
import kan.illuminated.chords.data.ChordsDb;
import kan.illuminated.chords.data.ChordsHistory;
import kan.illuminated.chords.data.ChordsHistory.Field;

import java.util.ArrayList;
import java.util.List;

import static kan.illuminated.chords.ApplicationPreferences.*;
import static kan.illuminated.chords.data.ChordsHistory.Field.*;

/**
 * @author KAN
 */
public class FavouritesActivity extends BaseChordsActivity {

	private static final String TAG = FavouritesActivity.class.getName();

	private class FavouritesListAdapter extends BaseAdapter {

		private List<Chords> favourites = new ArrayList<Chords>();

		void refresh() {
			favourites = ChordsDb.chordsDb().chordsHistory().readFavourites(filter, sortField);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return favourites.size();
		}

		@Override
		public Chords getItem(int position) {
			return favourites.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).chordId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			Chords chords = getItem(position);

			LayoutInflater li = getLayoutInflater();
			View view = li.inflate(R.layout.favourite_chord, parent, false);

			TextView titleText = (TextView) view.findViewById(R.id.titleText);
			titleText.setText(chords.title);

			TextView authorText = (TextView) view.findViewById(R.id.authorText);
			authorText.setText(chords.author);

			return view;
		}
	}

	private String  filter;
	private ChordsHistory.Field sortField = TITLE;

	private ListView favouritesList;
	private FavouritesListAdapter favouritesAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_favourite);

		super.onCreate(savedInstanceState);

		favouritesList = (ListView) findViewById(android.R.id.list);

		favouritesAdapter = new FavouritesListAdapter();
		favouritesList.setAdapter(favouritesAdapter);

		favouritesList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Chords chords = favouritesAdapter.getItem(position);

				ActivityUtils.showChordsActivity(FavouritesActivity.this, chords);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		Log.d(TAG, "onCreateOptionsMenu()");

		getMenuInflater().inflate(R.menu.favourites, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

		final MenuItem searchItem = menu.findItem(R.id.favourites_search);
		final SearchView searchView = (SearchView) searchItem.getActionView();

		SearchableInfo si = searchManager.getSearchableInfo(getComponentName());
		searchView.setSearchableInfo(si);

		searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

		if (filter != null) {
			searchItem.expandActionView();
			searchView.setQuery(filter, false /* submit */);
			searchView.clearFocus();
		}

		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				searchView.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				setFilter(newText);
				return true;
			}
		});

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.favourites_sort:

				new AlertDialog.Builder(this)
						.setTitle("Sort by")
						.setItems(new String[]{"Title", "Author"}, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									case 0:
										setSortField(TITLE); break;
									case 1:
										setSortField(AUTHOR); break;
								}
							}
						})
						.create().show();

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void setFilter(String filter) {
		this.filter = filter;
		favouritesAdapter.refresh();
	}

	private void setSortField(Field sortField) {
		this.sortField = sortField;
		favouritesAdapter.refresh();
	}

	@Override
	protected void onStart() {
		super.onStart();

		Log.d(TAG, "onStart()");

		loadSettings();

		favouritesAdapter.refresh();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onPause() {
		super.onPause();

		saveSettings();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void saveSettings() {

		SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
		Editor editor = preferences.edit();

		editor.putString(FAVOURITES_FILTER, filter);
		editor.putString(FAVOURITES_SORT, sortField.toString());

		editor.apply();
	}

	private void loadSettings() {

		SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

		filter = preferences.getString(FAVOURITES_FILTER, "");
		sortField = Field.valueOf(preferences.getString(FAVOURITES_SORT, TITLE.toString()));
	}
}
