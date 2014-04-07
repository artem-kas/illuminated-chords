package kan.illuminated.chords;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class MyTextView extends TextView {

	public MyTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		System.out.println("new my text view");
	}

	public MyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

		System.out.println("new my text view");
	}

	public MyTextView(Context context) {
		super(context);

		System.out.println("new my text view");
	}


}
