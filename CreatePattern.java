package com.tongs.funpatternwifi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.tongs.view.DrawView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.tongs.view.DrawView;

public class CreatePattern extends AppCompatActivity {
    public String TAG = "[dbg]CreatePattern";
    static DrawView drawView;
    static Button btnUndo, btnReset, btnConfirm, btnSave, btnLoad, btnExport;
    static LinearLayout layoutOption, layoutDraw;
    static TextView tvResult, tvCountDown, tvThick, tvThin, tvHideCtrl;
    static RadioButton radioOp1, radioOp2, radioOp3, radioOp4;
    static private SeekBar seekBar;
    static private boolean bHideCtrl = false; //true for full screen for drawing
    static public int nDrawColor = Color.RED;
    static public int nDrawThickness = 10;
    static public boolean bTimeoutAnswer = false;
    static public int nShowTestPath=0; //to show the test path after answer timeout if user choose a color
    private CustomPoint ScreenSize;
    static final Handler handler = new Handler(); //to prevent multiple timers
    static public ArrayList<SingleBird> alBirds = new ArrayList<>();
    static public int nReivewPattern = 4; //0-3: each for 1 of the 4 test patterns, 4: review all patterns
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //to export image file, need read permission?
        //if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        //    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        //}
        //if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        //    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        //}

        setContentView(R.layout.activity_create_pattern);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        layoutOption = findViewById(R.id.groupOption);
        layoutDraw = findViewById(R.id.groupDraw);
        tvResult = findViewById(R.id.tvResult);
        tvThick = findViewById(R.id.tvThickness);
        tvThin = findViewById(R.id.tvThin);
        tvHideCtrl = findViewById(R.id.tvHideCtrl);
        //tvDrawColor = findViewById(R.id.tvDrawColor);
        tvCountDown = findViewById(R.id.tvCountDown);
        drawView = (DrawView) findViewById(R.id.mDrawView);
        btnUndo = (Button) findViewById(R.id.btnUndo);
        btnLoad = (Button) findViewById(R.id.btnLoad);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnExport = (Button) findViewById(R.id.btnExport);
        radioOp1 = (RadioButton) findViewById(R.id.radioOption1);
        radioOp2 = (RadioButton) findViewById(R.id.radioOption2);
        radioOp3 = (RadioButton) findViewById(R.id.radioOption3);
        radioOp4 = (RadioButton) findViewById(R.id.radioOption4);
        UpdateTitleBar("");

        if (FileSaveLoad.IsDataExist(getBaseContext())) EnableButton(btnLoad, true, 0xFF99EAA4);
        else EnableButton(btnLoad, false, 0);
        EnableButton(btnSave, false, 0);
        EnableButton(btnExport, false, 0);

