package kan.illuminated.chords.ui;

import android.app.Activity;
import android.view.animation.AnimationUtils;
import android.widget.ScrollView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * must be used from ui thread
 */
public class Autoscroller {

	public static interface AutoscrollerListener {
		void onStop();
	}

	private AutoscrollerListener	listener;

	private Activity	activity;
	private ScrollView	scrollView;

	private Timer autoscrollTimer;

	private int		previousEffectiveScroll;
	private float	scrollDelta;
	private float	scrollVelocity	= 10; // pps
	private long	previousTime;

	private int		tick;


	public Autoscroller(Activity activity, ScrollView scrollView, AutoscrollerListener listener) {
		this(activity, scrollView);
		this.listener	= listener;
	}

	public Autoscroller(Activity activity, ScrollView scrollView) {
		this.activity = activity;
		this.scrollView = scrollView;
	}

	public float getScrollVelocity() {
		return scrollVelocity;
	}

	public void setScrollVelocity(float scrollVelocity) {
		this.scrollVelocity = scrollVelocity;
	}

	public void start() {

		if (isRunning()) {
			return;
		}

		previousEffectiveScroll	= 0;
		scrollDelta				= 0;
		previousTime			= AnimationUtils.currentAnimationTimeMillis();

		autoscrollTimer = new Timer("chords autoscroll");
		autoscrollTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onScrollTick();
					}
				});
			}
		}, 0, 10);
	}

	public void stop() {
		if (autoscrollTimer != null) {
			autoscrollTimer.cancel();
			autoscrollTimer.purge();
			autoscrollTimer = null;

			if (listener != null)
				listener.onStop();
		}
	}

	private void onScrollTick() {

		if (!isRunning()) {
			// may have last ticks reach ui thread after scroller was stopped
			return;
		}

		long time = AnimationUtils.currentAnimationTimeMillis();
		long dt = time - previousTime;
		previousTime = time;

		float des = scrollVelocity * dt / 1000.0f;
		scrollDelta += des;

		int es = (int) scrollDelta;

		if (es != previousEffectiveScroll) {

			int ds = es - previousEffectiveScroll;
			previousEffectiveScroll = es;

			int s = scrollView.getScrollY();

			scrollView.scrollBy(0, ds);

			if (scrollView.getScrollY() == s) {
				stop();
				return;
			}

			System.out.println("scrolled in " + tick + " ticks from " + s + " to " + scrollView.getScrollY());
			tick = 0;
		}

		tick++;
	}

	public boolean isRunning() {
		return autoscrollTimer != null;
	}
}
