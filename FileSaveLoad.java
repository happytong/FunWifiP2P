package com.tongs.funpatternwifi;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.Toast;

import com.tongs.view.DrawView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.tongs.view.DrawView;

public class FileSaveLoad {
    static private String TAG="[dbg]FileSaveLoad";
    Context context;
    File fullpath;
    static String filename="pattern.dat";
    boolean bFileExist= false;
    static public boolean IsDataExist(Context context)
    {
        String path=context.getFilesDir().getAbsolutePath()+"/"+filename;
        File file = new File ( path );
        return file.exists();
    }
    public FileSaveLoad(Context context, boolean read)
    {
        this.context = context;
        if (read) ReadFile();
        else  WriteToFile();
    }
    public void WriteToFile() {
        if (DrawView.alTouchPoints.size() < 10) return;

        DataOutputStream dos= null;
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            dos = new DataOutputStream(fos);

            //Toast.makeText(context, "save="+fullpath, Toast.LENGTH_SHORT).show();
            dos.writeInt(ConfigActivity.cfgTeleport ? 1:0);

            int n=0;
            for (CustomPoint point : DrawView.alTouchPoints){
                dos.writeInt(point.x);
                dos.writeInt(point.y);
                Log.i(TAG, "save "+point);
                dos.writeInt( DrawView.alDrawColor.get(n));
                dos.writeInt(DrawView.alDrawThickness.get(n));
                n++;
            }

            dos.close();
            Toast.makeText(context, context.getResources().getString(R.string.text_pattern_saved), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            throw new RuntimeException("error writing new file", e);
        }

    }
    public void ReadFile() {
        FileInputStream fis = null;
        DataInputStream dis= null;
        try{
            //Toast.makeText(context, "load="+fullpath, Toast.LENGTH_SHORT).show();
            fis = context.openFileInput(filename);
            dis = new DataInputStream(fis);
            int i = dis.readInt();
            ConfigActivity.cfgTeleport = i > 0 ? true : false;
            while (dis.available() > 0)
            {
                int x = dis.readInt();
                if (dis.available() > 0)
                {
                    int y = dis.readInt();
                    DrawView.alTouchPoints.add(new CustomPoint(x,y));

                    //set default
                    DrawView.alDrawColor.add(dis.readInt());
                    DrawView.alDrawThickness.add(dis.readInt());
                    Log.i(TAG, "load "+x +"," +y);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("error reading file", e);
        }

    }
}
