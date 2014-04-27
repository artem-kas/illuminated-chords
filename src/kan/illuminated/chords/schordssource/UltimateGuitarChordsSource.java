package kan.illuminated.chords.schordssource;

import kan.illuminated.chords.Chords;
import kan.illuminated.chords.activity.BackgroundTask;
import kan.illuminated.chords.data.ChordsDb;
import kan.illuminated.chords.data.ChordsHistory;
import kan.illuminated.chords.net.PersistentTask;

public class UltimateGuitarChordsSource extends BackgroundTask<Chords> {

	private static final String TAG = UltimateGuitarChordsSource.class.getName();

	private class ReadChordsTask extends PersistentTask<String, Chords> {

		@Override
		protected Chords run(String in) {
			UltimateGuitarChordsRequest ugcr = new UltimateGuitarChordsRequest();
			Chords chords = ugcr.getChords(in);
			return chords;
		}

		@Override
		protected void onResponseTaskThread(Chords chords) {
			ChordsHistory chordsHistory = ChordsDb.chordsDb().chordsHistory();
			chordsHistory.storeChords(chords);
		}

		@Override
		protected void onFailTaskThread(Exception e) {
		}

		@Override
		protected void onResponseCallerThread(Chords chords) {
			readyChords = chords;

			if (resultListener != null) {
				resultListener.onBackgroundTaskReady(chords);
			}
		}

		@Override
		protected void onFailCallerThread(Exception e) {
			if (resultListener != null) {
				resultListener.onBackgroundTaskFailed(e);
			} else {
				// TODO - remember exception
			}
		}
	}

	private class RefreshChordsTask extends PersistentTask<String, Chords> {

		@Override
		protected Chords run(String in) {
			UltimateGuitarChordsRequest ugcr = new UltimateGuitarChordsRequest();
			Chords chords = ugcr.getChords(in);
			return chords;
		}

		@Override
		protected void onResponseTaskThread(Chords chords) {
			ChordsHistory chordsHistory = ChordsDb.chordsDb().chordsHistory();
			chordsHistory.refreshChords(chords);
		}

		@Override
		protected void onFailTaskThread(Exception e) {
		}
	}


	private String  url;

	private ReadChordsTask      readChordsTask;
	private RefreshChordsTask   refreshChordsTask;

	private Chords readyChords;


	public String getUrl() {
		return url;
	}

	public Chords getChords() {
		return readyChords;
	}

	public Chords requestChords(String url) {

		this.url = url;

		ChordsHistory chordsHistory = ChordsDb.chordsDb().chordsHistory();

		Chords storedChords = chordsHistory.readChords(url);
		if (storedChords != null) {

			if (needChordsRefresh(storedChords)) {
				refreshChordsTask = new RefreshChordsTask();
				refreshChordsTask.execute(url);
			}

			readyChords = storedChords;

			return storedChords;
		}

		readChordsTask = new ReadChordsTask();
		readChordsTask.execute(url);

		return null;
	}

	private boolean needChordsRefresh(Chords chords) {

		long pause = chords.lastRead.getTime() - chords.previousRead.getTime();

		// refresh if chords are older than an hour
		return pause > 1000 * 60 * 60;
	}

	@Override
	public void setResultListener(ResultListener<Chords> resultListener) {
		super.setResultListener(resultListener);
	}

	@Override
	public void cancel() {
		if (readChordsTask != null) {
			readChordsTask.cancel();
		}
		if (refreshChordsTask != null) {
			refreshChordsTask.cancel();
		}
	}

}
