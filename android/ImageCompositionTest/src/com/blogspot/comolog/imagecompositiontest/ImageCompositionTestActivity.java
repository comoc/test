package com.blogspot.comolog.imagecompositiontest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
//import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
//import android.graphics.Shader;
import android.graphics.Xfermode;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class ImageCompositionTestActivity extends Activity {

    // create a bitmap with a circle, used for the "dst" image
    static Bitmap makeDst(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        
//        p.setColor(0xFFFFCC44);
        p.setAntiAlias(true);
        p.setMaskFilter(new BlurMaskFilter(15, Blur.NORMAL));
        p.setColor(0xFFFFFF00);    
        c.drawOval(new RectF(0, 0, w*3/4, h*3/4), p);
        return bm;
    }
    
    // create a bitmap with a rect, used for the "src" image
    static Bitmap makeSrc(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        
//        p.setColor(0xFF66AAFF);
      p.setColor(0xFFFF00FF);
        c.drawRect(w/3, h/3, w*19/20, h*19/20, p);
        return bm;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SampleView(this));
    }
    
    private static class SampleView extends View {
        private int W = 64;
        private int H = 64;
        private static final int ROW_MAX = 4;   // number of samples per row

        private Bitmap mSrcB;
        private Bitmap mDstB;
        private Bitmap mBack;
        
        private Handler mHandler = new Handler();

        private float mFraction = 0.0f;
        private int mAlpha = 0xff;
        private Runnable mRunnable = new Runnable() {

			@Override
			public void run() {
				float c = (float)Math.cos(mFraction * (float)Math.PI * 2.0f);
				mFraction += 0.01f;
				mFraction = mFraction - (int)mFraction;
				mAlpha = (int)(c * c * 0xff + 0.5f);
				
				SampleView.this.invalidate();
			}
        };
//        private Shader mBG;     // background checker-board pattern
        
        @Override
		public boolean onTouchEvent(MotionEvent event) {

        	mHandler.post(mRunnable);
        	return true;
		}

		private static final Xfermode[] sModes = {
            new PorterDuffXfermode(PorterDuff.Mode.CLEAR),
            new PorterDuffXfermode(PorterDuff.Mode.SRC),
            new PorterDuffXfermode(PorterDuff.Mode.DST),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER),
            new PorterDuffXfermode(PorterDuff.Mode.DST_OVER),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_IN), // OK
            new PorterDuffXfermode(PorterDuff.Mode.DST_IN),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT),
            new PorterDuffXfermode(PorterDuff.Mode.DST_OUT),
            new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP),
            new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP),
            new PorterDuffXfermode(PorterDuff.Mode.XOR),
            new PorterDuffXfermode(PorterDuff.Mode.DARKEN),
            new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN),
            new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY), // OK, if destination is white
            new PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        };
        
//        private static final String[] sLabels = {
//            "Clear", "Src", "Dst", "SrcOver",
//            "DstOver", "SrcIn", "DstIn", "SrcOut",
//            "DstOut", "SrcATop", "DstATop", "Xor",
//            "Darken", "Lighten", "Multiply", "Screen"
//        };
        
        public SampleView(Context context) {
            super(context);
            
            // make a ckeckerboard pattern
//            Bitmap bm = Bitmap.createBitmap(new int[] { 0xFFFFFFFF, 0xFFCCCCCC,
//                                            0xFFCCCCCC, 0xFFFFFFFF }, 2, 2,
//                                            Bitmap.Config.RGB_565);
//            mBG = new BitmapShader(bm,
//                                   Shader.TileMode.REPEAT,
//                                   Shader.TileMode.REPEAT);
//            Matrix m = new Matrix();
//            m.setScale(6, 6);
//            mBG.setLocalMatrix(m);
            
            mBack = BitmapFactory.decodeResource(context.getApplicationContext().getResources(), R.drawable.pattern);
        }
        
        @Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        	if (w < h)
        		W = H = w / ROW_MAX - 15;
        	else
        		W = H = h / ROW_MAX - 15;
        	
        	if (W <= 0)
        		W = H = 1;
        	
            mSrcB = makeSrc(W, H);
            mDstB = makeDst(W, H);
        	
        	super.onSizeChanged(w, h, oldw, oldh);
		}

		@Override protected void onDraw(Canvas canvas) {
//            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(mBack, null, new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), null);
            
            Paint labelP = new Paint(Paint.ANTI_ALIAS_FLAG);
            labelP.setTextAlign(Paint.Align.CENTER);

            Paint paint = new Paint();
            paint.setFilterBitmap(false);
            
            canvas.translate(15, 35);

            int x = 0;
            int y = 0;
            for (int i = 0; i < sModes.length; i++) {
                // draw the border
                paint.setStyle(Paint.Style.STROKE);
                paint.setShader(null);
                canvas.drawRect(x - 0.5f, y - 0.5f,
                                x + W + 0.5f, y + H + 0.5f, paint);
                
                // draw the backgrownd
//                canvas.drawBitmap(mBack, null, new RectF(x, y, x + W, y + H), null);
                // draw the checker-board pattern
                paint.setStyle(Paint.Style.FILL);
//                paint.setShader(mBG);
//                canvas.drawRect(x, y, x + W, y + H, paint);
                
                // draw the src/dst example into our offscreen bitmap
                int sc = canvas.saveLayer(x, y, x + W, y + H, null,
                                          Canvas.MATRIX_SAVE_FLAG |
                                          Canvas.CLIP_SAVE_FLAG |
                                          Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                                          Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                                          Canvas.CLIP_TO_LAYER_SAVE_FLAG);
                canvas.translate(x, y);
                paint.setAlpha(mAlpha);
                canvas.drawBitmap(mDstB, 0, 0, paint);
                paint.setXfermode(sModes[i]);
                paint.setAlpha(0xff);
                canvas.drawBitmap(mSrcB, 0, 0, paint);
                paint.setXfermode(null);
                canvas.restoreToCount(sc);
                
                // draw the label
//                canvas.drawText(sLabels[i],
//                                x + W/2, y - labelP.getTextSize()/2, labelP);
                
                x += W + 10;
                
                // wrap around when we've drawn enough for one row
                if ((i % ROW_MAX) == ROW_MAX - 1) {
                    x = 0;
                    y += H + 30;
                }
            }
        }
    }
}

