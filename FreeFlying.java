package com.tongs.funpatternwifi;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.graphics.Point;
import android.util.Log;

import java.util.Random;

public class FreeFlying {
    public String TAG = "[dbg]FreeFlying";
    static final int MAX_SPEED = 13; //1.33m/30ms = 160km/h
    static final int AVERAGE_SPEED = 8; //0.8m/30ms average 97km/h
    static public CustomPoint SCREEN_SIZE; //flying area
    //static public final Point BIRD_SIZE=new Point(10, 15); //x: body with wing extend, y: body length; assume x >> y, so x is used for collision detection
    private CustomPoint currPosition; //current position of the bird
    private int speedValueCurr; //absolute value, continuous change
    private int speedAngleCurr; //0-359
    private int speedValueNext; //next speed
    private int speedAngleNext; //
    private SingleBird bird;

    /* Design rules:
    1. per tick at 30ms, minimum visible change is 1 pixel (0.1 meter)
    2. edge/corner detection: 10 ticks without change of speed, AxisX/Y reach 0 or screen size
    3. current speed is initialized at first, then set the next speed randomly if no collision detected (weight on remaining same)
    4. avoid edge/corner: turning, reduce speed, change angle based on current angle and next angle
        - 4 ranges of the angle: 0=0-90 (right/downward), 1=90-179 (left/downward), 2=180-269 (left/upward), 3=270-359 (right/upward)
        - turning angle in <5 ticks to next range based on current and next angles
        - turning clockwise: 0 - 1 - 2 - 3 - 0
        - turning anticlockwise: 0 - 3 - 2 - 1 - 0
    5. known issue: sometimes the bird quick flying around a point for a few seconds, looks weird
     */

