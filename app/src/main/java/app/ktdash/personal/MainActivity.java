package app.ktdash.personal;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String HOME = "https://ktdash.app/";
    private WebView web;
    private ProgressBar progress;
    private LinearLayout error;
    private ValueCallback<Uri[]> files;
    private Uri cameraPhotoUri;
    private static final int PHOTO_PICKER_REQUEST = 10;
    private String retryUrl = HOME;
    private boolean failed;
    private final AppUpdates updates = new AppUpdates();
    private static final ExecutorService COOKIE_WRITER = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable checkUpdates = () -> updates.check(this, false);
    private boolean resumed;
    private boolean pageReady;

    private static boolean trusted(Uri uri) {
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
        styleButton(b);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, -2, 1);
        layout.setMargins(dp(3),dp(3),dp(3),dp(3));
        row.addView(b, layout); return b;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void styleButton(Button b) {
        b.setAllCaps(false); b.setTextColor(Color.BLACK); b.setTextSize(14);
        b.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        b.setMinHeight(dp(48)); b.setPadding(dp(12),dp(8),dp(12),dp(8));
        GradientDrawable box = new GradientDrawable(); box.setColor(AppTheme.ORANGE);
        box.setCornerRadius(dp(8)); box.setStroke(dp(2), Color.BLACK);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), box, null));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(3),dp(3),dp(3),dp(3));
        button("Home", bar, v -> { if (!HOME.equals(web.getUrl())) web.loadUrl(HOME); });
        button("Reload", bar, v -> { if (failed) web.loadUrl(retryUrl); else web.reload(); });
        button("Updates", bar, v -> updates.check(this, true));
        root.addView(bar);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(ColorStateList.valueOf(AppTheme.ORANGE));
        root.addView(progress, new LinearLayout.LayoutParams(-1, 5));
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this); frame.addView(web, new android.widget.FrameLayout.LayoutParams(-1,-1));
        error = new LinearLayout(this); error.setOrientation(LinearLayout.VERTICAL);
        error.setGravity(android.view.Gravity.CENTER); error.setPadding(32,32,32,32);
        error.setBackgroundColor(AppTheme.ORANGE);
        TextView message = new TextView(this); message.setText("KTDash could not load.\nCheck your connection and try again.");
        message.setTextColor(Color.BLACK); message.setTextSize(18); message.setGravity(17); error.addView(message);
        Button retry = new Button(this); retry.setText("Try again"); styleButton(retry); retry.setOnClickListener(v -> web.loadUrl(retryUrl)); error.addView(retry);
        frame.addView(error, new android.widget.FrameLayout.LayoutParams(-1,-1)); error.setVisibility(View.GONE);
        setContentView(root); root.requestApplyInsets();
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(true); settings.setBuiltInZoomControls(true); settings.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.addJavascriptInterface(new BundledModels(getApplicationContext()), "KTDashModelAssets");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                if (trusted(request.getUrl())) return false;
                external(request.getUrl()); return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                pageReady = false; mainHandler.removeCallbacks(checkUpdates);
                failed = false; if (trusted(Uri.parse(url))) retryUrl = url;
                error.setVisibility(View.GONE); progress.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (trusted(Uri.parse(url))) { AppTheme.apply(view); RosterPortraits.apply(view); }
                progress.setVisibility(View.INVISIBLE);
                pageReady = !failed;
                scheduleUpdateCheck();
            }
            @Override public void onPageCommitVisible(WebView view, String url) {
                if (trusted(Uri.parse(url))) { AppTheme.apply(view); RosterPortraits.apply(view); }
            }
            @Override public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                if (trusted(Uri.parse(url))) RosterPortraits.apply(view);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError detail) {
                if (request.isForMainFrame()) showError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) showError();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                return RosterPortraits.requestAsset(web, message.message()) || super.onConsoleMessage(message);
            }
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                // Keep the bar's space reserved so the page does not jump while loading.
                progress.setVisibility(value == 100 || failed ? View.INVISIBLE : View.VISIBLE);
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (files != null) files.onReceiveValue(null); files = callback;
                try {
                    Intent picker = params.createIntent();
                    if (acceptsImages(params)) {
                        Intent camera = cameraIntent();
                        if (camera != null) {
                            Intent chooser = Intent.createChooser(picker, "Choose an operative photo");
                            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
                            startActivityForResult(chooser, PHOTO_PICKER_REQUEST);
                        } else startActivityForResult(picker, PHOTO_PICKER_REQUEST);
                    } else startActivityForResult(picker, PHOTO_PICKER_REQUEST);
                }
                catch (ActivityNotFoundException | SecurityException e) {
                    discardCameraPhoto(); files.onReceiveValue(null); files = null;
                    Toast.makeText(MainActivity.this,"No photo picker available",Toast.LENGTH_SHORT).show();
                }
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
            } catch (Exception e) { Toast.makeText(this,"Download unavailable. Please try again later.",1).show(); }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
    }
    private void scheduleUpdateCheck() {
        mainHandler.removeCallbacks(checkUpdates);
        if (resumed && pageReady) mainHandler.postDelayed(checkUpdates, 3000);
    }
    private void showError() {
        failed = true; pageReady = false; mainHandler.removeCallbacks(checkUpdates);
        progress.setVisibility(View.INVISIBLE); error.setVisibility(View.VISIBLE);
    }
    private boolean acceptsImages(WebChromeClient.FileChooserParams params) {
        String[] types = params.getAcceptTypes();
        if (types == null || types.length == 0) return false;
        for (String type : types) if (type != null && (type.equals("*/*") || type.startsWith("image/"))) return true;
        return false;
    }
    private Intent cameraIntent() {
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (capture.resolveActivity(getPackageManager()) == null) return null;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "ktdash-model-" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= 29) values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KTDash");
        cameraPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (cameraPhotoUri == null) return null;
        capture.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
        capture.setClipData(ClipData.newRawUri("KTDash model photo", cameraPhotoUri));
        capture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return capture;
    }
    private void discardCameraPhoto() {
        Uri uri = cameraPhotoUri; cameraPhotoUri = null;
        if (uri != null) try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request == PHOTO_PICKER_REQUEST && files != null) {
            Uri captured = cameraPhotoUri; cameraPhotoUri = null;
            boolean tookPhoto = result == RESULT_OK && captured != null && (data == null || data.getData() == null);
            Uri[] resultUris = tookPhoto ? new Uri[]{captured} : WebChromeClient.FileChooserParams.parseResult(result,data);
            if (!tookPhoto && captured != null) try { getContentResolver().delete(captured, null, null); } catch (Exception ignored) {}
            files.onReceiveValue(resultUris); files=null;
        } else if (request == PHOTO_PICKER_REQUEST) discardCameraPhoto();
    }
    @Override protected void onSaveInstanceState(Bundle out) { web.saveState(out); super.onSaveInstanceState(out); }
    @Override protected void onPause() {
        resumed = false; mainHandler.removeCallbacks(checkUpdates); web.onPause();
        CookieManager cookies = CookieManager.getInstance();
        // flush() performs disk I/O and blocks its caller; never run it on the UI thread.
        COOKIE_WRITER.execute(cookies::flush);
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true;
        if (web != null) web.onResume();
        scheduleUpdateCheck();
    }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        web.invalidate(); web.requestApplyInsets();
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){var v=window.__ktdashPictureViewer;return !!(v&&v.close());})()", closed -> {
            if (!"true".equals(closed)) { if (web.canGoBack()) web.goBack(); else MainActivity.super.onBackPressed(); }
        });
    }
    @Override protected void onDestroy() {
        mainHandler.removeCallbacks(checkUpdates);
        if (files != null) files.onReceiveValue(null);
        web.destroy(); super.onDestroy();
    }
}
