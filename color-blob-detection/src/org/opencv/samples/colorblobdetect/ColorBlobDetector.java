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
import org.opencv.core.Size;
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
    Mat mMask1 = new Mat();
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

    public Mat process(Mat rgbaImage) {
    	Bitmap bmp = null;
    	Mat circles = new Mat();
    	Point pt;
    	int radius;
    	
    	Log.d("Color", "H Lower: " + mLowerBound.val[0] + "Higher: " + mUpperBound.val[0]);
    	Log.d("Color", "S Lower: " + mLowerBound.val[1] + "Higher: " + mUpperBound.val[1]);
    	Log.d("Color", "V Lower: " + mLowerBound.val[2] + "Higher: " + mUpperBound.val[2]);
    /*	mLowerBound.val[0] = 0;
        mUpperBound.val[0] = 10;

        mLowerBound.val[1] = 100;
        mUpperBound.val[1] = 150;

        mLowerBound.val[2] = 150;
        mUpperBound.val[2] = 255;

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;*/
    	
    	/*mLowerBound.val[0] = 30;
        mUpperBound.val[0] = 160;

        mLowerBound.val[1] = 0;
        mUpperBound.val[1] = 0;

        mLowerBound.val[2] = 0;
        mUpperBound.val[2] = 0;

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;*/
    	
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        //Imgproc.blur(mPyrDownMat, mPyrDownMat, new Size(2,2));
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        //Core.inRange(mHsvMat, new Scalar(225, 111, 205), new Scalar(255, 211, 205), mMask1);
        //Core.bitwise_xor(mMask, mMask1, mMask);
        //Core.inRange(mHsvMat, new Scalar(0,127,200), new Scalar(28, 226, 300), mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());
        Imgproc.pyrUp(mDilatedMask, rgbaImage);
        Imgproc.pyrUp(rgbaImage, rgbaImage);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        
       
        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
       
        
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
		return rgbaImage;
       
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
