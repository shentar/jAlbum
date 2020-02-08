package com.jalbum;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/***
 * 功能：用线程保存图片
 *
 * @author wangyp
 */
public class SaveImage extends AsyncTask<String, Void, String> {
    private static final String TMP_EXT_STR = "._bak";
    private String cookies;
    private String imgurl = "";
    private Context context;

    public SaveImage(Context context, String imgurl, String cookies) {
        this.context = context;
        this.imgurl = imgurl;
        this.cookies = cookies;
    }

    @Override
    protected String doInBackground(String... params) {
        String result = "";
        try {
            String sdcard = Environment.getExternalStorageDirectory().toString();
            File file = new File(sdcard + "/DCIM/Download/");
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    result = "保存失败，无法创建【" + sdcard + "/DCIM/Download/" + "】目录。";
                    return result;
                }
            }

            URL url = new URL(imgurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setRequestProperty("Cookie", cookies);

            if (conn.getResponseCode() == 200) {
                InputStream inputStream = new BufferedInputStream(conn.getInputStream());
                byte[] buffer = new byte[4096];
                int len = 0;

                String filename = getTmpFileName(conn);

                file = new File(sdcard + "/DCIM/Download/" + filename);

                BufferedOutputStream outStream =
                        new BufferedOutputStream(new FileOutputStream(file));
                while ((len = inputStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                outStream.close();

                if (StringUtils.endsWith(filename, TMP_EXT_STR)) {
                    String finalFilename = StringUtils
                            .substring(filename, 0, filename.length() - TMP_EXT_STR.length());
                    File finalFile = new File(sdcard + "/DCIM/Download/" + finalFilename);
                    if (!file.renameTo(finalFile)) {
                        result = "保存失败：文件改名失败。";
                        return result;
                    } else {
                        result = "图片已保存至：\n" + finalFile.getAbsolutePath();
                        return result;
                    }
                }

                result = "图片已保存至：\n" + file.getAbsolutePath();
            }
        } catch (Exception e) {
            result = "保存失败！" + e.getLocalizedMessage();
        }
        return result;
    }

    private String getTmpFileName(HttpURLConnection conn) {
        // Content-Disposition: filename=mmexport1549772790530.jpg
        String disposition = conn.getHeaderField("Content-Disposition");
        String filename = null;
        filename = StringUtils.substringAfter(disposition, "filename=");

        if (StringUtils.isBlank(filename)) {
            filename = new Date().getTime() + "";
        } else {
            if (StringUtils.contains(filename, ";")) {
                filename = StringUtils.substringBefore(filename, ";");
            }

            int offset = StringUtils.indexOf(filename, ".");
            if (offset != StringUtils.INDEX_NOT_FOUND) {
                String ext = StringUtils.substring(filename, offset);
                String name = StringUtils.substring(filename, 0, offset);
                filename = name + "_" + new Date().getTime() + ext + TMP_EXT_STR;
            }
        }

        return filename;
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
    }
}
