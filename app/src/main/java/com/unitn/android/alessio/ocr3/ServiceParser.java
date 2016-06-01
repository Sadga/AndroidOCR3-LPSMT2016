package com.unitn.android.alessio.ocr3;

import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alessio on 28/04/16.
 */
public class ServiceParser extends IntentService {

    int index;

    public ServiceParser() {
        super("Parser");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        index = intent.getExtras().getInt("ocrelement");

        Message msg = data.getInstance().getUiHandler().obtainMessage();

        TessBaseAPI baseApi = new TessBaseAPI(new progress(data.getInstance().getOcrElements().get(index)));
        baseApi.init(data.getInstance().getDataPath(), data.getInstance().getLanguage());
        baseApi.setImage(util.optimizeImage(data.getInstance().getOcrElements().get(index).getImageFullRes()));
        data.getInstance().getOcrElements().get(index).deleteImageFullRes();
        data.getInstance().getOcrElements().get(index).setText(baseApi.getUTF8Text());
        data.getInstance().getOcrElements().get(index).setDate(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        data.getInstance().getOcrElements().get(index).setConfidence(baseApi.meanConfidence());
        Log.v(data.getInstance().getTAG(), "OCRED TEXT: " + data.getInstance().getOcrElements().get(index).getText());
        Log.v(data.getInstance().getTAG(), "Confidence: " + baseApi.meanConfidence());
        baseApi.end();

        data.getInstance().getOcrElements().get(index).setProgress(100);
        msg.obj = "Refresh";
        data.getInstance().getUiHandler().sendMessage(msg);
        System.gc();
    }

    class progress implements TessBaseAPI.ProgressNotifier{
        OCRElement elem;
        int progre;

        public progress(OCRElement element){
            this.elem = element;
            progre = 1;
        }

        @Override
        public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
            float progr = progressValues.getPercent();
            progr = (((progr-30f)/70f)*101f)+1f;
            Log.v(data.getInstance().getTAG(), "progresso: "+(int)progr);
            elem.setProgress((int)progr);
            if(progr-progre>=1 || progr==100) {
                progre = (int)progr;
                Message msg = data.getInstance().getUiHandler().obtainMessage();
                msg.obj = "Refresh";
                data.getInstance().getUiHandler().sendMessage(msg);
            }

        }
    }
}