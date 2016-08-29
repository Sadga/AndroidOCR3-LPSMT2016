package com.unitn.android.alessio.ocr3;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by alessio on 27/04/16.
 */
public class util {
    private static String preference_lang = "com.unitn.android.alessio.ocr3.PREFERENCES.lang";
    private static String prefTutorialMain = "com.unitn.android.alessio.ocr3.PREFERENCES.tutorial.main";
    private static String prefTutorialElem = "com.unitn.android.alessio.ocr3.PREFERENCES.tutorial.elem";
    private static String prefTutorialParse = "com.unitn.android.alessio.ocr3.PREFERENCES.tutorial.parse";


    public static Bitmap resizeBmp(Bitmap img, int width, int height) {
        return Bitmap.createScaledBitmap(img, width, height, true);
    }

    public static Bitmap rotateBitmap(Bitmap img, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public static Bitmap convertBW(Bitmap img) {
        Bitmap dest = Bitmap.createBitmap(
                img.getWidth(), img.getHeight(), img.getConfig());

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int pixelColor = img.getPixel(x, y);
                int pixelAlpha = Color.alpha(pixelColor);
                int pixelRed = Color.red(pixelColor);
                int pixelGreen = Color.green(pixelColor);
                int pixelBlue = Color.blue(pixelColor);

                int pixelBW = (pixelRed + pixelGreen + pixelBlue) / 3;
                int newPixel = Color.argb(
                        pixelAlpha, pixelBW, pixelBW, pixelBW);

                dest.setPixel(x, y, newPixel);
            }
        }

