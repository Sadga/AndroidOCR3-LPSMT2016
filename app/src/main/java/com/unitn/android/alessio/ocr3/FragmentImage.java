package com.unitn.android.alessio.ocr3;

import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by alessio on 02/05/16.
 */
public class FragmentImage extends Fragment {

    private OCRElement ocrElement;

    public FragmentImage() {
    }

    public void setOcrElement(OCRElement ocrElem) {
        this.ocrElement = ocrElem;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            ocrElement = savedInstanceState.getParcelable("ocrElem");
        }

        View rootView = inflater.inflate(R.layout.fragment_image_page, container, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ocrElement.getImageFullRes();//return not handled cause i need only to be set the image on the object
                Message msg = data.getInstance().getUiHandler().obtainMessage();
                msg.obj = "SetImage";
                data.getInstance().getUiHandler().sendMessage(msg);
            }
        }).start();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ocrElem", ocrElement);
    }
}
