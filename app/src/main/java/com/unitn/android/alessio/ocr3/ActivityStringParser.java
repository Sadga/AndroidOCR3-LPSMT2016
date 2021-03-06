package com.unitn.android.alessio.ocr3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
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
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

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
    private FloatingActionButton discardedFab, addElemFab, exportFab;
    private boolean discardedShow = false;
    private customCoordinatorLayout topView;
    private FrameLayout bottomView;
    private FloatingActionMenu fabMenu;
    private MaterialSpinner tutorialSpinner;


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

            if(i==0){
                tutorialSpinner = spin;
            }

            spinners[i]=spin;

            gridTop.addView(spin);
        }

        ListView discardList = (ListView)findViewById(R.id.discardList);
        ArrayAdapter<String> discardAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, discarded);
        discardList.setAdapter(discardAdapter);


        fabMenu = (FloatingActionMenu)findViewById(R.id.fabMenu);

        addElemFab = (FloatingActionButton)findViewById(R.id.newElemFab);
        addElemFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabMenu.close(true);
                addBlankElement();
            }
        });

        exportFab = (FloatingActionButton)findViewById(R.id.exportFab);
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
            case android.R.id.home:
                fabMenu.close(false);
                discardedShow = false;
                onBackPressed();
                return true;
            case R.id.tutorial:
                showTutorial(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        util.savePrefs();
    }

    @Override
    public void onBackPressed() {
        if(fabMenu.isOpened()){
            fabMenu.close(false);
        }else if(discardedShow) {
            discardedFab.setLabelText("Show discarded elements");
            discardedFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_keyboard_arrow_up_white_24px));
            hideDiscarded();
            discardedShow = false;
        }else{
            Log.v(data.getInstance().getTAG(), "Back");
            super.onBackPressed();
            overridePendingTransition(R.anim.pull_in_left, R.anim.push_out_right);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        showTutorial();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_parsetext, menu);
        return true;
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

        for(int i = 0; i < rows.length; i++){
            p = Pattern.compile("(\\|)(DE||de||De||" +
                    "Da||DA||da||" +
                    "DAI||dai||Dai||" +
                    "La||LA||la||" +
                    "D||d||" +
                    "DELLA||Della||della||" +
                    "DALLE||Dalle||dalle||" +
                    "DELLE||Delle||delle)(\\|)");
            m = p.matcher(rows[i]);
            while(m.find()){
                Log.v(data.getInstance().getTAG(), "Match on: "+rows[i]);
                String tmp = "";
                for (int k = 0; k < rows[i].length(); k++){
                    if(k == m.end()-1
                            ){
                        tmp+=" ";
                    }else{
                        tmp+=rows[i].charAt(k);
                    }
                }
                rows[i] = tmp;
            }
            /*for (int j = 0; j < m.groupCount(); j++){
                Log.v(data.getInstance().getTAG(), "Match on: "+rows[i]+" Found: "+m.group(j));
                String tmp = "";
                for (int k = 0; k < rows[i].length(); k++){
                    if(k == m.end(j)){
                        tmp+=" ";
                    }else{
                        tmp+=rows[i].charAt(k);
                    }
                }
                rows[i] = tmp;
            }*/
        }

        int[] lengths = new int[15];//massimo numero di campi ammessi 15

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

        int[] dateProb = new int[mediumLength];
        for (int i = 0; i < mediumLength; i++){
            dateProb[i] = 0;
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
                    dateProb[j]+=caBeDate(voices[j]);
                    voicesAL.add(voices[j]);
                }
                entry.add(voicesAL);
            }else{
                discarded.add(rows[i]);
            }
        }

        int tmpMaxProb = 0;
        int dateColumn = -1;

        for (int i = 0; i < mediumLength; i++){
            if(dateProb[i] > entry.size()){
                if(dateProb[i]>tmpMaxProb){
                    tmpMaxProb = dateProb[i];
                    dateColumn = i;
                }
            }
        }

        Log.v(data.getInstance().getTAG(), "Column with date: "+dateColumn);

        if(dateColumn > -1){
            for (int i = 0; i < entry.size(); i++){
                entry.get(i).set(dateColumn, getDateFromString(entry.get(i).get(dateColumn)));
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
            pixels = (int) (150 * scale + 0.5f);
        }else{
            pixels = (int) (100 * scale + 0.5f);
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
    }

    private void hideDiscarded(){
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        final int pixels;
        if(getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT){
            pixels = (int) (150 * scale + 0.5f);
        }else{
            pixels = (int) (100 * scale + 0.5f);
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

    private int caBeDate(String str){//0 = false; 1 = 2 digit number;
        if(!isNumber(str)){
            return 0;
        }if(str.length()<2){
            return 0;
        }if(str.length()==2){
            return 1;
        }if(str.length() == 3){
            return 2;
        }if(str.length() == 4){
            return 3;
        }if(str.length()>4){
            return 6;
        }
        return 0;
    }

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

    private String getDateFromString(String str){
        return str.substring(str.length()-2, str.length());
    }

    private void showTutorial(){
        showTutorial(false);
    }

    private void showTutorial(boolean force){
        if(data.getInstance().isTutorialStringParser() || force){

            fabMenu.open(false);

            ShowcaseConfig config = new ShowcaseConfig();
            config.setDelay(150); // half second between each showcase view

            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this);

            sequence.setConfig(config);

            sequence.addSequenceItem(fabMenu,
                    "From this menu you can perform various actions", "GOT IT");

            sequence.addSequenceItem(addElemFab,
                    "you can add a new element in the list", "GOT IT");

            sequence.addSequenceItem(discardedFab,
                    "you can see the list of discarded elements, those elements cannot be recognized automatically", "GOT IT");

            sequence.addSequenceItem(exportFab,
                    "and you can save the csv on the device and then share it with various apps", "GOT IT");

            sequence.addSequenceItem(tutorialSpinner,
                    "At the top of each column you can select the type of data", "GOT IT");

            sequence.addSequenceItem(c.getTutorialText(),
                    "if you find errors in the data you can change it, just tap on the cell", "GOT IT");

            sequence.start();

            data.getInstance().setTutorialStringParser(false);
        }
    }

}
