package com.unitn.android.alessio.ocr3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ActivityOCRElement extends AppCompatActivity {


    private static final int ROTATE_IMAGE = 1;
    private PhotoViewAttacher mAttacher;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private OCRElement ocrElement;
    private FragmentText textFr;
    private FragmentImage imageFr;
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocrelement);

        data.getInstance().setUiHandler(new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(((String)msg.obj).compareTo("SetImage") == 0){
                    final ImageView image = (ImageView)findViewById(R.id.imageView);
                    image.setImageBitmap(util.resizeBmp(util.optimizeImage(ocrElement.getImageFullRes(), false),ocrElement.getImageFullRes().getWidth()/2, ocrElement.getImageFullRes().getHeight()/2));
                    mAttacher = new PhotoViewAttacher(image);
                }else if(((String)msg.obj).compareTo("updateMAttacher") == 0){
                    mAttacher.update();
                }
            }

        });

        index = getIntent().getExtras().getInt("ocrelement");
        ocrElement = data.getInstance().getOcrElements().get(index);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        textFr = new FragmentText();
        textFr.setOcrElement(index);
        imageFr = new FragmentImage();
        imageFr.setOcrElement(ocrElement);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

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
    public void onWindowFocusChanged(boolean hasFocus) {
        if(!hasFocus){
            ocrElement.deleteImageFullRes();
            System.gc();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ocrelement, menu); //todo: Gestione voci menu
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

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return textFr;
                case 1:
                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    return imageFr;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Text";
                case 1:
                    return "Photo";
            }
            return null;
        }
    }
}