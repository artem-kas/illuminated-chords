package kan.illuminated.chords.activity;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import kan.illuminated.chords.Chords;
import kan.illuminated.chords.Chords.ChordMark;
import kan.illuminated.chords.MyTextView;
import kan.illuminated.chords.R;
import kan.illuminated.chords.UltimateGuitarChordsSource;
import kan.illuminated.chords.ui.Autoscroller;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.*;

public class ChordsActivity extends BaseChordsActivity {

	private enum EState {
		NEW,
		LOADING,
		CHORDS
	}

	private class LoadChordsTask extends AsyncTask<String, Integer, Chords>
	{

		@Override
		protected Chords doInBackground(String... params) {

			return new UltimateGuitarChordsSource().getChords(params[0]);
		}

		@Override
		protected void onPostExecute(Chords chords) {

			onChordsLoaded(chords);
		}

	}

	private class ChordsScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		float originalScale;

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {

			System.out.println("onScaleBegin() " + detector.getScaleFactor());

			originalScale = state.textScale;

			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {

			System.out.println("onScale() " + detector.getScaleFactor());

			setTextScale(originalScale * detector.getScaleFactor());

			zoomText(detector.getFocusX(), detector.getFocusY());

			return false;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {

			System.out.println("onScaleEnd() " + detector.getScaleFactor());
		}

	}


	private class ChordsGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {

			toggleScreen();

			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {

			if (state.textScale != 1.0f) {
				resetTextZoom(e.getX(), e.getY());
			}
			else {
				zoomToLongestLine(e.getX(), e.getY());
			}

			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

			// hide navigation on user scroll, not autoscroll
			fullScreen(true);

			return false;
		}

	}

	private static class State {

		Chords	chords;

		int		chordsOffset;
		float	textScale = 1.0f;

		boolean	scrolling	= false;
	}

	private static final float MIN_TEXT_SCALE	= 0.5f;
	private static final float MAX_TEXT_SCALE	= 2.0f;

	private EState	stateStatus = EState.NEW;

	private boolean fullScreen = false;

	private ScaleGestureDetector scaleGestureDetector;

//	private View		topRoot;
//	private View		topSpace;
//	private View		topBar;
	private View		bottomBar;
	private View		playButton;
	private View		pauseButton;
//	private TextView	topTitle;
//	private TextView	topSubtitle;
	private ScrollView	scrollChords;
	private MyTextView	textChords;
	private SeekBar		speedSeek;
	private ProgressBar	loadingProgress;


	private float baseTextSize;
	private int actionBarHeight;

	// thread safe - used only form ui thread
	private static final State state = new State();

	private Timer fullScreenTimer;

	private Autoscroller	autoscroller;
	private static final float			minAutoscrollSpeed	= 5;	// pps
	private static final float			maxAutoscrollSpeed	= 25;	// pps


	public ChordsActivity() {
		System.out.println("ChordsActivity() ctor");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		requestWindowFeature(Window.FEATURE_NO_TITLE);

//		getWindow().addFlags(
//				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_chords);

//		topRoot			= findViewById(R.id.chordsTopRoot);
//		topSpace		= findViewById(R.id.chordsTopSpace);
//		topBar			= findViewById(R.id.chordsTopBar);
		bottomBar		= findViewById(R.id.chordsBottomBar);
		playButton		= findViewById(R.id.chordsPlay);
		pauseButton		= findViewById(R.id.chordsPause);
//		topTitle		= (TextView) findViewById(R.id.topTitle);
//		topSubtitle		= (TextView) findViewById(R.id.topSubtitle);
		scrollChords	= (ScrollView) findViewById(R.id.scrollChords);
		textChords		= (MyTextView) findViewById(R.id.textChords);
		speedSeek		= (SeekBar) findViewById(R.id.chordsPlaySeek);
		loadingProgress	= (ProgressBar) findViewById(R.id.loadingProgress);

		baseTextSize = textChords.getTextSize();

		autoscroller = new Autoscroller(this, scrollChords, new Autoscroller.AutoscrollerListener() {
			@Override
			public void onStop() {
				onAutoscrollStop();
			}
		});

		speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

				fullScreenIfIdle();
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				int p = seekBar.getProgress();
				int m = seekBar.getMax();

				setScrollVelocity(
						minAutoscrollSpeed + (maxAutoscrollSpeed - minAutoscrollSpeed) * ((float) p / (float) m));

				fullScreenIfIdle();
			}
		});

		initStyles();

