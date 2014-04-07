package kan.illuminated.chords.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.animation.Animation;

public class BaseChordsActivity extends Activity {

	protected int shortAnimTime = 200;

	protected static final int animationInterpolatorId	= android.R.anim.decelerate_interpolator;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
	}

}
