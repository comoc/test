/*
	The MIT License
	Copyright (c) 2012 Akihiro Komori
	
	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:
	
	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.
	
	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.
 */
package com.blogspot.comolog.stablecamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class StableCameraActivity extends Activity {
	private Preview mPreview;
	private SurfaceView mSurfaceViewIndicator;
	private Camera mCamera;
	private int mNumberOfCameras;
	private int mCameraCurrentlyLocked;
	private int mDefaultCameraId;
	private ImageView mImageView;
	private SurfaceHolder mHolderIndicator;
	private GestureDetector mGestureDetector;
	private boolean mIsWaitingForTaken;
	private List<String> mFlashModes;
	private static final String TAG = "StableCameraActivity";
	private boolean mIsTouching;
	private float mTouchX;
	private float mTouchY;
	private SensorManager mSensorManager;
	private static final float TOUCH_CIRCLE_RADIUS = 100;
	private int mCameraRotation = 0;
	private SensorEventListener mSensorEventListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (mHolderIndicator == null)
				return;

			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
				return;

			if (!mIsWaitingForTaken) {
				double rad = Math.atan2(event.values[1], event.values[0]);
				double deg = ((rad / Math.PI) * 180) + (rad > 0 ? 0 : 360);
				int orientation = (int) (deg + 0.5);
				CameraInfo info = new android.hardware.Camera.CameraInfo();
				Camera.getCameraInfo(mCameraCurrentlyLocked, info);
				orientation = (orientation + 45) / 90 * 90;
				int rotation = 0;
				orientation += 270; // add offset. because of the view is
									// landscape.
				if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
					rotation = (info.orientation - orientation + 360) % 360;
				} else { // back-facing camera
					rotation = (info.orientation + orientation) % 360;
				}
				mCameraRotation = rotation;
			}

			String str = new String();
			for (float v : event.values) {
				str += "" + v + ", ";
			}
			Canvas canvas = mHolderIndicator.lockCanvas();
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
			Paint paint = new Paint();
			paint.setColor(Color.GREEN);
			canvas.drawText(str, 10, 10, paint);

			int w = canvas.getWidth();
			int h = canvas.getHeight();
			int halfW = w / 2;
			int halfH = h / 2;

			// scale
			float x0 = -halfW;
			float y0 = 0.0f;
			float x1 = halfW;
			float y1 = 0.0f;

			// rotate
			double a = -Math.atan2(event.values[1], event.values[0]);
			float c = (float) Math.cos(a);
			float s = (float) Math.sin(a);
			float x0d = c * x0 - s * y0;
			float y0d = s * x0 + c * y0;
			float x1d = c * x1 - s * y1;
			float y1d = s * x1 + c * y1;

			// translate
			x0d += halfW;
			y0d += halfH;
			x1d += halfW;
			y1d += halfH;

			canvas.drawLine(x0d, y0d, x1d, y1d, paint);

			if (mIsTouching) {
				canvas.drawCircle(mTouchX, mTouchY, TOUCH_CIRCLE_RADIUS, paint);
			}

			final int INDICATOR_LINE_WIDTH = 10;
			canvas.drawLine(halfW, 0, halfW, INDICATOR_LINE_WIDTH, paint);
			canvas.drawLine(halfW, h - INDICATOR_LINE_WIDTH, halfW, h, paint);
			canvas.drawLine(0, halfH, INDICATOR_LINE_WIDTH, halfH, paint);
			canvas.drawLine(w - INDICATOR_LINE_WIDTH, halfH, w, halfH, paint);
			mHolderIndicator.unlockCanvasAndPost(canvas);
		}
	};

	private Camera.AutoFocusCallback mAutoFocusListener = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Log.i(TAG, "AutoFocus : " + success);
			camera.autoFocus(null);
			camera.takePicture(mShutterListener, null, mPictureListener);
		}
	};

	private Camera.ShutterCallback mShutterListener = new Camera.ShutterCallback() {
		@Override
		public void onShutter() {
			Log.i(TAG, "onShutter");
			mPreview.setVisibility(View.INVISIBLE);
			mSurfaceViewIndicator.setVisibility(View.INVISIBLE);
		}
	};

	public static String getPath(Context context, Uri uri) {
		ContentResolver contentResolver = context.getContentResolver();
		String[] columns = { MediaStore.Images.Media.DATA };
		Cursor cursor = contentResolver.query(uri, columns, null, null, null);
		cursor.moveToFirst();
		String path = cursor.getString(0);
		cursor.close();
		return path;
	}

	private Uri saveBitmap(Bitmap bitmap, Date date) {

		// Matrix matrix = new Matrix();
		// matrix.postRotate(mCameraRotation);

		final String PATH = Environment.getExternalStorageDirectory()
				.toString();
		FileOutputStream fos = null;
		SimpleDateFormat sdFormat = new SimpleDateFormat(
				"yyyy_MM_dd_hh_mm_ss_SSS");
		String fileName = sdFormat.format(date) + ".jpg";

		File file = new File(PATH, fileName);
		try {
			if (file.createNewFile()) {
				fos = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.close();
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		Uri uri = Uri.fromFile(file);
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, uri.getLastPathSegment());
		values.put(MediaStore.Images.Media.DISPLAY_NAME,
				uri.getLastPathSegment());
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.Images.Media.DATA, uri.getPath());
		values.put(MediaStore.Images.Media.DATE_TAKEN,
				System.currentTimeMillis());

		Uri imageUri = getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		return imageUri;
	}

	private int degreesToExifOrientation(int normalizedAngle) {
		if (normalizedAngle == 0) {
			return ExifInterface.ORIENTATION_NORMAL;
		} else if (normalizedAngle == 90) {
			return ExifInterface.ORIENTATION_ROTATE_90;
		} else if (normalizedAngle == 180) {
			return ExifInterface.ORIENTATION_ROTATE_180;
		} else if (normalizedAngle == 270) {
			return ExifInterface.ORIENTATION_ROTATE_270;
		}
		return ExifInterface.ORIENTATION_NORMAL;
	}

	private void setExifAttributes(Camera.Parameters params, Date date,
			ExifInterface exif) {
		if (mLocation != null) {
			// create a reference for Latitude and Longitude
			double lat = mLocation.getLatitude();
			if (lat < 0) {
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
				lat = -lat;
			} else {
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
			}

			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
					formatLatLongString(lat));

			double lon = mLocation.getLongitude();
			if (lon < 0) {
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
				lon = -lon;
			} else {
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
			}
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
					formatLatLongString(lon));

			exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH,
					Float.toHexString(params.getFocalLength()));
			exif.setAttribute(ExifInterface.TAG_FLASH, params.getFlashMode());
			exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE,
					params.getWhiteBalance());
		}

		exif.setAttribute(ExifInterface.TAG_MAKE, android.os.Build.MANUFACTURER);
		exif.setAttribute(ExifInterface.TAG_MODEL, android.os.Build.MODEL);

		SimpleDateFormat sFormatter = new SimpleDateFormat(
				"yyyy:MM:dd HH:mm:ss");
		exif.setAttribute(ExifInterface.TAG_DATETIME, sFormatter.format(date));

		int exifOrientation = degreesToExifOrientation(mCameraRotation);
		exif.setAttribute(ExifInterface.TAG_ORIENTATION,
				Integer.toString(exifOrientation));
	}

	/*
	 * formnat the Lat Long values according to standard exif format
	 */
	private static String formatLatLongString(double d) {
		// format latitude and longitude according to exif format
		StringBuilder b = new StringBuilder();
		b.append((int) d);
		b.append("/1,");
		d = (d - (int) d) * 60;
		b.append((int) d);
		b.append("/1,");
		d = (d - (int) d) * 60000;
		b.append((int) d);
		b.append("/1000");
		return b.toString();
	}

	private Camera.PictureCallback mPictureListener = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.i(TAG, "Picture taken");
			if (data != null) {
				Date date = new Date();

				Log.i(TAG, "JPEG Picture Taken");

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
						data.length, options);

				int originalWidth = options.outWidth;
				int originalHeight = options.outHeight;

				int targetWidth;
				int targetHeight;

				WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
				Display disp = wm.getDefaultDisplay();
				int width = disp.getWidth();
				int height = disp.getHeight();

				if (originalWidth > originalHeight * width / height) {
					targetWidth = width * originalWidth / originalHeight;
					targetHeight = height;
				} else if (originalWidth < originalHeight * width / height) {
					targetWidth = width;
					targetHeight = height * originalHeight / originalWidth;
				} else {
					targetWidth = width;
					targetHeight = height;
				}

				int scaleW = originalHeight / targetHeight;
				int scaleH = originalWidth / targetWidth;

				int sampleSize = Math.max(scaleW, scaleH) - 1;

				options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				options.inSampleSize = sampleSize;
				// options.inPreferredConfig = Config.RGB_565;
				options.inPurgeable = true;
				bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
						options);

				mImageView.setImageBitmap(bitmap);
				mImageView.setVisibility(View.VISIBLE);

				options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				options.inSampleSize = 1;
				// options.inPreferredConfig = Config.RGB_565;
				options.inPurgeable = true;
				bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
						options);
				Uri path = saveBitmap(bitmap, date);

				try {
					String p = getPath(StableCameraActivity.this, path);
					ExifInterface exifInterface = new ExifInterface(p);
					setExifAttributes(camera.getParameters(), date,
							exifInterface);
					exifInterface.saveAttributes();
				} catch (IOException e) {
					e.printStackTrace();
				}

				Log.i(StableCameraActivity.TAG, path.toString());

				mIsWaitingForTaken = false;
			}
		}
	};

	private Location mLocation;

	private LocationListener mLocationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			mLocation = new Location(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				Log.v("Status", "AVAILABLE");
				break;
			case LocationProvider.OUT_OF_SERVICE:
				Log.v("Status", "OUT_OF_SERVICE");
				mLocation = null;
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.v("Status", "TEMPORARILY_UNAVAILABLE");
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);

		mPreview = new Preview(this);
		FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);
		fl.addView(mPreview);
		mPreview.bringToFront();

		mImageView = (ImageView) findViewById(R.id.imageView);
		mImageView.bringToFront();
		mImageView.setVisibility(View.INVISIBLE);

		mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);

		mNumberOfCameras = Camera.getNumberOfCameras();
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int i = 0; i < mNumberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				mDefaultCameraId = i;
			}
		}

		mSurfaceViewIndicator = new SurfaceView(this);
		fl.addView(mSurfaceViewIndicator, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		mSurfaceViewIndicator.setZOrderOnTop(true);
		mHolderIndicator = mSurfaceViewIndicator.getHolder();
		mHolderIndicator.setFormat(PixelFormat.TRANSPARENT);
		mHolderIndicator.addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				mHolderIndicator = null;
				finalizeSensor();
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mHolderIndicator = holder;
				initializeSensor();
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
			}
		});

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
		String bestProvider = locationManager.getBestProvider(criteria, true);
		locationManager.requestLocationUpdates(bestProvider, 0, 0,
				mLocationListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCamera = Camera.open();
		mCameraCurrentlyLocked = mDefaultCameraId;
		mPreview.setCamera(mCamera);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.camera_menu, menu);
		if (mNumberOfCameras <= 1) {
			MenuItem mi = menu.getItem(0);
			mi.setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.switch_cam:
			if (mNumberOfCameras == 1) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(this.getString(R.string.camera_alert))
						.setNeutralButton("Close", null);
				AlertDialog alert = builder.create();
				alert.show();
				return true;
			}

			if (mCamera != null) {
				mCamera.stopPreview();
				mPreview.setCamera(null);
				mCamera.release();
				mCamera = null;
			}

			mCamera = Camera.open((mCameraCurrentlyLocked + 1)
					% mNumberOfCameras);
			mCameraCurrentlyLocked = (mCameraCurrentlyLocked + 1)
					% mNumberOfCameras;
			mPreview.switchCamera(mCamera);

			Camera.Parameters cameraParameters = mCamera.getParameters();
			List<String> flashModes = cameraParameters.getSupportedFlashModes();
			mFlashModes = new ArrayList<String>();
			for (String fm : flashModes) {
				mFlashModes.add(new String(fm));
			}

			// Start the preview
			mCamera.startPreview();
			return true;
		case R.id.flash_mode:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mIsTouching = event.getAction() != MotionEvent.ACTION_UP;
		mTouchX = event.getX();
		mTouchY = event.getY();
		return mGestureDetector.onTouchEvent(event);
		// return super.onTouchEvent(event);
	}

	private final SimpleOnGestureListener mSimpleOnGestureListener = new SimpleOnGestureListener() {

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			return super.onDoubleTap(event);
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent event) {
			return super.onDoubleTapEvent(event);
		}

		@Override
		public boolean onDown(MotionEvent event) {
			return super.onDown(event);
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
				float velocityX, float velocityY) {
			return super.onFling(event1, event2, velocityX, velocityY);
		}

		@Override
		public void onLongPress(MotionEvent event) {
			super.onLongPress(event);
		}

		@Override
		public boolean onScroll(MotionEvent event1, MotionEvent event2,
				float distanceX, float distanceY) {
			if (!mIsWaitingForTaken) {
				if (mImageView.getVisibility() != View.VISIBLE) {
					if (mCamera != null) {
						mIsWaitingForTaken = true;
						mCamera.autoFocus(mAutoFocusListener);
					}
				}
			}

			return super.onScroll(event1, event2, distanceX, distanceY);
		}

		@Override
		public void onShowPress(MotionEvent event) {

			super.onShowPress(event);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			if (!mIsWaitingForTaken) {
				if (mImageView.getVisibility() == View.VISIBLE) {
					mImageView.setVisibility(View.INVISIBLE);
					mPreview.setVisibility(View.VISIBLE);
					mSurfaceViewIndicator.setVisibility(View.VISIBLE);
					mCamera.startPreview();
				} else {
					if (mCamera != null) {
						mIsWaitingForTaken = true;
						mCamera.autoFocus(mAutoFocusListener);
					}
				}
			}
			return super.onSingleTapConfirmed(event);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) {

			return super.onSingleTapUp(event);
		}
	};

	private void initializeSensor() {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = mSensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			mSensorManager.registerListener(mSensorEventListener,
					sensors.get(0), SensorManager.SENSOR_DELAY_UI);
		}
	}

	private void finalizeSensor() {
		if (mSensorEventListener != null)
			mSensorManager.unregisterListener(mSensorEventListener);
	}
}
