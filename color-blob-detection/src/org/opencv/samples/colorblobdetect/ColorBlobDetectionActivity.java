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
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;	// determines if a color has been selected
    private boolean 			 appHasStarted = false;		// determines if tracking is on/off
    private boolean				 appFirstRun = false;		// determines if app has just started
    
    private boolean 			 badDistance = false;		// used for forward/back alg if the marker is outside a threshold
    
    private Mat                  mRgba;				// used for color detection
    private Scalar               mBlobColorRgba;	// used for color detection
    private Scalar               mBlobColorHsv;		// used for color detection
    private ColorBlobDetector    mDetector;			// used for color detection
    private Mat                  mSpectrum;			// used for color detection
    private Size                 SPECTRUM_SIZE;		// used for color detection
    private Scalar               CONTOUR_COLOR;  	// used for color detection
    private int                  mPrevLocation = NODIR;	// used for left/right alg if marker leaves screen without crossing threshold
    private int					 mThreshold = 400; // should change all thresholds to a dynamic value
    private boolean              mLost = false;	// used for left/right alg if marker leaves screen
    
    private Button               startStopBtn;

    private float 				 volume = 1.0f;
    
    // voice command players
    private MediaPlayer 		 moveLeftSound;
    private MediaPlayer			 moveRightSound;
    private MediaPlayer			 speedUpSound;
    private MediaPlayer			 slowDownSound;
    private MediaPlayer			 appStartedSound;
    private MediaPlayer			 appResumedSound;
    private MediaPlayer			 appClosedSound;
    private MediaPlayer			 goodDistanceSound;
    private MediaPlayer			 goodPositionSound;
    private MediaPlayer			 lostMarkerSound;
    private MediaPlayer			 appReadySound;
    private MediaPlayer			 trackingStoppedSound;
    
    private MatOfPoint2f mMOP2f1; 
    private MatOfPoint2f mMOP2f2;
    
    private CameraBridgeViewBase mOpenCvCameraView;

    // directional constants
    private static final int     NODIR = -1;
    private static final int	 LEFTDIR = 0;
    private static final int	 RIGHTDIR = 1;
    private static final int	 FRONTDIR = 2;
    private static final int	 BACKDIR = 3;
    
    private static final int	 TIMER_MAX = 25;		// number of frames before repeating some commands
    private static final int	 NUM_PLAYED_MAXLR = 3;	// caps number of times left/right was played
    private static final int	 NUM_PLAYED_MAXFB = 3;	// caps number of times front/back was played
    private static final int	 STOP_SOUND_MAX_PLAY = 3;	// caps number of times "stop" was played
    
    private boolean			    safeStateFlag = false;	// set on each frame if valid circle found
    private boolean				first = true;
    private int					dirTimer = 0;	// counts up to TIMER_MAX to repeat some commands
    private int					dirsPlayedLR = 0;	// counts number of times left/right was played
    private int					dirsPlayedFB = 0;	// counts number of times front/back was played
    private int					timesPlayedStopSound = 0;	// counts number of times "stop" was played
    
    private int					prevDirFB = NODIR;
    
    private static final int	MIN_RADIUS_HIGH = 20; // not calibrated
    private static final int	MIN_RADIUS_LOW = 10; // not calibrated
    
    private PowerManager.WakeLock wl;
    
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        // initialize button
        startStopBtn = (Button) findViewById(R.id.buttonStartStop);
        startStopBtn.setText(R.string.START_APP_STRING);
        
        // initialize camera view
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        // initialize wake lock, used to keep app running while screen is off
        PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WAKELOCK");
        
        //Create a separate Media Player Object for each sound that will be played
        appReadySound = MediaPlayer.create(getApplicationContext(), R.raw.app_ready);
        moveLeftSound = MediaPlayer.create(getApplicationContext(), R.raw.move_left);
        moveRightSound = MediaPlayer.create(getApplicationContext(), R.raw.move_right);
        speedUpSound = MediaPlayer.create(getApplicationContext(), R.raw.speed_up);
        slowDownSound = MediaPlayer.create(getApplicationContext(), R.raw.slow_down);
        appStartedSound = MediaPlayer.create(getApplicationContext(), R.raw.app_started);
        appResumedSound = MediaPlayer.create(getApplicationContext(), R.raw.app_resumed);
        appClosedSound = MediaPlayer.create(getApplicationContext(), R.raw.app_closed);
        goodDistanceSound = MediaPlayer.create(getApplicationContext(), R.raw.good_distance);
        goodPositionSound = MediaPlayer.create(getApplicationContext(), R.raw.good_position);
        lostMarkerSound = MediaPlayer.create(getApplicationContext(), R.raw.lost_marker);
        appReadySound = MediaPlayer.create(getApplicationContext(), R.raw.app_ready);
        trackingStoppedSound = MediaPlayer.create(getApplicationContext(), R.raw.tracking_stopped);
        
        //Set the volume for each sound to max
        appReadySound.setVolume(volume,volume);
        moveLeftSound.setVolume(volume,volume);
        moveRightSound.setVolume(volume,volume);
        speedUpSound.setVolume(volume,volume);
        slowDownSound.setVolume(volume,volume);
        appStartedSound.setVolume(volume,volume);
        appResumedSound.setVolume(volume,volume);
        appClosedSound.setVolume(volume,volume);
        goodDistanceSound.setVolume(volume,volume);
        goodPositionSound.setVolume(volume,volume);
        lostMarkerSound.setVolume(volume,volume);
        appReadySound.setVolume(volume,volume);
        trackingStoppedSound.setVolume(volume,volume);
                
        appReadySound.start();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        if (appHasStarted) {
        	appResumedSound.start();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        
        // release objects
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        
        appReadySound.release();
        moveLeftSound.release();
        moveRightSound.release();
        speedUpSound.release();
        slowDownSound.release();
        appStartedSound.release();
        appResumedSound.release();
        appClosedSound.release();
        goodDistanceSound.release();
        goodPositionSound.release();
        lostMarkerSound.release();
        appReadySound.release();
        trackingStoppedSound.release();
        
        wl.release();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        if (first) {
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

    // button press callback
    public void startStopAppOnClick(View view) {
    	//App is not tracking
    	if (!appHasStarted) {
    		startStopBtn.setText(R.string.STOP_APP_STRING);
    		appHasStarted = true;
    		wl.acquire();
    		
    		//App has tracked at least once
    		if (appFirstRun) {
    			appResumedSound.start();
    		}
    		else{
                appStartedSound.start();
    			appFirstRun = true;
    		}
      	}
    	//App was in running state
    	else {
    		badDistance = false;
    		mLost = false;
    		safeStateFlag = false;
    		mPrevLocation = NODIR;
    		appHasStarted = false;
    		wl.release();
    		
    		startStopBtn.setText(R.string.START_APP_STRING);
    		trackingStoppedSound.start();
    	}
    }
    
    // callback for touching camera view to select color
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        
        if (!mIsColorSelected) {
	        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
	        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
	
	        int x = (int)event.getX() - xOffset;
	        int y = (int)event.getY() - yOffset;
	
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
	
	        /*Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
	                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");*/
	
	        mDetector.setHsvColor(mBlobColorHsv);
	        /*Log.i("HSV", mBlobColorHsv.val[0] + " " + mBlobColorHsv.val[1] +
	                " " + mBlobColorHsv.val[2] + " " + mBlobColorHsv.val[3]);*/
	        
	        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
	
	        touchedRegionRgba.release();
	        touchedRegionHsv.release();

	        mIsColorSelected = true;

	        // duplicates start button code
	        startStopBtn.setText(R.string.STOP_APP_STRING);
    		appHasStarted = true;
    		appFirstRun = true;
    		appStartedSound.start();
    		wl.acquire();
        }

        return false; // don't need subsequent touch events
    }

    // the marker is always represented as a circle
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
    
    // on each photo taken by the camera, happens repeatedly while app is tracking
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {    	
        mRgba = inputFrame.rgba();
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
                
                // look through possible contours for shape with biggest radius
                if (circle[i].mRadius > maxRad && (circle[i].mRadius >= MIN_RADIUS_HIGH ||
                     (this.safeStateFlag && circle[i].mRadius >= MIN_RADIUS_LOW))) {
               		maxRad = circle[i].mRadius;
               		bestMatch = i;
                }

                //Convert back to MatOfPoint and put the new values back into the contours list
                mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);                
            }
            
            if (bestMatch >= 0) {
            	myCircle = circle[bestMatch];
            	this.safeStateFlag = true;
            	Core.circle(mRgba, myCircle.mCenter, (int) myCircle.mRadius, CONTOUR_COLOR, 3);
            }
            else {
                this.safeStateFlag = false;
            }

            
         	// tracking algorithm is below
            // all thresholds should be changed to dynamic values based on video frame size
            // these values are based on max x coordinate = 800 and max y coordinate = 480
            // max x and y coordinates can be found in the log, probably from the openCV code
            
            
            // marker is detected
            if (this.safeStateFlag) {
            	timesPlayedStopSound = 0;
            	dirsPlayedLR = 0;
            	dirsPlayedFB = 0;
            	
            	/*// used for testing
	            Log.d("VIEWSIZE", "Point: X: " + myCircle.mCenter.x + " Y: " + myCircle.mCenter.y);
	            Log.d("VIEWSIZE", "Radius: " + myCircle.mRadius);*/
	           
	            Core.circle(mRgba, myCircle.mCenter, (int) myCircle.mRadius, CONTOUR_COLOR, 3);
	            
	            // prevLocation used to give left/right command if marker disappears from screen without passing thresholds
	            // this means the app will give correct left/right commands at very high turning speed/lateral speed 
	            // Y threshold used in case marker disappears off top or bottom of screen
	            if (myCircle.mCenter.y > 100 && myCircle.mCenter.y < 350) {
	            	if (myCircle.mCenter.x < 400) { // 0 deg from center
	 	            	mPrevLocation = LEFTDIR;
	 	            }
	 	            else {
	 	            	mPrevLocation = RIGHTDIR;
	 	            }
	            }
	            else if (!mLost) {
	            	mPrevLocation = NODIR;
	            }
	            
	            // applies if target is beyond on-screen L/R threshold
	            if (!mLost || this.dirTimer == TIMER_MAX) {
		            if (myCircle.mCenter.x < 100) {		// approx. 19 deg to left
		            	moveLeftSound.start();
	                    mLost = true;
	                    this.dirTimer = 0;
	                    this.dirsPlayedLR++;
	            		mPrevLocation = LEFTDIR;
	 	            }
	 	            else if (myCircle.mCenter.x > 700) {		// approx. 19 deg to right
	 	            	moveRightSound.start();
	                    mLost = true;
	                    this.dirTimer = 0;
	                    this.dirsPlayedLR++;
	 	            	mPrevLocation = RIGHTDIR;
	 	            }
	            }
	            
	            // If returning from a bad position
	            if (mLost) {
		            if (myCircle.mCenter.x > 250 && myCircle.mCenter.x < 550) {  // approx. 7 deg to left and right of center
		            	goodPositionSound.start();
		                mLost = false;
		                this.dirsPlayedLR = 0;
		                this.dirTimer = 0;
		            }
	            }
	            // Detecting a bad distance based on radius
	            // checks that marker is not on edge of screen, distorting radius
	            else if ((!badDistance && (myCircle.mCenter.x < 750 && myCircle.mCenter.x > 50 && 
	                  myCircle.mCenter.y > 50 && myCircle.mCenter.y < 400)) || this.dirTimer == TIMER_MAX) {
            		if (myCircle.mRadius > 75) { // approx. 3'8"
            		 	slowDownSound.start();
	                    badDistance = true;
	                    this.dirTimer = 0;
	                    this.prevDirFB = BACKDIR;
            		}
            		else if (myCircle.mRadius < 28) { // approx. 10'0"
            			speedUpSound.start();
        				badDistance = true;
        				this.dirTimer = 0;
        				this.prevDirFB = FRONTDIR;
            		}
            	}
	            // If previously detected a bad distance and returned from bad distance
	            else if ((badDistance && myCircle.mRadius < 65 && myCircle.mRadius > 45)) { // approx. 5'6" and 8'0"
	            	goodDistanceSound.start();
            		badDistance = false;
            		this.dirTimer = 0;
            		this.prevDirFB = NODIR;
            	}
            }
            
            // for distance commands to repeat while marker is off screen
            /*else if (badDistance && this.dirTimer == TIMER_MAX) {
                if (this.dirsPlayedFB >= NUM_PLAYED_MAXFB && timesPlayedStopSound < STOP_SOUND_MAX_PLAY) {
        			lostMarkerSound.start();
        			this.dirTimer = 0;
        			this.prevDirFB = NODIR;
        			timesPlayedStopSound++;
                }
  	          	else if (this.prevDirFB == BACKDIR) {
  	          		slowDownSound.start();
  	                  this.dirTimer = 0;
  	                  this.dirsPlayedFB++;
  	          	}
  	          	else if (this.prevDirFB == FRONTDIR) {
  	          		speedUpSound.start();
  	  				this.dirTimer = 0;
  	  				this.dirsPlayedFB++;
  	          	}

            }*/
            
            // for position commands to play while marker is off screen
            else if (!mLost || this.dirTimer == TIMER_MAX) {
        		if (this.dirsPlayedLR >= NUM_PLAYED_MAXLR || (mPrevLocation == NODIR && !mLost)) {
        			if (timesPlayedStopSound < STOP_SOUND_MAX_PLAY){
        				lostMarkerSound.start();
                    	this.dirTimer = 0;
                    	mLost = true;
                    	timesPlayedStopSound++;
        			}
            	}
        		else if (mPrevLocation == LEFTDIR) {
        			moveLeftSound.start();
                    this.dirTimer = 0;
                    this.dirsPlayedLR++;
                    mLost = true;
            	}
            	else if (mPrevLocation == RIGHTDIR) {
            		moveRightSound.start();
                    this.dirTimer = 0;
                    this.dirsPlayedLR++;
                    mLost = true;
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