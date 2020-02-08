package com.jalbum;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.TextView;

public class JAlbumLongClickListener implements View.OnLongClickListener {
    private JalbumWebView view = null;
    private Context context = null;

    public JAlbumLongClickListener(JalbumWebView webView, MainActivity mainActivity) {
        super();
        this.view = webView;
        this.context = mainActivity;
    }

    @Override
    public boolean onLongClick(View v) {
        if (!(v instanceof WebView)) {
            return true;
        }

        WebView.HitTestResult result = ((WebView) v).getHitTestResult();
        int type = result.getType();
        if (WebView.HitTestResult.IMAGE_TYPE != type) {
            return true;
        }
        final String imgurl = result.getExtra();
        final PopWindow itemLongClickedPopWindow =
                new PopWindow(context, PopWindow.IMAGE_VIEW_POPUPWINDOW,
                        DisplayUtils.dip2px(context, 120), DisplayUtils.dip2px(context, 40));
        itemLongClickedPopWindow
                .showAtLocation(v, Gravity.START | Gravity.TOP, (int) view.getDownX(),
                        (int) view.getDownY());
        itemLongClickedPopWindow.getView(R.id.item_longclicked_saveImage)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!(v instanceof TextView)) {
                            return;
                        }

                        itemLongClickedPopWindow.dismiss();
                        new SaveImage(context, imgurl,
                                CookieManager.getInstance().getCookie(view.getUrl())).execute();
                    }
                });

        return true;
    }
}
