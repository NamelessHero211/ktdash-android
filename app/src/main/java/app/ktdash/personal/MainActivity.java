package app.ktdash.personal;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.*;
import android.widget.*;

public class MainActivity extends Activity {
    private static final String HOME = "https://ktdash.app/";
    private WebView web;
    private ProgressBar progress;
    private LinearLayout error;
    private ValueCallback<Uri[]> files;
    private String retryUrl = HOME;
    private boolean failed;
    private final AppUpdates updates = new AppUpdates();

    private boolean trusted(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) &&
            ("ktdash.app".equalsIgnoreCase(uri.getHost()) || "www.ktdash.app".equalsIgnoreCase(uri.getHost()));
    }
    private void external(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("https") || scheme.equals("http") || scheme.equals("mailto") || scheme.equals("tel"))) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show(); }
    }
    private Button button(String label, LinearLayout row, View.OnClickListener action) {
        Button b = new Button(this); b.setText(label); b.setOnClickListener(action);
        row.addView(b, new LinearLayout.LayoutParams(0, -2, 1)); return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(23,34,44));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        LinearLayout bar = new LinearLayout(this);
        button("Home", bar, v -> web.loadUrl(HOME));
        button("Reload", bar, v -> { if (failed) web.loadUrl(retryUrl); else web.reload(); });
        button("Browser", bar, v -> external(Uri.parse(web.getUrl() == null ? HOME : web.getUrl())));
        button("Updates", bar, v -> updates.check(this, true));
        root.addView(bar);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, 5));
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this); frame.addView(web, new android.widget.FrameLayout.LayoutParams(-1,-1));
        error = new LinearLayout(this); error.setOrientation(LinearLayout.VERTICAL);
        error.setGravity(android.view.Gravity.CENTER); error.setPadding(32,32,32,32);
        error.setBackgroundColor(Color.rgb(23,34,44));
        TextView message = new TextView(this); message.setText("KTDash could not load.\nCheck your connection and try again.");
        message.setTextColor(Color.WHITE); message.setTextSize(18); message.setGravity(17); error.addView(message);
        Button retry = new Button(this); retry.setText("Try again"); retry.setOnClickListener(v -> web.loadUrl(retryUrl)); error.addView(retry);
        frame.addView(error, new android.widget.FrameLayout.LayoutParams(-1,-1)); error.setVisibility(View.GONE);
        setContentView(root); root.requestApplyInsets();
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(true); settings.setBuiltInZoomControls(true); settings.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                if (trusted(request.getUrl())) return false;
                external(request.getUrl()); return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                failed = false; if (trusted(Uri.parse(url))) retryUrl = url;
                error.setVisibility(View.GONE); progress.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE); CookieManager.getInstance().flush();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError detail) {
                if (request.isForMainFrame()) showError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) showError();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) { progress.setProgress(value); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (files != null) files.onReceiveValue(null); files = callback;
                try { startActivityForResult(params.createIntent(), 10); }
                catch (ActivityNotFoundException e) { files.onReceiveValue(null); files = null; Toast.makeText(MainActivity.this,"No file picker available",0).show(); }
                return true;
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, size) -> {
            Uri uri = Uri.parse(url);
            if (!trusted(uri)) { external(uri); return; }
            try {
                String name = URLUtil.guessFileName(url, disposition, mime).replaceAll("[^a-zA-Z0-9._-]", "_");
                DownloadManager.Request request = new DownloadManager.Request(uri);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.addRequestHeader("User-Agent", agent);
                request.setTitle(name); if (mime != null) request.setMimeType(mime);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, System.currentTimeMillis()+"-"+name);
                ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this,"Download started — open it from the notification",0).show();
            } catch (Exception e) { Toast.makeText(this,"Download unavailable. Try the Browser button.",1).show(); }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
    }
    private void showError() { failed = true; progress.setVisibility(View.GONE); error.setVisibility(View.VISIBLE); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request == 10 && files != null) { files.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data)); files=null; }
    }
    @Override protected void onSaveInstanceState(Bundle out) { web.saveState(out); super.onSaveInstanceState(out); }
    @Override protected void onPause() { web.onPause(); CookieManager.getInstance().flush(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); updates.check(this, false); }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if (files != null) files.onReceiveValue(null); web.destroy(); super.onDestroy(); }
}
