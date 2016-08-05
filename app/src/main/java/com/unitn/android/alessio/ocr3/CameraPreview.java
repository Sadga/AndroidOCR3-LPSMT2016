package com.unitn.android.alessio.ocr3;

/**
 * Created by alessio on 28/04/16.
 */

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


/** A basic Camera preview class */
public class CameraPreview implements CameraBridgeViewBase.CvCameraViewListener2 {
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.v(data.getInstance().getTAG(), width+", "+height);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        return mRgba; // This function must return

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }
}