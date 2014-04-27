package kan.illuminated.chords.activity;

/**
 * @author KAN
 */
public abstract class BackgroundTask<Result> {

	public static interface ResultListener<Result> {
		void onBackgroundTaskReady(Result result);
		void onBackgroundTaskFailed(Exception e);
	}

	protected ResultListener<Result> resultListener;

	public void setResultListener(ResultListener<Result> resultListener) {
		this.resultListener = resultListener;
	}

	public void removeResultListener() {
		resultListener = null;
	}

	public abstract void cancel();
}
