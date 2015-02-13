package kan.illuminated.chords.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import kan.illuminated.chords.R;
import kan.illuminated.chords.activity.BackgroundTask.ResultListener;

import static kan.illuminated.chords.ApplicationPreferences.*;

public class BaseChordsActivity<BackgroundResult> extends Activity implements ResultListener<BackgroundResult> {

	protected int shortAnimTime = 200;

	protected static final int animationInterpolatorId	= android.R.anim.decelerate_interpolator;

	protected static final String   BACKGROUND_FRAGMENT = "BACKGROUND_FRAGMENT";

	private class NavListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return MainMenuItem.values().length;
		}

		@Override
		public Object getItem(int position) {
			return MainMenuItem.values()[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final MainMenuItem item = (MainMenuItem) getItem(position);

			View view = getLayoutInflater().inflate(R.layout.main_menu_item, parent, false);

			TextView text = (TextView) view.findViewById(R.id.menu_item_text);
			text.setText(item.titleId);

			text.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					rootDrawer.closeDrawer(Gravity.LEFT);

					Intent i = new Intent(BaseChordsActivity.this, item.activity);
					startActivity(i);
				}
			});

			Object activity = BaseChordsActivity.this;
			if (item.activity.isAssignableFrom(activity.getClass())) {
				text.setBackgroundColor(getResources().getColor(R.color.menu_active));
			}

			return view;
		}
	}

	protected static class AnimationListener implements Animation.AnimationListener {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

	}

	protected static class BackgroundFragment<BackgroundResult> extends Fragment {

		BackgroundTask<BackgroundResult> backgroundTask;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			setRetainInstance(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
		}
	}

	protected BackgroundFragment<BackgroundResult> backgroundFragment;

	private ListView navList;
	private DrawerLayout rootDrawer;

	private ActionBarDrawerToggle rootDrawerToggle;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		saveLastAction();

		shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		navList = (ListView) findViewById(R.id.navList);
		if (navList != null) {
			navList.setAdapter(new NavListAdapter());
		}

		rootDrawer = (DrawerLayout) findViewById(R.id.rootDrawer);
		if (rootDrawer != null) {
			rootDrawerToggle = new ActionBarDrawerToggle(
					this, rootDrawer, R.drawable.ic_drawer, R.string.navigation_open, R.string.navigation_close) {

				@Override
				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);
				}

				@Override
				public void onDrawerClosed(View drawerView) {
					super.onDrawerClosed(drawerView);
				}
			};

			rootDrawer.setDrawerListener(rootDrawerToggle);

			rootDrawerToggle.syncState();
		}

		super.onPostCreate(savedInstanceState);
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		rootDrawerToggle.onConfigurationChanged(newConfig);
	}

	protected void initBackgroundFragment() {

		FragmentManager fm = getFragmentManager();
		backgroundFragment = (BackgroundFragment) fm.findFragmentByTag(BACKGROUND_FRAGMENT);

		if (backgroundFragment == null) {
			backgroundFragment = createBackgroundFragment();
			fm.beginTransaction()
					.add(backgroundFragment, BACKGROUND_FRAGMENT)
					.commit();
		}
	}

	protected BackgroundFragment<BackgroundResult> createBackgroundFragment() {
		return new BackgroundFragment<BackgroundResult>();
	}

	/**
	 * previous task will be cancelled if present
	 */
	protected void registerBackgroundTask(BackgroundTask<BackgroundResult> task) {
		if (backgroundFragment.backgroundTask != null) {
			backgroundFragment.backgroundTask.cancel();
		}

		backgroundFragment.backgroundTask = task;
		backgroundFragment.backgroundTask.setResultListener(this);
	}

	protected BackgroundTask<BackgroundResult> getBackgroundTask() {
		if (backgroundFragment != null) {
			return backgroundFragment.backgroundTask;
		}

		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (backgroundFragment != null && backgroundFragment.backgroundTask != null) {
			backgroundFragment.backgroundTask.removeResultListener();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (rootDrawerToggle != null && rootDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackgroundTaskReady(BackgroundResult backgroundResult) {
	}

	@Override
	public void onBackgroundTaskFailed(Exception e) {
	}

	private void saveLastAction() {

		SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
		Editor editor = preferences.edit();

		editor.putString(LAST_ACTION, this.getClass().getName());

		editor.apply();
	}

	protected void showToast(String message) {
		Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
		toast.show();
	}
}
