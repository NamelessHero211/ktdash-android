package app.ktdash.personal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class AppUpdates {
    private static final String REPO = "NamelessHero211/ktdash-android";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String DOWNLOAD_PREFIX = "https://github.com/" + REPO + "/releases/download/";
    private boolean checking;

    void check(Activity activity, boolean manual) {
        if (checking) return;
        SharedPreferences prefs = activity.getSharedPreferences("app_updates", 0);
        long now = System.currentTimeMillis();
        if (!manual && now - prefs.getLong("last_attempt", 0) < 12 * 60 * 60 * 1000L) return;
        prefs.edit().putLong("last_attempt", now).apply();
        checking = true;
        if (manual) Toast.makeText(activity, "Checking for app updates…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            String download = null;
            String version = null;
            String message = "You have the latest published app version.";
            try {
                connection = (HttpURLConnection) new URL(API).openConnection();
                connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "KTDash-Unofficial-Android");
                connection.setInstanceFollowRedirects(false);
                int status = connection.getResponseCode();
                if (status == 404) {
                    message = "No app update has been published yet.";
                } else if (status != 200) {
                    throw new java.io.IOException("Update service unavailable");
                } else {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (InputStream input = connection.getInputStream()) {
                        byte[] buffer = new byte[4096]; int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (bytes.size() + count > 256 * 1024) throw new java.io.IOException("Response too large");
                            bytes.write(buffer, 0, count);
                        }
                    }
                    JSONObject release = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                    String tag = release.getString("tag_name");
                    if (!release.optBoolean("draft") && !release.optBoolean("prerelease") && tag.matches("android-[0-9]{1,9}")) {
                        long published = Long.parseLong(tag.substring("android-".length()));
                        long installed = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                        if (published > installed) {
                            JSONArray assets = release.getJSONArray("assets");
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                String url = asset.optString("browser_download_url");
                                if ("KTDash.apk".equals(asset.optString("name")) &&
                                    url.equals(DOWNLOAD_PREFIX + tag + "/KTDash.apk") && asset.optLong("size") > 0) {
                                    download = url; version = release.optString("name", tag); break;
                                }
                            }
                            if (download == null) throw new java.io.IOException("Release has no APK");
                        }
                    } else {
                        throw new java.io.IOException("Unexpected release format");
                    }
                }
            } catch (Exception e) {
                message = "Could not check for updates. Try again later.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String link = download;
            final String releaseName = version;
            final String result = message;
            activity.runOnUiThread(() -> {
                checking = false;
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (link != null) {
                    new AlertDialog.Builder(activity).setTitle("KTDash update available")
                        .setMessage(releaseName + " is available. Download the APK, then open it to install the update. Your app data is kept.")
                        .setPositiveButton("Download APK", (dialog, which) -> {
                            try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))); }
                            catch (android.content.ActivityNotFoundException e) {
                                Toast.makeText(activity, "No browser is available to download this update.", Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton("Later", null).show();
                } else if (manual) {
                    Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
                }
            });
        }, "KTDash-update-check").start();
    }
}
