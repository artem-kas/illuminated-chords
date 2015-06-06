package kan.illuminated.chords.activity;

import static kan.illuminated.chords.ApplicationPreferences.APP_PREFERENCES;
import static kan.illuminated.chords.ApplicationPreferences.LAST_ACTIVITY;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import kan.illuminated.chords.R;

/**
 * @author KAN
 */
public class LaunchActivity extends Activity {

	private static final String TAG = LaunchActivity.class.getName();

	public LaunchActivity() {
		Log.d(TAG, "LaunchActivity ctor");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "LaunchActivity onCreate");

		setContentView(R.layout.activity_launch);

		Intent intent = getIntent();
		System.out.println(intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		Log.d(TAG, "LaunchActivity onSaveInstanceState");
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		Log.d(TAG, "LaunchActivity onRestoreInstanceState");
	}

	@Override
	protected void onStart() {
		super.onStart();

		Log.d(TAG, "LaunchActivity onStart");

		SharedPreferences preferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

		String lastActivity = preferences.getString(LAST_ACTIVITY, null);
		Class<? extends Activity> lastActivityCls = SearchActivity.class;
		Log.d(TAG, "launching activity " + lastActivity);
		if (lastActivity != null) {
			try {
				lastActivityCls = (Class<? extends Activity>) Class.forName(lastActivity);
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "can't find last activity class " + lastActivity);
			}
		}

		if (lastActivityCls == ChordsActivity.class) {
			TaskStackBuilder.create(this)
					.addNextIntentWithParentStack(new Intent(this, SearchActivity.class))
					.addNextIntent(new Intent(this, ChordsActivity.class))
					.startActivities();
		} else {
			startActivity(new Intent(this, lastActivityCls));
//			startActivity(new Intent(this, SearchActivity.class));
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		Log.d(TAG, "LaunchActivity onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(TAG, "LaunchActivity onResume");
	}
}
