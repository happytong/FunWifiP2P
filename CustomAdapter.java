package com.tongs.funpatternwifi;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CustomAdapter extends ArrayAdapter<String> {
    static private String TAG="[dbg]CustomAdapter";
    private final Context mContext;
    private final int mResource;
    private final String[] mData;
    private final List<Integer> mTextColors = new ArrayList<>(); ; // List to store text colors
    private final int mDefaultColor;
    private final int mDefaultTextSize;

    public CustomAdapter(Context context, int resource, String[] data) {
        super(context, resource, data);
        mContext = context;
        mResource = resource;
        mData = data;
        mDefaultColor = mContext.getResources().getColor(android.R.color.black);
        mDefaultTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.text_size_medium);
        for (int i = 0; i < mData.length; i++) {
            mTextColors.add(mDefaultColor);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView " + position);
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(mResource, parent, false);
        }

        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText(mData[position]);
        textView.setTextColor(mTextColors.get(position)); // Set text color from list
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mDefaultTextSize);

        return view;
    }

    public void setText(int position, String text) {
        if (position >= 0 && position < mData.length) {
            mData[position] = text;
            notifyDataSetChanged();
        }
    }

    public void setColor(int position, int color) {
        if (position >= 0 && position < mTextColors.size()) {
            mTextColors.set(position, color); // Update text color in color list
            notifyDataSetChanged();
        }
    }

    public String getItemText(int position) {
        if (position >= 0 && position < mData.length) {
            return mData[position];
        }
        return null;
    }

    public int getItemColor(int position) {
        if (position >= 0 && position < mTextColors.size()) {
            return mTextColors.get(position);
        }
        return mDefaultColor;
    }

    public void updateItem(String content, int color) {
        if (mData == null) return;
        for (int i = 0; i < mData.length; i++) {
            if (mData[i].contains(content)) {
                //mData[i] = mData[i].replace(content, "");
                mTextColors.set(i, color);
            }
        }
        notifyDataSetChanged();
    }
    public void updateItemYesNo(String str1, String str2, int color1, int color2, int color3) {
        if (mData == null) return;
        for (int i = 0; i < mData.length; i++) {
            if (mData[i].contains(str1)) {
                if (mData[i].contains(str2)) mTextColors.set(i, color1);
                else mTextColors.set(i, color2);
            }
            else if (!mData[i].contains(str2))mTextColors.set(i, color3);
        }
        notifyDataSetChanged();
    }
    public void updateItemAppend(String content, int color, String info) {
        if (mData == null) return;
        for (int i = 0; i < mData.length; i++) {
            if (mData[i].contains(content)) {
                mData[i] += info;
                mTextColors.set(i, color);
            }
        }
        notifyDataSetChanged();
    }
}


