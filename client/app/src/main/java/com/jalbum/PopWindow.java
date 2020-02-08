package com.jalbum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

public class PopWindow extends PopupWindow {
    public static final int IMAGE_VIEW_POPUPWINDOW = 5;
    private View itemLongClickedPopWindowView;
    private Context context;
    private int type;

    public PopWindow(Context context, int type, int width, int height) {
        super(context);
        this.context = context;
        this.type = type;
        this.initTab();
        setWidth(width);
        setHeight(height);
        setContentView(this.itemLongClickedPopWindowView);
        setOutsideTouchable(true);
        setFocusable(true);
    }

    private void initTab() {
        LayoutInflater itemLongClickedPopWindowInflater = LayoutInflater.from(this.context);
        if (type == IMAGE_VIEW_POPUPWINDOW) {
            this.itemLongClickedPopWindowView =
                    itemLongClickedPopWindowInflater.inflate(R.layout.longclicked_img, null);
        }
    }

    public View getView(int id) {
        return this.itemLongClickedPopWindowView.findViewById(id);
    }
}