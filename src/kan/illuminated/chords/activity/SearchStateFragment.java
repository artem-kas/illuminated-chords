package kan.illuminated.chords.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import kan.illuminated.chords.activity.SearchActivity.State;

/**
 * @author KAN
 */
public class SearchStateFragment extends Fragment {

	private static final String TAG = SearchStateFragment.class.getSimpleName();

	State state;


	@Override
	public void onAttach(Activity activity) {

		Log.d(TAG, "onAttach()");

		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		Log.d(TAG, "onActivityCreated()");

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {

		Log.d(TAG, "onStart()");

		super.onStart();
	}

	@Override
	public void onResume() {

		Log.d(TAG, "onResume()");

		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		Log.d(TAG, "onSaveInstanceState()");

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		Log.d(TAG, "onConfigurationChanged()");

		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onPause() {

		Log.d(TAG, "onPause()");

		super.onPause();
	}

	@Override
	public void onStop() {

		Log.d(TAG, "onStop(), this is " + this + ", state is " + state);

		super.onStop();
	}

	@Override
	public void onDestroy() {

		Log.d(TAG, "onDestroy()");

		super.onDestroy();

		if (state != null) {
			if (state.chordsSearcher != null) {
				state.chordsSearcher.cancelQuery();
			}
		}
	}

	@Override
	public void onDetach() {

		Log.d(TAG, "onDetach()");

		super.onDetach();
	}
}
