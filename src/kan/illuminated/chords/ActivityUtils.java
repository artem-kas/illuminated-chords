package kan.illuminated.chords;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import kan.illuminated.chords.activity.ChordsActivity;

/**
 * @author KAN
 */
public class ActivityUtils {

	public static void showChordsActivity(Activity invokerActivity, Chords chords) {

		Intent i = new Intent(invokerActivity, ChordsActivity.class);
		i.setData(Uri.parse(chords.url));

		invokerActivity.startActivity(i);
	}
}
