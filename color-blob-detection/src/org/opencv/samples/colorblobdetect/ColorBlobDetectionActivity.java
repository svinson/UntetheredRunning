package org.opencv.samples.colorblobdetect;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private boolean 			 appHasStarted = false;
    private boolean				 appFirstRun = false;
    
    private boolean 			 badDistance = false;
    
    private Mat                  mRgba;
    //private Mat                  binaryImg;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private int                  mPrevLocation = NODIR;
    private int					 mThreshold = 400; // should change to a dynamic value
    private boolean              mLost = false;
    
    private Button               startStopBtn;

    private float 				 volume = 1.0f;
    private MediaPlayer			 mp;
    
    private MatOfPoint2f mMOP2f1; 
    private MatOfPoint2f mMOP2f2;
    
    private CameraBridgeViewBase mOpenCvCameraView;

    // directional constants
    private static final int     NODIR = -1;
    private static final int	 LEFTDIR = 0;
    private static final int	 RIGHTDIR = 1;
    private static final int	 FRONTDIR = 2;
    private static final int	 BACKDIR = 3;
    private static final int	 DANGERDIR = 4;
    
    private static final int	 TIMER_MAX = 50;
    private static final int	 NUM_PLAYED_MAX = 4;
    
    private boolean			     safeStateFlag = false;	// set flag if valid circle found
    private boolean				first = true;
    private int					dirTimer = 0;
    private int					dirsPlayedLR = 0;
    private int					dirsPlayedFB = 0;
    private int					prevDirFB = NODIR;
    
    private static final int	MIN_RADIUS_HIGH = 22; // not calibrated
    private static final int	MIN_RADIUS_LOW = 10; // not calibrated
    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {

    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
    	Log.d("WiFi","Wi" + wifiManager.isWifiEnabled());
    	while(wifiManager.isWifiEnabled() == true){
    		wifiManager.setWifiEnabled(false);
    		Log.d("WiFi","WiFi stuck");
    	}
    	Log.d("WiFi","Wi Off?" + wifiManager.isWifiEnabled());
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        startStopBtn = (Button) findViewById(R.id.buttonStartStop);
        startStopBtn.setText(R.string.START_APP_STRING);
        
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize(320, 240);
        
        mp = MediaPlayer.create(getApplicationContext(), R.raw.app_ready);
		mp.setVolume(volume, volume);
        mp.start();
        
        Log.d("VIEWSIZE", "width: " + mOpenCvCameraView.getWidth());
        Log.d("VIEWSIZE", "height: " + mOpenCvCameraView.getHeight());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mp = MediaPlayer.create(getApplicationContext(), R.raw.app_closed);
		mp.setVolume(volume, volume);
        mp.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        if (appHasStarted) {
        	mp = MediaPlayer.create(getApplicationContext(), R.raw.app_resumed);
        	mp.setVolume(volume, volume);
        	mp.start();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mp = null;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        //binaryImg = new Mat();
        if(first) {
        	mDetector = new ColorBlobDetector();
        }
        first = false;
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public void startStopAppOnClick(View view) {
    	//App is in not tracking state
    	if (!appHasStarted) {
    		startStopBtn.setText(R.string.STOP_APP_STRING);
    		appHasStarted = true;
    		//App has tracked at least once
    		if (appFirstRun) {
    			mp = MediaPlayer.create(getApplicationContext(), R.raw.app_resumed);
    		}
    		else{
    			mp = MediaPlayer.create(getApplicationContext(), R.raw.app_started);
    			appFirstRun = true;
    		}
    		mp.setVolume(volume, volume);  
            mp.start();
      	}
    	//App was in running state
    	else {
    		badDistance = false;
    		mLost = false;
    		safeStateFlag = false;
    		mPrevLocation = NODIR;
    		
    		startStopBtn.setText(R.string.START_APP_STRING);
    		mp = MediaPlayer.create(getApplicationContext(), R.raw.tracking_stopped);
    		appHasStarted = false;
    		mp.setVolume(volume, volume);  
            mp.start();
    	}
    }
    
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        if (!mIsColorSelected) {
	        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
	        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
	
	        int x = (int)event.getX() - xOffset;
	        int y = (int)event.getY() - yOffset;
	
	        // Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
	
	        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
	
	        Rect touchedRect = new Rect();
	
	        touchedRect.x = (x>4) ? x-4 : 0;
	        touchedRect.y = (y>4) ? y-4 : 0;
	
	        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
	        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;
	
	        Mat touchedRegionRgba = mRgba.submat(touchedRect);
	
	        Mat touchedRegionHsv = new Mat();
	        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
	
	        // Calculate average color of touched region
	        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
	        int pointCount = touchedRect.width*touchedRect.height;
	        for (int i = 0; i < mBlobColorHsv.val.length; i++)
	            mBlobColorHsv.val[i] /= pointCount;
	
	        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
	
	        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
	                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
	
	        mDetector.setHsvColor(mBlobColorHsv);
	        Log.i("HSV", mBlobColorHsv.val[0] + " " + mBlobColorHsv.val[1] +
	                " " + mBlobColorHsv.val[2] + " " + mBlobColorHsv.val[3]);
	        
	        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
	
	        mIsColorSelected = true;
	
	        touchedRegionRgba.release();
	        touchedRegionHsv.release();
	        
	        appHasStarted = true;
	        startStopBtn.setText(R.string.STOP_APP_STRING);
    		appHasStarted = true;
    		appFirstRun = true;
   			mp = MediaPlayer.create(getApplicationContext(), R.raw.app_started);
    		mp.setVolume(volume, volume);  
            mp.start();
        }

        return false; // don't need subsequent touch events
    }

    private class Circle {
    	public Point mCenter;
    	public float mRadius; 
    	
    	public Circle() {
    		mCenter = new Point();
    		mRadius = 0;
    	}
    	
    	public Circle(Point center, float radius) {
    		mCenter = center;
    		mRadius = radius;
    	}
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	
    	Log.i("Test02", "Start" + SystemClock.elapsedRealtime());
    	
        mRgba = inputFrame.rgba();
        //binaryImg = inputFrame.gray();
        mMOP2f1 = new MatOfPoint2f();
        mMOP2f2 = new MatOfPoint2f();
 
        if (mIsColorSelected && appHasStarted) {
            mRgba = mDetector.process(mRgba);
            
            this.dirTimer++;
            
            List<MatOfPoint> contours = mDetector.getContours();
            Circle[] circle = new Circle[contours.size()];
            Circle myCircle = new Circle();

            Point center; 
            float[] radius;
            int bestMatch = -1;
            float maxRad = -1;
            
            for(int i=0; i<contours.size(); i++) {
				//Convert contours(i) from MatOfPoint to MatOfPoint2f
                contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
				//Processing on mMOP2f1 which is in type MatOfPoint2f
                Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, Imgproc.arcLength(mMOP2f1, true) * 0.02, true);
                Point[] points = mMOP2f2.toArray();
                for(Point x : points) {
                	Core.circle(mRgba, x, 5, CONTOUR_COLOR, 3);
                }

                center = new Point();
                radius = new float[1];
                Imgproc.minEnclosingCircle(mMOP2f2, center, radius);
                circle[i] = new Circle(center, radius[0]);
                
                if (points.length > 6 && circle[i].mRadius > maxRad &&
                    (circle[i].mRadius > MIN_RADIUS_HIGH || (this.safeStateFlag && circle[i].mRadius > MIN_RADIUS_LOW))) {
               		maxRad = circle[i].mRadius;
               		bestMatch = i;
                }

                //Convert back to MatOfPoint and put the new values back into the contours list
                mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);                
                //Log.d("Center", "Center: X: " + center.x + " Y: " + center.y);
                //Log.d("Center", "Radius: " + radius[0]);
            }
            
            if (bestMatch >= 0) {
            	if (this.safeStateFlag == false) {
            		// maybe add feedback here saying marker was detected
            	}
            	myCircle = circle[bestMatch];
            	this.safeStateFlag = true;
            	Core.circle(mRgba, myCircle.mCenter, (int) myCircle.mRadius, CONTOUR_COLOR, 3);
            }
            else {
                this.safeStateFlag = false;
            }
            
            /*for(int i=0; i < contours.size(); i++) {
            	if (circle[i] != null)
            		Log.d("List", "Circle: " + i + "X: " + circle[i].mCenter.x + "Y: " + circle[i].mCenter.y);
            }*/
            
            if (this.safeStateFlag) {
	            Log.d("TEST", "Point: X: " + myCircle.mCenter.x + " Y: " + myCircle.mCenter.y);
	            Log.d("TEST", "Radius: " + myCircle.mRadius);
	            // only used for testing
	           
	            Core.circle(mRgba, myCircle.mCenter, (int) myCircle.mRadius, CONTOUR_COLOR, 3);
	            
	            if (myCircle.mCenter.y > 50 && myCircle.mCenter.y < 400) {
	            	if (myCircle.mCenter.x < 400) { // change 400 to mThreshold
	 	            	mPrevLocation = LEFTDIR;
	 	            }
	 	            else {
	 	            	mPrevLocation = RIGHTDIR;
	 	            }
	            }
	            else {
	            	mPrevLocation = NODIR;
	            }
	            
	            if (mLost && myCircle.mCenter.x > 200 && myCircle.mCenter.x < 600) {
	            	mp = MediaPlayer.create(getApplicationContext(), R.raw.good_position);
	        		mp.setVolume(volume, volume);
	                mp.start();
	                mLost = false;
	                this.dirsPlayedLR = 0;
	                this.dirTimer = 0;
	            }
	            else if (mLost && myCircle.mCenter.x <= 100 && this.dirTimer == TIMER_MAX) {
	            	mp = MediaPlayer.create(getApplicationContext(), R.raw.move_left);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
                    this.dirsPlayedLR = 0;
	            }
	            else if (mLost && myCircle.mCenter.x >= 700 && this.dirTimer == TIMER_MAX) {
	            	mp = MediaPlayer.create(getApplicationContext(), R.raw.move_right);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
                    this.dirsPlayedLR = 0;
	            }
            
	            if ((!badDistance && (myCircle.mCenter.x < 750 && myCircle.mCenter.x > 50 && 
	                  myCircle.mCenter.y > 50 && myCircle.mCenter.y < 400)) || this.dirTimer == TIMER_MAX) {
            		if (myCircle.mRadius > 75) { // needs calibration
            			mp = MediaPlayer.create(getApplicationContext(), R.raw.slow_down);
	                    mp.setVolume(volume, volume);
	                    mp.start();
	                    badDistance = true;
	                    this.dirTimer = 0;
	                    this.prevDirFB = BACKDIR;
            		}
            		else if (myCircle.mRadius < 35) { // needs calibration
        				mp = MediaPlayer.create(getApplicationContext(), R.raw.speed_up);
        				mp.setVolume(volume, volume);
        				mp.start();
        				badDistance = true;
        				this.dirTimer = 0;
        				this.prevDirFB = FRONTDIR;
            		}
            	}
            	if ((badDistance && myCircle.mRadius < 55 && myCircle.mRadius > 44)) { // || this.dirTimer == TIMER_MAX) { // commented out becasue it was overlapping other commands
    				mp = MediaPlayer.create(getApplicationContext(), R.raw.good_distance);
    				mp.setVolume(volume, volume);
    				mp.start();
            		badDistance = false;
            		this.dirTimer = 0;
            		this.prevDirFB = NODIR;
            	}
            	
            	this.dirsPlayedFB = 0;
            }
            
            else if (!mLost || this.dirTimer == TIMER_MAX) {
        		if (this.dirsPlayedLR >= NUM_PLAYED_MAX) {
        			mp = MediaPlayer.create(getApplicationContext(), R.raw.lost_marker);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
            		mPrevLocation = NODIR;
            	}
        		else if (mPrevLocation == LEFTDIR) {
                    mp = MediaPlayer.create(getApplicationContext(), R.raw.move_left);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
                    this.dirsPlayedLR++;
            	}
            	else if (mPrevLocation == RIGHTDIR) {
                    mp = MediaPlayer.create(getApplicationContext(), R.raw.move_right);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
                    this.dirsPlayedLR++;
            	}
            	
            	mLost = true;
            }

            // (This should work assuming the circle radius doesn't get too small as the 
            // target leaves the side of the screen for left/right)
            else if (badDistance && this.dirTimer == TIMER_MAX) {
                if (this.dirsPlayedFB >= NUM_PLAYED_MAX) {
                	mp = MediaPlayer.create(getApplicationContext(), R.raw.lost_marker);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
                    this.prevDirFB = NODIR;
                }
            	else if (this.prevDirFB == BACKDIR) {
            		mp = MediaPlayer.create(getApplicationContext(), R.raw.slow_down);
                    mp.setVolume(volume, volume);
                    mp.start();
                    this.dirTimer = 0;
                    this.dirsPlayedFB++;
            	}
            	else if (this.prevDirFB == FRONTDIR) {
            		mp = MediaPlayer.create(getApplicationContext(), R.raw.speed_up);
    				mp.setVolume(volume, volume);
    				mp.start();
    				this.dirTimer = 0;
    				this.dirsPlayedFB++;
            	}
            }
            
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }
        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}