package com.tongs.funpatternwifi;

import android.graphics.Point;
import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.tongs.view.DrawView;

import java.util.ArrayList;
import java.util.Random;

import com.tongs.view.DrawView;

public class OperationCtrl {
    static private String TAG="[dbg]OperationCtrl";
    public enum STATE
    {
        START_UP,  //app just open, setting loaded

        //Host states
        CREATE_PATTERN,         //draw the pattern onscreen to creating the base pattern

        REVIEW_ALL_PATTERNS,     //show all patterns
        CHANGE_PATTERN,         //re-generate a test pattern

        //Client state (players)
        OBSERVE_PATTERN,        //animation for observing
        ANSWER_PATTERN,         //show exam questions
        ANSWERED,               //user has answered, show result (ok or failed)
        //NOT_ANSWERED,        //replaced with ANSWER_RESULT   //timeout of Answer, show failed
    };
    public enum ANSWER_RESULT {
        NOT_YET,      //answering
        PASSED,     //correct
        FAILED,     //wrong
        TIMEOUT,    //not answered
    };
    static public STATE nState = STATE.START_UP;
    static public ANSWER_RESULT eResult = ANSWER_RESULT.NOT_YET;
    static public ArrayList<Point> alTargetPattern = new ArrayList<>();
    static public int nQuestionIndex = 0; //the pattern index of the answer (0-3)
    static long lObserveStartTime = 0, lAnswerStartTime=0;

    static public void Reset()
    {
        nState = STATE.START_UP;
        DrawView.alTouchPoints.clear();
        FlyingPaths.alBasePath.clear();
        FlyingPaths.alPaths.clear();
    }
    static public void GenerateTargetPattern()
    {
        //target pattern:
        FlyingPaths.SetBasePath(DrawView.alTouchPoints);
        GenerateTestPaths();
    }

    static public void GenerateTestPaths()
    {
        if (FlyingPaths.alBasePath.size() > 0) {
            FlyingPaths.AddTestPaths(4, ConfigActivity.cfgDifficulty);
            FlyingPaths.SetStartingPosition();
        }
    }
    static public void GenerateQuestion()
    {
        Random rand = new Random();
        nQuestionIndex = rand.nextInt(FlyingPaths.alPaths.size()) ;
        Log.i(TAG, "GenerateQuestion "+ nQuestionIndex + "/"+FlyingPaths.alPaths.size()+ " state="+nState);

    }
    static boolean IsPatternAvailable()
    {
        return FlyingPaths.alPaths!=null && FlyingPaths.alPaths.size() > 0;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public void StartObserveTime()
    {
        lObserveStartTime = Calendar.getInstance().getTimeInMillis()/1000;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public void StartAnswerTime()
    {
        lAnswerStartTime = Calendar.getInstance().getTimeInMillis()/1000;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public long GetObserveTime()
    {
        return Calendar.getInstance().getTimeInMillis()/1000 - lObserveStartTime;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    static public long GetAnswerTime()
    {
        return Calendar.getInstance().getTimeInMillis()/1000 - lAnswerStartTime;
    }
}
