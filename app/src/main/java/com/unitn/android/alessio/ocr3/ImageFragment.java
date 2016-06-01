package com.unitn.android.alessio.ocr3;

import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by alessio on 02/05/16.
 */
public class ImageFragment extends Fragment {

    private OCRElement ocrElement;
    private ImageView imageV;
    private int imageVisualized = 0;

    public ImageFragment() {
    }

    public void setOcrElement(OCRElement ocrElement) {
        this.ocrElement = ocrElement;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.image_page, container, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ocrElement.getImageFullRes();//return not handled cause i need only to be set the image on the object
                Message msg = data.getInstance().getUiHandler().obtainMessage();
                msg.obj = "SetImage";
                data.getInstance().getUiHandler().sendMessage(msg);
            }
        }).start();

        imageV = (ImageView)rootView.findViewById(R.id.imageView);
        imageV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(data.getInstance().getTAG(),"long click");
                if(imageVisualized == 0){
                    imageV.setImageBitmap(util.resizeBmp(ocrElement.getImageFullRes(),1920, 1080));
                    imageVisualized = 1;
                }else {
                    imageV.setImageBitmap(util.resizeBmp(util.optimizeImage(ocrElement.getImageFullRes()),1920, 1080));
                    imageVisualized = 0;
                }
                Message msg = data.getInstance().getUiHandler().obtainMessage();
                msg.obj = "updateMAttacher";
                data.getInstance().getUiHandler().sendMessage(msg);
                return true;
            }
        });

        return rootView;
    }
}
