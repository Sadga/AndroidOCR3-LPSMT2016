package com.unitn.android.alessio.ocr3;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class ActivityMain extends AppCompatActivity {

    private static final int SELECT_PICTURE = 1;
    private static final int TAKE_PHOTO = 2;
    private static final int ROTATE_IMAGE = 3;
    private customAdapter adapter;
    private static String preference_file_key = "com.unitn.android.alessio.ocr3.PREFERENCES";
    private FloatingActionButton fab, fab2;
    private OCRElement tempElem;
    private boolean fabActive = true;
    private boolean cameraOk = false;
    private SwipyRefreshLayout mRefreshLayout;


    @Override @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(data.getInstance().getTAG(), "init");

        super.onCreate(savedInstanceState);

        if (!OpenCVLoader.initDebug()) {// opencv INIT
            data.getInstance().setUsingOpenCV(false);
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            data.getInstance().setUsingOpenCV(true);
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        data.getInstance().setAsset(getAssets());
        data.getInstance().setDataPath(Environment.getExternalStorageDirectory().toString() + "/ocr/");

        data.getInstance().setSharedPref(getApplicationContext().getSharedPreferences(
                preference_file_key, Context.MODE_PRIVATE));

        util.restorePrefs(); // Restore Preferences

        //ANDROID 6 PERMISSION MANAGEMENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(getApplicationContext().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED &&
                    getApplicationContext().checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_DENIED){
                requestPermissions(new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"},3);
            }else if (getApplicationContext().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},1);
            }else{
                if(util.initFiles() != 0){//inizializzazione file base (se esistono ritorna positivo)
                    Toast.makeText(getApplicationContext(), "Impossible to init files, please check permissions", Toast.LENGTH_LONG);
                    finish();
                }
            }
        }else{
            if(util.initFiles() != 0){//inizializzazione file base (se esistono ritorna positivo)
                Toast.makeText(getApplicationContext(), "Impossible to init files, please check permissions", Toast.LENGTH_LONG);
                finish();
            }
            cameraOk = true;
        }
        //ANDROID 6 PERMISSION MANAGEMENT


        ListView listView = (ListView)findViewById(R.id.listView);

        if(savedInstanceState != null){ //TODO: RESTORE DATA SAVED
            //Altre cose da ripristinare savedinstance
            adapter = data.getInstance().getAdapter();
            listView.setAdapter(adapter);
        }else if(util.restoreData(getApplicationContext()) == 0){
            adapter = new customAdapter(data.getInstance().getOcrElements(), getApplicationContext());
            listView.setAdapter(adapter);
        }else{
            data.getInstance().setOcrElements(new ArrayList<OCRElement>());
            adapter = new customAdapter(data.getInstance().getOcrElements(), getApplicationContext());
            listView.setAdapter(adapter);
        }

        data.getInstance().setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id){

                OCRElement item = (OCRElement)adapter.getItemAtPosition(position);

                if(item.getDate().compareTo("Text parsing")!=0){
                    Log.v(data.getInstance().getTAG(), "item: "+item.getDate());

                    Intent intent = new Intent(getApplicationContext(), ActivityOCRElement.class);
                    intent.putExtra("ocrelement", position);
                    startActivity(intent);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                entryMenu(pos);
                return true;
            }
        });

        mRefreshLayout = (SwipyRefreshLayout)findViewById(R.id.swipyrefreshlayout);
        mRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                Toast.makeText(getApplicationContext(), "Refresh", Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fabActive){
                    if(cameraOk){
                        startActivityCamera();
                    }else{
                        Toast.makeText(getApplicationContext(), "Camera not initialized please provide permission or restart the app", Toast.LENGTH_SHORT).show();
                        cameraPermission();
                    }
                }
            }
        });

        fab2 = (FloatingActionButton)findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);
            }
        });

    }

    //todo REMOVE
    private void addElement(){
        tempElem = new OCRElement();
        tempElem.setImageFile(new File("/storage/emulated/0/ocr/images/IMG_20160530_155223.jpg"));
        Log.v(data.getInstance().getTAG(), tempElem.getImageFile().getAbsolutePath());
        tempElem.setDate("Titolo di Prova");
        tempElem.setText("Normative\n" +
                "\n" +
                "Invia feedback; sul dispositivo\n" +
                "Numero modello\n" +
                "\n" +
                "Nexus 5\n" +
                "\n" +
                "Veraîpne di Android\n" +
                "\n" +
                "M\n" +
                "\n" +
                "Versînne; banda di b”ase\n" +
                "M8974A»2.0…50.226\n" +
                "\n" +
                "veîsîpne Kernel\n" +
                "3.4.0-gb87733d\n" +
                "android-bui|d@wp ee24.hot.corpgoogle.com #1\n" +
                "Thu May 21 0511 9113 UTC 2015\n" +
                "Numero build\n" +
                "\n" +
                "Mp2440");
        tempElem.setProgress(100);
        tempElem.setConfidence(87);
        data.getInstance().getOcrElements().add(tempElem);
        tempElem = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(!hasFocus){
            util.saveData(getApplicationContext());
            util.savePrefs();
        }else {
            if(adapter != null){
                adapter.notifyDataSetChanged();
                data.getInstance().setUiHandler(new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        if(((String)msg.obj).compareTo("Refresh") == 0){
                            adapter.notifyDataSetChanged();
                        }
                    }
            });
            }
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onBackPressed() {
        util.savePrefs();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //util.saveData();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //util.saveData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent setting = new Intent(getApplicationContext(), ActivitySettings.class);
            startActivity(setting);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(util.initFiles() != 0){//inizializzazione file base (se esistono ritorna positivo)
                        Toast.makeText(getApplicationContext(), "Impossible to init files, please chech permissions", Toast.LENGTH_LONG).show();
                        finish();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Impossible to init files, please chech permissions", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
            case 2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraOk = true;
                } else {
                    cameraOk = false;
                }
                return;
            }
            case 3: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraOk = true;
                }else{
                    cameraOk = false;
                }
                if (grantResults.length > 1
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    if(util.initFiles() != 0){//inizializzazione file base (se esistono ritorna positivo)
                        Toast.makeText(getApplicationContext(), "Impossible to init files, please chech permissions", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Impossible to init files, please chech permissions", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
        }
    }

    @TargetApi(23)
    private void cameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{"android.permission.CAMERA"}, 2);
            } else {
                cameraOk = true;
            }
        }
    }

    private void entryMenu(int index){
        final int position = index;
        CharSequence elements[] = new CharSequence[] {"Delete element", "Change title", "Scan again"};
        android.app.AlertDialog.Builder menu = new android.app.AlertDialog.Builder(this);
        menu.setTitle("Select an action");
        menu.setItems(elements, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0: {
                        data.getInstance().getOcrElements().remove(position);
                        return;
                    }
                    case 1: {
                        changeTitle(position);
                        return;
                    }
                    case 2: {
                        parseElement(position);
                        return;
                    }
                }
            }
        });
        menu.show();
    }

    private void changeTitle(int n){
        final int index = n;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Set a new title");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(data.getInstance().getOcrElements().get(index).getTitle());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                data.getInstance().getOcrElements().get(index).setTitle(input.getText().toString());
                adapter.notifyDataSetChanged();
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

    private void startActivityCamera(){
        Intent intent = new Intent(getApplicationContext(), ActivityCameraView.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, (View)fab, "mainFab");
        startActivityForResult(intent, TAKE_PHOTO, options.toBundle());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent dati) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_PICTURE: {
                    Log.v(data.getInstance().getTAG(), "IN 1");
                    Uri selectedImageUri = dati.getData();
                    String path = getPath(selectedImageUri);
                    Log.v(data.getInstance().getTAG(), "Path = " + path);
                    if (path != null) {
                        File imgFile = new File(path);
                        Log.v(data.getInstance().getTAG(), imgFile.getAbsolutePath());
                        if (!imgFile.getAbsolutePath().contains("ocr/images")) {
                            Bitmap image = util.openImageFile(imgFile);
                            imgFile = util.saveBitmap(image, getOutputMediaFile());//copy image in my folder
                        } else {
                            Toast.makeText(getApplicationContext(), "Image in app folder", Toast.LENGTH_SHORT).show();
                        }
                        startActivityRotate(imgFile);
                    } else {
                        Toast.makeText(getApplicationContext(), "Impossible to read the selected image", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                case ROTATE_IMAGE: {
                    int result = dati.getExtras().getInt("result");
                    if(result == 1){
                        Log.v(data.getInstance().getTAG(), "result 1");
                        File file = (File)dati.getExtras().get("file");
                        addAndParseElement(file);
                    }else{
                        Log.v(data.getInstance().getTAG(), "result 0");
                    }
                    return;
                }
                case TAKE_PHOTO: {
                    int result = dati.getExtras().getInt("result");
                    if(result == 1){
                        Log.v(data.getInstance().getTAG(), "result 1");
                        File file = (File)dati.getExtras().get("file");
                        addAndParseElement(file);
                    }else{
                        Log.v(data.getInstance().getTAG(), "result 0");
                    }
                    return;
                }
            }
        }
    }

    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    private static File getOutputMediaFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(data.getInstance().getDataPath() + "images" + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }

    private void startActivityRotate(File imgFile){
        Intent intent = new Intent(getApplicationContext(), ActivityRotation.class);
        intent.putExtra("file", imgFile);
        startActivityForResult(intent, ROTATE_IMAGE);
    }

    private void parseElement(int index){
        data.getInstance().getOcrElements().get(index).setProgress(0);
        data.getInstance().getOcrElements().get(index).setDate("Text parsing");
        data.getInstance().getOcrElements().get(index).setText("please wait");
        Intent intent = new Intent(getApplicationContext(), ServiceParser.class);
        intent.putExtra("ocrelement", index);
        startService(intent);
    }

    private void addAndParseElement(File img){
        tempElem = new OCRElement();
        Log.v(data.getInstance().getTAG(), img.getAbsolutePath());
        tempElem.setImageFile(img);
        data.getInstance().getOcrElements().add(tempElem);
        tempElem = null;
        data.getInstance().getOcrElements().get(data.getInstance().getOcrElements().size()-1).setDate("Text parsing");
        data.getInstance().getOcrElements().get(data.getInstance().getOcrElements().size()-1).setText("please wait");
        adapter.notifyDataSetChanged();

        parseElement(data.getInstance().getOcrElements().size()-1);
    }
}
