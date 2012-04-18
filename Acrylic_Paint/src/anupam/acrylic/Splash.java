package anupam.acrylic;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import anupam.acrylic.R;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Thread t = new Thread() {

			public void run() {
				try {

					Thread.sleep(3000);

					Intent i = new Intent();
					i.setClassName("anupam.acrylic",
							"anupam.acrylic.EasyPaint");
					startActivity(i);
					finish();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		};
		t.start();
	}

}
