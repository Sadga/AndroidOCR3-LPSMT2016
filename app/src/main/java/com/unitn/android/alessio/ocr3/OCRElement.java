package com.unitn.android.alessio.ocr3;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * Created by alessio on 27/04/16.
 */

public class OCRElement implements Parcelable {

    private File imageFile;
    private String date;
    private String title = "";
    private String text;
    private int type;
    private int progress;
    private int confidence;
    private Bitmap thumbnail = null;
    private Bitmap imageFullRes = null;

    public OCRElement(){
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = util.cleanString(text);
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public File getImageFile() {
        return imageFile;
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public int getProgress() {
        return progress;
    }

    public String getDate() {
        return date;
    }

    public int getConfidence() {
        return confidence;
    }

    public Bitmap getImageFullRes() {
        if(imageFullRes == null){
            imageFullRes = util.openImageFile(imageFile);
        }
        return imageFullRes;
    }

    public Bitmap getThumbnail(){
        if(thumbnail == null) {
            thumbnail = util.resizeBmp(util.openImageFileThumbnail(imageFile), 100, 100);
        }
        return thumbnail;
    }

    public void deleteImageFullRes(){
        imageFullRes = null;
    }

    protected OCRElement(Parcel in) {
        String tmp = in.readString();
        imageFile = tmp == null?null:new File(tmp);
        date = in.readString();
        title = in.readString();
        text = in.readString();
        type = in.readInt();
        progress = in.readInt();
        confidence = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageFile==null?null:imageFile.getAbsolutePath());
        dest.writeString(date);
        dest.writeString(title);
        dest.writeString(text);
        dest.writeInt(type);
        dest.writeInt(progress);
        dest.writeInt(confidence);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<OCRElement> CREATOR = new Parcelable.Creator<OCRElement>() {
        @Override
        public OCRElement createFromParcel(Parcel in) {
            return new OCRElement(in);
        }

        @Override
        public OCRElement[] newArray(int size) {
            return new OCRElement[size];
        }
    };
}
