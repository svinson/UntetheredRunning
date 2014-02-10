package org.opencv.samples.colorblobdetect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    final static String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent arg1) {
        Log.w(TAG, "starting service...");
		Intent myIntent = new Intent(context, ColorBlobDetectionActivity.class);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(myIntent);
    }

}