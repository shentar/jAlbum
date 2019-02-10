package com.jalbum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.*;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{

    private WebView webView = null;
    private int downX = 0;
    private int downY = 0;
    private String cookies = null;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        webView = new WebView(this)
        {
            @Override
            public boolean onTouchEvent(MotionEvent event)
            {
                downX = (int) event.getX();
                downY = (int) event.getY();
                return super.onTouchEvent(event);
            }
        };

        setContentView(webView);

        CookieManager.getInstance().setAcceptCookie(true);
        webView.loadUrl("http://photo.codefine.site:2148");
        WebSettings webSettings = webView.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);

        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUserAgentString("jAlbum_android_apk_client");

        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                view.loadUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }

            public void onPageFinished(WebView view, String url)
            {
                CookieManager cookieManager = CookieManager.getInstance();
                cookies = cookieManager.getCookie(url);
                super.onPageFinished(view, url);
            }
        });

        webView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {

                if (v instanceof WebView)
                {
                    WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                    int type = result.getType();
                    if (WebView.HitTestResult.IMAGE_TYPE == type)
                    {
                        final String imgurl = result.getExtra();
                        final ItemLongClickedPopWindow itemLongClickedPopWindow =
                                new ItemLongClickedPopWindow(MainActivity.this,
                                                             ItemLongClickedPopWindow.IMAGE_VIEW_POPUPWINDOW,
                                                             dip2px(MainActivity.this, 120),
                                                             dip2px(MainActivity.this, 40));
                        itemLongClickedPopWindow
                                .showAtLocation(v, Gravity.LEFT | Gravity.TOP, downX, downY);
                        itemLongClickedPopWindow.getView(R.id.item_longclicked_saveImage)
                                .setOnClickListener(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        if (v instanceof TextView)
                                        {
                                            itemLongClickedPopWindow.dismiss();
                                            new SaveImage(MainActivity.this, imgurl, cookies)
                                                    .execute();
                                        }
                                    }
                                });

                    }
                }
                return true;
            }
        });
    }

    public void onBackPressed()
    {
        if (webView != null && webView.canGoBack())
        {
            webView.goBack();
        }
        else
        {
            super.onBackPressed();
        }
    }

    protected void onDestroy()
    {

        if (webView != null)
        {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(false);
            webView.loadUrl("about:blank");
            webView.pauseTimers();
            webView = null;
        }

        super.onDestroy();
    }

    private static int dip2px(Context context, float dpValue)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
