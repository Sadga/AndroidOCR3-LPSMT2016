package com.unitn.android.alessio.ocr3;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;

import java.util.ArrayList;

/**
 * Created by alessio on 27/04/16.
 */
public class data {
    private static data ourInstance = new data();
    private static String TAG = "com.android.alessio.ocr3";
    private String language;
    private String dataPath;
    private AssetManager asset;
    private ArrayList<OCRElement> ocrElements;
    private customAdapter adapter;
    private Handler uiHandler;
    private SharedPreferences sharedPref;
    private boolean usingOpenCV = true;
    private boolean tutorialMain, tutorialOCRElement, tutorialStringParser;


    public static data getInstance() {
        return ourInstance;
    }

    private data() {
    }

    public void setOcrElements(ArrayList<OCRElement> ocrElements) {
        this.ocrElements = ocrElements;
    }

    public void setAsset(AssetManager asset) {
        this.asset = asset;
    }

    public void setLanguage(String language) {
        this.language = language;
        util.checkDB();
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public void setAdapter(customAdapter adapter) {
        this.adapter = adapter;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    public void setSharedPref(SharedPreferences sharedPref) {
        this.sharedPref = sharedPref;
    }

    public void setUsingOpenCV(boolean usingOpenCV) {
        this.usingOpenCV = usingOpenCV;
    }

    public void setTutorialMain(boolean tutorialMain) {
        this.tutorialMain = tutorialMain;
    }

    public void setTutorialOCRElement(boolean tutorialOCRElement) {
        this.tutorialOCRElement = tutorialOCRElement;
    }

    public void setTutorialStringParser(boolean tutorialStringParser) {
        this.tutorialStringParser = tutorialStringParser;
    }

    public ArrayList<OCRElement> getOcrElements() {
        return ocrElements;
    }

    public AssetManager getAsset() {
        return asset;
    }

    public String getLanguage() {
        return language;
    }

    public String getDataPath() {
        return dataPath;
    }

    public Handler getUiHandler() {
        return uiHandler;
    }

    public boolean isUsingOpenCV() {
        return usingOpenCV;
    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public customAdapter getAdapter() {
        return adapter;
    }

    public String getTAG() {
        return TAG;
    }

    public boolean isTutorialMain() {
        return tutorialMain;
    }

    public boolean isTutorialOCRElement() {
        return tutorialOCRElement;
    }

    public boolean isTutorialStringParser() {
        return tutorialStringParser;
    }
}
