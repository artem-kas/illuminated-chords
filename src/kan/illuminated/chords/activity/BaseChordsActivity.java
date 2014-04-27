package kan.illuminated.chords.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.animation.Animation;
import kan.illuminated.chords.activity.BackgroundTask.ResultListener;

public class BaseChordsActivity<BackgroundResult> extends Activity implements ResultListener<BackgroundResult> {

	protected int shortAnimTime = 200;

	protected static final int animationInterpolatorId	= android.R.anim.decelerate_interpolator;

	protected static final String   BACKGROUND_FRAGMENT = "BACKGROUND_FRAGMENT";

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


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
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
	public void onBackgroundTaskReady(BackgroundResult backgroundResult) {
	}

	@Override
	public void onBackgroundTaskFailed(Exception e) {
	}

}
