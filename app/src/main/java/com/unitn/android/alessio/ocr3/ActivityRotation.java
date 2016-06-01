package com.unitn.android.alessio.ocr3;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.isseiaoki.simplecropview.CropImageView;
import com.isseiaoki.simplecropview.callback.SaveCallback;

import java.io.File;

public class ActivityRotation extends AppCompatActivity {

    private ImageButton acceptPreview, rejectPreview, rotateLeft,rotateRight;
    private CropImageView imagePreview;
    private File file;
    private static int ROTATE_IMAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rotate);

        if(savedInstanceState != null){
            file = new File(savedInstanceState.getString("file"));
        }else {
            file = (File)getIntent().getExtras().get("file");
        }

        imagePreview = (CropImageView)findViewById(R.id.imagePreview);
        imagePreview.setInitialFrameScale(1.0f);
        imagePreview.setCropMode(CropImageView.CropMode.FREE);
        imagePreview.setGuideShowMode(CropImageView.ShowMode.NOT_SHOW);
        imagePreview.startLoad(Uri.fromFile(file), null);

        Toast.makeText(getApplicationContext(), "Rotate the image so the text is straight", Toast.LENGTH_LONG).show();

        imagePreview.setCompressFormat(Bitmap.CompressFormat.JPEG);

        rotateLeft = (ImageButton) findViewById(R.id.rotateLeft);
        rotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePreview.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D);
            }
        });

        rotateRight = (ImageButton) findViewById(R.id.rotateRight);
        rotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePreview.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
            }
        });

        acceptPreview = (ImageButton)findViewById(R.id.previewAccept);
        acceptPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent resultData = new Intent();
                imagePreview.startCrop(Uri.fromFile(file), null, new SaveCallback() {
                    @Override
                    public void onSuccess(Uri outputUri) {
                        resultData.putExtra("result", 1);
                        Log.v(data.getInstance().getTAG(),"URI:"+Uri.fromFile(file));
                        resultData.putExtra("file", file);
                        setResult(Activity.RESULT_OK, resultData);
                        finish();
                    }

                    @Override
                    public void onError() {
                        setResult(Activity.RESULT_CANCELED, resultData);
                        finish();
                    }
                });

            }
        });

        rejectPreview = (ImageButton)findViewById(R.id.previewCancel);
        rejectPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void startActivityRotate(File imgFile){
        Intent intent = new Intent(getApplicationContext(), ActivityRotation.class);
        intent.putExtra("file", imgFile);
        startActivityForResult(intent, ROTATE_IMAGE);
    }

    @Override
    public void onBackPressed() {
        Intent resultData = new Intent();
        resultData.putExtra("result", 0);
        setResult(Activity.RESULT_OK, resultData);
        finish();
        //super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putString("file", file.getAbsolutePath());
        file = null;
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
