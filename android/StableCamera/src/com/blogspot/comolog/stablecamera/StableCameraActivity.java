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
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    
	private SensorManager mSensorManager;
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
			float c = (float)Math.cos(a);
			float s = (float)Math.sin(a);
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
			final int INDICATOR_LINE_WIDTH = 10;
			canvas.drawLine(halfW, 0, halfW, INDICATOR_LINE_WIDTH, paint);
			canvas.drawLine(halfW, h - INDICATOR_LINE_WIDTH, halfW, h, paint);
			canvas.drawLine(0, halfH, INDICATOR_LINE_WIDTH, halfH, paint);
			canvas.drawLine(w - INDICATOR_LINE_WIDTH, halfH, w, halfH, paint);
			mHolderIndicator.unlockCanvasAndPost(canvas);
		}
	};    
    
    
    private Camera.AutoFocusCallback mAutoFocusListener =  
    	new Camera.AutoFocusCallback() {    
    	@Override  
    	public void onAutoFocus(boolean success, Camera camera) {  
    		Log.i(TAG,"AutoFocus : " + success);  
    		camera.autoFocus(null);  
    		camera.takePicture(mShutterListener, null, mPictureListener);  
    	}  
    };  

    private Camera.ShutterCallback mShutterListener =   
    	new Camera.ShutterCallback() {  
    	@Override  
    	public void onShutter() {  
    		Log.i(TAG, "onShutter"); 
    		mPreview.setVisibility(View.INVISIBLE);
			mSurfaceViewIndicator.setVisibility(View.INVISIBLE);
    	}  
    };
    
    private Uri saveBitmap(Bitmap bitmap) {
    	final String PATH = Environment.getExternalStorageDirectory().toString();
    	FileOutputStream fos = null;
    	//ファイル名の生成。日付形式
    	Date today = new Date();    
    	SimpleDateFormat sdFormat= new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss_SSS");   
    	String fileName = sdFormat.format(today) + ".jpg";

    	File file = new File(PATH,fileName);
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
    	values.put(MediaStore.Images.Media.TITLE,uri.getLastPathSegment());
    	values.put(MediaStore.Images.Media.DISPLAY_NAME,uri
    	    .getLastPathSegment());
    	values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
    	values.put(MediaStore.Images.Media.DATA,uri.getPath());
    	values.put(MediaStore.Images.Media.DATE_TAKEN,System
    	    .currentTimeMillis());

    	Uri imageUri = getContentResolver()
    	    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    	    values);    	
    	return imageUri;
    }
    
    private Camera.PictureCallback mPictureListener =   
    	new Camera.PictureCallback() {     
    	@Override  
    	public void onPictureTaken(byte[] data, Camera camera) {  
    		Log.i(TAG, "Picture taken");  
    		if(data != null) {  
    			Log.i(TAG, "JPEG Picture Taken");  

    			BitmapFactory.Options options = new BitmapFactory.Options();
    			options.inJustDecodeBounds = true;
    			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

    			int originalWidth = options.outWidth;
    			int originalHeight = options.outHeight;

    			int targetWidth;
    			int targetHeight;

    			WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    			Display disp = wm.getDefaultDisplay();
    			int width = disp.getWidth();
    			int height = disp.getHeight();

    			if(originalWidth > originalHeight * width / height){
    				targetWidth = width * originalWidth / originalHeight;
    				targetHeight = height;
    			}
    			else if(originalWidth < originalHeight * width / height){
    				targetWidth = width;
    				targetHeight = height * originalHeight / originalWidth;
    			}
    			else{
    				targetWidth = width;
    				targetHeight = height;
    			}

    			int scaleW = originalHeight / targetHeight;
    			int scaleH = originalWidth / targetWidth;

    			int sampleSize = Math.max(scaleW, scaleH) - 1;

    			options = new BitmapFactory.Options();
    			options.inJustDecodeBounds = false;
    			options.inSampleSize = sampleSize;
//    			options.inPreferredConfig = Config.RGB_565;
    			options.inPurgeable = true;
    			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
    			    			
    			mImageView.setImageBitmap(bitmap);  
    			mImageView.setVisibility(View.VISIBLE);
    			
    			options = new BitmapFactory.Options();
    			options.inJustDecodeBounds = false;
    			options.inSampleSize = 1;
//    			options.inPreferredConfig = Config.RGB_565;
    			options.inPurgeable = true;
    			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);    			
    			Uri path = saveBitmap(bitmap);
    			Log.i(StableCameraActivity.TAG, path.toString());
    			
    			mIsWaitingForTaken = false;
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

		// Find the total number of cameras available
		mNumberOfCameras = Camera.getNumberOfCameras();

		// Find the ID of the default camera
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
	}

    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        mCameraCurrentlyLocked = mDefaultCameraId;
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate our menu which can gather user input for switching camera
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.camera_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.switch_cam:
            // check for availability of multiple cameras
            if (mNumberOfCameras == 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(this.getString(R.string.camera_alert))
                       .setNeutralButton("Close", null);
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }

            // OK, we have multiple cameras.
            // Release this camera -> mCameraCurrentlyLocked
            if (mCamera != null) {
                mCamera.stopPreview();
                mPreview.setCamera(null);
                mCamera.release();
                mCamera = null;
            }

            // Acquire the next camera and request Preview to reconfigure
            // parameters.
            mCamera = Camera
                    .open((mCameraCurrentlyLocked + 1) % mNumberOfCameras);
            mCameraCurrentlyLocked = (mCameraCurrentlyLocked + 1)
                    % mNumberOfCameras;
            mPreview.switchCamera(mCamera);

            Camera.Parameters params = mCamera.getParameters();
            List<String> flashModes = params.getSupportedFlashModes();
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
		return mGestureDetector.onTouchEvent(event);
		//return super.onTouchEvent(event);
	}
	
	//複雑なタッチイベント処理
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
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) { 

			return super.onFling(event1, event2, velocityX, velocityY); 
		} 

		@Override 
		public void onLongPress(MotionEvent event) { 

			super.onLongPress(event); 
		} 

		@Override 
		public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) { 

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
					if(mCamera != null) {
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
		mSensorManager = (SensorManager)getSystemService(
				Context.SENSOR_SERVICE);
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
