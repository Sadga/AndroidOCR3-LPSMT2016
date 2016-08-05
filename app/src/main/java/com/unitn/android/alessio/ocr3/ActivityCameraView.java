package com.unitn.android.alessio.ocr3;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*private void initCamera(Camera cam){

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

}*/


public class ActivityCameraView extends AppCompatActivity {

    private final static String TAG = "Camera2testJ";
    private Size mPreviewSize;

    private TextureView mTextureView;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;

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
    private int width, height;
    private RelativeLayout previewFrame;
    private CameraBridgeViewBase mOpenCvCameraView;
    private int orientamento;

    private Button mBtnShot;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);//90
        ORIENTATIONS.append(Surface.ROTATION_90, 90);//0
        ORIENTATIONS.append(Surface.ROTATION_180, 180);//270
        ORIENTATIONS.append(Surface.ROTATION_270, 270);//180
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        mTextureView = (TextureView) findViewById(R.id.cameraView);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.e(TAG, "Shot clicked");
                takePicture();
            }

        });

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientamento = orientation;
                if (!buttonChanging) {
                    if (orientation < 290 && orientation > 250 && buttonRotation != 2) {
                        buttonLeft();
                    } else if (orientation < 110 && orientation > 70 && buttonRotation != 3) {
                        buttonRight();
                    } else if (orientation < 20 || orientation > 340 && buttonRotation != 1) {
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

        fabBack = (FloatingActionButton) findViewById(R.id.fabBack);
        fabBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        fabFlash = (FloatingActionButton) findViewById(R.id.fabFlash);
        fabFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

    }

    protected void takePicture() {
        Log.e(TAG, "takePicture");
        if (null == mCameraDevice) {
            Log.e(TAG, "mCameraDevice is null, return");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());

            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            final File file = new File(Environment.getExternalStorageDirectory() + "/DCIM", "pic.jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {

                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }

            };

            HandlerThread thread = new HandlerThread("CameraPicture");
            thread.start();
            final Handler backgroudHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, backgroudHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request, TotalCaptureResult result) {

                    super.onCaptureCompleted(session, request, result);
                    startPreview();
                }

            };

            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {

                    try {
                        session.capture(captureBuilder.build(), captureListener, backgroudHandler);
                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, backgroudHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
    }

    private void openCamera() {

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openCamera E");
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            manager.openCamera(cameraId, mStateCallback, null);//controllo fatto in precedenza quindi non necessario qui(ma continua a visualizzare errore)
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener(){

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable, width="+width+",height="+height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.e(TAG, "onSurfaceTextureUpdated");
        }

    };

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {

            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {

            Log.e(TAG, "onError");
        }

    };

    @Override
    protected void onPause() {

        Log.e(TAG, "onPause");
        super.onPause();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    protected void startPreview() {

        if(null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            Log.e(TAG, "startPreview fail, return");
            return;
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(null == texture) {
            Log.e(TAG,"texture is null, return");
            return;
        }

        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {

            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        transformImage(mTextureView.getWidth(), mTextureView.getHeight());

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {

                    mPreviewSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                    Toast.makeText(ActivityCameraView.this, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e) {

            e.printStackTrace();
        }
    }

    protected void updatePreview() {

        if(null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }

        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        //todo: back pressed
        setResult(Activity.RESULT_CANCELED);
        supportFinishAfterTransition();
    }

    /*private void startActivityRotate(File imgFile){
        mCamera.stopPreview();
        Intent intent = new Intent(getApplicationContext(), ActivityRotation.class);
        intent.putExtra("file", imgFile);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, (View)previewFrame, "photo");
        startActivityForResult(intent, ROTATE_IMAGE, options.toBundle());
    }*/

    /*private static File getOutputMediaFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(data.getInstance().getDataPath() + "images" + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }*/

    private void returnToMain(int result, File file){
        Intent resultData = new Intent();
        resultData.putExtra("result", result);
        resultData.putExtra("file", file);
        setResult(Activity.RESULT_OK, resultData);
        supportFinishAfterTransition();
    }

    private void transformImage(int width, int height){
        if(mPreviewSize == null || mTextureView == null){
            return;
        }
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.v(data.getInstance().getTAG(), "rotation = "+rotation);
        RectF textureRectF = new RectF(0, 0, width, height);
        RectF previewRectF = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180){
            previewRectF.offset(centerX - previewRectF.centerX(), centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float)width/mPreviewSize.getWidth(), (float)height/mPreviewSize.getHeight());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90*(rotation), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void buttonUp(){
        buttonChanging = true;
        ObjectAnimator button1Up = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), 0f);
        ObjectAnimator button2Up = ObjectAnimator.ofFloat(fabBack, "rotation", fab.getRotation(), 0f);
        ObjectAnimator button3Up = ObjectAnimator.ofFloat(fabFlash, "rotation", fab.getRotation(), 0f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(button1Up, button2Up, button3Up);
        animSet.setDuration(300);
        animSet.addListener(animatorListener);
        animSet.start();
        buttonRotation = 1;

    }

    private void buttonLeft(){
        buttonChanging = true;
        ObjectAnimator button1Left = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), 90f);
        ObjectAnimator button2Left = ObjectAnimator.ofFloat(fabBack, "rotation", fab.getRotation(), 90f);
        ObjectAnimator button3Left = ObjectAnimator.ofFloat(fabFlash, "rotation", fab.getRotation(), 90f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(button1Left, button2Left, button3Left);
        animSet.setDuration(300);
        animSet.addListener(animatorListener);
        animSet.start();
        buttonRotation = 2;
    }

    private void buttonRight(){
        buttonChanging = true;
        ObjectAnimator button1Right = ObjectAnimator.ofFloat(fab, "rotation", fab.getRotation(), -90f);
        ObjectAnimator button2Right = ObjectAnimator.ofFloat(fabBack, "rotation", fab.getRotation(), -90f);
        ObjectAnimator button3Right = ObjectAnimator.ofFloat(fabFlash, "rotation", fab.getRotation(), -90f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(button1Right, button2Right, button3Right);
        animSet.setDuration(300);
        animSet.addListener(animatorListener);
        animSet.start();
        buttonRotation = 3;
    }

    private Animator.AnimatorListener animatorListener  = new Animator.AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
        }
        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            buttonChanging = false;
        }
        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };
}