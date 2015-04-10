package kan.illuminated.chords.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import kan.illuminated.chords.R;

import static kan.illuminated.chords.ApplicationPreferences.*;

/**
 * @author KAN
 */
public class LaunchActivity extends Activity {

	private static final String TAG = LaunchActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_launch);

		SharedPreferences preferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

		String lastActivity = preferences.getString(LAST_ACTIVITY, null);
		Class<? extends Activity> lastActivityCls = SearchActivity.class;
		if (lastActivity != null) {
			try {
				lastActivityCls = (Class<? extends Activity>) Class.forName(lastActivity);
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "can't find last activity class " + lastActivity);
			}
		}

		startActivity(new Intent(this, lastActivityCls));
//		startActivity(new Intent(this, SearchActivity.class));
	}
}
