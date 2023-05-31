package com.tongs.funpatternwifi;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

public class FlyingPaths {
    //BREAKPOINT is a flog of end of a line, not showing up in the path, to allow multiple lines of a path
    static public CustomPoint BREAKPOINT = new CustomPoint(-500, -500);//different device may have different screen size //MainActivity.ScreenSize.x+100, MainActivity.ScreenSize.y+100);

    static private String TAG="[dbg]FlyingPaths";
    static public ArrayList<CustomPoint> alBasePath = new ArrayList<>();
    static public ArrayList<ArrayList<CustomPoint>> alPaths = new ArrayList<ArrayList<CustomPoint>>(); //2d for multiple paths

    static public ArrayList<Integer> alPosition = new ArrayList<Integer>(); //current position for the each path
    static public void SetBasePath(ArrayList<CustomPoint> points)
    {
        alBasePath.clear();
        if (ConfigActivity.cfgTeleport)
        {
            SetBasePathTeleport(points);
            return;
        }

        //copy and refill the points
        int nIndex = 0;
        for (int i=0; i<points.size()-1; i++)
        {
            CustomPoint p1 = new CustomPoint(points.get(i));
            alBasePath.add(p1);
            Log.i(TAG, "SetBasePath refill ["+i+"@"+ (alBasePath.size()-1) +": p1="+p1);
            CustomPoint p2 = points.get(i+1);
            int offsetX =p2.x - p1.x;
            int offsetY =p2.y - p1.y;
            int timeX = Math.abs(offsetX/20);
            int timeY = Math.abs(offsetY/20);
            if (timeX > 0 || timeY > 0)
            {
                int times = Math.max(timeX, timeY);
                int stepX = offsetX/times;
                int stepY = offsetY/times;
                while (--times >0)
                {
                    CustomPoint point = new CustomPoint(p1.x+stepX, p1.y+stepY);
                    alBasePath.add(point);
                    Log.i(TAG, "SetBasePath refill insert "+times+" @"+(alBasePath.size()-1)+point);
                }
            }
            //Log.i(TAG, "AddPathPoints path="+nLastPathNum + ", refill "+i+"]: p2="+p2);
        }
        alBasePath.add(new CustomPoint(points.get(points.size()-1)));
        Log.i(TAG, "SetBasePath done, size="+alBasePath.size() + " last=" +points.get(points.size()-1));
    }
    static public void SetBasePathTeleport(ArrayList<CustomPoint> points)
    {
        alBasePath.clear();

        //copy and refill the points if not BREAKPOINT
        int nIndex = 0;
        for (int i=0; i<points.size()-1; i++)
        {
            CustomPoint p1 = new CustomPoint(points.get(i));
            alBasePath.add(p1);
            if (p1.equals(BREAKPOINT)) continue;

            Log.i(TAG, "SetBasePathTeleport refill ["+i+"@"+ (alBasePath.size()-1) +": p1="+p1);
            CustomPoint p2 = points.get(i+1);
            if (p2.equals(BREAKPOINT)) {
                alBasePath.add(p2);
                continue;
            }

            int offsetX =p2.x - p1.x;
            int offsetY =p2.y - p1.y;
            int timeX = Math.abs(offsetX/20);
            int timeY = Math.abs(offsetY/20);
            if (timeX > 0 || timeY > 0)
            {
                int times = Math.max(timeX, timeY);
                int stepX = offsetX/times;
                int stepY = offsetY/times;
                while (--times >0)
                {
                    CustomPoint point = new CustomPoint(p1.x+stepX, p1.y+stepY);
                    alBasePath.add(point);
                    Log.i(TAG, "SetBasePathTeleport refill insert "+times+" @"+(alBasePath.size()-1)+point);
                }
            }
            //Log.i(TAG, "AddPathPoints path="+nLastPathNum + ", refill "+i+"]: p2="+p2);
        }
        alBasePath.add(new CustomPoint(points.get(points.size()-1)));
        Log.i(TAG, "SetBasePathTeleport done, size="+alBasePath.size() + " last=" +points.get(points.size()-1));
    }
    static private ArrayList<CustomPoint> GeneratePath(ArrayList<CustomPoint> oriPoints, int nSimilarity)
    {
        int nOriSize = oriPoints.size();

        ArrayList<CustomPoint> path = new ArrayList<>();

        int nNewSize = nOriSize; //for difficult case
        float fRate = 0.7f; //difficult case, similarity = 70%
        switch (nSimilarity)
        {
            case 0: //easy
                nNewSize = (int)(1.5f*nOriSize);
                fRate = 0.3f;
                break;
            case 1: //medium
                nNewSize = (int)(1.2f*nOriSize);
                fRate = 0.5f;
                break;
            default: //2 difficult
                break;
        }
        //get first (random) and last from orignal path
        Random rand = new Random();
        int nStart = rand.nextInt(nOriSize-1);
        CustomPoint point1=GetPrevNonBreakpoint(oriPoints, nStart);
        int nEnd = nStart + (int)(nOriSize*fRate);
        Log.i(TAG, "GeneratePath to copy from original "+(nEnd-nStart)+" points, start="+nStart + ", end="+nEnd + ", size ori="+nOriSize + " new="+nNewSize);
        //copy this porting from original
        if ( nEnd >= nOriSize) {
            nEnd -= nOriSize;
            for (int n=nStart; n<nOriSize; n++)
            {
                path.add(new CustomPoint(oriPoints.get(n)));
            }
            for (int n=0; n<=nEnd; n++)
            {
                path.add(new CustomPoint(oriPoints.get(n)));
            }
        }
        else
        {
            for (int n=nStart; n<=nEnd; n++)
            {
                path.add(new CustomPoint(oriPoints.get(n)));
            }
        }
        CustomPoint point2 = new CustomPoint(oriPoints.get(nEnd));
        path.add(new CustomPoint(point2)); //20220609 new Point
        int count =path.size();
        Log.i(TAG, "GeneratePath copy points from p1 "+oriPoints.get(nStart)+" - " +path.get(path.size()-1) +"=p2 "+point2 +" size="+count);

        //fill in the remaining portion with some random pattern
        point2 = GetPrevNonBreakpoint(oriPoints, nEnd);
        if (point2.equals(BREAKPOINT)) {
            Log.i(TAG, "GeneratePath stop! after copied points from p1 "+oriPoints.get(nStart)+" - " +path.get(path.size()-1) +"=p2 "+point2 +" size="+count);
            return path; //should never reach here
        }

        //first half random increase, with limit of viewing area
        Log.i(TAG, "GeneratePath size="+ alPaths.size() + ", screen size= "+MainActivity.ScreenSize);
        SingleBird bird = new SingleBird(alPaths.size(), MainActivity.ScreenSize, FreeFlying.MAX_SPEED);
        bird.currPosition = point2;
        int nHalf = (nNewSize-count)/2;
        Log.i(TAG, "GeneratePath "+bird.id + " fill 1st half "+nHalf + " from="+point2 + " count="+count);
        for (int n=0; n<nHalf; n++)
        {
            FreeFlying adj = new FreeFlying(bird); //let the bird fly
            bird = adj.updatePosition();
            path.add(new CustomPoint(bird.currPosition));
            count++;
            Log.i(TAG, "GeneratePath, fill 1st half "+nHalf + " count="+count+ ":"+ bird.currPosition);
        }
        //2nd half get closer to point1
        int nOffsetx = (bird.currPosition.x - point1.x)/nHalf;
        int nOffsety = (bird.currPosition.y - point1.y)/nHalf;
        Log.i(TAG, "GeneratePath fill 2nd half "+nHalf + " from="+bird.currPosition + " to p1 "+ point1+", offset "+nOffsetx + ","+nOffsety);
        point2 = new CustomPoint(bird.currPosition);
        for (int n=0; n<nHalf; n++)
        {
            FreeFlying adj = new FreeFlying(bird);
            //reduce the range
            //finetune: make the range as a square to maximize the field
            if (Math.abs(nOffsetx) > Math.abs(nOffsety)) {
                point2.x -= nOffsetx;
                point2.y = point1.y + point1.x - point2.x;
            }
            else
            {
                point2.y -= nOffsety;
                point2.x = point1.x + point1.y - point2.y;
            }
            //limit to screen size
            if (point2.x < 0) point2.x = 0;
            if (point2.x > MainActivity.ScreenSize.x ) point2.x = MainActivity.ScreenSize.x;
            if (point2.y < 0) point2.y = 0;
            if (point2.y > MainActivity.ScreenSize.y ) point2.y = MainActivity.ScreenSize.y;

            bird.SetField(point2, point1);
            bird = adj.updatePosition();
            path.add(new CustomPoint(bird.currPosition));
            count++;
            //Log.i(TAG, "GeneratePath fill 2nd half "+nHalf + " count="+count+ ":"+ bird.currPosition + " field=" +point2 +","+point1);
        }
        Log.i(TAG, "GeneratePath, filled, count="+count+ "/"+nNewSize + ": last="+ bird.currPosition);
        return path;

    }
    static private ArrayList<CustomPoint> GeneratePathRotate(ArrayList<CustomPoint> oriPoints)
    {
        //rotate 45 degree counterclockwise around the center of the screen
        ArrayList<CustomPoint> path = new ArrayList<>();
        int n=0; //for logging
        int Xcenter= MainActivity.ScreenSize.x/2;
        int Ycenter= MainActivity.ScreenSize.y/2;

        Random random=new Random();
        int degree = random.nextInt(40 ) + 30; //rotate 30 - 70 degree
        double rad = degree * Math.PI / 180;

        for (CustomPoint point:oriPoints) {
            CustomPoint p = new CustomPoint(point.x, point.y);
            if (point.x == BREAKPOINT.x) p.x = point.x;
            else {
                p.x = (int)((point.x - Xcenter) * Math.cos(rad) + (point.y - Ycenter) * Math.sin(rad) + Xcenter);
                p.y = (int)((Xcenter - point.x) * Math.sin(rad) + (point.y - Ycenter) * Math.cos(rad) + Ycenter);

                if (p.y < 0 ) p.y = - p.y; //fold back
                if (p.y > MainActivity.ScreenSize.y ) p.y = MainActivity.ScreenSize.y * 2 - p.y; //fold back
            }

            Log.i(TAG, "Rotate " +(++n)+ ": " + point + " -> " + p + ", degree "+ degree);
            path.add(p);
        }
        return path;

    }
    static private ArrayList<CustomPoint> GeneratePathZoom(ArrayList<CustomPoint> oriPoints)
    {
        //zoom in
        ArrayList<CustomPoint> path = new ArrayList<>();
        int n=0; //for logging
        int Xcenter= MainActivity.ScreenSize.x/2;
        int Ycenter= MainActivity.ScreenSize.y/2;
        Random random=new Random();
        int change = random.nextInt(4 )+1;
        int offset = 1;
        //CustomPoint prev = new CustomPoint(Xcenter, Ycenter);
        for (CustomPoint point:oriPoints) {
            CustomPoint p = new CustomPoint(point.x, point.y);

            if (point.x == BREAKPOINT.x) p.x = point.x;
            else {
                offset += change;
                if (offset > Ycenter) offset /= 20;
                if (p.x < Xcenter) p.x -= offset;
                else p.x += offset;
                if (p.y < Ycenter) p.y -= offset;
                else p.y += offset;

                if (p.y < 0 ) p.y = - p.y; //fold back
                if (p.y > MainActivity.ScreenSize.y ) p.y = MainActivity.ScreenSize.y * 2 - p.y; //fold back
                if (p.x < 0 ) p.x = - p.x;
                if (p.x > MainActivity.ScreenSize.x ) p.x = MainActivity.ScreenSize.x * 2 - p.x; //fold back
            }

            Log.i(TAG, "LeftZoom " +(++n)+ ": " + point + " -> " + p + ", change "+change + ", offset "+offset);
            path.add(p);
        }
        return path;

    }
    static private ArrayList<CustomPoint> GeneratePathYX(ArrayList<CustomPoint> oriPoints)
    {
        //swap X - Y
        ArrayList<CustomPoint> path = new ArrayList<>();
        for (CustomPoint point:oriPoints) {
            CustomPoint p = new CustomPoint(point.y, point.x);
            if (point.y > MainActivity.ScreenSize.x ) p.x = (int)(MainActivity.ScreenSize.x * 2.5) - point.y; //fold back on right half
            if (point.x > MainActivity.ScreenSize.y ) p.y = MainActivity.ScreenSize.y * 2 - point.x; //fold back
            path.add(p);
        }
        return path;

    }
    static private ArrayList<CustomPoint> GeneratePathMoveX(ArrayList<CustomPoint> oriPoints)
    {
        //keep same Y, but X moved 1/3 of the screen
        int nOriSize = oriPoints.size();
        int offset=MainActivity.ScreenSize.x/3;

        ArrayList<CustomPoint> path = new ArrayList<>();
        boolean bBreakEmerge = false; //when exceed the screen, add a breakpoint first time
        boolean bBreakNotEmerge = true; //from exceed screen to non-exceed screen, add a breakpoint
        for (CustomPoint point:oriPoints) {
            CustomPoint p = new CustomPoint(point.x+offset, point.y);
            if (point.x == BREAKPOINT.x) p.x = point.x;
            else{
                if (p.x > MainActivity.ScreenSize.x ){
                    bBreakNotEmerge = false;
                    if (!bBreakEmerge) path.add(new CustomPoint(BREAKPOINT.x, BREAKPOINT.y));
                    bBreakEmerge = true;
                    p.x -= MainActivity.ScreenSize.x; //emerge left
                }
                else
                {
                    bBreakEmerge = false;

                    if (!bBreakNotEmerge) path.add(new CustomPoint(BREAKPOINT.x, BREAKPOINT.y));
                    bBreakNotEmerge = true;
                }
            }
            path.add(p);
        }
        return path;
    }
    static private ArrayList<CustomPoint> GeneratePathMoveY(ArrayList<CustomPoint> oriPoints)
    {
        //keep same X, but Y moved 1/3 of the screen
        int offset=MainActivity.ScreenSize.y/3;

        ArrayList<CustomPoint> path = new ArrayList<>();
        boolean bBreakEmerge = false; //when exceed the screen, add a breakpoint first time
        boolean bBreakNotEmerge = true; //from exceed screen to non-exceed screen, add a breakpoint
        for (CustomPoint point:oriPoints) {
            CustomPoint p = new CustomPoint(point.x, point.y+offset);
            if (point.y == BREAKPOINT.y) p.y = point.y;
            else{
                if (p.y > MainActivity.ScreenSize.y ){
                    bBreakNotEmerge = false;
                    if (!bBreakEmerge) path.add(new CustomPoint(BREAKPOINT.x, BREAKPOINT.y));
                    bBreakEmerge = true;
                    p.y -= MainActivity.ScreenSize.y; //emerge top
                }
                else
                {
                    bBreakEmerge = false;

                    if (!bBreakNotEmerge) path.add(new CustomPoint(BREAKPOINT.x, BREAKPOINT.y));
                    bBreakNotEmerge = true;
                }
            }
            path.add(p);
        }
        return path;

    }
    static private CustomPoint GetPrevNonBreakpoint(ArrayList<CustomPoint> points, int start)
    {
        if (!ConfigActivity.cfgTeleport) return new CustomPoint(points.get(start));
        if (!points.get(start).equals(BREAKPOINT)) return new CustomPoint(points.get(start));

        //should skip BREAKPOINT by getting previous point
        while (start > 0)
        {
            if (!points.get(start-1).equals(BREAKPOINT))
            {
                return new CustomPoint(points.get(start-1));
            }
            start --;
        }
        //not found, search from tail
        start = points.size();
        while (start > 0)
        {
            if (!points.get(start-1).equals(BREAKPOINT))
            {
                return new CustomPoint(points.get(start-1));
            }
            start--;
        }
        //not found at all
        return BREAKPOINT;
    }

