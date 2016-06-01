package com.unitn.android.alessio.ocr3;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ActivityCameraView extends AppCompatActivity {

    private static final int ROTATE_IMAGE = 2;
    private static int FOCUS_AREA_SIZE = 300;
    private OrientationEventListener mOrientationListener;
    private int buttonRotation = 1;
    private FloatingActionButton fab, fabBack, fabFlash;
    private ImageView takenPhoto;
    private Camera mCamera;
    private int photoWidth, photoHeight;
    private boolean buttonChanging = false;
    private CameraPreview mPreview;
    private boolean flashActive = false;
    private int width,height;
    private RelativeLayout previewFrame;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[]dati,Camera camera){
            File pictureFile=getOutputMediaFile();
            if(pictureFile==null){
                Log.d(data.getInstance().getTAG(),"Errore nel salvataggio dell' immagine");
                return;
            }

            Bitmap bmp= BitmapFactory.decodeByteArray(dati,0,dati.length);
            mCamera.stopPreview();
            Bitmap tmpImage= util.rotateBitmap(bmp,90);
            pictureFile= util.saveBitmap(tmpImage,pictureFile);
            Log.v(data.getInstance().getTAG(),"Saved Image passing to activity: "+pictureFile.getAbsolutePath());
            startActivityRotate(pictureFile);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if(!buttonChanging){
                    if(orientation < 290 && orientation > 250 && buttonRotation != 2){
                        buttonLeft();
                    }else if(orientation < 110 && orientation > 70 && buttonRotation != 3){
                        buttonRight();
                    }else if(orientation < 20 || orientation > 340 && buttonRotation != 1){
                        buttonUp();
                    }
                }
            }
        };

        if (mOrientationListener.canDetectOrientation() == true) {
            Log.v(data.getInstance().getTAG(), "Can detect orientation");
            mOrientationListener.enable();
        } else {
            Log.v(data.getInstance().getTAG(), "Cannot detect orientation");
            mOrientationListener.disable();
        }

        fabBack = (FloatingActionButton)findViewById(R.id.fabBack);
        fabBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        fabFlash = (FloatingActionButton)findViewById(R.id.fabFlash);
        fabFlash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                toggleFlash();
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCamera!=null){
                    mCamera.takePicture(null, null, mPicture);
                }
            }

        });

        new cameraInitTask().execute();

    }

    private void initCamera(Camera cam){
        mCamera = cam;

        if (mCamera == null) {
            Log.w(data.getInstance().getTAG(), "Impossibile aprire la fotocamera");
            return;
        }

        Log.v(data.getInstance().getTAG(), "Camera OK");

        Camera.Parameters params = mCamera.getParameters();
        final List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            Log.v(data.getInstance().getTAG(), "Auto Focus OK");
        }

        photoHeight = 0;
        photoWidth = 0;
        float base = (16f / 9f);
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            float format = ((float) size.width / (float) size.height);
            if (format == base) {
                Log.v(data.getInstance().getTAG(), "possible photo size: " + size.width + "X" + size.height + " [" + (format == base ? "16:9" : "4:3") + "]");
                if (Math.abs(size.width - 4128) < Math.abs(photoWidth - 4128)) {
                    photoWidth = size.width;
                    photoHeight = size.height;
                }
            }
        }

        params.setPictureSize(photoWidth, photoHeight);
        params.setPreviewSize(1280, 720);

        mCamera.setParameters(params);
        Log.v(data.getInstance().getTAG(), "Photo size: " + mCamera.getParameters().getPictureSize().width + "X" + mCamera.getParameters().getPictureSize().height);

        setCameraDisplayOrientation(0, 0, mCamera);

        mPreview = new CameraPreview(mCamera, getApplicationContext());
        previewFrame = (RelativeLayout) findViewById(R.id.cameraPreview);
        previewFrame.addView(mPreview);

        previewFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mCamera != null) {
                    mCamera.cancelAutoFocus();
                    Rect focusRect = calculateFocusArea(event.getX(), event.getY());

                    Camera.Parameters parameters = mCamera.getParameters();
                    if (parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    if (parameters.getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                        mylist.add(new Camera.Area(focusRect, 1000));
                        parameters.setFocusAreas(mylist);
                    }

                    try {
                        mCamera.cancelAutoFocus();
                        mCamera.setParameters(parameters);
                        mCamera.startPreview();
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                                    Camera.Parameters parameters = camera.getParameters();
                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                    if (parameters.getMaxNumFocusAreas() > 0) {
                                        parameters.setFocusAreas(null);
                                    }
                                    camera.setParameters(parameters);
                                    camera.startPreview();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        findViewById(R.id.outerFrame).setVisibility(View.INVISIBLE);
        setResult(Activity.RESULT_CANCELED);
        supportFinishAfterTransition();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(!hasFocus){
            flashActivate(false);
        }
        super.onWindowFocusChanged(hasFocus);
    }

    private void startActivityRotate(File imgFile){
        Intent intent = new Intent(getApplicationContext(), ActivityRotation.class);
        intent.putExtra("file", imgFile);
        startActivityForResult(intent, ROTATE_IMAGE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent dati) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ROTATE_IMAGE: {
                    int result = dati.getExtras().getInt("result");
                    if(result == 1){
                        Log.v(data.getInstance().getTAG(), "result 1");
                        File file = (File)dati.getExtras().get("file");
                        returnToMain(result, file);
                    }else{
                        Log.v(data.getInstance().getTAG(), "result 0");
                    }
                }
            }
        }
    }

    private static File getOutputMediaFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(data.getInstance().getDataPath() + "images" + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }

    private void returnToMain(int result, File file){
        Intent resultData = new Intent();
        resultData.putExtra("result", result);
        resultData.putExtra("file", file);
        setResult(Activity.RESULT_OK, resultData);
        supportFinishAfterTransition();
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / mPreview.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / mPreview.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }

    private void setCameraDisplayOrientation(int degrees, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void toggleFlash(){
        if(flashActive){
            flashActivate(false);
        }else{
            flashActivate(true);
        }
    }

    private void flashActivate(boolean active){
        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) && mCamera != null){
            if(active){
                Camera.Parameters params = mCamera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(params);
                fabFlash.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_on_white_24px));
                flashActive = true;
            }else{
                Camera.Parameters params = mCamera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(params);
                fabFlash.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_off_white_24px));
                flashActive = false;
            }
        }else{
            Toast.makeText(getApplicationContext(), "Flash not supported", Toast.LENGTH_SHORT);
        }
    }

    private void buttonUp(){
        buttonChanging = true;
        ObjectAnimator button1Up = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), 0f);
        ObjectAnimator button2Up = ObjectAnimator.ofFloat(fabBack, "rotation", fab.getRotation(), 0f);
        ObjectAnimator button3Up = ObjectAnimator.ofFloat(fabFlash, "rotation", fab.getRotation(), 0f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(button1Up, button2Up, button3Up);
        animSet.setDuration(300);
        animSet.start();
        buttonRotation = 1;
        buttonChanging = false;
    }

    private void buttonLeft(){
        buttonChanging = true;
        ObjectAnimator button1Left = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), 90f);
        ObjectAnimator button2Left = ObjectAnimator.ofFloat(fabBack, "rotation", fab.getRotation(), 90f);
        ObjectAnimator button3Left = ObjectAnimator.ofFloat(fabFlash, "rotation", fab.getRotation(), 90f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(button1Left, button2Left, button3Left);
        animSet.setDuration(300);
        animSet.start();
        buttonRotation = 2;
        buttonChanging = false;
    }

    private void buttonRight(){
        buttonChanging = true;
        ObjectAnimator button1Right = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), -90f);
        ObjectAnimator button2Right = ObjectAnimator.ofFloat(fabBack, "rotation", fab.getRotation(), -90f);
        ObjectAnimator button3Right = ObjectAnimator.ofFloat(fabFlash, "rotation", fab.getRotation(), -90f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(button1Right, button2Right, button3Right);
        animSet.setDuration(300);
        animSet.start();
        buttonRotation = 3;
        buttonChanging = false;
    }

    private class cameraInitTask extends AsyncTask<Void, Void, Camera> {
        protected Camera doInBackground(Void... voids) {
            Log.v(data.getInstance().getTAG(), "async");
            return util.getCameraInstance(getApplicationContext());
        }

        @Override
        protected void onPostExecute(Camera cam) {
            initCamera(cam);
            super.onPostExecute(cam);
        }
    }
}


