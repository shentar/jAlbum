package com.jalbum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class JalbumWebView extends WebView {
    private float downX = 0;

    private float downY = 0;

    public JalbumWebView(Context context) {
        super(context);
    }

    public JalbumWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public JalbumWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public JalbumWebView(Context context, AttributeSet attrs, int defStyleAttr,
                         boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        downX = (int) event.getX();
        downY = (int) event.getY();
        return super.onTouchEvent(event);
    }

    public float getDownX() {
        return downX;
    }

    public void setDownX(float downX) {
        this.downX = downX;
    }

    public float getDownY() {
        return downY;
    }

    public void setDownY(float downY) {
        this.downY = downY;
    }
}
