/*
 * Copyright (C) 2014 Valerio Bozzolan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package anupam.acrylic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class EasyPaint extends GraphicsActivity implements
		ColorPickerDialog.OnColorChangedListener {

	private Paint mPaint;
	private MaskFilter mEmboss;
	private MaskFilter mBlur;

	public static int DEFAULT_BRUSH_SIZE = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// it removes the title from the actionbar(more space for icons?)
		// this.getActionBar().setDisplayShowTitleEnabled(false);

		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		// it removes the navigation bar, but you can't paint without it and
		// once it is shown again, it doesn't hide again

		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		setContentView(new MyView(this));

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.GREEN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(DEFAULT_BRUSH_SIZE);

		mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);

		mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);

		if (isFirstTime()) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.app_name);
			alert.setMessage(R.string.app_description);
			alert.setNegativeButton(R.string.continue_fuck,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Toast.makeText(getApplicationContext(),
									R.string.here_is_your_canvas,
									Toast.LENGTH_SHORT).show();
						}
					});

			alert.show();
		} else {
			Toast.makeText(getApplicationContext(),
					R.string.here_is_your_canvas, Toast.LENGTH_SHORT).show();
		}

	}

	public void colorChanged(int color) {
		mPaint.setColor(color);
	}

	public class MyView extends View {

		private class SuperPath extends Path {
			private Integer idPointer;
			private float lastX;
			private float lastY;

			SuperPath() {
				this.idPointer = null;
			}

			public void setLastXY(float x, float y) {
				this.lastX = x;
				this.lastY = y;
			}

			public float getLastX() {
				return lastX;
			}

			public float getLastY() {
				return lastY;
			}

			public void touchStart(float x, float y) {
				this.reset();
				this.moveTo(x, y);
				this.lastX = x;
				this.lastY = y;
			}

			public void touchMove(float x, float y) {
				float dx = Math.abs(x - lastX);
				float dy = Math.abs(y - lastY);
				if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
					this.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
					lastX = x;
					lastY = y;
				}
			}

			public boolean isFreeFromPointer() {
				return idPointer == null;
			}

			public boolean isRelatedToPointer(int idPointer) {
				return this.idPointer != null
						&& (int) this.idPointer == idPointer;
			}

			public void freeFromPointer() {
				idPointer = null;
			}

			public void setRelatedPointer(int idPointer) {
				this.idPointer = idPointer;
			}
		}

		private class SuperMultiPathManager {
			public SuperPath[] superMultiPaths;

			SuperMultiPathManager(int points) {
				superMultiPaths = new SuperPath[points];
				for (int i = 0; i < points; i++) {
					superMultiPaths[i] = new SuperPath();
				}
			}

			public SuperPath getSuperPathRelatedToPointer(int id) {
				for (int i = 0; i < superMultiPaths.length; i++) {
					if (superMultiPaths[i].isRelatedToPointer(id)) {
						return superMultiPaths[i];
					}
				}
				return null;
			}

			public SuperPath addSuperPathRelatedToPointer(int id) {
				for (int i = 0; i < superMultiPaths.length; i++) {
					if (superMultiPaths[i].isFreeFromPointer()) {
						superMultiPaths[i].setRelatedPointer(id);
						return superMultiPaths[i];
					}
				}
				Log.e("anupam", "anupamdio Tutte le dita usate???");
				return null;
			}
		}

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;
		private SuperMultiPathManager superMultiPathManager;

		public MyView(Context c) {
			super(c);

			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;

			mBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			superMultiPathManager = new SuperMultiPathManager(5);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(0xFFFFFFFF);
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
			for(int i=0; i<superMultiPathManager.superMultiPaths.length; i++) {
				canvas.drawPath(superMultiPathManager.superMultiPaths[i], mPaint);
			}
		}

		private static final float TOUCH_TOLERANCE = 4;

		/*
		 * private void touch_start(float x, float y) { mPath.reset();
		 * mPath.moveTo(x, y); mX = x; mY = y; }
		 */

		/*
		 * private void touch_move(float x, float y) { float dx = Math.abs(x -
		 * mX); float dy = Math.abs(y - mY); if (dx >= TOUCH_TOLERANCE || dy >=
		 * TOUCH_TOLERANCE) { mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
		 * mX = x; mY = y; } }
		 */

		/*
		 * private void touch_up() { mPath.lineTo(mX, mY); // commit the path to
		 * our offscreen mCanvas.drawPath(mPath, mPaint); // kill this so we
		 * don't double draw mPath.reset(); }
		 */

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			SuperPath superPath;
			int index = event.getActionIndex();
			int id = event.getPointerId(index);
			int qualcosa = event.getActionMasked();
			if (qualcosa == MotionEvent.ACTION_DOWN
					|| qualcosa == MotionEvent.ACTION_POINTER_DOWN) {
				Log.d("anupam", "anupamdio GIÙ " + id);
				superPath = superMultiPathManager
						.addSuperPathRelatedToPointer(id);
				if (superPath == null) {
					Log.e("asd", "anupamdio ERRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRr");
				}
				superPath.touchStart(event.getX(index), event.getY(index));
			} else if (qualcosa == MotionEvent.ACTION_MOVE) {
				for (int i = 0; i < event.getPointerCount(); i++) {
					id = event.getPointerId(i);
					index = event.findPointerIndex(id);
					Log.d("asd", "anupamdio moved " + id + " (" + event.getX(index) + ";" + event.getY(index) + ")");
					superPath = superMultiPathManager.getSuperPathRelatedToPointer(id);
					if (superPath == null) {
						Log.e("asd", "anupamdio ERRRRRRRRRRRRRRRRRRRRRRRRRRRr");
					}
					superPath.touchMove(event.getX(index), event.getY(index));
				}
			} else if (qualcosa == MotionEvent.ACTION_UP
					|| qualcosa == MotionEvent.ACTION_POINTER_UP
					|| qualcosa == MotionEvent.ACTION_CANCEL) {
				Log.d("anupam", "anupamdio ALZATO " + id);
				superPath = superMultiPathManager
						.getSuperPathRelatedToPointer(id);
				superPath.lineTo(superPath.getLastX(), superPath.getLastY());
				// commit the path to our offscreen
				mCanvas.drawPath(superPath, mPaint);
				// kill this so we don't double draw
				superPath.reset();

				superPath.freeFromPointer();
			}
			invalidate();
			return true;
			/*
			 * final int action = event.getAction(); switch (action &
			 * MotionEvent.ACTION_MASK) { case MotionEvent.ACTION_DOWN: { final
			 * float x = ev.getX(); final float y = ev.getY();
			 * 
			 * mLastTouchX = x; mLastTouchY = y;
			 * 
			 * // Save the ID of this pointer mActivePointerId =
			 * event.getPointerId(0); break; }
			 * 
			 * case MotionEvent.ACTION_MOVE: { // Find the index of the active
			 * pointer and fetch its position final int pointerIndex =
			 * ev.findPointerIndex(mActivePointerId); final float x =
			 * ev.getX(pointerIndex); final float y = ev.getY(pointerIndex);
			 * 
			 * final float dx = x - mLastTouchX; final float dy = y -
			 * mLastTouchY;
			 * 
			 * mPosX += dx; mPosY += dy;
			 * 
			 * mLastTouchX = x; mLastTouchY = y;
			 * 
			 * invalidate(); break; }
			 * 
			 * case MotionEvent.ACTION_UP: { mActivePointerId =
			 * INVALID_POINTER_ID; break; }
			 * 
			 * case MotionEvent.ACTION_CANCEL: { mActivePointerId =
			 * INVALID_POINTER_ID; break; }
			 * 
			 * case MotionEvent.ACTION_POINTER_UP: { // Extract the index of the
			 * pointer that left the touch sensor final int pointerIndex =
			 * (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
			 * MotionEvent.ACTION_POINTER_INDEX_SHIFT; final int pointerId =
			 * ev.getPointerId(pointerIndex); if (pointerId == mActivePointerId)
			 * { // This was our active pointer going up. Choose a new // active
			 * pointer and adjust accordingly. final int newPointerIndex =
			 * pointerIndex == 0 ? 1 : 0; mLastTouchX =
			 * ev.getX(newPointerIndex); mLastTouchY = ev.getY(newPointerIndex);
			 * mActivePointerId = ev.getPointerId(newPointerIndex); } break; } }
			 * return true;
			 */
		}
	}

	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int SIZE_MENU_ID = Menu.FIRST + 1;
	private static final int ERASE_MENU_ID = Menu.FIRST + 2;
	private static final int CLEAR_ALL = Menu.FIRST + 3;
	private static final int NORMAL_BRUSH = Menu.FIRST + 4;
	private static final int EMBOSS_MENU_ID = Menu.FIRST + 5;
	private static final int BLUR_MENU_ID = Menu.FIRST + 6;
	private static final int SAVE = Menu.FIRST + 7;
	private static final int SHARE = Menu.FIRST + 8;
	private static final int ABOUT = Menu.FIRST + 9;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, COLOR_MENU_ID, 0, R.string.color).setIcon(R.drawable.color)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SIZE_MENU_ID, 0, R.string.brush_size)
				.setIcon(R.drawable.size)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, ERASE_MENU_ID, 0, R.string.erase).setIcon(R.drawable.erase)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, CLEAR_ALL, 0, R.string.clear_all)
				.setIcon(R.drawable.clear_all)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, NORMAL_BRUSH, 0, R.string.normal).setIcon(R.drawable.size)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, EMBOSS_MENU_ID, 0, R.string.emboss)
				.setIcon(R.drawable.emboss)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, BLUR_MENU_ID, 0, R.string.blur).setIcon(R.drawable.blur)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SAVE, 0, R.string.save).setIcon(R.drawable.save)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SHARE, 0, R.string.share).setIcon(R.drawable.share)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, ABOUT, 0, R.string.about).setIcon(R.drawable.about)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

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
		case NORMAL_BRUSH:
			mPaint.setMaskFilter(null);
			return true;
		case COLOR_MENU_ID:
			new ColorPickerDialog(this, this, mPaint.getColor()).show();
			return true;
		case EMBOSS_MENU_ID:
			mPaint.setMaskFilter(mEmboss);
			return true;
		case BLUR_MENU_ID:
			mPaint.setMaskFilter(mBlur);
			return true;
		case SIZE_MENU_ID:
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.brush,
					(ViewGroup) findViewById(R.id.root));
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setView(layout);
			final AlertDialog alertDialog = builder.create();
			alertDialog.show();
			SeekBar sb = (SeekBar) layout.findViewById(R.id.seekBar1);
			sb.setProgress(getStrokeSize());
			final Button done = (Button) layout.findViewById(R.id.select_size);
			final TextView txt = (TextView) layout
					.findViewById(R.id.size_value);
			txt.setText(String.format(
					getResources().getString(R.string.default_size_is),
					getStrokeSize()));
			sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				public void onProgressChanged(SeekBar seekBar,
						final int progress, boolean fromUser) {
					// Do something here with new value
					mPaint.setStrokeWidth(progress);
					txt.setText(String.format(
							getResources().getString(
									R.string.your_selected_size_is), progress));
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
			});
			done.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					alertDialog.dismiss();
				}
			});
			return true;
		case ERASE_MENU_ID:
			LayoutInflater inflater_e = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout_e = inflater_e.inflate(R.layout.brush,
					(ViewGroup) findViewById(R.id.root));
			AlertDialog.Builder builder_e = new AlertDialog.Builder(this)
					.setView(layout_e);
			final AlertDialog alertDialog_e = builder_e.create();
			alertDialog_e.show();
			SeekBar sb_e = (SeekBar) layout_e.findViewById(R.id.seekBar1);
			sb_e.setProgress(getStrokeSize());
			final Button done_e = (Button) layout_e
					.findViewById(R.id.select_size);
			final TextView txt_e = (TextView) layout_e
					.findViewById(R.id.size_value);
			txt_e.setText(String.format(
					getResources().getString(R.string.default_size_is),
					getStrokeSize()));
			sb_e.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				public void onProgressChanged(SeekBar seekBar,
						final int progress, boolean fromUser) {
					// Do something here with new value
					mPaint.setStrokeWidth(progress);
					txt_e.setText(String.format(
							getResources().getString(
									R.string.your_selected_size_is), progress));
				}

				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}

				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
			});
			done_e.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					alertDialog_e.dismiss();
				}
			});
			// mPaint.setColor(bgColor);
			mPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
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

				alert.setTitle(R.string.about);

				alert.setMessage(R.string.app_description);

				alert.setPositiveButton(R.string.ok, null);

				alert.show();
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This takes the screenshot of the whole screen. Is this a good thing?
	 */
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
				Toast.makeText(
						getApplicationContext(),
						String.format(
								getResources().getString(
										R.string.saved_your_location_to),
								file.getAbsolutePath()), Toast.LENGTH_LONG)
						.show();
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

	private int getStrokeSize() {
		return (int) mPaint.getStrokeWidth();
	}
}