package kan.illuminated.chords.activity;

import android.app.ExpandableListActivity;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import kan.illuminated.chords.Chords;
import kan.illuminated.chords.R;
import kan.illuminated.chords.data.ChordsDb;
import kan.illuminated.chords.data.SearchHistory;
import kan.illuminated.chords.schordssource.ChordsSearcher.ChordsResultListener;
import kan.illuminated.chords.schordssource.UltimateGuitarChordsSearcher;
import kan.illuminated.chords.ui.DisplayInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SearchActivity extends ExpandableListActivity {

	private static final String TAG = SearchActivity.class.getSimpleName();

	private static final String SEARCH_FRAGMENT = "SEARCH_FRAGMENT";

	private static final Map<String, Integer> TYPE_ORDER    = new HashMap<String, Integer>();
	static {
		TYPE_ORDER.put("chords",    1);
		TYPE_ORDER.put("tab",       2);
		TYPE_ORDER.put("bass",      3);
		TYPE_ORDER.put("drums",     4);
		TYPE_ORDER.put("ukulele",   5);
	}

	private static class ChordsComparator implements Comparator<Chords> {
		@Override
		public int compare(Chords one, Chords two) {

			Integer tOne = TYPE_ORDER.get(one.type);
			Integer tTwo = TYPE_ORDER.get(two.type);

			if (tOne == null) {
				System.out.println("unknown type " + one.type);
				if (tTwo == null) {
					int c = one.type.compareTo(two.type);
					if (c != 0) {
						return c;
					}
				} else {
					return +1;
				}
			} else {
				if (tTwo == null) {
					System.out.println("unknown type " + two.type);
					return -1;
				} else {
					if (tOne.intValue() != tTwo.intValue()) {
						return tOne - tTwo;
					}
				}
			}

			if (one.rating == null && two.rating == null) {
				return 0;
			}

			if (one.rating != null && two.rating == null) {
				return -1;
			}

			if (one.rating == null && two.rating != null) {
				return +1;
			}

			if (one.rating.intValue() != two.rating.intValue()) {
				return two.rating - one.rating;
			}

			if (one.votes == null) {
				if (two.votes != null) {
					return +1;
				}
			} else {
				if (two.votes == null) {
					return -1;
				} else {
					if (one.votes.intValue() != two.votes.intValue()) {
						return two.votes - one.votes;
					}
				}
			}

			return 0;
		}
	}

	private static final ChordsComparator CHORDS_COMPARATOR = new ChordsComparator();

	private class SongChords {

		// general song info - title and author
		Chords	song;

		// most popular chords for each type
		List<Chords>	topChords;

		List<Chords>	chordsList;

		// false - full chords list
		boolean top = true;

		boolean getMore = false;
	}

	private class ExpandableChordsAdapter extends BaseExpandableListAdapter {

		private List<SongChords>	chords = new ArrayList<SongChords>();

		public ExpandableChordsAdapter() {
		}

		public void setChords(List<SongChords> chords) {
			this.chords = chords;
		}

		public List<SongChords> getChords() {
			return chords;
		}

		SongChords getSongChords(int group) {
			return chords.get(group);
		}

		Chords getChords(int group, int child) {
			SongChords sc = chords.get(group);
			if (sc.top) {
				return sc.topChords.get(child);
			} else {
				return sc.chordsList.get(child);
			}
		}

		boolean isMoreChords(int group, int child) {
			SongChords sc = chords.get(group);
			return sc.top && sc.topChords.size() <= child;
		}

		@Override
		public int getGroupCount() {
			return chords.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {

			SongChords sc = chords.get(groupPosition);
			if (sc.top && sc.topChords.size() != sc.chordsList.size()) {
				// one extra for 'more' bar
				return sc.topChords.size() + 1;
			} else {
				return sc.chordsList.size();
			}
		}

		@Override
		public Object getGroup(int groupPosition) {
			return chords.get(groupPosition);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return chords.get(groupPosition).chordsList.get(childPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return ((long)groupPosition << 32 ) & childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

			SongChords songChords = chords.get(groupPosition);
			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			if (!songChords.getMore) {

				// the song

				Chords chords = songChords.song;

				final View songView = li.inflate(R.layout.search_song, parent, false);

				TextView titleText = (TextView) songView.findViewById(R.id.titleText);
				titleText.setText(chords.title);

				TextView authorText = (TextView) songView.findViewById(R.id.authorText);
				authorText.setText(chords.author);

				System.out.println("convert view is " + convertView);

				return songView;
			}
			else {

				// progress bar

				View progressView = li.inflate(R.layout.search_progress, parent, false);

				state.chordsSearcher.fetchNext();

				return progressView;
			}

		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View v;
			if (isMoreChords(groupPosition, childPosition)) {
				v = li.inflate(R.layout.search_more, parent, false);
			} else {
				SongChords sc = chords.get(groupPosition);

				Chords c = null;
				if (sc.top) {
					c = sc.topChords.get(childPosition);
				} else {
					c = sc.chordsList.get(childPosition);
				}

				v = li.inflate(R.layout.search_chord, parent, false);
				TextView descText = (TextView) v.findViewById(R.id.descriptionText);
				String desc = c.type;
				if (c.rating != null) {
//					desc += "   " + c.rating + "|" + c.votes;

					ImageView ratingImage = (ImageView) v.findViewById(R.id.ratingImage);
					switch (c.rating) {
						case 1:
							ratingImage.setImageResource(R.drawable.rating_one);
							break;
						case 2:
							ratingImage.setImageResource(R.drawable.rating_two);
							break;
						case 3:
							ratingImage.setImageResource(R.drawable.rating_three);
							break;
						case 4:
							ratingImage.setImageResource(R.drawable.rating_four);
							break;
						case 5:
							ratingImage.setImageResource(R.drawable.rating_five);
							break;
					}
				}
				descText.setText(desc);
			}

			return v;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	public static class State {
		String                          query;

		// equals to the chords adapter chords list
		List<SongChords>	            chords = new ArrayList<SongChords>();

		UltimateGuitarChordsSearcher    chordsSearcher;
	}


	private ExpandableListView listSearch;
	private ExpandableChordsAdapter chordsAdapter;

	private ProgressBar progressBar;

	private SearchHistory searchHistory;

	private boolean haveSearchResult = false;

	private State state;


	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);

		System.out.println("screen size is " +
				(getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK));

		DisplayMetrics dm = getResources().getDisplayMetrics();
		System.out.println("density is " + dm.density);
		System.out.println("density dpi is " + dm.densityDpi);
		System.out.println("dpi is " + dm.xdpi + " - " + dm.ydpi);

		setContentView(R.layout.activity_search);

		listSearch = getExpandableListView();
		listSearch.setGroupIndicator(null);

		chordsAdapter = new ExpandableChordsAdapter();
		listSearch.setAdapter(chordsAdapter);

		progressBar = (ProgressBar) findViewById(R.id.searchLoadingProgress);

		searchHistory = ChordsDb.chordsDb().searchHistory();

		FragmentManager fm = getFragmentManager();
		SearchStateFragment ssf = (SearchStateFragment) fm.findFragmentByTag(SEARCH_FRAGMENT);

		if (ssf == null) {
			ssf = new SearchStateFragment();
			fm.beginTransaction()
					.add(ssf, SEARCH_FRAGMENT)
					.commit();

			ssf.state = new State();
		}

		state = ssf.state;

		listSearch.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long id) {

				System.out.println("item clicked " + id);

				Chords chords = (Chords) parentAdapter.getAdapter().getItem(position);

				Intent i = new Intent(SearchActivity.this, ChordsActivity.class);
				i.setData(Uri.parse(chords.url));

				startActivity(i);
			}
		});

		listSearch.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

				System.out.println("child clicked " + groupPosition + " " + childPosition + " " + id);

				ExpandableChordsAdapter adapter = (ExpandableChordsAdapter) parent.getExpandableListAdapter();

				if (adapter.isMoreChords(groupPosition, childPosition)) {
					SongChords sc = adapter.getSongChords(groupPosition);
					sc.top = false;

					chordsAdapter.notifyDataSetChanged();
				} else {
					Chords chords = adapter.getChords(groupPosition, childPosition);

					Intent i = new Intent(SearchActivity.this, ChordsActivity.class);
					i.setData(Uri.parse(chords.url));

					startActivity(i);
				}

				return true;
			}
		});

		Intent i = getIntent();
		if (Intent.ACTION_SEARCH.equals(i.getAction())) {

			// query from search view user input
			String query = i.getStringExtra(SearchManager.QUERY);
			if (query == null) {
				// or may be data from suggestion list
				query = i.getData() != null ? i.getData().toString() : null;
			}

			if (state.query != null && state.query.equals(query)) {

				// probably just screen orientation change

				Log.d(TAG, "repeating query [" + query + "]");

				chordsAdapter.setChords(state.chords);
				chordsAdapter.notifyDataSetChanged();

			} else {

				// new query

				Log.d(TAG, "new query [" + query + "]");

				state.query = query;
				state.chords = chordsAdapter.getChords();

				if (state.chordsSearcher != null) {
					state.chordsSearcher.cancelQuery();
				}
				state.chordsSearcher = new UltimateGuitarChordsSearcher();
				state.chordsSearcher.query(query);

				progressBar.setVisibility(View.VISIBLE);
			}
		}

		if (state.chordsSearcher != null) {
			// may immediately be invoked for delayed search responses - should be ok
			state.chordsSearcher.setChordsResultListener(new ChordsResultListener() {
				@Override
				public void onSearchUpdated(List<Chords> newChords, boolean last) {
					SearchActivity.this.onSearchReady(newChords, last);
				}
			});
		}

		// TODO - show default screen


		// this view is not full screen
		// remember nav bar size for future full screen activities
		listSearch.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				Rect wvr = new Rect();
				getWindow().getDecorView().getWindowVisibleDisplayFrame(wvr);

				DisplayInfo.notificationBarHeight = wvr.top;
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		System.out.println("onCreateOptionsMenu");

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

		SearchableInfo si = searchManager.getSearchableInfo(getComponentName());

		searchView.setSearchableInfo(si);

		searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

		if (state.query != null) {
			searchView.setQuery(state.query, false /* submit */);
			searchView.setIconified(false); // expand

			// steal focus back from search view's text field - it's set with iconify()
			listSearch.requestFocus();

			// clean up iconify's keyboard
			searchView.post(new Runnable() {

				@Override
				public void run() {
		            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		            if (imm != null) {
		            	imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
		            }
				}
			});
		}

		return true;
	}

	private void onSearchReady(List<Chords> newChords, boolean last) {

		System.out.println("onSearchReady() - " + this);

		// filter out unknown chords types
		for (Iterator<Chords> it = newChords.iterator(); it.hasNext(); ) {

			Chords c = it.next();

			if (c.type != null && (
					c.type.equals("tab pro") ||
					c.type.equals("video lesson") ||
					c.type.equals("guitar pro") ||
					c.type.equals("power tab"))) {

				it.remove();
			}
		}

		List<SongChords> scl = chordsAdapter.getChords();

		// remove 'more...' stub
		if (!scl.isEmpty()) {
			SongChords sc = scl.get(scl.size() - 1);
			if (sc.getMore) {
				scl.remove(scl.size() - 1);
			}
		}

		for (Chords c : newChords) {

			SongChords songChords = null;

			// do we already have a version of the same song?
			for (SongChords sc : scl) {

				Chords s = sc.song;
				if (s.author != null && s.author.equals(c.author) &&
						s.title != null && s.title.equals(c.title)) {

					songChords = sc;
					break;
				}
			}

			if (songChords == null) {
				songChords = new SongChords();

				songChords.song = c;
				songChords.chordsList = new ArrayList<Chords>();

				scl.add(songChords);
			}

			songChords.chordsList.add(c);

		}

		for (SongChords sc : scl) {
			Collections.sort(sc.chordsList, CHORDS_COMPARATOR);

			sc.topChords = new ArrayList<Chords>();
			String t = null;
			for (Chords c : sc.chordsList) {
				if (!c.type.equals(t)) {

					sc.topChords.add(c);

					t = c.type;
				}
			}
		}

		if (!scl.isEmpty()) {

			if (!haveSearchResult) {
				// received some data for a new query, save this query

				searchHistory.rememberQuery(state.chordsSearcher.getQuery());
			}

			haveSearchResult = true;
		}

		if (!last) {

			SongChords sc = new SongChords();
			sc.song = new Chords();
			sc.song.title = "more chords...";
			sc.getMore  = true;

			scl.add(sc);
		}

		progressBar.setVisibility(View.GONE);

		chordsAdapter.notifyDataSetChanged();
	}


	@Override
	protected void onStart() {
		super.onStart();

		Log.d(TAG, "onStart()");
	}

	@Override
	protected void onStop() {
		super.onStop();

		Log.d(TAG, "onStop()");
	}

	@Override
	protected void onDestroy() {

		Log.d(TAG, "onDestroy()");

		if (state.chordsSearcher != null) {
			// do not cancel, let it continue his work in background
			// just let it know no one can handle the result at the moment
			state.chordsSearcher.removeChordsResultListener();
		}

		super.onDestroy();
	}
}
