package com.unitn.android.alessio.ocr3;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.TextView;

import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;


public class customGrid extends GridLayout{
    private Activity activity;
    private String[] fields;
    private ArrayList<ArrayList<String>> entry;
    private int width, height;
    private TextView[][] textViews;
    private TextView tutorialText;

    public customGrid(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public TextView getTutorialText() {
        return tutorialText;
    }

    public ArrayList<ArrayList<String>> getEntry(){//todo remove white lines
        ArrayList<ArrayList<String>> entry = new ArrayList<ArrayList<String>>();
        int empty;

        for (int i=0; i<height; i++) {
            ArrayList<String> entryTmp = new ArrayList<String>();
            empty = 0;
            for (int j = 0; j < width; j++) {
                if(textViews[j][i] != null){
                    String tmpString = textViews[j][i].getText().toString();

                    if(tmpString != null && !tmpString.equals("")){
                        entryTmp.add(tmpString);
                        empty++;
                    }
                }
            }

            Log.v(data.getInstance().getTAG(), "riga = "+i+" empty = "+empty);

            if(empty==width){
                entry.add(entryTmp);
            }
        }

        return entry;
    }

    public void populateGridCells(ArrayList<ArrayList<String>> entry, int[] columnWidth, boolean newLine){//Matrix MUST be sqared
        super.removeAllViewsInLayout();

        width = entry.get(0).size();
        height = entry.size();

        textViews = new TextView[width][height];

        super.setColumnCount(width);//todo change programmatically
        super.setRowCount(height);

        for (int i=0; i<height; i++){

            for (int j=0; j<width; j++){
                MaterialEditText text = new MaterialEditText(getContext());
                text.setHideUnderline(true);
                text.setLayoutParams(new GridLayout.LayoutParams(spec(i), spec(j)));
                text.setWidth(columnWidth[j]);
                text.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.custom_border));
                text.setSingleLine();
                text.setId((10*i)+j);
                text.setNextFocusForwardId((j+1==entry.get(i).size()?(10*(i+1)):(10*i))+((j+1)%entry.get(i).size()));
                text.setNextFocusDownId((j+1==entry.get(i).size()?10*(i+1):10*i)+((j+1)%entry.get(i).size()));

                if((i == 0 && j == 0) || (i == 1 && j == 1)){
                    tutorialText = text;
                }

                textViews[j][i] = text;

                if(newLine && j==0){
                    text.requestFocus();
                }

                super.addView(text);

                text.setText(entry.get(i).get(j));
            }
        }

    }
}