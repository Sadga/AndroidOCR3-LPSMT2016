package com.unitn.android.alessio.ocr3;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.ArrayList;

public class customAdapter extends RecyclerView.Adapter<customAdapter.ViewHolder> {

    Context context;
    ArrayList<OCRElement> elements;
    private static LayoutInflater inflater = null;
    OnItemClickListener listener;
    OnItemLongClickListener longListener;

    public interface OnItemClickListener {
        void onItemClick(OCRElement item, int pos, RoundedImageView thumbnail, TextView title, DonutProgress percent);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(OCRElement item, int pos);
    }

    public customAdapter(ArrayList<OCRElement> elements, Context context, OnItemClickListener listener, OnItemLongClickListener longListener) {
        this.context = context;
        this.elements = elements;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.listener = listener;
        this.longListener = longListener;
    }

    public void add(OCRElement item) {
        elements.add(item);
        notifyItemInserted(elements.size()-1);
    }

    public void remove(int position) {
        elements.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.progress.setProgress(elements.get(position).getProgress());
        holder.progress.setTransitionName("progressTransition"+position);
        holder.thumbnail.setImageBitmap(elements.get(position).getThumbnail());
        holder.thumbnail.setTransitionName("imageTransition"+position);
        holder.titleText.setText(elements.get(position).getTitle().compareTo("")==0?elements.get(position).getDate():elements.get(position).getTitle());
        holder.titleText.setTransitionName("titleTransition"+position);
        holder.descr.setText((elements.get(position).getText().substring(0, Math.min(40,elements.get(position).getText().length()))).replaceAll("\n", " ") + "...");

        holder.setClickListener(elements.get(position), position, listener);
        holder.setLongClickListener(elements.get(position), position, longListener);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.listentry, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RoundedImageView thumbnail;
        public TextView titleText, descr;
        public DonutProgress progress;

        public ViewHolder(View itemView) {
            super(itemView);
            progress = (DonutProgress)itemView.findViewById(R.id.circle_progress);
            thumbnail = (RoundedImageView)itemView.findViewById(R.id.thumbnail);
            titleText = (TextView) itemView.findViewById(R.id.title);
            descr = (TextView) itemView.findViewById(R.id.description);
        }

        public void setClickListener(final OCRElement item, final int position, final OnItemClickListener listener){
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(item, position, thumbnail, titleText, progress);
                }
            });
        }

        public void setLongClickListener(final OCRElement item, final int position, final OnItemLongClickListener listener){
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    listener.onItemLongClick(item, position);
                    return true;
                }
            });
        }
    }
}