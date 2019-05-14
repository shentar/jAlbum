package com.jalbum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

public class Settings extends PopupWindow
{
    public static final int IMAGE_VIEW_POPUPWINDOW = 5;
    private Context context;
    private View settings = null;

    public Settings(Context context, int width, int height)
    {
        super(context);
        this.context = context;
        this.initTab();
        setWidth(width);
        setHeight(height);
        setContentView(this.settings);
        setOutsideTouchable(true);
        setFocusable(true);
    }

    private void initTab()
    {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        this.settings = inflater.inflate(R.layout.settings, null);
    }

    public View getView(int id)
    {
        return this.settings.findViewById(id);
    }
}
