package kan.illuminated.chords;

import android.app.Application;
import android.content.Context;

/**
 * @author KAN
 */
public class ChordsApplication extends Application {

	public static Context appContext;

	@Override
	public void onCreate() {

		System.out.println("creating chords application");

		super.onCreate();

		appContext = getApplicationContext();
	}
}
