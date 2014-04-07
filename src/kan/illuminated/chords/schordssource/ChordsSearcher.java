package kan.illuminated.chords.schordssource;

import kan.illuminated.chords.Chords;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ChordsSearcher {

	public interface ChordsResultListener {

		void onSearchUpdated(List<Chords> newChords, boolean last);
	}

	protected static final int  READY       = 0;
	protected static final int  RUNNING     = 1;
	protected static final int  CANCELLED   = 2;
	protected static final int  DONE        = 3;

	protected AtomicInteger state = new AtomicInteger(READY);

	protected ChordsResultListener chordsResultListener;

	public void query(String query) {

	}

	public void isQueryRunning() {
		// TODO
	}

	public void cancelQuery() {
		// TODO
	}

	public void setChordsResultListener(ChordsResultListener chordsResultListener) {
		this.chordsResultListener = chordsResultListener;
	}

	public void removeChordsResultListener() {
		chordsResultListener = null;
	}
}