        return dest;
    }


    public static Bitmap optimizeImage(Bitmap img, boolean deNoise) {

        //deNoise = true;//todo remove!!

        if (!data.getInstance().isUsingOpenCV()) {
            Log.v(data.getInstance().getTAG(), "OPENCV OPEN FAILED");
            return convertGS(img);
        } else {
            Bitmap result = img;
            Mat imageMat = new Mat();
            Utils.bitmapToMat(img, imageMat);
            Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);

            //Imgproc.GaussianBlur(imageMat, imageMat, new Size(3, 3), 0);
            Imgproc.adaptiveThreshold(imageMat, imageMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 51, 15);//51, 15

            if(deNoise){
                Log.v(data.getInstance().getTAG(), "Start Denoise");
                long startTime = System.currentTimeMillis();
                Photo.fastNlMeansDenoising(imageMat, imageMat, 7, 7, 21);//7, 21
                long stopTime = System.currentTimeMillis();
                long elapsedTime = stopTime - startTime;
                Log.v(data.getInstance().getTAG(), "Finish Denoise ("+(elapsedTime/1000)+"s)");
            }

            Core.addWeighted(imageMat, 0.8, imageMat, 0.2, 1, imageMat);

            //Imgproc.threshold(imageMat, imageMat, 200, 255, Imgproc.THRESH_OTSU);
            //Imgproc.threshold(imageMat, imageMat, 0, 255, Imgproc.THRESH_OTSU);
            Utils.matToBitmap(imageMat, result);
            return result;
        }
    }

    public static Bitmap convertGS(Bitmap img) {
        int width, height;
        height = img.getHeight();
        width = img.getWidth();

        Bitmap dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(img, 0, 0, paint);
        return dest;
    }

    public static Bitmap openImageFile(File image) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        //options.inSampleSize = 8;
        return BitmapFactory.decodeFile(image.getAbsolutePath(), options);
    }

    public static Bitmap openImageFileThumbnail(File image) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 8;
        return BitmapFactory.decodeFile(image.getAbsolutePath(), options);
    }

    @TargetApi(23)
    public static int initFiles() {
        String DATA_PATH = data.getInstance().getDataPath();
        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/", DATA_PATH + "images/", DATA_PATH + "csv/"};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(data.getInstance().getTAG(), "ERROR: Creation of directory " + path + " on sdcard failed");
                    return 1;
                } else {
                    Log.v(data.getInstance().getTAG(), "Created directory " + path + " on sdcard");
                }
            }
        }
        return checkDB();
    }

    public static int checkDB() {
        String DATA_PATH = data.getInstance().getDataPath();
        String lang = data.getInstance().getLanguage();
        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = data.getInstance().getAsset();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(data.getInstance().getTAG(), "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(data.getInstance().getTAG(), "Was unable to copy " + lang + " traineddata " + e.toString());
                return 2;
            }
        }
        return 0;
    }

    public static File saveBitmap(Bitmap bitmapImage, File path){

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static File saveText(String str, File path){
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(str.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static void savePrefs(){
        SharedPreferences sharedPref = data.getInstance().getSharedPref();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(preference_lang, data.getInstance().getLanguage());
        editor.putBoolean(prefTutorialMain, data.getInstance().isTutorialMain());
        editor.putBoolean(prefTutorialElem, data.getInstance().isTutorialOCRElement());
        editor.putBoolean(prefTutorialParse, data.getInstance().isTutorialStringParser());
        editor.commit();
    }

    public static void restorePrefs(){
        SharedPreferences sharedPref = data.getInstance().getSharedPref();
        data.getInstance().setLanguage(sharedPref.getString(preference_lang, "ita"));
        data.getInstance().setTutorialMain(sharedPref.getBoolean(prefTutorialMain, true));
        data.getInstance().setTutorialOCRElement(sharedPref.getBoolean(prefTutorialElem, true));
        data.getInstance().setTutorialStringParser(sharedPref.getBoolean(prefTutorialParse, true));
        checkDB();
    }

    public static String cleanString(String str){
        String finalStr = "";
        for(int i=0; i<str.length(); i++){
            char tmp = str.charAt(i);
            if(tmp != '\''){
                finalStr = finalStr + tmp;
            }
        }
        return finalStr;
    }

    public static int saveData(Context context){
        SQLiteDatabase db = context.openOrCreateDatabase("OCRElementsDB",MODE_PRIVATE,null);
        db.execSQL("DROP TABLE IF EXISTS OCRElements");
        db.execSQL("CREATE TABLE IF NOT EXISTS OCRElements(date VARCHAR,title VARCHAR, text VARCHAR, file VARCHAR, confidence INTEGER);");
        for (OCRElement ocrElement : data.getInstance().getOcrElements()){
            db.execSQL("INSERT INTO OCRElements VALUES('"+ocrElement.getDate()+"','"+ocrElement.getTitle()+"','"+ocrElement.getText()+"','"+ocrElement.getImageFile().getAbsolutePath()+"','"+ocrElement.getConfidence()+"');");
        }
        db.close();
        return 0;
    }

    public static int restoreData(Context context){
        data.getInstance().setOcrElements(new ArrayList<OCRElement>());

        SQLiteDatabase db = context.openOrCreateDatabase("OCRElementsDB",MODE_PRIVATE,null);
        Cursor resultSet;
        try{
            resultSet = db.rawQuery("Select * from OCRElements",null);
            //resultSet = db.rawQuery("DROP TABLE OCRElements",null);
        }catch(SQLiteException e){
            return 1;
        }
        if(resultSet.getCount()>0){
            resultSet.moveToFirst();
            for(int i = 0; i < resultSet.getCount(); i++){
                OCRElement tmp = new OCRElement();
                tmp.setDate(resultSet.getString(0));
                tmp.setTitle(resultSet.getString(1));
                tmp.setText(resultSet.getString(2));
                tmp.setImageFile(new File(resultSet.getString(3)));
                tmp.setConfidence(resultSet.getInt(4));
                tmp.setProgress(100);
                data.getInstance().getOcrElements().add(tmp);
                resultSet.moveToNext();
            }
            db.close();
            return 0;
        }else {
            return 1;
        }
    }

    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(Context context){
        if(util.checkCameraHardware(context)){
            Camera c = null;
            try {
                c = Camera.open(); // attempt to get a Camera instance
            }
            catch (Exception e){
                return null;
            }
            return c; // returns null if camera is unavailable
        }
        return null;
    }
}