//		topSpace.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//
//			@Override
//			public void onGlobalLayout() {
//
//				// save some vertical space for nav bar
//
//				// padding works only here... :/
//				topSpace.setPadding(0, DisplayInfo.notificationBarHeight, 0, 0);
//			}
//		});

		scaleGestureDetector = new ScaleGestureDetector(this, new ChordsScaleListener());

		final GestureDetector gestureDetector = new GestureDetector(this, new ChordsGestureListener());
		scrollChords.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				System.out.println("scroll touch");

				scaleGestureDetector.onTouchEvent(event);
				gestureDetector.onTouchEvent(event);
				return false;
			}
		});

		scrollChords.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
//				fullScreen();
			}
		});

		fullScreen(false);

		loadSettings();

		Uri uri = getIntent().getData();

		Log.d("kan", "url is [" + uri + "]");

		Chords sc = state.chords;

		if (sc == null || uri == null || !sc.url.equals(uri.toString())) {

			// load new chords

			// don't restore previous chords state if any
			state.chords = null;
			state.scrolling = false;

			loadChords(uri);
		}
		else {
			// restore previous chords if any

			if (state.chords != null) {
				setChordsText(state.chords);
			}

			stateStatus = EState.CHORDS;
		}

	}

	private void initStyles() {

		TypedArray themeAttrs = obtainStyledAttributes(
				new int[]{
						android.R.attr.actionBarSize,
						android.R.attr.actionBarStyle});

		actionBarHeight = themeAttrs.getDimensionPixelSize(0, 0);
		int abStyleId = themeAttrs.getResourceId(1, 0);

		System.out.println("action bar height is " + themeAttrs.getDimensionPixelSize(0, 0) + " " + themeAttrs.getDimension(0, 0));

		System.out.println("action bar style id is " + abStyleId + " (" + getResources().getResourceName(abStyleId) + ")");

		themeAttrs.recycle();

//		topBar.setMinimumHeight(actionBarHeight);


		if (abStyleId != 0) {
			// Widget.Holo.ActionBar
			TypedArray abAttrs = obtainStyledAttributes(abStyleId,
					new int[]{
						android.R.attr.background,
						android.R.attr.titleTextStyle,
						android.R.attr.subtitleTextStyle,
						android.R.attr.height,
						android.R.attr.paddingTop,
						android.R.attr.paddingLeft,
						android.R.attr.paddingBottom,
						android.R.attr.paddingRight});

			Drawable bgDrawable = abAttrs.getDrawable(0);
			int titleStyleId = abAttrs.getResourceId(1, 0);
			int subtitleStyleId = abAttrs.getResourceId(2, 0);

			int actionBarStyleHeight = abAttrs.getInteger(3, 0);
			System.out.println("action bar height is " + actionBarHeight + " " + actionBarStyleHeight);
			System.out.println("action bar padding is " +
					abAttrs.getInteger(4, 0) + " " +
					abAttrs.getInteger(5, 0) + " " +
					abAttrs.getInteger(6, 0) + " " +
					abAttrs.getInteger(7, 0) + " ");

			abAttrs.recycle();

//			topRoot.setBackground(bgDrawable);
//			topBar.setBackground(bgDrawable);
			bottomBar.setBackground(bgDrawable);

			if (titleStyleId != 0) {
				// TextAppearance.Holo.Widget.ActionBar.Title
				TypedArray tsAttrs = obtainStyledAttributes(titleStyleId,
						new int[]{
							android.R.attr.textSize,
							android.R.attr.textColor});

				int titleSize = tsAttrs.getDimensionPixelSize(0, 0);
				int titleColor = tsAttrs.getColor(1, 0);

				System.out.println("title color is " + titleColor);

				tsAttrs.recycle();

//				if (titleSize != 0)
//					topTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);
//
//				if (titleColor != 0)
//					topTitle.setTextColor(titleColor);
			}

			if (subtitleStyleId != 0) {
				TypedArray ssAttrs = obtainStyledAttributes(subtitleStyleId,
					new int[]{
						android.R.attr.textSize,
						android.R.attr.textColor});

				int titleSize = ssAttrs.getDimensionPixelSize(0, 0);
				int titleColor = ssAttrs.getColor(1, 0);

				ssAttrs.recycle();

//				if (titleSize != 0)
//					topSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);
//
//				if (titleColor != 0)
//					topSubtitle.setTextColor(titleColor);
			}
		}

	}

	private void loadChords(Uri uri) {

		new LoadChordsTask().execute(uri.toString());

		loadingProgress.setVisibility(View.VISIBLE);

		stateStatus = EState.LOADING;
	}

	private void onChordsLoaded(Chords chords) {

		loadingProgress.setVisibility(View.GONE);

		stateStatus = EState.CHORDS;

		state.chordsOffset = 0;
		state.chords = chords;

		setChordsText(chords);
	}

	private void setChordsText(Chords chords) {
		SpannableStringBuilder ssb = new SpannableStringBuilder(chords.text);
		for (ChordMark cm : chords.chordMarks) {
			ssb.setSpan(new ForegroundColorSpan(Color.rgb(48, 48, 192)), cm.from, cm.from + cm.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		textChords.setText(ssb);

		String title = chords.title;
		if (title == null)
			title = chords.text.substring(0, 15);

		ActionBar ab = getActionBar();
		ab.setTitle(title);
		ab.setSubtitle(chords.author);

//		topTitle.setText(chords.title);
//		topSubtitle.setText(chords.author);
	}

	private void fullScreen(boolean animated) {

		if (fullScreen)
			return;

		if (animated) {
//			topRoot.setVisibility(View.VISIBLE);
			bottomBar.setVisibility(View.VISIBLE);

//			TranslateAnimation tta = new TranslateAnimation(0, 0, 0, -topRoot.getHeight());
//			tta.setDuration(shortAnimTime);
//			tta.setInterpolator(AnimationUtils.loadInterpolator(this, animationInterpolatorId));
//			tta.setAnimationListener(new AnimationListener() {
//				@Override
//				public void onAnimationEnd(Animation animation) {
//					topRoot.setVisibility(View.GONE);
//				}
//			});
//			topRoot.startAnimation(tta);

			TranslateAnimation bta = new TranslateAnimation(0, 0, 0, bottomBar.getHeight());
			bta.setDuration(shortAnimTime);
			bta.setInterpolator(AnimationUtils.loadInterpolator(this, animationInterpolatorId));
			bta.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					bottomBar.setVisibility(View.GONE);
				}
			});
			bottomBar.startAnimation(bta);
		} else {
//			topRoot.setVisibility(View.GONE);
			bottomBar.setVisibility(View.GONE);
		}

		getActionBar().hide();

