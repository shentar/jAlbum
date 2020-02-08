package com.jalbum;

import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.PopupWindow;
import org.apache.commons.lang3.StringUtils;

public class Settings extends PopupWindow {
    public static final int IMAGE_VIEW_POPUPWINDOW = 5;
    private MainActivity context;
    private View settings = null;

    public Settings(MainActivity context, int width, int height) {
        super(context);
        this.context = context;
        this.initTab();
        setWidth(width);
        setHeight(height);
        setContentView(this.settings);
        setOutsideTouchable(true);
        setFocusable(true);
    }

    private void initTab() {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        this.settings = inflater.inflate(R.layout.settings, null);
    }

    public View getView(int id) {
        return this.settings.findViewById(id);
    }

    public void loadStatistics(View v) {
        loadReq(v, "statistics");
    }

    private void loadReq(View v, String act) {
        if (v instanceof WebView) {
            String url = getUrl();
            if (url == null) {
                return;
            }
            ((WebView) v).loadUrl(url + "/" + act);
        }
    }

    public void loadStatistics() {
        loadReq("statistics");
    }

    public void syncNow() {
        loadReq("syncnow");
    }


    public void flushNow() {
        loadReq("flushnow");
    }

    private synchronized void loadReq(String act) {
        String url = getUrl();
        if (url == null) {
            return;
        }
        WebView view = (WebView) getView(R.id.statistics);
        view.loadUrl(url + "/" + act);
    }


    private String getUrl() {
        String url = context.getBasedUrl();
        if (StringUtils.isBlank(url) || StringUtils.equalsIgnoreCase(url, MainActivity.DEFAULT_URL)) {
            return null;
        }
        return url;
    }


}
