package com.unitn.android.alessio.ocr3;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * Created by alessio on 02/05/16.
 */
public class FragmentText extends Fragment {

    private OCRElement ocrElement;
    private TextView title, textView;
    private EditText textEdit;
    private ScrollView editScroll, viewScroll;
    private View rootView;
    private int index;
    private Switch switchEdit;

    public FragmentText() {
    }

    public void setOcrElement(int index) {
        this.index = index;
        this.ocrElement = data.getInstance().getOcrElements().get(index);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            index = savedInstanceState.getInt("index");
            ocrElement = data.getInstance().getOcrElements().get(index);
        }

        rootView = inflater.inflate(R.layout.fragment_text_page, container, false);

        title = (TextView)rootView.findViewById(R.id.titleTextView);
        title.setText(ocrElement.getTitle().compareTo("")==0?ocrElement.getDate():ocrElement.getTitle()+" conf:"+ocrElement.getConfidence()+"%");

        textEdit = (EditText)rootView.findViewById(R.id.recognizedTextEdit);
        textEdit.setText(ocrElement.getText());
        textEdit.setVisibility(View.GONE);

        textView = (TextView)rootView.findViewById(R.id.recognizedTextView);
        textView.setText(ocrElement.getText());

        editScroll = (ScrollView)rootView.findViewById(R.id.editScroll);
        editScroll.setVisibility(View.GONE);
        viewScroll = (ScrollView)rootView.findViewById(R.id.viewScroll);

        switchEdit = (Switch)rootView.findViewById(R.id.editText);
        switchEdit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ObjectAnimator rotate1 = ObjectAnimator.ofFloat(viewScroll, "rotationY", 0f, 90f);
                    ObjectAnimator hide1 = ObjectAnimator.ofFloat(viewScroll, "alpha", 1f, 0f);
                    ObjectAnimator hide2 = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
                    ObjectAnimator rotate2 = ObjectAnimator.ofFloat(editScroll, "rotationY", -90f, 0f);
                    ObjectAnimator show1 = ObjectAnimator.ofFloat(editScroll, "alpha", 0f, 1f);
                    ObjectAnimator show2 = ObjectAnimator.ofFloat(textEdit, "alpha", 0f, 1f);
                    AnimatorSet animSet = new AnimatorSet();

                    AnimatorSet first = new AnimatorSet();
                    first.playTogether(rotate1, hide1, hide2);
                    AnimatorSet second = new AnimatorSet();
                    second.playTogether(show1, show2, rotate2);

                    animSet.setDuration(150);
                    animSet.playSequentially(first, second);
                    animSet.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            textEdit.setVisibility(View.VISIBLE);
                            editScroll.setVisibility(View.VISIBLE);
                            textEdit.setAlpha(0);
                            editScroll.setAlpha(0);
                        }
                        @Override
                        public void onAnimationRepeat(Animator animation) {}
                        @Override
                        public void onAnimationCancel(Animator animation) {}

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            textView.setVisibility(View.GONE);
                            viewScroll.setVisibility(View.GONE);
                            textEdit.setAlpha(1);
                            editScroll.setAlpha(1);
                            textView.setAlpha(0);
                            viewScroll.setAlpha(0);
                            switchEdit.setText("Save Text");
                        }
                    });
                    animSet.start();
                }else {
                    textView.setText(textEdit.getText().toString());
                    data.getInstance().getOcrElements().get(index).setText(textEdit.getText().toString());
                    util.saveData(getContext());

                    ObjectAnimator rotate1 = ObjectAnimator.ofFloat(editScroll, "rotationY", 0f, -90f);
                    ObjectAnimator hide1 = ObjectAnimator.ofFloat(editScroll, "alpha", 1f, 0f);
                    ObjectAnimator hide2 = ObjectAnimator.ofFloat(textEdit, "alpha", 1f, 0f);
                    ObjectAnimator rotate2 = ObjectAnimator.ofFloat(viewScroll, "rotationY", 90f, 0f);
                    ObjectAnimator show1 = ObjectAnimator.ofFloat(viewScroll, "alpha", 0f, 1f);
                    ObjectAnimator show2 = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
                    AnimatorSet animSet = new AnimatorSet();

                    AnimatorSet first = new AnimatorSet();
                    first.playTogether(rotate1, hide1, hide2);
                    AnimatorSet second = new AnimatorSet();
                    second.playTogether(show1, show2,rotate2);

                    animSet.setDuration(150);
                    animSet.playSequentially(first, second);
                    animSet.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            textView.setVisibility(View.VISIBLE);
                            viewScroll.setVisibility(View.VISIBLE);
                            textView.setAlpha(0);
                            viewScroll.setAlpha(0);
                        }
                        @Override
                        public void onAnimationRepeat(Animator animation) {}
                        @Override
                        public void onAnimationCancel(Animator animation) {}

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            textEdit.setVisibility(View.GONE);
                            editScroll.setVisibility(View.GONE);
                            textView.setAlpha(1);
                            viewScroll.setAlpha(1);
                            textEdit.setAlpha(0);
                            editScroll.setAlpha(0);
                            switchEdit.setText("Edit Text");
                        }
                    });
                    animSet.start();
                }

            }
        });

        Message msg = data.getInstance().getUiHandler().obtainMessage();
        msg.obj = "textStarted";
        data.getInstance().getUiHandler().sendMessage(msg);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", index);
    }

}