package kan.illuminated.chords.activity;

import static kan.illuminated.chords.DateUtils.*;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import kan.illuminated.chords.ActivityUtils;
import kan.illuminated.chords.Chords;
import kan.illuminated.chords.DetailedDateFormatter;
import kan.illuminated.chords.R;
import kan.illuminated.chords.data.ChordsDb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author KAN
 */
public class HistoryActivity extends BaseChordsActivity {

	private static class ChordsPeriod {
		private Date date;
		private List<Chords> chords = new ArrayList<Chords>();
	}

	private class ExpandableHistoryAdapter extends BaseExpandableListAdapter {

		private List<ChordsPeriod> chordsPeriods;

		private DetailedDateFormatter dateFormat = new DetailedDateFormatter();

		private ExpandableHistoryAdapter() {
			List<Chords> chords = ChordsDb.chordsDb().chordsHistory().readFullHistory();
			groupChords(chords);
		}

		private void groupChords(List<Chords> chords) {

			// currently groups by month

			chordsPeriods = new ArrayList<ChordsPeriod>();

			ChordsPeriod period = null;

			Date today = today();
			Date yesterday = yesterday();
			Date week = firstWeekDate();

			for (Chords c : chords) {
				Date chordsDate = toDay(c.lastRead);

				Date periodDate = null;
				if (periodDate == null) {
					if (chordsDate.equals(today)) {
						periodDate = today;
					}
				}

				if (periodDate == null) {
					if (chordsDate.equals(yesterday)) {
						periodDate = yesterday;
					}
				}

				if (periodDate == null) {
					if (chordsDate.compareTo(week) >= 0) {
						periodDate = week;
					}
				}

				if (periodDate == null) {
					periodDate = toMonth(chordsDate);
				}

				if (period == null || !period.date.equals(periodDate)) {
					period = new ChordsPeriod();
					period.date = periodDate;

					chordsPeriods.add(period);
				}

				period.chords.add(c);
			}
		}

		@Override
		public int getGroupCount() {
			return chordsPeriods.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return chordsPeriods.get(groupPosition).chords.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return chordsPeriods.get(groupPosition);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return chordsPeriods.get(groupPosition).chords.get(childPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return ((Chords)getChild(groupPosition, childPosition)).chordId;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

			ChordsPeriod period = chordsPeriods.get(groupPosition);

			LayoutInflater li = getLayoutInflater();
			View view = li.inflate(R.layout.history_date, parent, false);

			TextView dateText = (TextView) view.findViewById(R.id.dateText);
			dateText.setText(dateFormat.historyDate(period.date));

			return view;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

			Chords chords = chordsPeriods.get(groupPosition).chords.get(childPosition);

			LayoutInflater li = getLayoutInflater();
			View view = li.inflate(R.layout.history_chord, parent, false);

			TextView titleText = (TextView) view.findViewById(R.id.titleText);
			titleText.setText(chords.title);

			TextView authorText = (TextView) view.findViewById(R.id.authorText);
			authorText.setText(chords.author);

			return view;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public void deleteChild(int groupPosition, int childPosition) {
			chordsPeriods.get(groupPosition).chords.remove(childPosition);
			if (chordsPeriods.get(groupPosition).chords.isEmpty()) {
				chordsPeriods.remove(groupPosition);
			}
		}

		public void clear() {
			chordsPeriods = new ArrayList<ChordsPeriod>();
		}

	}

	private ExpandableListView listHistory;
	private ExpandableHistoryAdapter historyAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_history);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		listHistory = (ExpandableListView) findViewById(android.R.id.list);
		listHistory.setGroupIndicator(null);

		historyAdapter = new ExpandableHistoryAdapter();
		listHistory.setAdapter(historyAdapter);

		listHistory.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

				Chords chords = (Chords) historyAdapter.getChild(groupPosition, childPosition);

				ActivityUtils.showChordsActivity(HistoryActivity.this, chords);

				return true;
			}
		});

		registerForContextMenu(listHistory);

		if (historyAdapter.getGroupCount() > 0) {
			listHistory.expandGroup(0);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.history, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.history_clear:
				AlertDialog.Builder adb = new Builder(this)
//							.setTitle("delete")
						.setMessage("Clear chords history?")
						.setPositiveButton("Clear", new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								ChordsDb.chordsDb().chordsHistory().clearHistory();
								historyAdapter.clear();
								historyAdapter.notifyDataSetChanged();
							}
						})
						.setNegativeButton("Cancel", null);

				adb.create().show();

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		System.out.println("context menu for " + v);

		ExpandableListContextMenuInfo elMenuInfo = (ExpandableListContextMenuInfo) menuInfo;

		long elpos = elMenuInfo.packedPosition;
		if (ExpandableListView.getPackedPositionType(elpos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int gpos = ExpandableListView.getPackedPositionGroup(elpos);
			int cpos = ExpandableListView.getPackedPositionChild(elpos);

			Chords chords = (Chords) historyAdapter.getChild(gpos, cpos);

			getMenuInflater().inflate(R.menu.history_context, menu);
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		ExpandableListContextMenuInfo elMenuInfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
			case R.id.history_delete:
				long elpos = elMenuInfo.packedPosition;
				if (ExpandableListView.getPackedPositionType(elpos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
					final int gpos = ExpandableListView.getPackedPositionGroup(elpos);
					final int cpos = ExpandableListView.getPackedPositionChild(elpos);

					final Chords chords = (Chords) historyAdapter.getChild(gpos, cpos);

					AlertDialog.Builder adb = new Builder(this)
//							.setTitle("delete")
							.setMessage("Delete " + chords.title + " from history?")
							.setPositiveButton("Delete", new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									ChordsDb.chordsDb().chordsHistory().deleteFromHistory(chords.chordId);
									historyAdapter.deleteChild(gpos, cpos);
									historyAdapter.notifyDataSetChanged();
								}
							})
							.setNegativeButton("Cancel", null);

					adb.create().show();
				}
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
}
