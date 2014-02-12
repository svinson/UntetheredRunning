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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;


public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Mat                  binaryImg;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private EditText             stateText;

    private float 				 volume = 0.3f;
    private MediaPlayer			 mp;
    
    private MatOfPoint2f mMOP2f1; 
    private MatOfPoint2f mMOP2f2;
    
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                   // Log.i(TAG, "OpenCV loaded successfully");
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
      //  Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
       // Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize(320, 240);
        
        stateText = (EditText) findViewById(R.id.stateText);
        stateText.setText(R.string.NOT_TRACKING);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        binaryImg = new Mat();
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

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

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

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
    	
    	public boolean equals(Circle other) {
    		if(this.mCenter.y >= other.mCenter.y - 10 && this.mCenter.y <= other.mCenter.y + 10
    				&& this.mRadius > 20 && other.mRadius > 20) {
    			Log.d("EQ", "Center: X: " + this.mCenter.x + " Y: " + this.mCenter.y);
    			Log.d("EQ", "Center: X: " + other.mCenter.x + " Y: " + other.mCenter.y);
    			return true;
    		}
    		return false;
    	}
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        binaryImg = inputFrame.gray();
        mMOP2f1 = new MatOfPoint2f();
        mMOP2f2 = new MatOfPoint2f();
 
        double threshold = 120;
        Point leftBound = new Point(threshold, 0); //30 from left edge of view, with current #s
        Point rightBound = new Point(0, 0); //30 from right edge of view, with current #s
        boolean found = false;
      
        if (mIsColorSelected) {
            mRgba = mDetector.process(mRgba);
            if(false)
            	return binaryImg;
            List<MatOfPoint> contours = mDetector.getContours();
            Circle[] circle = new Circle[contours.size()];
            Circle myCircle = new Circle();
           // Log.e(TAG, "Contours count: " + contours.size());
            Point center; 
            float[] radius; 
            for(int i=0;i<contours.size();i++) {
				//Convert contours(i) from MatOfPoint to MatOfPoint2f
                contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
				//Processing on mMOP2f1 which is in type MatOfPoint2f
                Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, Imgproc.arcLength(mMOP2f1, true) * 0.02, true);
                Point[] points = mMOP2f2.toArray();
                for(Point x : points) {
                	//Log.d("Mine", "Point: X: " + x.x + " Y: " + x.y);
                	Core.circle(mRgba, x, 5, CONTOUR_COLOR, 3);
                }
               // Log.d("Mine", "Channels: " + mMOP2f2.total());
                //RotatedRect rec =  Imgproc.minAreaRect(mMOP2f2);
                center = new Point();
                radius = new float[1];
                Imgproc.minEnclosingCircle(mMOP2f2, center, radius);
                circle[i] = new Circle(center, radius[0]);
                Log.d("FOUND", "Circle: " + i + "X: " + center.x + "Y: " + center.y);
                if(points.length > 6)
                	Core.circle(mRgba, center, (int) radius[0], CONTOUR_COLOR, 3);
                //Core.circle(mRgba, center, 5, CONTOUR_COLOR, 3);
                //Convert back to MatOfPoint and put the new values back into the contours list
                mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);                
            	
                Log.d("Mine", "Radius Status" + checkDistance(radius[0]));
                Log.d("Center", "Center: X: " + center.x + " Y: " + center.y);
                Log.d("Center", "Radius: " + radius[0]);
            }
            
            for(int i=0; i < contours.size(); i++) {
            	Log.d("List", "Circle: " + i + "X: " + circle[i].mCenter.x + "Y: " + circle[i].mCenter.y);
            }
            
            for(int i=0; i< contours.size() - 1 && contours.size() > 1; i++) {
            	for(int j=i+1; j < contours.size(); j++) {
            		if(circle[i].equals(circle[j])) {
            			found = true;
            			Log.d("FOUNDMATCH", "i" + i + " j:" + j);
            			myCircle = circle[i];
            			//Log.d("Mine", "Center: X: " + myCircle.mCenter.x + " Y: " + myCircle.mCenter.y);
                        //Log.d("Mine", "Radius: " + myCircle.mRadius);
            		}
            	}
            }
            
            if(!found) 
            	return mRgba;
           
            Core.circle(mRgba, myCircle.mCenter, (int) myCircle.mRadius, CONTOUR_COLOR, 3);
            
            if (myCircle.mCenter.y < 40) {
                Log.d("Mine", "LEFT: Point: X: " + myCircle.mCenter.x + " Y: " + myCircle.mCenter.y);
                mp = MediaPlayer.create(getApplicationContext(), R.raw.rightbuzz);
                mp.setVolume(volume, volume);
                mp.start();
                stateText.setText(R.string.RIGHT);
            }
            if (myCircle.mCenter.y > 300) {
                Log.d("Mine", "RIGHT: Point: X: " + myCircle.mCenter.x + " Y: " + myCircle.mCenter.y);
                mp = MediaPlayer.create(getApplicationContext(), R.raw.leftbuzz);
                mp.setVolume(volume, volume);
                mp.start();
                stateText.setText(R.string.LEFT);
            }
            if (checkDistance(myCircle.mRadius) < 0) {
                mp = MediaPlayer.create(getApplicationContext(), R.raw.frontbuzz);
                mp.setVolume(volume, volume);
                mp.start();
                stateText.setText(R.string.FORWARD);
            }
            if (checkDistance(myCircle.mRadius) > 0) {
                mp = MediaPlayer.create(getApplicationContext(), R.raw.backbuzz);
                mp.setVolume(volume, volume);
                mp.start();
                stateText.setText(R.string.BACK);
            }

            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }
    
    private int checkDistance(float radius) {
    	int toReturn = 0;
    	
    	if (radius > 100)  // CHANGE THESE CONSTANTS
    		toReturn = (int)radius - 100;
    	else if (radius < 50)
    		toReturn = (int)radius - 50;
    	
        return toReturn;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
