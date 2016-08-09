package com.unitn.android.alessio.ocr3;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Message;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alessio on 28/04/16.
 */
public class ServiceParser extends IntentService {

    private int index;
    private Bitmap image;
    private int finished = 0;
    private Message msg;
    private String testo1,testo2;
    private int conf1, conf2;
    private int progr1, progr2;
    private final int THREAD1 = 1;
    private final int THREAD2 = 2;


    public ServiceParser() {
        super("Parser");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        index = intent.getExtras().getInt("ocrelement");

        msg = data.getInstance().getUiHandler().obtainMessage();

        image = util.optimizeImage(data.getInstance().getOcrElements().get(index).getImageFullRes());
        int height = image.getHeight()/2;
        boolean LineNotOk = true;
        while(LineNotOk){
            height --;
            LineNotOk = false;
            for (int i = 0; i < image.getWidth(); i++){
                if(image.getPixel(i, height)== Color.BLACK){
                    LineNotOk = true;
                }
            }
        }

        final Rect rect1 = new Rect(0,0,image.getWidth(), height);
        final Rect rect2 = new Rect(0,height+1, image.getWidth(), image.getHeight());

        data.getInstance().getOcrElements().get(index).setConfidence(0);

        Thread thread1 = new Thread(){
            @Override
            public void run(){
                TessBaseAPI baseApi = new TessBaseAPI(new progress(THREAD1));
                baseApi.init(data.getInstance().getDataPath(), data.getInstance().getLanguage());
                baseApi.setImage(image);
                baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);

                baseApi.setRectangle(rect1);

                testo1 = baseApi.getUTF8Text();
                conf1 = baseApi.meanConfidence();
                baseApi.end();
                finish();
            }
        };

        Thread thread2 = new Thread(){
            @Override
            public void run(){
                TessBaseAPI baseApi = new TessBaseAPI(new progress(THREAD2));
                baseApi.init(data.getInstance().getDataPath(), data.getInstance().getLanguage());
                baseApi.setImage(image);
                baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);

                baseApi.setRectangle(rect2);

                testo2 = baseApi.getUTF8Text();
                conf2 = baseApi.meanConfidence();
                baseApi.end();
                finish();
            }
        };

        thread1.start();
        thread2.start();
    }

    private void finish(){
        finished++;
        Log.v(data.getInstance().getTAG(), "finito uno");
        if(finished == 2){
            Log.v(data.getInstance().getTAG(), "finiti Entrambi");
            data.getInstance().getOcrElements().get(index).setText(testo1+testo2);
            data.getInstance().getOcrElements().get(index).setConfidence((conf1+conf2)/2);
            data.getInstance().getOcrElements().get(index).deleteImageFullRes();
            data.getInstance().getOcrElements().get(index).setDate(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
            Log.v(data.getInstance().getTAG(), "OCRED TEXT: " + data.getInstance().getOcrElements().get(index).getText());
            Log.v(data.getInstance().getTAG(), "Confidence: " + data.getInstance().getOcrElements().get(index).getConfidence());

            data.getInstance().getOcrElements().get(index).setProgress(100);
            msg.obj = "Refresh";
            data.getInstance().getUiHandler().sendMessage(msg);
            System.gc();
        }
    }

    private void setProgress(){
        int p = progr1<progr2?progr1:progr2;
        data.getInstance().getOcrElements().get(index).setProgress(p);
        Message msg = data.getInstance().getUiHandler().obtainMessage();
        msg.obj = "Refresh";
        data.getInstance().getUiHandler().sendMessage(msg);
    }

    class progress implements TessBaseAPI.ProgressNotifier{
        int thread, progre;

        public progress(int thread){
            this.thread = thread;
            progre = 1;
        }

        @Override
        public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
            float progr = progressValues.getPercent();
            progr = (((progr-30f)/70f)*101f)+1f;
            Log.v(data.getInstance().getTAG(), "progresso "+thread+" : "+(int)progr);

            if(progr-progre >= 1 || progr==100) {
                progre = (int)progr;
                if (thread == THREAD1){
                    progr1 = (int)progr;
                }else if(thread == THREAD2){
                    progr2 = (int)progr;
                }
                setProgress();
            }
        }
    }
}