        radioOp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickColorOption(0);
            }
        });
        radioOp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickColorOption(1);
            }
        });
        radioOp3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickColorOption(2);
            }
        });
        radioOp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickColorOption(3);
            }
        });
        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.Undo();
                        break;
                    case REVIEW_ALL_PATTERNS:
                        OperationCtrl.nState = OperationCtrl.STATE.CHANGE_PATTERN;
                        nReivewPattern = 0;
                        drawView.Draw();
                        break;
                    case CHANGE_PATTERN:
                        if (nReivewPattern == 4) nReivewPattern = 0;
                        //generate this pattern
                        FlyingPaths.RegeneratePath(nReivewPattern);
                        //show up
                        drawView.Draw();
                        break;
                    default:
                        MainActivity.bPlayerReceived = false;
                        finish();
                        break;
                }
            }
        });
        btnUndo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.UndoFast();
                        break;

                    default:

                        break;
                }
                return true;
            }
        });
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.LoadPattern();
                        break;
                    default:
                        break;
                }
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        drawView.SavePattern();
                        break;
                    default:
                        break;
                }
            }
        });
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        if (hasStoragePermission(1))
                            drawView.ExportPattern();
                        else {
                            Log.i(TAG, "btnExport inform permission");
                            ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                            Toast.makeText(CreatePattern.this, "Try again after you grant WRITE permission", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        btnReset = (Button) findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                drawView.Clear();
                EnableButton(btnSave, false, 0);
                EnableButton(btnExport, false, 0);
            }
        });
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "btnConfirm state "+ OperationCtrl.nState);
                switch (OperationCtrl.nState) {
                    case CREATE_PATTERN:
                        if (DrawView.alTouchPoints.size() > 0){
                            Log.i(TAG, "alTouchPoints.size="+ DrawView.alTouchPoints.size());
                            OperationCtrl.GenerateTargetPattern();
                            if (MainActivity.nUserRole == MainActivity.ROLE.SELF) finish();
                            else {
                                nReivewPattern = 4; //review all patterns
                                OperationCtrl.nState = OperationCtrl.STATE.REVIEW_ALL_PATTERNS;
                                GoScreenGeneratePattern();
                            }
                        }
                        else{
                            ShowWarning(getResources().getString(R.string.text_no_pattern));
                        }
                        break;
                    case REVIEW_ALL_PATTERNS:
                        finish(); // back to main
                        break;
                    case CHANGE_PATTERN:
                        //update current pattern
                        nReivewPattern ++;
                        if (nReivewPattern == 4)
                        {
                            OperationCtrl.nState = OperationCtrl.STATE.REVIEW_ALL_PATTERNS;
                        }
                        GoScreenGeneratePattern(); //go to next pattern
                        break;
                    case OBSERVE_PATTERN:
                        GoTestScreen();
                        break;
                    case ANSWER_PATTERN:
                        //get answer
                        int nResult = -1;
                        if (radioOp1.isChecked()) nResult = 0;
                        else if (radioOp2.isChecked()) nResult = 1;
                        else if (radioOp3.isChecked()) nResult = 2;
                        else if (radioOp4.isChecked()) nResult = 3;
                        if (nResult == OperationCtrl.nQuestionIndex) {
                            tvResult.setText(getResources().getString(R.string.text_well_done));
                            tvResult.setBackgroundColor(Color.BLUE);
                            OperationCtrl.eResult = OperationCtrl.ANSWER_RESULT.PASSED;
                        } else
                        {
                            tvResult.setText(getResources().getString(R.string.text_wrong));
                            tvResult.setBackgroundColor(Color.BLACK);
                            OperationCtrl.eResult = OperationCtrl.ANSWER_RESULT.FAILED;
                        }
                        tvResult.setVisibility(View.VISIBLE);

                        OperationCtrl.nState = OperationCtrl.STATE.ANSWERED;
                        UpdateButtons();
                        if (MainActivity.nUserRole == MainActivity.ROLE.PLAYER) btnUndo.performClick();
                        break;
                }
            }
        });

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setMax(360);
        // This listener listen to seek bar change event.
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                DecimalFormat decimalFormat = new DecimalFormat("0.00%");
                // Calculate progress value percentage.
                float progressPercentageFloat = (float)progress / (float)seekBar.getMax();
                String progressPercentage = decimalFormat.format(progressPercentageFloat);
                // Show the percentage in text view.
                float[] hsv={progress, 1, 1};
                if (progress == 0) nDrawColor = Color.BLACK;
                else if (progress == 360) nDrawColor = Color.WHITE;
                else nDrawColor = Color.HSVToColor(hsv);//Color.rgb((progress&0xff0000)>>16, (progress&0xff00)>>8, progress&0xff);//0xff000000+ progress;// (int)(progressPercentageFloat*Color.BLACK);
                seekBar.getProgressDrawable().setColorFilter(nDrawColor, PorterDuff.Mode.SRC_ATOP);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // When seek bar start slip.
                Log.i(TAG, "Start Slip.");
                ViewGroup.LayoutParams params = seekBar.getLayoutParams();
                params.height = 100;
                params.width = 900;
                seekBar.setLayoutParams(params);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // When seek bar stop slip.
                Log.i(TAG, "Stop Slip.");
                UpdateThickColor();
                ViewGroup.LayoutParams params = seekBar.getLayoutParams();
                params.height = 100;
                params.width = 100;
                seekBar.setLayoutParams(params);
            }
        });
        tvThick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nDrawThickness < 50)
                    nDrawThickness += 5;
            }
        });
        tvThin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nDrawThickness > 5)
                    nDrawThickness -= 5;
            }
        });
        tvHideCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bHideCtrl = !bHideCtrl;
                HideControl();
                SetTextviewHide();
            }
        });
    }
    static public void EnableButtonSave()
    {
        EnableButton(btnSave, true, 0xFF99EAA4);
        EnableButton(btnExport, true, 0xFF99EAA4);
    }
    static private void EnableButton(Button btn, boolean enable, int color)
    {
        btn.setEnabled(enable);
        if (enable) {
            btn.setBackgroundColor(color);
            btn.setTextColor(Color.BLACK);
        }
        else
        {
            btn.setBackgroundColor(Color.GRAY);
            btn.setTextColor(Color.BLACK);
        }
    }
    private void SetTextviewHide()
    {
        if (bHideCtrl) {
            tvHideCtrl.setText(">>");
        }
        else {
            tvHideCtrl.setText("<<");
        }
    }
    private void HideControl()
    {
        if (OperationCtrl.nState != OperationCtrl.STATE.CREATE_PATTERN) return;
        int showup = bHideCtrl ? View.INVISIBLE:View.VISIBLE;
        tvThick.setVisibility(showup);
        tvThin.setVisibility(showup);
        seekBar.setVisibility(showup);
        btnConfirm.setVisibility(showup);
        btnUndo.setVisibility(showup);
        btnReset.setVisibility(showup);
        btnSave.setVisibility(showup);
        btnLoad.setVisibility(showup);
        btnExport.setVisibility(showup);
        UpdateTitleBar("");
        drawView.Draw();
        Log.i(TAG, "HideControl");
    }
    private void ClickColorOption(int n)
    {
        nShowTestPath = n;
        if (OperationCtrl.nState == OperationCtrl.STATE.ANSWERED //|| OperationCtrl.nState == OperationCtrl.STATE.NOT_ANSWERED)
                && OperationCtrl.eResult != OperationCtrl.ANSWER_RESULT.NOT_YET)
            drawView.Draw();
        tvResult.setVisibility(View.INVISIBLE);
    }
    static private void ShowWarning(String info)
    {
        tvCountDown.setTextColor(Color.RED);
        tvCountDown.setText(info);
        tvCountDown.setVisibility(View.VISIBLE);
    }
    static public void HideWarning()
    {
        tvCountDown.setVisibility(View.INVISIBLE);
    }
    static public void HideResult(){tvResult.setVisibility(View.INVISIBLE);}
    private void UpdateThickColor()
    {
        tvThick.setBackgroundColor(nDrawColor);
        tvThick.setTextColor(Color.rgb(255-((nDrawColor>>16)&0xff), 255- ((nDrawColor>>8)&0xff), 255- (nDrawColor&0xff)));
        tvThin.setBackgroundColor(nDrawColor);
        tvThin.setTextColor(Color.rgb(255-((nDrawColor>>16)&0xff), 255- ((nDrawColor>>8)&0xff), 255- (nDrawColor&0xff)));
    }
    private void UpdateButtons() {
        Log.i(TAG, "UpdateButtons state="+OperationCtrl.nState);
        btnConfirm.setTextColor(Color.RED);
        btnConfirm.setBackgroundColor(Color.GREEN);
        btnConfirm.setTextColor(Color.BLACK);
        btnReset.setBackgroundColor(Color.GREEN);
        btnUndo.setBackgroundColor(Color.GREEN);
        tvCountDown.setVisibility(View.INVISIBLE);
        switch (OperationCtrl.nState) {
            case CREATE_PATTERN:
                UpdateTitleBar(getResources().getString(R.string.text_help_create));
                btnReset.setVisibility(View.VISIBLE);
                btnUndo.setVisibility(View.VISIBLE);
                layoutOption.setVisibility(View.INVISIBLE);
                tvResult.setVisibility(View.INVISIBLE);
                layoutDraw.setVisibility(View.VISIBLE);
                UpdateThickColor();
                SetTextviewHide();
                break;
            case REVIEW_ALL_PATTERNS:
            case CHANGE_PATTERN:
                UpdateTitleBar(getResources().getString(R.string.text_help_create_pattern_review));
                btnLoad.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
                btnExport.setVisibility(View.INVISIBLE);
                btnReset.setVisibility(View.INVISIBLE);
                btnUndo.setVisibility(View.VISIBLE);
                btnUndo.setText(getResources().getString(R.string.button_undo_change));
                layoutOption.setVisibility(View.INVISIBLE);
                tvResult.setVisibility(View.INVISIBLE);
                layoutDraw.setVisibility(View.VISIBLE);
                UpdateThickColor();
                SetTextviewHide();
                break;
            case OBSERVE_PATTERN:
                UpdateTitleBar(getResources().getString(R.string.text_help_observe));
                btnConfirm.setText(getResources().getString(R.string.button_test));
                btnReset.setVisibility(View.INVISIBLE);
                btnLoad.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
                btnExport.setVisibility(View.INVISIBLE);
                btnUndo.setVisibility(View.INVISIBLE);
                tvCountDown.setVisibility(View.VISIBLE);
                layoutOption.setVisibility(View.INVISIBLE);
                tvResult.setVisibility(View.INVISIBLE);
                layoutDraw.setVisibility(View.INVISIBLE);
                break;
            case ANSWER_PATTERN:
                UpdateTitleBar(getResources().getString(R.string.text_help_answer));
                btnConfirm.setText(getResources().getString(R.string.button_confirm));
                btnConfirm.setEnabled(true);
                btnReset.setVisibility(View.INVISIBLE);
                btnLoad.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
                btnUndo.setText(getResources().getString(R.string.button_return));
                btnUndo.setVisibility(View.INVISIBLE);
                layoutOption.setVisibility(View.VISIBLE);
                tvCountDown.setVisibility(View.VISIBLE);
                radioOp1.setTextColor(getResources().getColor(R.color.option1));
                radioOp2.setTextColor(getResources().getColor(R.color.option2));
                radioOp3.setTextColor(getResources().getColor(R.color.option3));
                radioOp4.setTextColor(getResources().getColor(R.color.option4));
                break;
            default:
                tvCountDown.setVisibility(View.VISIBLE);
                DisableConfirmButton();

                break;
        }
    }

    private void GoTestScreen() {
       OperationCtrl.GenerateQuestion();
       OperationCtrl.nState =OperationCtrl.STATE.ANSWER_PATTERN;
       OperationCtrl.StartAnswerTime();
        UpdateButtons();
        //Log.i(TAG, "btnConfirm state="+OperationCtrl.nState);
        drawView.Draw();
    }
    private void GoScreenGeneratePattern() {
        UpdateButtons();
        //Log.i(TAG, "btnConfirm state="+OperationCtrl.nState);
        drawView.Draw();
    }

    private boolean hasStoragePermission(int requestCode) {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;//performAction(...);
        }  else {
            // The registered ActivityResultCallback gets the result of this request.
            //Log.i(TAG, "hasStoragePermission request "+requestCode);
            // AndroidManifest.xml should add <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    requestCode);
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    drawView.ExportPattern();
                } else {
                    //Log.i(TAG, "onRequestPermissionsResult failed");
                    Toast.makeText(this, getResources().getString(R.string.text_need_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    public String GetInfo()
    {
        switch (OperationCtrl.nState)
        {
            case CREATE_PATTERN:
                return "";
            case OBSERVE_PATTERN:
                return "";
            case ANSWER_PATTERN:
                return " Q" + OperationCtrl.nQuestionIndex;
            default:
                return "";
        }
    }
    static public int GetBirdColor(int index)
    {
        return drawView.SetBirdColor(index);
        //SingleBird bird = alBirds.get(index);
        //return bird.color;
    }
    private void UpdateTitleBar(String info)
    {
        DrawView.Title=getResources().getString(R.string.app_name) + (info.isEmpty()? "": " - " + info);//"info + ScreenSize.x +"x"+ScreenSize.y + FlyingPaths.GetInfo() + GetInfo();

    }
    private void DisableConfirmButton()
    {
        btnConfirm.setEnabled(false);
        btnConfirm.setBackgroundColor(Color.GRAY);
    }
    private void ShowAllPatterns()
    {
        tvCountDown.setTextSize(20);
        tvCountDown.setText(getResources().getString(R.string.text_view_all));
        btnUndo.setVisibility(View.VISIBLE);
        drawView.bAllowShowAllPaths = true;
    }
    private void ShowAnswerCountdown()
    {
        if (OperationCtrl.nState == OperationCtrl.STATE.ANSWERED){
            if (MainActivity.nUserRole == MainActivity.ROLE.PLAYER)
            {
                btnUndo.performClick();
            }
            else if (MainActivity.nUserRole == MainActivity.ROLE.SELF)
            {
                ShowAllPatterns();
            }
            return;
        }

        long nDuration = OperationCtrl.GetAnswerTime();
        Log.i(TAG, "ShowAnswerCountdown "+nDuration + " role="+ MainActivity.nUserRole + " state="+OperationCtrl.nState);
        if (nDuration > ConfigActivity.cfgTimeAnswer)
        {
            if (OperationCtrl.nState != OperationCtrl.STATE.ANSWERED)
            {
                OperationCtrl.nState = OperationCtrl.STATE.ANSWERED;
                OperationCtrl.eResult = OperationCtrl.ANSWER_RESULT.TIMEOUT;
            }

            DisableConfirmButton();
            if (MainActivity.nUserRole == MainActivity.ROLE.SELF)
            {
                ShowAllPatterns();
            }
            return;
        }
        if (nDuration < ConfigActivity.cfgTimeAnswer - 5)
        {
            //normal count down
            tvCountDown.setTextColor(Color.BLACK);
            tvCountDown.setTextSize(20);
        }
        else
        {
            tvCountDown.setTextColor(Color.RED);
            tvCountDown.setTextSize(30);
        }
        tvCountDown.setText(""+ (ConfigActivity.cfgTimeAnswer - nDuration));
    }
    private void ShowCountDown()
    {
        long nDuration = 0;
        switch (OperationCtrl.nState)
        {
            case OBSERVE_PATTERN:
                //bTimeoutAnswer = false;
                nDuration = OperationCtrl.GetObserveTime();
                if (nDuration < ConfigActivity.cfgTimeObserve - 5)
                {
                    //normal count down
                    tvCountDown.setTextColor(Color.BLACK);
                    tvCountDown.setTextSize(20);
                }
                else
                {
                    tvCountDown.setTextColor(Color.RED);
                    tvCountDown.setTextSize(30);
                }
                tvCountDown.setText(""+ (ConfigActivity.cfgTimeObserve - nDuration));
                if (nDuration > ConfigActivity.cfgTimeObserve) GoTestScreen();
                break;
            case ANSWER_PATTERN:
                ShowAnswerCountdown();
                break;
            case ANSWERED:
                ShowAnswerCountdown();
                break;

            default:
                break;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        UpdateButtons();

        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point(0,0);
        display.getSize(point);
        ScreenSize = new CustomPoint(point);

        FreeFlying.SCREEN_SIZE = ScreenSize;
        Log.i(TAG, "onResume nState="+OperationCtrl.nState);
        if (alBirds.size() != ConfigActivity.cfgBirdNum){
            alBirds.clear();
        }
        SingleBird.UpdateBirdSize();
        if (OperationCtrl.nState == OperationCtrl.STATE.OBSERVE_PATTERN) {
            Flying();
            OperationCtrl.StartObserveTime();
        }

    }

    public void UpdateBirdPosition(int nIndex) {
        //free running in the screen area
        SingleBird bird = alBirds.get(nIndex);

        //first 4 are the testing birds
        if (nIndex < FlyingPaths.alPaths.size())
        {
            bird.currPosition = FlyingPaths.GetNextPosition(nIndex);
            if (bird.currPosition.equals(FlyingPaths.BREAKPOINT)) bird.currPosition = FlyingPaths.GetNextPosition(nIndex);
        }
        else { //free flying
            FreeFlying adj = new FreeFlying(bird);
            bird = adj.updatePosition();
            //bird.info += "["+bird.collideLeft+","+bird.collideRight+", "+bird.collideTop+","+bird.collideBottom+"]";
            if (bird.currPosition.x < 0 || bird.currPosition.x > ScreenSize.x || bird.currPosition.y < 0 || bird.currPosition.y > ScreenSize.y) {
                bird.currPosition.x = 0;
                bird.currPosition.y = 0;
            }
        }
          //Log.i(TAG, "UpdateBirdPosition x="+bird.currPosition.x + " y="+bird.currPosition.y + ", bird speed next=" + bird.speedValueNext + "@" + bird.speedAngleNext);
        alBirds.set(nIndex, bird);
    }
    private void Flying()
    {
        //final Handler handler=new Handler();

        handler.post(new Runnable(){
            int nTimeCount = 0;
            public void run(){

                if (OperationCtrl.nState != OperationCtrl.STATE.OBSERVE_PATTERN) {
                    //Log.i(TAG, "Flying stop nState="+OperationCtrl.nState);
                    handler.postDelayed(this, ConfigActivity.cfgSpeed);

                    //for answer count down
                    if (OperationCtrl.nState == OperationCtrl.STATE.ANSWER_PATTERN || OperationCtrl.nState == OperationCtrl.STATE.ANSWERED) {
                        ShowCountDown();
                    }
                    return;
                }
                Log.i(TAG, "Flying nState="+OperationCtrl.nState);
                boolean spawning = false;
                //Log.i(TAG, "Flying "+ alBirds.size() + ", screen.x=" + FlyingAdjustment.SCREEN_SIZE.x );
                int nIndex = alBirds.size();
                if (nIndex < ConfigActivity.cfgBirdNum)
                {
                    SingleBird bird = new SingleBird(nIndex, ScreenSize, FreeFlying.MAX_SPEED);
                    bird.SetInfo(Integer.toString(nIndex));

                    alBirds.add(bird);
                    //DrawView.alPoints.add(bird.currPosition);
                    spawning = true;
                }
                for (int i=0; i<alBirds.size(); i++) {
                    UpdateBirdPosition(i);

                }

                drawView.Draw();

                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(this, spawning?10:ConfigActivity.cfgSpeed);

                //update the title bar with time info
                if (nTimeCount%(500/ConfigActivity.cfgSpeed) == 0) ShowCountDown();//UpdateTitleBar(testInfo);
                nTimeCount++;
            }
        });
    }
}