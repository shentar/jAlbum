package com.jalbum;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.apache.commons.lang3.StringUtils;

public class MainActivity extends AppCompatActivity
{
    public static final String DEFAULT_URL = "https://shentar.github.io/2018/02/11/jalbum/";
    private JalbumWebView webView = null;
    private String cookies = null;
    private SharedPreferences preferences = null;
    private Settings settings = null;

    private static int dip2px(Context context, float dpValue)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preferences =
            getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        webView = findViewById(R.id.web_content);
        CookieManager.getInstance().setAcceptCookie(true);
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
                if (!(v instanceof WebView))
                {
                    return true;
                }

                WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                int type = result.getType();
                if (WebView.HitTestResult.IMAGE_TYPE != type)
                {
                    return true;
                }

                final String imgurl = result.getExtra();
                final ItemLongClickedPopWindow itemLongClickedPopWindow =
                    new ItemLongClickedPopWindow(MainActivity.this,
                                                 ItemLongClickedPopWindow.IMAGE_VIEW_POPUPWINDOW,
                                                 dip2px(MainActivity.this, 120),
                                                 dip2px(MainActivity.this, 40));
                itemLongClickedPopWindow
                    .showAtLocation(v, Gravity.START | Gravity.TOP, (int) webView.getDownX(),
                                    (int) webView.getDownY());
                itemLongClickedPopWindow.getView(R.id.item_longclicked_saveImage)
                    .setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (!(v instanceof TextView))
                            {
                                return;
                            }

                            itemLongClickedPopWindow.dismiss();
                            new SaveImage(MainActivity.this, imgurl, cookies).execute();
                        }
                    });

                return true;
            }
        });

        preferences.registerOnSharedPreferenceChangeListener(
            new SharedPreferences.OnSharedPreferenceChangeListener()
            {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s)
                {
                    if (StringUtils.equals(s, "url"))
                    {
                        loadContent();
                    }
                }
            });
        settings = new Settings(MainActivity.this, getScreenWidth(MainActivity.this),
                                getScreenHeight(MainActivity.this) / 2);
        settings.getView(R.id.button_logout_logout).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SharedPreferences.Editor editor = preferences.edit();
                CookieManager.getInstance().removeAllCookies(null);
                editor.putString("url", DEFAULT_URL);
                editor.apply();
                settings.dismiss();
            }
        });

        settings.getView(R.id.button_go).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EditText editText = (EditText) settings.getView(R.id.url_edit_text);

                if (editText.getText() == null)
                {
                    settings.dismiss();
                    return;
                }

                String url = editText.getText().toString();
                if (StringUtils.isBlank(url))
                {
                    settings.dismiss();
                    return;
                }

                url = url.toLowerCase();
                if (!url.startsWith("http://") && !url.startsWith("https://"))
                {
                    url = "http://" + url;
                }

                String oldUrl = preferences.getString("url", DEFAULT_URL);
                if (StringUtils.equalsIgnoreCase(url, oldUrl))
                {
                    settings.dismiss();
                    return;
                }

                SharedPreferences.Editor editor = preferences.edit();

                editor.putString("url", url);
                editor.apply();
                settings.dismiss();
            }
        });

        loadContent();

        Button button = findViewById(R.id.setting_buttion);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (view instanceof Button)
                {
                    if (view.getId() == R.id.setting_buttion)
                    {
                        settings.showAtLocation(view, Gravity.CENTER, 0, 0);
                        EditText editText = (EditText) settings.getView(R.id.url_edit_text);
                        String oldUrl = preferences.getString("url", DEFAULT_URL);
                        editText.setText(oldUrl);
                    }
                }
            }
        });
    }

    private void loadContent()
    {
        webView.loadUrl(preferences.getString("url", "http://photo.codefine.site:2148"));
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
        unsetContent();
        super.onDestroy();
    }

    private void unsetContent()
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
    }


    //获取屏幕的宽度
    public static int getScreenWidth(Context context)
    {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getWidth();
    }

    //获取屏幕的高度
    public static int getScreenHeight(Context context)
    {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }
}
