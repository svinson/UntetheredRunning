package org.opencv.samples.colorblobdetect;

import java.util.ArrayList;
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
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class ColorBlobDetectionActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private Mat                  mRgba;
    private Mat					 thresholdImg;
    private Scalar               CONTOUR_COLOR;
    private MatOfPoint2f mMOP2f1; 
    private MatOfPoint2f mMOP2f2;
    private Point pt;
	private int radius;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
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
        mOpenCvCameraView.setMaxFrameSize(320, 240);
        //mOpenCvCameraView.setMaxFrameSize(640, 480);
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
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mMOP2f1 = new MatOfPoint2f();
        mMOP2f2 = new MatOfPoint2f();
    	thresholdImg = inputFrame.gray();
        Mat circles = new Mat();
        //Imgproc.pyrDown(thresholdImg, thresholdImg);
        Imgproc.GaussianBlur(thresholdImg, thresholdImg, new Size(9,9), 2, 2);
        //Imgproc.GaussianBlur(thresholdImg, thresholdImg, new Size(9,9), 2, 2);

        //Imgproc.pyrDown(thresholdImg, thresholdImg);
        //Imgproc.HoughCircles(thresholdImg, circles, Imgproc.CV_HOUGH_GRADIENT, 2.0, thresholdImg.height()/4);
        Imgproc.HoughCircles(thresholdImg, circles, Imgproc.CV_HOUGH_GRADIENT, 2.0, thresholdImg.height()/4, 150, 120, 0, 0);
        //Imgproc.HoughCircles(thresholdImg, circles, Imgproc.CV_HOUGH_GRADIENT, 2.0, thresholdImg.height()/4, 80, 100, 0, 0);
        for (int x = 0; x < circles.cols(); x++) 
        {
	        double vCircle[] = circles.get(0,x);
	
	        if (vCircle == null)
	            break;
	
	        pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
	        radius = (int)Math.round(vCircle[2]);
	        Log.d("Mine", "Point: X: " + pt.x + " Y: " + pt.y);
	        Log.d("Mine", "Radius: " + radius);
	        // draw the found circle
	        Core.circle(mRgba, pt, radius, new Scalar(0,255,0), 3);
	        Core.circle(mRgba, pt, 3, new Scalar(0,0,255), 3);
        }
        Log.d("HEYO", "NUM: " + circles.cols());

    /*	List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    	Mat mHierarchy = new Mat();
        float[] radi = new float[1];
        pt = new Point();
        Imgproc.blur(thresholdImg, thresholdImg, new Size(3,3));
       // Imgproc.dilate(thresholdImg, thresholdImg, new Mat());
        Imgproc.Canny(thresholdImg, thresholdImg, 30, 60);
        //Imgproc.HoughLinesP(thresholdImg, thresholdImg, 1, 3.14/180, 100);
       // Imgproc.dilate(thresholdImg, thresholdImg, new Mat());
        Imgproc.findContours(thresholdImg, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for(int i=0;i<contours.size();i++) {
	        contours.get(0).convertTo(mMOP2f1, CvType.CV_32FC2);
	        Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 3, true);
	        Imgproc.minEnclosingCircle(mMOP2f2, pt, radi);
	        Point[] points = mMOP2f2.toArray();
	        if(points.length > 6) {
	        	Core.circle(mRgba, pt, (int) radi[0], CONTOUR_COLOR, 3);
	        	
	        }
        }*/
        //Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
        
        /*
         * List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    	Mat mHierarchy = new Mat();
        float[] radi = new float[1];
        pt = new Point();
        Imgproc.Canny(thresholdImg, thresholdImg, 100, 300);
        Imgproc.findContours(thresholdImg, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for(int i=0;i<contours.size();i++) {
			//Convert contours(i) from MatOfPoint to MatOfPoint2f
            contours.get(0).convertTo(mMOP2f1, CvType.CV_32FC2);
			//Processing on mMOP2f1 which is in type MatOfPoint2f
            Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 50, true);
            Point[] points = mMOP2f2.toArray();
            for(Point x : points) {
            	Log.d("Mine", "Point: X: " + x.x + " Y: " + x.y);
            	Core.circle(mRgba, x, 5, CONTOUR_COLOR, 3);
            }
           // Log.d("Mine", "Channels: " + mMOP2f2.total());
            //RotatedRect rec =  Imgproc.minAreaRect(mMOP2f2);
            Imgproc.minEnclosingCircle(mMOP2f2, pt, radi);
            if(points.length > 6)
            	Core.circle(mRgba, pt, (int) radi[0], CONTOUR_COLOR, 3);
            //Core.circle(mRgba, center, 5, CONTOUR_COLOR, 3);
            //Convert back to MatOfPoint and put the new values back into the contours list
            mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);
            Log.d("Mine", "Center: X: " + pt.x + " Y: " + pt.y);
            Log.d("Mine", "Radius: " + radi[0]);
        }
        
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
         */
        
        
        //if (mIsColorSelected) {
            //mDetector.process(mRgba);
            /*List<MatOfPoint> contours = mDetector.getContours();
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
            
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);*/
       // }

        return mRgba;
        //return thresholdImg;
    }

}
