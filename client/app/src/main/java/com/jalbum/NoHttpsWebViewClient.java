package com.jalbum;

import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class NoHttpsWebViewClient extends WebViewClient {
    public NoHttpsWebViewClient() {
        super();
    }

    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed();
    }
}
