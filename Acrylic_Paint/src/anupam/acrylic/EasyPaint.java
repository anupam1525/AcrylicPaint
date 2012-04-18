/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package anupam.acrylic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import anupam.acrylic.R;

public class EasyPaint extends GraphicsActivity implements
		ColorPickerDialog.OnColorChangedListener {

	RelativeLayout rl;
	TextView txt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(new MyView(this));

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(0xFFF00000);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(5);

		if (isFirstTime()) {

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Acrylic Paint - Info");

			String msg1 = "Acrylic Paint is a coloring tool which helps you to make anything of your imagination. "
					+ "Pick your colors and start painting.";
			String msg2 = "";
			String msg3 = "";
			String msg4 = "Press menu for more options !!";
			String msg5 = "Happy Coloring !!!";

			alert.setMessage(msg1 + "\n" + msg2 + "\n" + msg3 + "\n" + msg4
					+ "\n" + msg5);

			alert.setNegativeButton("Continue",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							Toast.makeText(getApplicationContext(),
									"Here is your canvas. Start coloring !!",
									Toast.LENGTH_LONG).show();

						}
					});

			alert.show();
		}
		if (isSecondTime()) {
			Toast.makeText(getApplicationContext(),
					"Here is your canvas. Start coloring !!", Toast.LENGTH_LONG)
					.show();
		}

	}

	private Paint mPaint;
	private int bgColor = 0xFFFFFFFF;

	public void colorChanged(int color) {
		mPaint.setColor(color);
	}

	public class MyView extends View {

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;

		public MyView(Context c) {
			super(c);

			mBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(0xFFFFFFFF);

			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			canvas.drawPath(mPath, mPaint);
		}

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 4;

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();
				break;
			}
			return true;
		}
	}

	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int ERASE_MENU_ID = Menu.FIRST + 1;
	private static final int CLEAR_ALL = Menu.FIRST + 2;
	private static final int SAVE = Menu.FIRST + 3;
	private static final int SHARE = Menu.FIRST + 4;
	private static final int ABOUT = Menu.FIRST + 5;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, COLOR_MENU_ID, 0, "Color").setIcon(R.drawable.color);
		menu.add(0, ERASE_MENU_ID, 0, "Erase").setIcon(R.drawable.erase);
		menu.add(0, CLEAR_ALL, 0, "Clear All").setIcon(R.drawable.clear_all);
		menu.add(0, SAVE, 0, "Save").setIcon(R.drawable.save);
		menu.add(0, SHARE, 0, "Share").setIcon(R.drawable.share);
		menu.add(0, ABOUT, 0, "About").setIcon(R.drawable.about);

		/****
		 * Is this the mechanism to extend with filter effects? Intent intent =
		 * new Intent(null, getIntent().getData());
		 * intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		 * menu.addIntentOptions( Menu.ALTERNATIVE, 0, new ComponentName(this,
		 * NotesList.class), null, intent, 0, null);
		 *****/
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mPaint.setXfermode(null);
		mPaint.setAlpha(0xFF);

		switch (item.getItemId()) {
		case COLOR_MENU_ID:
			new ColorPickerDialog(this, this, mPaint.getColor()).show();
			return true;
		case ERASE_MENU_ID:
			mPaint.setColor(bgColor);
			return true;
		case CLEAR_ALL:
			Intent intent = getIntent();
			overridePendingTransition(0, 0);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0);
			startActivity(intent);
			return true;
		case SAVE:
			takeScreenshot(true);
			break;
		case SHARE:
			File screenshotPath = takeScreenshot(false);
			Intent i = new Intent();
			i.setAction(Intent.ACTION_SEND);
			i.setType("image/png");
			i.putExtra(Intent.EXTRA_SUBJECT,
					getString(anupam.acrylic.R.string.share_title_template));
			i.putExtra(Intent.EXTRA_TEXT,
					getString(anupam.acrylic.R.string.share_text_template));
			i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenshotPath));
			try {
				startActivity(Intent.createChooser(i,
						getString(anupam.acrylic.R.string.toolbox_share_title)));
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(this.getApplicationContext(),
						anupam.acrylic.R.string.no_way_to_share,
						Toast.LENGTH_LONG).show();
			}
			break;
		case ABOUT:
			try {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle("Acrylic Paint - Info");

				String msg1 = "- Acrylic Paint is a coloring tool for kids. Pick your colors and start painting.";
				String msg2 = "- You can save your creation or directly share it from your device.";

				alert.setMessage(msg1 + "\n" + msg2);

				alert.setPositiveButton("Rate It.",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setData(Uri
										.parse("market://details?id=anupam.acrylic"));
								startActivity(intent);

							}
						});

				alert.setNegativeButton("No, Thanks.",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						});

				alert.show();
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		return super.onOptionsItemSelected(item);
	}

	private File takeScreenshot(boolean showToast) {
		View v = getWindow().getDecorView();

		v.setDrawingCacheEnabled(true);
		Bitmap cachedBitmap = v.getDrawingCache();
		Bitmap copyBitmap = cachedBitmap.copy(Bitmap.Config.RGB_565, true);
		FileOutputStream output = null;
		File file = null;
		try {
			File path = Places.getScreenshotFolder();
			Calendar cal = Calendar.getInstance();

			file = new File(path,

			cal.get(Calendar.YEAR) + "_" + (1 + cal.get(Calendar.MONTH)) + "_"
					+ cal.get(Calendar.DAY_OF_MONTH) + "_"
					+ cal.get(Calendar.HOUR_OF_DAY) + "_"
					+ cal.get(Calendar.MINUTE) + "_" + cal.get(Calendar.SECOND)
					+ ".png");
			output = new FileOutputStream(file);
			copyBitmap.compress(CompressFormat.PNG, 100, output);
		} catch (FileNotFoundException e) {
			file = null;
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		if (file != null) {
			if (showToast)
				Toast.makeText(getApplicationContext(),
						"Saved your creation to " + file.getAbsolutePath(),
						Toast.LENGTH_LONG).show();
			// sending a broadcast to the media scanner so it will scan the new
			// screenshot.
			Intent requestScan = new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			requestScan.setData(Uri.fromFile(file));
			sendBroadcast(requestScan);

			return file;
		} else {
			return null;
		}
	}

	private boolean isFirstTime() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		boolean ranBefore = preferences.getBoolean("RanBefore", false);
		if (!ranBefore) {
			// first time
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("RanBefore", true);
			editor.commit();
		}
		return !ranBefore;
	}

	private boolean isSecondTime() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		boolean ranBefore = preferences.getBoolean("SecondRun", true);
		if (ranBefore) {
			// seond time
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("SecondRun", false);
			editor.commit();
		}
		return !ranBefore;
	}

	public boolean connectionAvailable() {
		boolean connected = false;
		@SuppressWarnings("static-access")
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
		if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState() == NetworkInfo.State.CONNECTED
				|| connectivityManager.getNetworkInfo(
						ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
			connected = true;
		}
		return connected;
	}
}
