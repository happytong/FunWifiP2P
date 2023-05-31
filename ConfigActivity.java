package com.tongs.funpatternwifi;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ConfigActivity extends AppCompatActivity {
    private String TAG="[dbg]ConfigActivity";
    static public int cfgBirdNum = 10; //1+ birds flying
    static public int cfgSpeed = 10; //30ms - 1000ms
    static public int cfgTimeObserve= 30; //30s
    static public int cfgTimeAnswer= 15; //30s
    static public int cfgDifficulty = 1; //0-easy, 1-medium, 2-difficult
    static public boolean cfgShowId = false; //display each bird info
    static public boolean cfgTeleport = true; //true: allow broken lines of pattern
    static public int cfgSize= 1; //0=small;1=big
    static public int cfgBackground = 0; //background color of observation
    private int nBackgroundSelect = cfgBackground;

    TextView tvBirdNumber, tvSpeed;
    TextView tvTimeObserve, tvTimeAnswer;
    CheckBox cbShowId, cbTeleport;
    Spinner spinnerSize, spinnerDifficulty;

    private int nBirdNum=cfgBirdNum;
    private int nSpeed=cfgSpeed;
    private int nTimeObserve=cfgTimeObserve;
    private int nTimeAnswer=cfgTimeAnswer;
    private int nSize=cfgSize;
    private int nDifficulty=cfgDifficulty;
    private int nHighlightInput = 0; //for each TextView to update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        tvBirdNumber = findViewById(R.id.tvBirdNum);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvTimeObserve = findViewById(R.id.tvTimeoutObserve);
        tvTimeAnswer = findViewById(R.id.tvTimeoutAnswer);
        cbShowId = findViewById(R.id.checkBoxShowID);
        cbTeleport = findViewById(R.id.checkBoxTeleportation);
        Log.i(TAG, "onCreate: spinnerDifficulty ");
        //spinners
        spinnerDifficulty = (Spinner) findViewById(R.id.spinnerDifficulty);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.array_difficulty,  R.layout.spinner_format);//android.R.layout.simple_spinner_item,
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //string tutorialsName = parent.getItemAtPosition(position).toString();
                //Toast.makeText(parent.getContext(), "Selected: " + position, Toast.LENGTH_LONG).show();
                nDifficulty = position;
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });
        Log.i(TAG, "onCreate: spinnerColor ");
        spinnerSize = (Spinner) findViewById(R.id.spinnerSize);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.array_size, R.layout.spinner_format);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSize.setAdapter(adapter);
        spinnerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nSize = position;
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        //Log.i(TAG, "onCreate: mode="+ cfgMode );
        tvBirdNumber.setText(getResources().getString(R.string.bird_count)+":"+cfgBirdNum);
        tvSpeed.setText(getResources().getString(R.string.speed)+":"+cfgSpeed+"ms");
        tvTimeObserve.setText(getResources().getString(R.string.timeout_observe)+":"+cfgTimeObserve+"s");
        tvTimeAnswer.setText(getResources().getString(R.string.timeout_answer)+":"+cfgTimeAnswer+"s");
        cbShowId.setChecked(cfgShowId);
        cbTeleport.setChecked(cfgTeleport);
        spinnerDifficulty.setSelection(cfgDifficulty);
        spinnerSize.setSelection(cfgSize);

        //cbFollow.setChecked(cfgFollow);
        ((GridLayout)findViewById(R.id.layoutCfg)).setBackgroundColor(getCfgBackground(cfgBackground));

        final Button btnUpdate = (Button) findViewById(R.id.btnConfirm);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save the setting
                cfgBirdNum = nBirdNum;
                if (cfgBirdNum < 4) cfgBirdNum = 4;
                //if (cfgBirdNum > 1000) cfgBirdNum = 1000;

                cfgSpeed = nSpeed;
                if (cfgSpeed < 10) cfgSpeed = 10;
                if (cfgSpeed > 500) cfgSpeed = 500;

                cfgTimeObserve = nTimeObserve;
                cfgTimeAnswer = nTimeAnswer;

                if (cfgDifficulty != nDifficulty){
                    cfgDifficulty = nDifficulty;
                    OperationCtrl.GenerateTestPaths();
                }

                cfgSize = nSize;

                if (cfgTeleport != cbTeleport.isChecked()){
                    Log.i(TAG, "call OperationCtrl.Reset");
                    OperationCtrl.Reset();
                }
                cfgTeleport = cbTeleport.isChecked();

                // Log.i(TAG, "Update total safearea=" + ", screen area="+ (FlyingAdjustment.SCREEN_SIZE.x * FlyingAdjustment.SCREEN_SIZE.y));

                cfgShowId = cbShowId.isChecked();
                cfgBackground = nBackgroundSelect;
                SingleBird.UpdateBirdSize();
                //Log.i(TAG, "btnUpdate finish: mode="+ cfgMode );
                finish();
            }
        });

        final Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "btnCancel ");
                finish();
            }
        });

        tvBirdNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                HighlightBirdNumber();
            }
        });
        tvSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nHighlightInput =2;
                tvBirdNumber.setBackgroundColor(0xfff6f4f4);
                tvSpeed.setBackgroundColor(Color.CYAN);
                tvTimeAnswer.setBackgroundColor(0xfff6f4f4);
                tvTimeObserve.setBackgroundColor(0xfff6f4f4);
            }
        });
        tvTimeAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nHighlightInput =4;
                tvBirdNumber.setBackgroundColor(0xfff6f4f4);
                tvSpeed.setBackgroundColor(0xfff6f4f4);
                tvTimeAnswer.setBackgroundColor(Color.CYAN);
                tvTimeObserve.setBackgroundColor(0xfff6f4f4);
            }
        });
        tvTimeObserve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nHighlightInput =3;
                tvBirdNumber.setBackgroundColor(0xfff6f4f4);
                tvSpeed.setBackgroundColor(0xfff6f4f4);
                tvTimeAnswer.setBackgroundColor(0xfff6f4f4);
                tvTimeObserve.setBackgroundColor(Color.CYAN);
            }
        });
        final Button btnPlus = (Button) findViewById(R.id.btnPlus);
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(nHighlightInput)
                {
                    case 0:
                        HighlightBirdNumber();
                        return;
                    case 1: //bird number
                        if (nBirdNum<10) nBirdNum++;
                        else nBirdNum *=1.1;
                        tvBirdNumber.setText(getResources().getString(R.string.bird_count)+":"+nBirdNum);
                        break;
                    case 2: //speed
                        nSpeed *=1.1;
                        tvSpeed.setText(getResources().getString(R.string.speed)+":"+nSpeed+"ms");
                        break;
                    case 3: //observe timing
                        if (nTimeObserve < 100) nTimeObserve *=1.1;
                        if (nTimeObserve<10) nTimeObserve++;
                        tvTimeObserve.setText(getResources().getString(R.string.timeout_observe)+":"+nTimeObserve+"s");
                        break;
                    case 4: //answer timing
                        if (nTimeAnswer < 100) nTimeAnswer *=1.1;
                        if (nTimeAnswer<10) nTimeAnswer++;

                        tvTimeAnswer.setText(getResources().getString(R.string.timeout_answer)+":"+nTimeAnswer+"s");
                        break;
                }
            }
        });
        final Button btnMinus = (Button) findViewById(R.id.btnMinus);
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(nHighlightInput)
                {
                    case 0: HighlightBirdNumber(); return;
                    case 1: //bird number
                        if(nBirdNum>=5) nBirdNum *=0.9;
                        tvBirdNumber.setText(getResources().getString(R.string.bird_count)+":"+nBirdNum);
                        break;
                    case 2: //speed
                        if (nSpeed>=12) nSpeed *=0.9;
                        else nSpeed = 10;
                        tvSpeed.setText(getResources().getString(R.string.speed)+":"+nSpeed+"ms");
                        break;
                    case 3: //observe time
                        if (nTimeObserve > 5) nTimeObserve *=0.9;
                        tvTimeObserve.setText(getResources().getString(R.string.timeout_observe)+":"+nTimeObserve+"s");
                        break;
                    case 4: //stroke show time
                        if (nTimeAnswer > 5) nTimeAnswer *=0.9;
                        tvTimeAnswer.setText(getResources().getString(R.string.timeout_answer)+":"+nTimeAnswer+"s");
                        break;
                }
            }
        });

    }
    public void HighlightBirdNumber()
    {
        nHighlightInput =1;
        tvBirdNumber.setBackgroundColor(Color.CYAN);
        tvSpeed.setBackgroundColor(0xfff6f4f4);
        tvTimeObserve.setBackgroundColor(0xfff6f4f4);
        tvTimeAnswer.setBackgroundColor(0xfff6f4f4);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        Log.i(TAG, "onTouchEvent "+ event.getAction());
        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:
                //change background
                nBackgroundSelect ++;
                if (nBackgroundSelect > 8) nBackgroundSelect = 0;
                //tvBackground.setBackgroundColor(getCfgBackground(nBackgroundSelect));
                GridLayout rl = (GridLayout)findViewById(R.id.layoutCfg);
                rl.setBackgroundColor(getCfgBackground(nBackgroundSelect));
                Log.i(TAG, "onTouchEvent setBackgroundColor " + nBackgroundSelect + ","+ getCfgBackground(nBackgroundSelect));
                break;
        }
        return true;
    }
    static public int getCfgBackground(int n)
    {
        switch (n)
        {
            //only with 0xFFrrggbb, Export to JPG will show the same background...
            case 0: return 0xFFeeeeee;//0x44b5e1f2;
            case 1: return 0xFFdddddd;//220000ff;
            case 2: return 0xFFffeeee;//2200ff00;
            case 3: return 0xFFffeeff;//336fc4e1;
            case 4: return 0xFFeeddee;//0x222200;//0x22ffff00;
            case 5: return 0xFFddeeee;//22ff00ff;
            case 6: return 0xFFeeffee;
            case 7: return 0xFFeeeedd;//2200ffff;
            default: return 0xffffffdd;//22ffffff;
        }
    }
}