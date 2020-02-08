package com.jalbum;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

public class JalbumWebChromeClient extends WebChromeClient {
    private final Activity context;
    private final JalbumWebView webView;
    private final FrameLayout mFrameLayout;
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;

    public JalbumWebChromeClient(FrameLayout frameLayout, Activity context, JalbumWebView webView) {
        super();
        this.mFrameLayout = frameLayout;
        this.webView = webView;
        this.context = context;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        mCustomView = view;
        mFrameLayout.addView(mCustomView);
        mCustomViewCallback = callback;
        webView.setVisibility(View.GONE);
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void onHideCustomView() {
        webView.setVisibility(View.VISIBLE);
        if (mCustomView == null) {
            return;
        }
        mCustomView.setVisibility(View.GONE);
        mCustomViewCallback.onCustomViewHidden();
        mCustomView = null;
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onHideCustomView();
    }

}
