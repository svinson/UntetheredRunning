package org.opencv.samples.colorblobdetect;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private float 				 volume = 0.3f;
    private MediaPlayer			 mp = new MediaPlayer();
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private MatOfPoint2f mMOP2f1; 
    private MatOfPoint2f mMOP2f2;
    
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
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
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
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

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

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

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mMOP2f1 = new MatOfPoint2f();
        mMOP2f2 = new MatOfPoint2f();

        //Copied threshold code from last year's group
        double threshold = 120;
        Point leftBound = new Point(threshold, 0); //30 from left edge of view, with current #s
        Point rightBound = new Point(0, 0); //30 from right edge of view, with current #s
 
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Point center = new Point();
            float[] radius = new float[1];
            for(int i=0;i<contours.size();i++) {
				//Convert contours(i) from MatOfPoint to MatOfPoint2f
                contours.get(0).convertTo(mMOP2f1, CvType.CV_32FC2);
				//Processing on mMOP2f1 which is in type MatOfPoint2f
                Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 8, true);
                Point[] points = mMOP2f2.toArray();
                for(Point x : points) {
                	Log.d("Mine", "Point: X: " + x.x + " Y: " + x.y);
                	Core.circle(mRgba, x, 5, CONTOUR_COLOR, 3);
                }
               // Log.d("Mine", "Channels: " + mMOP2f2.total());
                //RotatedRect rec =  Imgproc.minAreaRect(mMOP2f2);
                Imgproc.minEnclosingCircle(mMOP2f2, center, radius);
                if(points.length > 6)
                	Core.circle(mRgba, center, (int) radius[0], CONTOUR_COLOR, 3);
                //Core.circle(mRgba, center, 5, CONTOUR_COLOR, 3);
                //Convert back to MatOfPoint and put the new values back into the contours list
                mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);
                Log.d("Mine", "Center: X: " + center.x + " Y: " + center.y);
                Log.d("Mine", "Radius: " + radius[0]);
            }
            
            
            //Object Threshold code
            
            //4 1/2 ft too far back go forward
 //           if (logoArray[0].width <= 52) { //52 is the width of the logo at 4 1/2 ft, found through testing
         	   //Sends buzz to front and plays "front" sound clip
//                mp = MediaPlayer.create(getApplicationContext(), R.raw.frontbuzz);
//                mp.setVolume(volume, volume);
 //       	       mp.start();
  //          }
            //2 1/2 ft too close go backward
  //          else if (logoArray[0].width >= 100) { //100 is the width of the logo at 2 1/2 ft, found through testing
         	 //Sends buzz to back and plays "back" sound clip
    //            mp = MediaPlayer.create(getApplicationContext(), R.raw.backbuzz);
     //           mp.setVolume(volume, volume);
       // 	       mp.start();
       //     }
            //move left
            if (center.x <= leftBound.x) { //if logo crosses to left of left bound, left feedback
         	 //Sends buzz to left and plays "left" sound clip

         	   mp = MediaPlayer.create(getApplicationContext(), R.raw.leftbuzz);
         	   mp.setVolume(volume, volume);
        	   mp.start();
            }
            //move right
            else if (center.x >= rightBound.x) { //if logo crosses to right of right bound, right feedback
         	 //Sends buzz to right and plays "right" sound clip
         	   mp = MediaPlayer.create(getApplicationContext(), R.raw.rightbuzz);
         	   mp.setVolume(volume, volume);
        	   mp.start();
            }
            //good position
            else {

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
