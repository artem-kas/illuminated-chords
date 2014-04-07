package kan.illuminated.chords;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpQuery {

	public InputStream query(String url) {

		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			return conn.getInputStream();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
