package com.flick.walls;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main extends Activity implements Runnable {

    WebView view;
    volatile String status = "idle";
    String link = "";
    int mode = 1;
    static final String agent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36";
    static final String site = "https://www.ignitewallpaper.com/";
    static final String media = "https://static.wixstatic.com/";

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        view = new WebView(this);
        view.setBackgroundColor(Color.BLACK);
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        view.addJavascriptInterface(this, "Native");
        view.setWebViewClient(new WebViewClient());
        setContentView(view);
        view.loadDataWithBaseURL(site, asset("index.html"), "text/html", "UTF-8", site);
    }

    @Override
    public void onBackPressed() {
        if (view.canGoBack()) {
            view.goBack();
        } else {
            super.onBackPressed();
        }
    }

    String everything(InputStream stream) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count = stream.read(chunk);
        while (count != -1) {
            bytes.write(chunk, 0, count);
            count = stream.read(chunk);
        }
        stream.close();
        return bytes.toString("UTF-8");
    }

    String asset(String file) {
        try {
            return everything(getAssets().open(file));
        } catch (Exception problem) {
            return "";
        }
    }

    HttpURLConnection open(String address) throws Exception {
        URL target = new URL(address);
        HttpURLConnection line = (HttpURLConnection) target.openConnection();
        line.setConnectTimeout(15000);
        line.setReadTimeout(30000);
        line.setRequestProperty("User-Agent", agent);
        return line;
    }

    @JavascriptInterface
    public String read(String key) {
        SharedPreferences box = getSharedPreferences("flick", MODE_PRIVATE);
        return box.getString(key, "");
    }

    @JavascriptInterface
    public void write(String key, String value) {
        SharedPreferences box = getSharedPreferences("flick", MODE_PRIVATE);
        box.edit().putString(key, value).apply();
    }

    @JavascriptInterface
    public String page(String address) {
        if (!address.startsWith(site)) {
            return "";
        }
        try {
            return everything(open(address).getInputStream());
        } catch (Exception problem) {
            return "";
        }
    }

    @JavascriptInterface
    public void paint(String address, int which) {
        if (status.equals("working")) {
            return;
        }
        if (!address.startsWith(media)) {
            status = "fail";
            return;
        }
        link = address;
        mode = which;
        status = "working";
        new Thread(this).start();
    }

    @JavascriptInterface
    public String state() {
        return status;
    }

    @Override
    public void run() {
        try {
            InputStream stream = open(link).getInputStream();
            WallpaperManager manager = WallpaperManager.getInstance(this);
            manager.setStream(stream, null, true, mode);
            stream.close();
            status = "done";
        } catch (Throwable problem) {
            status = "fail";
        }
    }
}
