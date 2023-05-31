package com.tongs.funpatternwifi;

import android.graphics.Point;

import java.util.Random;

public class SingleBird {
    static public CustomPoint BIRD_SIZE=new CustomPoint(10, 15); //x: body with wing extend, y: body length; assume x >> y, so x is used for collision detection
    public static CustomPoint SAFE_RANGE=new CustomPoint(2*SingleBird.BIRD_SIZE.x, 2*SingleBird.BIRD_SIZE.y);
    public CustomPoint currPosition; //current position of the bird
    public int speedValueCurr; //absolute value, continuous change
    public int speedAngleCurr; //0-359
    public int speedValueNext; //next speed
    public int speedAngleNext; //
    public String info;
    public int id; //used by FollowFlying
    public CustomPoint fieldLeftTop;
    public CustomPoint fieldRigtBottom;
    //public int color;
    //testing
    public int collideLeft, collideRight, collideTop, collideBottom;
    public enum INFO
    {
        DEFAULT, //id
        PATH, // ^
    };

    public SingleBird(int id0, CustomPoint screenSize, int maxSpeed)
    {
        id = id0;
        //randomly for left side or right side
        Random rand = new Random();
        int x=rand.nextInt(2)==0?0: screenSize.x;
        currPosition = new CustomPoint(x, rand.nextInt(screenSize.y));

        speedValueCurr =  rand.nextInt(maxSpeed);
        speedAngleCurr = rand.nextInt(359);
        speedValueNext = 0;
        speedAngleNext = 0;
        info = ""+id;
        collideLeft= collideRight= collideTop =collideBottom =0;
        SetDefaultField();

    }

    static public void UpdateBirdSize()
    {
        if (ConfigActivity.cfgSize==0)
        {
            BIRD_SIZE.x = 10;
            BIRD_SIZE.y = 15;
        }
        else //use color, make big size
        {
            BIRD_SIZE.x = 15;
            BIRD_SIZE.y = 25;
        }
    }

    public void SetDefaultField()
    {
        fieldLeftTop = new CustomPoint(50, 100);
        fieldRigtBottom = new CustomPoint(FreeFlying.SCREEN_SIZE.x -50, FreeFlying.SCREEN_SIZE.y-200);
    }
    public boolean isDefaultField()
    {
        CustomPoint p1 = new CustomPoint(50, 100);
        CustomPoint p2 =new CustomPoint(FreeFlying.SCREEN_SIZE.x -50, FreeFlying.SCREEN_SIZE.y-200);
        return fieldLeftTop.equals(p1)  && fieldRigtBottom.equals(p2);
    }
    public String GetFieldInfo()
    {
        return "("+fieldLeftTop+"-"+fieldRigtBottom+")"
                + (fieldRigtBottom.x-fieldLeftTop.x) + "x" + (fieldRigtBottom.y-fieldLeftTop.y);
    }
    public boolean IsOutField()
    {
        return currPosition.x < fieldLeftTop.x || currPosition.x >= fieldRigtBottom.x
                ||currPosition.y < fieldLeftTop.y || currPosition.y >= fieldRigtBottom.y;
    }
    public void SetField(CustomPoint p1, CustomPoint p2)
    {
        fieldLeftTop= p1;
        fieldRigtBottom = p2;
    }
    public int GetFieldLeft()
    {
        return fieldLeftTop.x;
    }
    public int GetFieldRight()
    {
        return fieldRigtBottom.x;
    }
    public int GetFieldTop()
    {
        return fieldLeftTop.y;
    }
    public int GetFieldBottom()
    {
        return fieldRigtBottom.y;
    }
    public void SetBird(SingleBird bird)
    {
        currPosition = bird.currPosition;
        speedValueCurr = bird.speedValueCurr;
        speedAngleCurr = bird.speedAngleCurr;
        speedValueNext = bird.speedValueNext;
        speedAngleNext = bird.speedAngleNext;
    }

    public void SetInfo(String str)
    {
        info = str;
    }
    public void SetInfo(INFO type, int n)
    {
        switch(type)
        {
            case DEFAULT:
                info = id+":";
                break;
            case PATH:
                info = id+":"+n+"^";
                break;
            default:
                break;
        }
    }
    public int GetRange()
    {
        if (speedAngleNext < 90) return 0;
        else if (speedAngleNext < 180) return 1;
        else if (speedAngleNext < 270) return 2;
        else return 3;
    }
    public int GetRange(SingleBird bird1)
    {
        //check bird1 is in which range to me
        int x0 = currPosition.x;
        int y0 = currPosition.y;
        int x1 = bird1.currPosition.x;
        int y1 = bird1.currPosition.y;
        //special case: on axis
        if (x0 == x1) return y1>y0 ? 1:3;
        if (y0 == y1) return x1>x0 ? 0:2;
        //normal cases
        if( x1 > x0) return y1>y0 ? 0:3;
        else return y1>y0 ? 1:2;
    }
    public boolean IsLeadingAnother(SingleBird bird1, int x, int y)
    {
        if (id == bird1.id ) return false;//that is me
        //leading: same range around
        return (GetRange() == GetRange(bird1) &&
                Math.abs(bird1.currPosition.x - currPosition.x) < x &&
                Math.abs(bird1.currPosition.y - currPosition.y) < y);
    }
}
