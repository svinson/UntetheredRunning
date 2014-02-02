package org.opencv.samples.colorblobdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class ColorBlobDetector {
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25,50,50,0);
    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
    	Bitmap bmp = null;
    	Mat circles = new Mat();
    	Point pt;
    	int radius;
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        Log.d("Mine", "Lower: " + mLowerBound.val[2] + "Higher: " + mUpperBound.val[2]);
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        //Core.inRange(mHsvMat, new Scalar(20,100,150), new Scalar(70, 210, 200), mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        
        bmp = Bitmap.createBitmap(mDilatedMask.cols(), mDilatedMask.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mDilatedMask, bmp);
        /*try {
        	File f = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "test.bmp");
            FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory()
                    + File.separator + "test.bmp");
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
	     } catch (Exception e) {
	            e.printStackTrace();
	     }*/
        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        /*Imgproc.HoughCircles(mDilatedMask, circles, Imgproc.CV_HOUGH_GRADIENT, 2.0,100);// mDilatedMask.rows()/8);
        
        for (int x = 0; x < circles.cols(); x++) 
        {
	        double vCircle[] = circles.get(0,x);
	
	        if (vCircle == null)
	            break;
	
	        pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
	        radius = (int)Math.round(vCircle[2]);
	
	        // draw the found circle
	        Core.circle(rgbaImage, pt, radius, new Scalar(0,255,0), 3);
	        Core.circle(rgbaImage, pt, 3, new Scalar(0,0,255), 3);
        }*/
        
        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
