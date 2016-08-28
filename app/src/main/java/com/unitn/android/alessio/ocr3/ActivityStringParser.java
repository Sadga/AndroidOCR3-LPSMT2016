package com.unitn.android.alessio.ocr3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ganfra.materialspinner.MaterialSpinner;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.widget.GridLayout.spec;


public class ActivityStringParser extends AppCompatActivity {
    private customGrid c,gridTop;
    private String text;
    private ArrayList<ArrayList<String>> entry;
    private int[] larghezza;
    private String[] n, name, surname, birth, club;
    private Spinner[] spinners;
    private ArrayList<String> discarded;
    private FloatingActionButton discardedFab;
    private boolean discardedShow = false;
    customCoordinatorLayout topView;
    FrameLayout bottomView;
    FloatingActionMenu fabMenu;

    private float mx, my;
    private float curX, curY;

    private ScrollView vScroll;
    private HorizontalScrollView hScroll, hScrollTop;

    private int mediumLength=0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_string_parser);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        text = getIntent().getExtras().getString("text");
        entry = parseText(text);

        larghezza = getWidths(entry);

        c = (customGrid)findViewById(R.id.grid);
        c.populateGridCells(entry, larghezza, false);

        gridTop = (customGrid)findViewById(R.id.gridTop);


        ArrayList<String> fields = new ArrayList<>();
        fields.add("N");
        fields.add("Name");
        fields.add("Surname");
        fields.add("Birth Year");
        fields.add("Club");
        fields.add("Ignored");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, fields);


        spinners = new Spinner[mediumLength];
        for (int i=0; i<mediumLength; i++){
            MaterialSpinner spin = new MaterialSpinner(getApplicationContext());
            spin.setAdapter(adapter);
            spin.setLayoutParams(new GridLayout.LayoutParams(spec(0), spec(i)));
            GridLayout.LayoutParams params = (GridLayout.LayoutParams)spin.getLayoutParams();
            params.width = larghezza[i];
            params.height = 100;
            spin.setSelection(i%6);
            spin.setBackgroundResource(R.drawable.custom_border_spinner);
            spin.setLayoutParams(params);
            spin.setPopupBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.popup_background));
            spinners[i]=spin;

            gridTop.addView(spin);
        }

        ListView discardList = (ListView)findViewById(R.id.discardList);
        ArrayAdapter<String> discardAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, discarded);
        discardList.setAdapter(discardAdapter);


        fabMenu = (FloatingActionMenu)findViewById(R.id.fabMenu);

        FloatingActionButton addElemFab = (FloatingActionButton)findViewById(R.id.newElemFab);
        addElemFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabMenu.close(true);
                addBlankElement();
            }
        });

        FloatingActionButton exportFab = (FloatingActionButton)findViewById(R.id.exportFab);
        exportFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabMenu.close(true);
                exportList();
            }
        });

        topView = (customCoordinatorLayout)findViewById(R.id.gridLayout);
        topView.init();

        bottomView = (FrameLayout)findViewById(R.id.discardLayout);

        discardedFab = (FloatingActionButton)findViewById(R.id.discardedFab);
        discardedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabMenu.close(true);
                if(discardedShow){
                    discardedFab.setLabelText("Show discarded elements");
                    discardedFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_keyboard_arrow_up_white_24px));
                    hideDiscarded();
                    discardedShow = false;
                }else {
                    discardedFab.setLabelText("Hide discarded elements");
                    discardedFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_keyboard_arrow_down_white_24px));
                    showDiscarded();
                    discardedShow = true;
                }
            }
        });


        vScroll = (ScrollView) findViewById(R.id.vScroll);
        hScroll = (HorizontalScrollView) findViewById(R.id.hScroll);

        hScroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollX = hScroll.getScrollX();
                int scrollY = hScroll.getScrollY();
                hScrollTop.scrollTo(hScroll.getScrollX(), hScroll.getScrollY());
            }
        });

        hScrollTop = (HorizontalScrollView) findViewById(R.id.hScrollTop);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                fabMenu.close(true);
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(fabMenu.isOpened()){
            fabMenu.close(true);
        }else{
            super.onBackPressed();
            overridePendingTransition(R.anim.pull_in_left, R.anim.push_out_right);
        }
    }

    private ArrayList<ArrayList<String>> parseText(String text){

        ArrayList<ArrayList<String>> entry = null;
        String str = "";

        for(int i = 0; i<text.length(); i++){   // --> clean string from unwanted characters
            char c = text.charAt(i);
            if((c >= 'a' && c <= 'z')||(c >= 'A' && c <= 'Z')||(c >='0' && c <= '9')||(c==' ')||(c=='\n')){
                str+=c;
            }
        }                                       // <-- clean string from unwanted characters

        Pattern p = Pattern.compile("[[\\s]&&[^\n]]+");
        Matcher m = p.matcher(str);
        str = m.replaceAll(" ");

        p = Pattern.compile("[\\s]*[\n]+[\\s]*");
        m = p.matcher(str);
        str = m.replaceAll("\n");

        p = Pattern.compile("[[\\s]&&[^\n]]+");
        m = p.matcher(str);
        str = m.replaceAll("|");

        String[] rows = str.split("[\n]");

        int[] lengths = new int[15];//massimo numero di campi ammessi

        for(int i = 0; i < 15; i++) {
            lengths[i]=0;
        }

        for(int i = 0; i < rows.length; i++) {
            String[] voices = rows[i].split("[\\|]");
            if(voices.length<15)
                lengths[voices.length]++;
        }

        int max = 0;
        for(int i = 2; i < 15; i++) {
            if(lengths[i]>max){
                max=lengths[i];
                mediumLength = i;
            }
        }

        discarded = new ArrayList<String>();
        discarded.add("These elements are discarded, you can insert them manually");

        for(int i = 0; i < rows.length; i++) {
            if(entry == null){
                entry = new ArrayList<ArrayList<String>>();
            }
            String[] voices = rows[i].split("[\\|]");
            if(voices.length == mediumLength){
                ArrayList<String> voicesAL = new ArrayList<String>(mediumLength);
                for (int j = 0; j < voices.length; j++){
                    getDateFromString(voices[j]);
                    voicesAL.add(voices[j]);
                }
                entry.add(voicesAL);
            }else{
                discarded.add(rows[i]);
            }
        }

        String stringa="";
        for (int i=0; i<entry.size(); i++){
            stringa="";
            for (int j=0; j<entry.get(i).size(); j++){
                stringa+=entry.get(i).get(j)+" ";
            }
        }

        return entry;
    }

    private void showDiscarded(){
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        final int pixels;
        if(getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT){
            pixels = (int) (250 * scale + 0.5f);
        }else{
            pixels = (int) (150 * scale + 0.5f);
        }

        Animation top = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                customCoordinatorLayout.MarginLayoutParams p = (customCoordinatorLayout.MarginLayoutParams) topView.getLayoutParams();
                p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, (int)(pixels * interpolatedTime));
                topView.requestLayout();
            }
        };
        top.setDuration(250);

        Animation bottom = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                bottomView.getLayoutParams().height = (int)(pixels * interpolatedTime);
                bottomView.requestLayout();
            }
        };
        bottom.setDuration(250);

        topView.startAnimation(top);
        bottomView.startAnimation(bottom);


        /*customCoordinatorLayout.MarginLayoutParams p = (customCoordinatorLayout.MarginLayoutParams) topView.getLayoutParams();
        p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, pixels);
        topView.requestLayout();*/
    }

    private void hideDiscarded(){
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        final int pixels;
        if(getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT){
            pixels = (int) (250 * scale + 0.5f);
        }else{
            pixels = (int) (150 * scale + 0.5f);
        }

        Animation top = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                customCoordinatorLayout.MarginLayoutParams p = (customCoordinatorLayout.MarginLayoutParams) topView.getLayoutParams();
                p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, (int)(pixels - (pixels * interpolatedTime)));
                topView.requestLayout();
            }
        };
        top.setDuration(250);

        Animation bottom = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                bottomView.getLayoutParams().height = (int)(pixels - (pixels * interpolatedTime));
                bottomView.requestLayout();
            }
        };
        bottom.setDuration(250);

        topView.startAnimation(top);
        bottomView.startAnimation(bottom);
    }

    private void exportList(){
        entry = c.getEntry();
        String[] fields = getFields();

        n = new String[entry.size()];
        name= new String[entry.size()];
        surname = new String[entry.size()];
        birth = new String[entry.size()];
        club = new String[entry.size()];

        for (int i = 0; i < fields.length; i++){
            switch (fields[i]){
                case "N":
                    for (int j = 0; j < entry.size(); j++){
                        n[j] = entry.get(j).get(i);
                    }
                    break;
                case "Name":
                    for (int j = 0; j < entry.size(); j++){
                        name[j] = entry.get(j).get(i);
                    }
                    break;
                case "Surname":
                    for (int j = 0; j < entry.size(); j++){
                        surname[j] = entry.get(j).get(i);
                    }
                    break;
                case "Birth Year":
                    for (int j = 0; j < entry.size(); j++){
                        birth[j] = entry.get(j).get(i);
                    }
                    break;
                case "Club":
                    for (int j = 0; j < entry.size(); j++){
                        club[j] = entry.get(j).get(i);
                    }
                    break;
                default:break;
            }
        }

        String str = "";

        for (int j = 0; j < entry.size(); j++){
            n[j] = n[j]==null?"":n[j];
            name[j] = name[j]==null?"":name[j];
            surname[j] = surname[j]==null?"":surname[j];
            birth[j] = birth[j]==null?"":birth[j];
            club[j] = club[j]==null?"":club[j];
            str+="\""+n[j]+"\",\""+name[j]+"\",\""+surname[j]+"\",\""+birth[j]+"\",\""+club[j]+"\"\n";
        }

        Log.v(data.getInstance().getTAG(), str);
        String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
        File file = new File(data.getInstance().getDataPath() + "csv" + File.separator + "LIST_" + timeStamp + ".csv");
        util.saveText(str, file);

        Toast.makeText(getApplicationContext(), "CSV saved to: "+file.toString(), Toast.LENGTH_LONG).show();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivity(Intent.createChooser(share, "Share CSV"));

    }

    private String[] getFields(){//TODO CONTROLLO ERRORI CAMPI DUPLICATI
        String[] fields = new String[entry.get(0).size()];

        for (int i = 0; i < entry.get(0).size(); i++){
            fields[i] = spinners[i].getSelectedItem().toString();
        }

        return fields;
    }

    private void addBlankElement(){
        ArrayList<String> voicesAL = new ArrayList<String>(mediumLength);
        for (int i = 0; i < mediumLength; i++){
            voicesAL.add("");
        }
        entry = c.getEntry();
        entry.add(voicesAL);
        c.populateGridCells(entry, larghezza, true);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private int[] getWidths(ArrayList<ArrayList<String>> e){//30 Ogni cifra; matrice deve essere rettangolare
        final int PIXEL_PER_CHAR = 75;

        int[][] nChar = new int[e.size()][e.get(0).size()];
        int[] lengths = new int[e.get(0).size()];
        for(int i = 0; i < e.get(0).size(); i++){
            lengths[i] = 300;
        }

        for(int i = 0; i < e.size(); i++){
            for (int j = 0; j < e.get(i).size(); j++){
                nChar[i][j] = e.get(i).get(j).length();
            }
        }

        for(int i = 0; i < e.size(); i++){
            for (int j = 0; j < e.get(i).size(); j++){
                if(lengths[j] < (nChar[i][j]*PIXEL_PER_CHAR)){
                    lengths[j] = (nChar[i][j]*PIXEL_PER_CHAR);
                }
            }
        }

        return lengths;
    }

    /*@Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                mx = event.getX();
                my = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                curX = event.getX();
                curY = event.getY();
                vScroll.scrollBy((int) (mx - curX), (int) (my - curY));
                hScroll.scrollBy((int) (mx - curX), (int) (my - curY));
                mx = curX;
                my = curY;
                break;
            case MotionEvent.ACTION_UP:
                curX = event.getX();
                curY = event.getY();
                vScroll.scrollBy((int) (mx - curX), (int) (my - curY));
                hScroll.scrollBy((int) (mx - curX), (int) (my - curY));
                break;
        }

        return super.dispatchTouchEvent(event);
    }*/

    private boolean isNumber(String str){
        boolean number = true;
        for (int i = 0; i < str.length(); i++){
            if (str.charAt(i)<'0' || str.charAt(i)>'9'){
                number = false;
                break;
            }
        }
        return number;
    }

    private int getDateFromString(String str){
        int year = 2;
        int date;
        if (isNumber(str)){
            date = Integer.parseInt(str);
        }
        return year;
    }

}
