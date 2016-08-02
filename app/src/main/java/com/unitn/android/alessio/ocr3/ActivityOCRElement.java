package com.unitn.android.alessio.ocr3;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.io.File;

public class ActivityOCRElement extends AppCompatActivity {


    private static final int ROTATE_IMAGE = 1;
    private OCRElement ocrElement;
    private FragmentText textFr;
    private FragmentImage imageFr;
    private FrameLayout container;
    private DonutProgress progress;
    private int index;
    private RoundedImageView thumbnail;
    private ImageView fullImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocrelement);
        Toolbar toolbar = (Toolbar) findViewById(R.id.ocrElementToolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            ocrElement = savedInstanceState.getParcelable("ocrElement");    
        }else{
            index = getIntent().getExtras().getInt("ocrelement");
            ocrElement = data.getInstance().getOcrElements().get(index);
        }

        thumbnail = (RoundedImageView)findViewById(R.id.thumbnail);
        thumbnail.setImageBitmap(ocrElement.getThumbnail());
        thumbnail.setTransitionName("imageTransition"+index);

        fullImage = (ImageView)findViewById(R.id.maxImageView);
        fullImage.setImageBitmap(ocrElement.getImageFullRes());

        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        progress = (DonutProgress)findViewById(R.id.circle_progress);
        progress.setTransitionName("progressTransition"+index);

        container = (FrameLayout) findViewById(R.id.container);
        textFr = new FragmentText();
        textFr.setOcrElement(index);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, textFr).commit();
        }

        imageFr = new FragmentImage();
        imageFr.setOcrElement(ocrElement);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putParcelable("ocrElement", ocrElement);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.rescan:
                reScanImage();
                break;
            case R.id.image_rotate:
                rotateImage();
                break;
            case R.id.change_title:
                changeTitle();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        ActivityCompat.finishAfterTransition(this);
        //super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(!hasFocus){
            ocrElement.deleteImageFullRes();
            System.gc();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ocrelement, menu);
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent dati) {

        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case ROTATE_IMAGE:
                    int result = dati.getExtras().getInt("result");
                    if(result == 1){
                        data.getInstance().getOcrElements().get(index).setImageFile((File)dati.getExtras().get("file"));
                        reScanImage();
                    }else{
                    }
                    break;
            }
        }
    }

    private void reScanImage(){
        data.getInstance().getOcrElements().get(index).setProgress(0);
        data.getInstance().getOcrElements().get(index).setDate("Text parsing");
        data.getInstance().getOcrElements().get(index).setText("please wait");
        Intent intent = new Intent(getApplicationContext(), ServiceParser.class);
        intent.putExtra("ocrelement", index);
        startService(intent);
        finish();
    }

    private void rotateImage(){
        Toast.makeText(getApplicationContext(), "When the transformations are applied the image will be rescanned" , Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), ActivityRotation.class);
        intent.putExtra("file", data.getInstance().getOcrElements().get(index).getImageFile());
        startActivityForResult(intent, ROTATE_IMAGE);
    }


    private void changeTitle(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set a new title");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(data.getInstance().getOcrElements().get(index).getTitle());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                data.getInstance().getOcrElements().get(index).setTitle(input.getText().toString());
                ((TextView)findViewById(R.id.titleTextView)).setText(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}