    public FreeFlying(SingleBird bird0)
    {
        bird = bird0;
        currPosition = bird.currPosition;
        speedValueCurr = bird.speedValueCurr;
        speedAngleCurr = bird.speedAngleCurr;
        speedValueNext = bird.speedValueNext;
        speedAngleNext = bird.speedAngleNext;

    }
    public void setSpeedNext()
    {
        if (speedValueNext == 0)
        {
            Log.i(TAG, "setSpeedNext speedValueNext=0 curr="+speedValueCurr+ "@"+speedAngleCurr );
            speedValueNext = speedValueCurr + 1;
            speedAngleNext = speedAngleCurr + 1;
            if (speedValueNext >= MAX_SPEED ) speedValueNext --;
            if (speedAngleNext > 359) speedAngleNext = 0;
        }
        else
        {
            int speedValueNew = speedValueNext; //next speed
            int speedAngleNew = speedAngleNext;

            int x = isCollideX();
            int y = isCollideY();

            //dbg
            if(x==1) bird.collideLeft ++;
            if(x==2) bird.collideRight ++;
            if(y==3) bird.collideTop ++;
            if(y==4) bird.collideBottom ++;

            if (x > 0 && y > 0)
            {
                //corner
                if (x==1 && y==3)
                {
                    if (isClockwise())
                    {
                        //Log.i(TAG, "clockwise corner top-left, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 0: 45
                        if (speedAngleNext >= 225 )
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext >=90)
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext <= 35 || speedAngleNext >= 55)
                        {
                            speedAngleNew = 45;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                    else
                    {
                        //Log.i(TAG, "anticlockwise corner top-left, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 0: 45
                        if (speedAngleNext >= 225 )
                        {
                            speedAngleNew = speedAngleNext - 40;//
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext >=90)
                        {
                            speedAngleNew = speedAngleNext - 40;//
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 35 || speedAngleNext >= 55)
                        {
                            speedAngleNew = 45;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                }
                else if (x==1 && y==4) {
                    if (isClockwise()) {
                        //Log.i(TAG, "clockwise corner bottom-left, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 3: 315
                        if (speedAngleNext <= 135)
                        {
                            speedAngleNew = speedAngleNext + 40;//faster turning
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext <= 270)
                        {
                            speedAngleNew = speedAngleNext + 40;//faster turning
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 305 || speedAngleNext >= 325)
                        {
                            speedAngleNew = 315;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    } else {
                        //Log.i(TAG, "anticlockwise corner bottom-left");
                        // turn to range 3: 315
                        if (speedAngleNext <= 135)
                        {
                            speedAngleNew = speedAngleNext - 40;//faster turning
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 270)
                        {
                            speedAngleNew = speedAngleNext - 40;//
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext <= 305 || speedAngleNext >= 325)
                        {
                            speedAngleNew = 315;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                }
                else if (x==2 && y==3) {
                    if (isClockwise())
                    {
                        //Log.i(TAG, "clockwise corner top-right, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 1: 135
                        if (speedAngleNext >= 180 && speedAngleNext <= 315)
                        {
                            speedAngleNew = speedAngleNext + 40;//faster turning
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext <= 90 || speedAngleNext > 315)
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 125 || speedAngleNext >= 145)
                        {
                            speedAngleNew = 135;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                    else
                    {
                        //Log.i(TAG, "anticlockwise corner top-right, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 1: 135
                        if (speedAngleNext >= 180 && speedAngleNext <= 315)
                        {
                            speedAngleNew = speedAngleNext - 40;//faster turning
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 90 || speedAngleNext > 315)
                        {
                            speedAngleNew = speedAngleNext - 40;//
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext <= 125 || speedAngleNext >= 145)
                        {
                            speedAngleNew = 135;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                }
                else if (x==2 && y==4) {
                    if (isClockwise())
                    {
                        //Log.i(TAG, "clockwise corner bottom-right, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 2: 225
                        if (speedAngleNext >= 270 || speedAngleNext <= 45)
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if (speedAngleNext <= 180)//speedAngleNext >45 &&
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 215 || speedAngleNext >= 235)
                        {
                            speedAngleNew = 225;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                    else
                    {
                        //Log.i(TAG, "anticlockwise corner bottom-right, speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr);
                        // turn to range 2: 225
                        if (speedAngleNext >= 270 || speedAngleNext <= 45)
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 0.5);
                        }
                        else if ( speedAngleNext <= 180)//speedAngleNext >45 &&
                        {
                            speedAngleNew = speedAngleNext + 40;//
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                        else if (speedAngleNext <= 215 || speedAngleNext >= 235)
                        {
                            speedAngleNew = 225;
                            speedValueNew = (int) (speedValueNext * 1.5);
                        }
                    }
                }
                //else Log.i(TAG, "corner check, should not be here:" + x + "/"+y);

                if (speedValueNew == 1) speedValueNew = AVERAGE_SPEED;
            }
            else if (x>0) {
                switch (x) {
                    case 1://left edge
                        if (isClockwise()) {
                            //Log.i(TAG, "clockwise left edge");
                            if (speedAngleNext >= 90 && speedAngleNext <= 270) //turn to range 3
                            {
                                //if (speedAngleNext >= 180+45 ) speedAngleNew = speedAngleNext + 20;
                                speedAngleNew = speedAngleNext + 40;//faster turning
                            }

                            //need to get X > 0 to make sense
                            int offsetX = (int) (speedValueNext * cos(Math.toRadians(speedAngleNext)));
                            if (offsetX <= 0) {
                                speedAngleNew = speedAngleNext + 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "clockwise left edge, adjust further: " + speedValueNew + "@" + speedAngleNew);
                            }
                            if (speedAngleNext >= 90 && speedAngleNext <= 180) speedValueNew = (int) (speedValueNext * 0.8);
                            else if (speedAngleNext >= 180 && speedAngleNext <= 270) speedValueNew = (int) (speedValueNext * 1.2);

                        } else {
                            //Log.i(TAG, "anticlockwise left edge");
                            if (speedAngleNext >= 90 && speedAngleNext <= 270) //turn to range 0
                            {
                                //if (speedAngleNext >= 180-45 ) speedAngleNew = speedAngleNext - 20;
                                speedAngleNew = speedAngleNext - 40;//faster turning
                            }

                            //need to get X > 0 to make sense
                            int offsetX = (int) (speedValueNext * cos(Math.toRadians(speedAngleNext)));
                            if (offsetX <= 0) {
                                speedAngleNew = speedAngleNext - 20;
                                speedValueNew *= 2;
                                // Log.i(TAG, "anticlockwise left edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext >= 180 && speedAngleNext <= 270) speedValueNew = (int) (speedValueNext * 0.8);
                            else if (speedAngleNext >= 90 && speedAngleNext <= 180) speedValueNew = (int) (speedValueNext * 1.2);
                        }
                        break;
                    case 2://right edge
                        if (isClockwise()) {
                            // Log.i(TAG, "clockwise right edge");
                            if (speedAngleNext <= 90 || speedAngleNext >= 270) //turn to range 1: 90-180
                            {
                                //if (speedAngleNext >= 45 ) speedAngleNew = speedAngleNext + 20;
                                speedAngleNew = speedAngleNext + 40;//faster turning
                            }

                            //need to get X < 0 to make sense
                            int offsetX = (int) (speedValueNext * cos(Math.toRadians(speedAngleNext)));
                            if (offsetX >= 0) {
                                speedAngleNew = speedAngleNext + 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "clockwise right edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext >= 270) speedValueNew = (int) (speedValueNext * 0.8);
                            else if (speedAngleNext <= 90) speedValueNew = (int) (speedValueNext * 1.2);
                        } else {
                            //Log.i(TAG, "anticlockwise right edge");
                            if (speedAngleNext <= 90 || speedAngleNext >= 270) //turn to range 2
                            {
                                //if (speedAngleNext >= 270+45 ) speedAngleNew = speedAngleNext + 20;
                                speedAngleNew = speedAngleNext - 40;//faster turning
                            }
                            //need to get X < 0 to make sense
                            int offsetX = (int) (speedValueNext * cos(Math.toRadians(speedAngleNext)));
                            if (offsetX >= 0) {
                                speedAngleNew = speedAngleNext - 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "anticlockwise right edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext <= 90) speedValueNew = (int) (speedValueNext * 0.8);
                            else if (speedAngleNext >= 270) speedValueNew = (int) (speedValueNext * 1.2);
                        }
                        break;
                    default://free flying
                        //Log.i(TAG, "Switch(x) should not be here:" + x);
                        break;
                }
            }
            else if (y>0)
            {
                switch (y)
                {
                    case 3://top edge
                        if (isClockwise())
                        {
                            //Log.i(TAG, "clockwise top edge");
                            if (speedAngleNext >= 180 ) //turn to range 0
                            {
                                //if (speedAngleNext >= 270+45 ) speedAngleNew = speedAngleNext + 20;
                                speedAngleNew = speedAngleNext + 40;
                            }

                            //need to get Y > 0 to make sense
                            int offsetY = (int) (speedValueNext*sin(Math.toRadians(speedAngleNext)));
                            if (offsetY <= 0)
                            {
                                speedAngleNew = speedAngleNext + 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "clockwise top edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext >= 180 && speedAngleNext <= 270)  speedValueNew = (int) (speedValueNext * 0.8);
                            else if (speedAngleNext >= 270)  speedValueNew = (int) (speedValueNext * 1.2);
                        }
                        else
                        {
                            //Log.i(TAG, "anticlockwise top edge");
                            if (speedAngleNext >= 180 ) //turn to range 1
                            {
                                //if (speedAngleNext >= 270-45 ) speedAngleNew = speedAngleNext - 20;
                                speedAngleNew = speedAngleNext - 40;
                            }
                            //need to get Y > 0 to make sense
                            int offsetY = (int) (speedValueNext*sin(Math.toRadians(speedAngleNext)));
                            if (offsetY <= 0)
                            {
                                speedAngleNew = speedAngleNext - 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "anticlockwise top edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext >= 180 && speedAngleNext <= 270)  speedValueNew = (int) (speedValueNext * 1.2);
                            else if (speedAngleNext >= 270)  speedValueNew = (int) (speedValueNext * 0.8);
                        }
                        break;
                    case 4://bottom edge
                        if (isClockwise())
                        {
                            //Log.i(TAG, "clockwise bottom edge");
                            if (speedAngleNext <=180 ) //turn to range 2: 180-270
                            {
                                //if (speedAngleNext >= 90+45 ) speedAngleNew = speedAngleNext + 20;
                                speedAngleNew = speedAngleNext + 40;//faster turning
                            }

                            //need to get Y < 0 to make sense
                            int offsetY = (int) (speedValueNext*sin(Math.toRadians(speedAngleNext)));
                            if (offsetY >= 0)
                            {
                                speedAngleNew = speedAngleNext + 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "clockwise bottom edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext <= 90)  speedValueNew = (int) (speedValueNext * 0.8);
                            else if (speedAngleNext >= 90 && speedAngleNext <= 180)  speedValueNew = (int) (speedValueNext * 1.2);
                        }
                        else
                        {
                            //Log.i(TAG, "anticlockwise bottom edge");
                            if (speedAngleNext <=180 ) //turn to range 3: 270-0
                            {
                                //if (speedAngleNext < 45 ) speedAngleNew = speedAngleNext - 20;
                                speedAngleNew = speedAngleNext - 40;//faster turning
                            }
                            //need to get Y < 0 to make sense
                            int offsetY = (int) (speedValueNext*sin(Math.toRadians(speedAngleNext)));
                            if (offsetY >= 0)
                            {
                                speedAngleNew = speedAngleNext - 20;
                                speedValueNew *= 2;
                                //Log.i(TAG, "anticlockwise bottom edge, adjust further: speedAngleNext=" + speedAngleNew);
                            }
                            if (speedAngleNext <= 90)  speedValueNew = (int) (speedValueNext * 1.2);
                            else if (speedAngleNext >= 90 && speedAngleNext <= 180)  speedValueNew = (int) (speedValueNext * 0.8);
                        }
                        break;
                    default://free flying
                        //Log.i(TAG, "Switch(y) should not be here:" + y);
                        break;
                }
            }
            else {
                //free flying
                speedValueNew = getFreeFlyingSpeedValue();
                speedAngleNew = getFreeFlyingSpeedAngle();
            }

            if (speedAngleNew > 359 ) speedAngleNew -= 360;
            if (speedAngleNew < 0 ) speedAngleNew += 360;

            speedAngleCurr = speedAngleNext;
            speedAngleNext = speedAngleNew;

            speedValueCurr = speedValueNext;
            speedValueNext = speedValueNew;
            if (speedValueNext <= 0) speedValueNext = AVERAGE_SPEED/2;
            else if (speedValueNext >= MAX_SPEED) speedValueNext = MAX_SPEED-1;
            speedValueCurr = speedValueNext; //in case of changed
        }
    }
    private int getFreeFlyingSpeedValue()
    {
        //80% chance remain, 10% +, 10% -
        Random rand = new Random();
        int n = rand.nextInt(10);
        int next = n<8 ? speedValueNext : (n%2==0 ? speedValueNext - n/3:speedValueNext + n/3);
        return next>MAX_SPEED? MAX_SPEED -1 : next;
    }
    private int getFreeFlyingSpeedAngle()
    {
        //80% chance remain the direction
        Random rand = new Random();
        int n = rand.nextInt(10);
        int next = n<8 ? speedAngleNext : (n%2==0 ? speedAngleNext - n:speedAngleNext + n);
        return next>359? next-359 : (next<0 ? next+359:next);
    }
    private boolean isClockwise()
    {
        //Log.i(TAG, "isClockwise: speedAngleNext="+ speedAngleNext +", speedAngleCurr="+speedAngleCurr );
        return (speedAngleNext > speedAngleCurr ? (speedAngleNext >= 270 && speedAngleCurr <= 90 ? false:true) :
                (speedAngleNext <90 && speedAngleCurr >=270 ? true:false));
    }
    private int isCollideX()
    {
        if (currPosition.x <= bird.GetFieldLeft()) return 1;//left edge
        if (currPosition.x >= bird.GetFieldRight()) return 2; //right edge

        int xMoving = (int) (speedValueNext * sin(speedAngleNext) * 10);
        //Log.i(TAG, "isCollideX: currX="+currPosition.x+ ", movingX="+xMoving + " ("+ SCREEN_SIZE.x+","+SCREEN_SIZE.y+")");
        if (currPosition.x - xMoving <= bird.GetFieldLeft()) return 1;//left edge
        if (currPosition.x + xMoving >= bird.GetFieldRight()) return 2; //right edge
        return 0;
    }
    private int isCollideY()
    {
        if (currPosition.y <= bird.GetFieldTop()) return 3; //top edge
        if (currPosition.y >= bird.GetFieldBottom()) return 4;//bottom edge

        int yMoving = (int) (speedValueNext * cos(speedAngleNext) * 10);
        //Log.i(TAG, "isCollideY: currY="+currPosition.y+ ", movingY="+yMoving+ " ("+ SCREEN_SIZE.x+","+SCREEN_SIZE.y+")");
        if (currPosition.y - yMoving <= bird.GetFieldTop()) return 3; //top edge
        if (currPosition.y + yMoving >= bird.GetFieldBottom()) return 4;//bottom edge
        return 0;
    }
    public SingleBird updatePosition()
    {
        setSpeedNext();

        int offsetX=(int) (speedValueNext*cos(Math.toRadians(speedAngleNext)));
        int offsetY = (int) (speedValueNext*sin(Math.toRadians(speedAngleNext)));
        currPosition.x += offsetX;
        currPosition.y += offsetY;

        if (bird.id < FlyingPaths.alPaths.size())
            Log.i(TAG, "updatePosition bird: "+ bird.id +" speed: "+speedValueNext + "@" + speedAngleNext
                + " position: " + currPosition.x + ","+currPosition.y + " ("+offsetX + "," + offsetY +")"
                + " field:"+bird.GetFieldInfo()) ;
        return updateBird();
    }
    public SingleBird updateBird()
    {
        bird.currPosition = currPosition;
        bird.speedValueCurr = speedValueCurr;
        bird.speedAngleCurr = speedAngleCurr;
        bird.speedValueNext = speedValueNext;
        bird.speedAngleNext = speedAngleNext;
        return bird;
    }
}
