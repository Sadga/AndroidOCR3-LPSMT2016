package com.unitn.android.alessio.ocr3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.ArrayList;

public class customAdapter extends BaseAdapter {

    Context context;
    ArrayList<OCRElement> elements;
    private static LayoutInflater inflater = null;

    public customAdapter(ArrayList<OCRElement> elements, Context context) {
        this.context = context;
        this.elements = elements;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return elements.size();
    }

    @Override
    public Object getItem(int position) {
        return elements.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.listentry, null);
        DonutProgress progress = (DonutProgress)vi.findViewById(R.id.circle_progress);
        progress.setProgress(elements.get(position).getProgress());
        RoundedImageView thumbnail = (RoundedImageView)vi.findViewById(R.id.thumbnail);
        thumbnail.setImageBitmap(elements.get(position).getThumbnail());
        TextView titleText = (TextView) vi.findViewById(R.id.title);
        titleText.setText(elements.get(position).getTitle().compareTo("")==0?elements.get(position).getDate():elements.get(position).getTitle());
        TextView descr = (TextView) vi.findViewById(R.id.description);
        descr.setText((elements.get(position).getText().substring(0, Math.min(40,elements.get(position).getText().length()))).replaceAll("\n", " ") + "...");
        return vi;
    }
}