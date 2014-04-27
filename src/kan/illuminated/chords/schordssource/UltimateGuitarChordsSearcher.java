package kan.illuminated.chords.schordssource;

import android.os.Handler;
import android.util.Log;
import kan.illuminated.chords.Chords;
import kan.illuminated.chords.net.PersistentTask;
import kan.illuminated.chords.net.PersistentTask.PersistentTaskException;
import kan.illuminated.chords.schordssource.UltimateGuitarSearchRequest.UltimateGuitarSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kan
 */
public class UltimateGuitarChordsSearcher extends ChordsSearcher {

	private static final String TAG = UltimateGuitarChordsSearcher.class.getSimpleName();

	private static class UltimateGuitarQuery {
		private String  query;
		private Integer page;

		private UltimateGuitarQuery(String query, Integer page) {
			this.query = query;
			this.page = page;
		}
	}

	private class UltimateGuitarSearchTask extends PersistentTask<UltimateGuitarQuery, UltimateGuitarSearchResult> {

		private UltimateGuitarQuery query;

		@Override
		public UltimateGuitarSearchResult run(UltimateGuitarQuery query) {
			this.query = query;
			return runRequest(query);
		}

		@Override
		public void onResponseTaskThread(UltimateGuitarSearchResult out) {
			onSearchReadyTaskThread(query, out);
		}

	}

	private static class DelayedResult {
		UltimateGuitarQuery         query;
		UltimateGuitarSearchResult  requestChords;
		boolean                     last;
	}

	// must be used from ui thread
	private List<Chords> chords = new ArrayList<Chords>();

	// known requests for current query by pages
	// guarded by itself
	private final ArrayList<UltimateGuitarSearchTask> requests = new ArrayList<UltimateGuitarSearchTask>();

	// posted to ui thread but not handled yet
	private final Set<UltimateGuitarSearchResult> pendingResponses = Collections.newSetFromMap(new ConcurrentHashMap<UltimateGuitarSearchResult, Boolean>());

	// ready results waiting in ui thread for the listener
	private Queue<DelayedResult> delayedResults = new LinkedList<DelayedResult>();

	private final Handler handler = new Handler();

	private volatile String query;


	private boolean canRunTasks() {
		int s = state.get();
		return s == READY || s == RUNNING;
	}

	@Override
	public void query(String query) {

		if (!state.compareAndSet(READY, RUNNING)) {
			throw new RuntimeException("this query is already running");
		}

		this.query = query;

		registerTask(new UltimateGuitarQuery(query, 1));
		runTask(1);
	}

	public String getQuery() {
		return query;
	}

	private void registerTask(UltimateGuitarQuery query) {

		if (!canRunTasks())
			return;

		synchronized (requests) {

			UltimateGuitarSearchTask task = new UltimateGuitarSearchTask();

			while (query.page + 1 > requests.size()) {
				requests.add(null);
			}

			if (requests.get(query.page) != null) {
				return;
			}

			requests.set(query.page, task);

			// search just got cancelled
			if (!canRunTasks()) {
				requests.set(query.page, null);
			}
		}

	}

	private void runTask(Integer page) {

		if (!canRunTasks())
			return;

		UltimateGuitarSearchTask task;
		synchronized (requests) {
			task = requests.get(page);
		}

		task.execute(new UltimateGuitarQuery(query, page));

		if (!canRunTasks()) {
			task.cancel();
		}
	}

	@Override
	public void cancelQuery() {

		// may cancel running, ready or just just started tasks
		if (state.compareAndSet(RUNNING, CANCELLED) ||
				state.compareAndSet(READY, CANCELLED) ||
				state.compareAndSet(RUNNING, CANCELLED)) {

			synchronized (requests) {
				for (UltimateGuitarSearchTask ugst : requests) {
					if (ugst != null) {
						ugst.cancel();
					}
				}
			}
		}
	}

	/**
	 * @return true if at least one query have not completed yet
	 */
	public boolean haveUncompletedTasks(Integer currentPage) {

		if (!canRunTasks()) {
			return false;
		}

		synchronized (requests) {

			if (requests.isEmpty()) {
				return true;
			}

			for (int i = 0; i < requests.size(); i++) {

				if (currentPage != null && i == currentPage.intValue()) {
					// request for current page may still be finishing,
					// count it as completed
					continue;
				}

				UltimateGuitarSearchTask ugst = requests.get(i);

				if (ugst != null && !ugst.isCompleted())
					return true;
			}
		}

		return false;
	}

	public boolean isInProgress() {

		synchronized (requests) {
			for (UltimateGuitarSearchTask ugst : requests) {
				if (ugst != null && ugst.isRunning())
					return true;
			}
		}

		return false;
	}

	// running in a worker thread
	private UltimateGuitarSearchResult runRequest(UltimateGuitarQuery query) {

		UltimateGuitarSearchRequest sr = new UltimateGuitarSearchRequest();
		try {
			return sr.search(query.query, query.page);
		} catch (Exception e) {
			// TODO - log
			e.printStackTrace();

			throw new PersistentTaskException(e);
		}
	}

	public void fetchNext() {
		if (isInProgress() || !canRunTasks()) {
			return;
		}

		UltimateGuitarSearchTask task = null;
		int p;
		synchronized (requests) {
			for (p = 0; p < requests.size(); p++) {
				UltimateGuitarSearchTask ugst = requests.get(p);
				if (ugst != null && ugst.isNew()) {
					task = ugst;
					break;
				}
			}
		}

		if (task == null) {
			return;
		}

		task.execute(new UltimateGuitarQuery(query, p));

		if (!canRunTasks()) {
			task.cancel();
		}
	}

	private void onSearchReadyTaskThread(
			final UltimateGuitarQuery query, final UltimateGuitarSearchResult requestChords) {

		// redraw current list within ui thread

		pendingResponses.add(requestChords);

		handler.post(new Runnable() {
			@Override
			public void run() {
				onSearchReadyUiThread(query, requestChords);
			}
		});

		// query other pages

		for (Integer p : requestChords.availablePages) {
			registerTask(new UltimateGuitarQuery(query.query, p));
		}
	}

	private void onSearchReadyUiThread(
			UltimateGuitarQuery query, UltimateGuitarSearchResult requestChords) {

		System.out.println("onSearchReadyUiThread()");

		// (re)draw chords list

		pendingResponses.remove(requestChords);

		// TODO - failed task should also call this method to hide loading bar

		// may include 'future' chords for delayed responses
		chords.addAll(requestChords.chordsList);

		// check if this is the last chords set
		//  no requests are running or queued and no responses are waiting for the ui thread

		if (chordsResultListener != null) {
			chordsResultListener.onSearchUpdated(requestChords.chordsList, isProcessingLastResponse(query.page));
		} else {
			Log.d(TAG, "search result queued");

			DelayedResult dr = new DelayedResult();
			dr.query            = query;
			dr.requestChords    = requestChords;
			dr.last             = isProcessingLastResponse(query.page);
			delayedResults.add(dr);
		}
	}

	private boolean isProcessingLastResponse(Integer page) {

		if (haveUncompletedTasks(page)) {
			return false;
		} else {
			// all tasks may be completed but not handled yet

			return pendingResponses.isEmpty();
		}
	}

	@Override
	public void setChordsResultListener(ChordsResultListener chordsResultListener) {
		super.setChordsResultListener(chordsResultListener);

		DelayedResult dr;
		while ((dr = delayedResults.poll()) != null) {

			Log.d(TAG, "processing delayed result with new listener");

			chordsResultListener.onSearchUpdated(dr.requestChords.chordsList, dr.last);
		}
	}
}