//		getWindow().clearFlags(
//				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		scrollChords.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LOW_PROFILE);

		fullScreen = true;
	}

	private void navigationScreen() {

		if (!fullScreen)
			return;

		if (stateStatus == EState.CHORDS) {
//			topRoot.setVisibility(View.VISIBLE);
			bottomBar.setVisibility(View.VISIBLE);

//			TranslateAnimation tta = new TranslateAnimation(0, 0, -topRoot.getHeight(), 0);
//			tta.setDuration(shortAnimTime);
//			tta.setInterpolator(AnimationUtils.loadInterpolator(this, animationInterpolatorId));
//			tta.setAnimationListener(new AnimationListener() {
//				@Override
//				public void onAnimationStart(Animation animation) {
//					topRoot.setVisibility(View.VISIBLE);
//				}
//			});
//			topRoot.startAnimation(tta);

			TranslateAnimation bta = new TranslateAnimation(0, 0, bottomBar.getHeight(), 0);
			bta.setDuration(shortAnimTime);
			bta.setInterpolator(AnimationUtils.loadInterpolator(this, animationInterpolatorId));
			bta.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					bottomBar.setVisibility(View.VISIBLE);
				}
			});
			bottomBar.startAnimation(bta);
		}

		getActionBar().show();

//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		scrollChords.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		fullScreen = false;

		fullScreenIfIdle();
	}

	private void toggleScreen() {

		if (fullScreen)
			navigationScreen();
		else
			fullScreen(true);
	}

	private void fullScreenIfIdle() {

		if (fullScreenTimer != null) {
			fullScreenTimer.cancel();
			fullScreenTimer.purge();
		}

		fullScreenTimer = new Timer();
		fullScreenTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						fullScreen(true);
					}
				});
			}
		}, 5000);

	}

	@Override
	protected void onStart() {
		super.onStart();

		System.out.println("onStart()");

		textChords.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextSize * state.textScale);
	}

	@Override
	protected void onResume() {
		super.onResume();

		System.out.println("onResume()");

		System.out.println("text clickable is " + textChords.isClickable());

		// TODO - resume full screen timer

		if (state.scrolling)
			autoscroller.start();
	}

	@Override
	protected void onPause() {
		super.onPause();

		System.out.println("onPause()");

		// set text view selection to the visible area
		// text view always scrolls to selection when restored, want it to be visible
		int o = textChords.getOffsetForPosition(0, scrollChords.getScrollY());

		if (textChords.getText() instanceof Spannable)
			Selection.setSelection((Spannable)textChords.getText(), o);

		if (fullScreenTimer != null) {
			fullScreenTimer.cancel();
			fullScreenTimer.purge();
			fullScreenTimer = null;
		}

		state.scrolling = autoscroller.isRunning();

		autoscroller.stop();
	}

	@Override
	protected void onStop() {
		super.onStop();

		System.out.println("onStop()");

		saveSettings();
	}

	@Override
	protected void onDestroy() {
		System.out.println("onDestroy()");

		state.chordsOffset = textChords.getOffsetForPosition(0, scrollChords.getScrollY());

		super.onDestroy();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (state.chords != null) {
			scrollChords.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					Layout layout = textChords.getLayout();

					final Rect r = new Rect();
					int line = layout.getLineForOffset(state.chordsOffset);
					layout.getLineBounds(line, r);

					scrollChords.scrollTo(0, r.top);

					scrollChords.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});
		}

		System.out.println("onRestoreInstanceState()");

		new Throwable().printStackTrace(System.out);

		System.out.println("saved state is " + System.identityHashCode(savedInstanceState));

		System.out.println("autoscroll velocity is " + savedInstanceState.getFloat("autoscrollVelocity"));

		System.out.println("selection is " + textChords.getSelectionStart());

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		System.out.println("onSaveInstanceState()");
		new Throwable().printStackTrace(System.out);

		System.out.println("out state is " + System.identityHashCode(outState));

		outState.putFloat("autoscrollVelocity", autoscroller.getScrollVelocity());
	}

	private void saveSettings() {

		SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
		Editor editor = preferences.edit();

		editor.putFloat("autoscrollVelocity", autoscroller.getScrollVelocity());

		editor.commit();
	}

	private void loadSettings() {

		SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

		setScrollVelocity(preferences.getFloat("autoscrollVelocity", 10));
	}

	private void setTextScale(float scale) {
		state.textScale = min(max(scale, MIN_TEXT_SCALE), MAX_TEXT_SCALE);
	}

	private void setScrollVelocity(float velocity) {

		float v = max(minAutoscrollSpeed, Math.min(velocity, maxAutoscrollSpeed));

		autoscroller.setScrollVelocity(v);

		int m = speedSeek.getMax();

		int p = Math.round((v - minAutoscrollSpeed) / (maxAutoscrollSpeed - minAutoscrollSpeed) * m);

		if (p != speedSeek.getProgress())
			speedSeek.setProgress(p);
	}

	private void zoomText(final float pivotX, final float pivotY) {
		zoomText(pivotX, pivotY, false);
	}

	private void zoomText(final float pivotX, final float pivotY, boolean fitLine) {

		final int p = textChords.getOffsetForPosition(
				pivotX, pivotY + scrollChords.getScrollY());

		// scale text size
		// text view will keep new size word wrapped so horizontal scroll is avoided
		textChords.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextSize * state.textScale);

		if (fitLine) {

			float dw = Layout.getDesiredWidth(state.chords.text, textChords.getPaint());

			while (dw > textChords.getWidth()) {
				float ts = state.textScale;

				setTextScale(state.textScale * 0.95f);
				if (ts == state.textScale) {
					// hit zoom limits
					break;
				}

				textChords.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextSize * state.textScale);

				dw = Layout.getDesiredWidth(state.chords.text, textChords.getPaint());
			}
		}

		System.out.println("text width is " + textChords.getWidth());

		// preserve pivot position
		textChords.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				System.out.println("on global layout");

				Layout layout = textChords.getLayout();
				int line = layout.getLineForOffset(p);

				Rect r = new Rect();
				layout.getLineBounds(line, r);

				scrollChords.scrollTo(0, (int) ((r.bottom + r.top) / 2.0 - pivotY));

				textChords.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});

	}

	private void resetTextZoom(final float pivotX, final float pivotY) {

		state.textScale = 1.0f;

		zoomText(pivotX, pivotY);
	}

	public void zoomToLongestLine(final float pivotX, final float pivotY) {

		if (state.chords == null)
			return;

		float w = Layout.getDesiredWidth(state.chords.text, textChords.getPaint());

		System.out.println("desired width is " + w);

		int cw = textChords.getWidth();

		setTextScale(state.textScale / w * cw);
		zoomText(pivotX, pivotY, true);
	}

	public void onPlayClick(View v) {

		fullScreen(true);
		startAutoscroll();
	}

	public void onPauseClick(View v) {

		fullScreenIfIdle();
		stopAutoscroll();
	}

	private void startAutoscroll() {
		autoscroller.start();

		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.VISIBLE);

		initStyles();
	}

	private void stopAutoscroll() {
		// also calls onAutoscrollStop
		autoscroller.stop();
	}

	private void onAutoscrollStop() {
		// called by autoscroller.stop()
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}
}