    static public void AddTestPaths(int nTotal, int difficulty)
    {
        alPaths.clear();
        for (int i=0; i<nTotal; i++)
        {
            //ArrayList<Point> path=new ArrayList<>();
            Log.i(TAG, "AddTestPaths "+i + "/"+nTotal);
            ArrayList<CustomPoint> path = GeneratePath(alBasePath, difficulty);
            alPaths.add(path);
            alPosition.add(0);
            Log.i(TAG, "AddTestPaths done for "+i);
        }
    }
    static public void RegeneratePath(int nIndex)
    {
        if (nIndex >= alPaths.size()) return;

        Random random=new Random();
        int n = random.nextInt(10 );
        Log.i(TAG, "RegeneratePath "+ nIndex + ": "+n);
        ArrayList<CustomPoint> path;
        switch (n)
        {
            case 0:
                path = GeneratePathYX(alBasePath);
                break;
            case 1:
                path = GeneratePathMoveX(alBasePath);
                break;
            case 2:
                path = GeneratePathZoom(alBasePath);
                break;
            case 3:
                path = GeneratePathMoveY(alBasePath);
                break;
            case 4:
            case 5:
                path = GeneratePathRotate(alBasePath);
                break;
            default:

                path = GeneratePath(alBasePath, ConfigActivity.cfgDifficulty);
                break;
        }
        alPaths.set(nIndex, path);
    }
    static public CustomPoint GetNextPosition(int nPath)
    {
        int n = alPosition.get(nPath);
        if ( n < alPaths.get(nPath).size()-1 ) n++;
        else n=0;
        Log.i(TAG, "GetNextPosition path="+nPath + ": "+n + "/"+alPaths.get(nPath).size());
        alPosition.set(nPath, n);
        return alPaths.get(nPath).get(n);
    }
    static public void SetStartingPosition()
    {
        int n=0;
        for (ArrayList<CustomPoint> path:alPaths
             ) {
            Random random=new Random();
            alPosition.set(n, random.nextInt(path.size()-1) );
        }
    }
    static public String GetInfo()
    {
        String info = " basePath "+alBasePath.size() + ":";
        for ( int n =0; n<alPaths.size(); n++)
            info += " " + alPaths.get(n).size();
        return info;
    }
    static public void NormalizePaths()
    {
        //this is to adjust the points so can fit in different screen size (among the devices)
        if (alPaths.size() == 0) return;

        //get the max rectangular of the points
        int maxX = 0, maxY=0;
        for (ArrayList<CustomPoint> path : alPaths)
        {
            for (CustomPoint point:path)
            {
                if (maxX < point.x) maxX = point.x;
                if (maxY < point.y) maxY = point.y;
            }
        }

        //get the ratio to screen size
        float rateX = (float) maxX/MainActivity.ScreenSize.x;
        float rateY = (float) maxY/MainActivity.ScreenSize.y;
        //float rate = rateX < rateY ? rateY:rateX; //get smaller rate

        //convert all paths
        for (ArrayList<CustomPoint> path : alPaths)
        {
            for (CustomPoint point:path)
            {
                if (point.x == BREAKPOINT.x) continue;

                int x = (int)(point.x / rateX);
                int y = (int)(point.y / rateY);
                Log.i (TAG, "NormalizePaths: " + point + " -> ("+ x +", " + y +"), rate=" + rateX + ","+rateY + "; screen " +MainActivity.ScreenSize);
                point.setX(x);
                point.setY(y);
                Log.i (TAG, "NormalizePaths: updated " + point );
            }
        }

    }
}
