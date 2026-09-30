package com.protocolpoliceduty.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int REQ_FILE = 4101;
  private static final String AUTH = "com.protocolpoliceduty.app.fileprovider";
  private static final String URL = "file:///android_asset/index.html";
  private static final String BG = "#0B1426";

  private WebView web;
  private ValueCallback<Uri[]> fileCb;

  @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.parseColor(BG));
    web = new WebView(this);
    web.setBackgroundColor(Color.parseColor(BG));
    root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    setContentView(root);

    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    s.setTextZoom(100);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);
    s.setJavaScriptCanOpenWindowsAutomatically(true);

    web.addJavascriptInterface(new Bridge(), "PDRNative");

    web.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        return openExternal(r.getUrl());
      }

      @Override
      public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
        runOnUiThread(new Runnable() { public void run() { recreate(); } });
        return true;
      }
    });

    web.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onJsAlert(WebView v, String url, String msg, final JsResult r) {
        if (isFinishing()) { r.cancel(); return true; }
        new AlertDialog.Builder(MainActivity.this).setTitle("PROTOCOL SAKHA").setMessage(msg)
            .setPositiveButton("ઓકે", (d, w) -> r.confirm())
            .setOnCancelListener(d -> r.cancel()).show();
        return true;
      }

      @Override
      public boolean onJsConfirm(WebView v, String url, String msg, final JsResult r) {
        if (isFinishing()) { r.cancel(); return true; }
        new AlertDialog.Builder(MainActivity.this).setTitle("PROTOCOL SAKHA").setMessage(msg)
            .setPositiveButton("ઓકે", (d, w) -> r.confirm())
            .setNegativeButton("રદ કરો", (d, w) -> r.cancel())
            .setOnCancelListener(d -> r.cancel()).show();
        return true;
      }

      @Override
      public boolean onJsPrompt(WebView v, String url, String msg, String def, final JsPromptResult r) {
        if (isFinishing()) { r.cancel(); return true; }
        final EditText in = new EditText(MainActivity.this);
        in.setText(def == null ? "" : def);
        in.setSelectAllOnFocus(true);
        new AlertDialog.Builder(MainActivity.this).setTitle("PROTOCOL SAKHA").setMessage(msg).setView(in)
            .setPositiveButton("ઓકે", (d, w) -> r.confirm(in.getText().toString()))
            .setNegativeButton("રદ કરો", (d, w) -> r.cancel())
            .setOnCancelListener(d -> r.cancel()).show();
        return true;
      }

      @Override
      public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
        if (fileCb != null) { fileCb.onReceiveValue(null); }
        fileCb = cb;
        try {
          Intent i = p.createIntent();
          i.setType("*/*");                       // some pickers grey-out .json otherwise
          i.removeExtra(Intent.EXTRA_MIME_TYPES);
          startActivityForResult(i, REQ_FILE);
        } catch (Exception e) {
          fileCb = null;
          cb.onReceiveValue(null);
          return false;
        }
        return true;
      }
    });

    web.loadUrl(URL);
  }

  @Override
  protected void onActivityResult(int req, int res, Intent data) {
    if (req == REQ_FILE) {
      if (fileCb != null) {
        fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
        fileCb = null;
      }
      return;
    }
    super.onActivityResult(req, res, data);
  }

  @Override
  public void onBackPressed() {
    web.evaluateJavascript("(function(){try{return !!(window.__pdrBack&&window.__pdrBack())}catch(e){return false}})()",
        new ValueCallback<String>() {
          @Override public void onReceiveValue(String v) {
            if (!"true".equals(v)) {
              if (web.canGoBack()) web.goBack(); else finish();
            }
          }
        });
  }

  @Override protected void onPause() { super.onPause(); if (web != null) web.onPause(); }
  @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }

  @Override
  protected void onDestroy() {
    if (web != null) { ViewGroup p = (ViewGroup) web.getParent(); if (p != null) p.removeView(web); web.destroy(); web = null; }
    super.onDestroy();
  }

  private boolean openExternal(Uri u) {
    String sc = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
    if (sc.equals("file") || sc.equals("blob") || sc.equals("data") || sc.equals("about") || sc.equals("javascript")) return false;
    try {
      Intent i = new Intent(Intent.ACTION_VIEW, u);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(i);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "આ લિંક ખોલવા માટે એપ મળી નથી", Toast.LENGTH_SHORT).show();
    }
    return true;
  }

  /** Native helpers used by the JS shim that is embedded in index.html (Capacitor-compatible API). */
  private class Bridge {
    private String docName(String relPath) {
      String n = relPath == null ? "file" : relPath;
      int k = n.lastIndexOf('/');
      if (k >= 0) n = n.substring(k + 1);
      n = n.replaceAll("[\\\\/:*?\"<>|]+", "_");
      return n.trim().isEmpty() ? "file" : n;
    }

    private Uri findDoc(String name) {
      ContentResolver cr = getContentResolver();
      Uri col = MediaStore.Files.getContentUri("external");
      Cursor c = cr.query(col, new String[]{MediaStore.MediaColumns._ID},
          MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?",
          new String[]{name, "Documents/PROTOCOL-SAKHA/"}, null);
      Uri found = null;
      if (c != null) {
        try { if (c.moveToFirst()) found = ContentUris.withAppendedId(col, c.getLong(0)); } finally { c.close(); }
      }
      return found;
    }

    /** Auto-backup: Documents/PROTOCOL-SAKHA/<name> (Android 10+ via MediaStore, older: app files folder). */
    @JavascriptInterface
    public boolean saveDocument(String relPath, String content, boolean isBase64) {
      try {
        String name = docName(relPath);
        byte[] data = isBase64 ? Base64.decode(content, Base64.DEFAULT) : content.getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= 29) {
          ContentResolver cr = getContentResolver();
          Uri target = findDoc(name);
          if (target == null) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, name.endsWith(".json") ? "application/json" : "application/octet-stream");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/PROTOCOL-SAKHA");
            target = cr.insert(MediaStore.Files.getContentUri("external"), v);
          }
          if (target == null) return false;
          OutputStream os = cr.openOutputStream(target, "wt");
          if (os == null) return false;
          try { os.write(data); } finally { os.close(); }
          return true;
        } else {
          File dir = new File(getExternalFilesDir(null), "PROTOCOL-SAKHA");
          if (!dir.exists()) dir.mkdirs();
          FileOutputStream fo = new FileOutputStream(new File(dir, name));
          try { fo.write(data); } finally { fo.close(); }
          return true;
        }
      } catch (Exception e) {
        return false;
      }
    }

    @JavascriptInterface
    public String readDocument(String relPath) {
      try {
        String name = docName(relPath);
        InputStream is;
        if (Build.VERSION.SDK_INT >= 29) {
          Uri u = findDoc(name);
          if (u == null) return null;
          is = getContentResolver().openInputStream(u);
        } else {
          File f = new File(new File(getExternalFilesDir(null), "PROTOCOL-SAKHA"), name);
          if (!f.exists()) return null;
          is = new FileInputStream(f);
        }
        if (is == null) return null;
        try {
          ByteArrayOutputStream bo = new ByteArrayOutputStream();
          byte[] buf = new byte[8192];
          int n;
          while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
          return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        } finally { is.close(); }
      } catch (Exception e) {
        return null;
      }
    }

    @JavascriptInterface
    public void exit() { runOnUiThread(new Runnable() { public void run() { finish(); } }); }

    @JavascriptInterface
    public String saveCacheFile(String name, String b64) {
      try {
        File dir = new File(getCacheDir(), "shared");
        if (!dir.exists()) dir.mkdirs();
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) if (System.currentTimeMillis() - f.lastModified() > 24L * 3600 * 1000) f.delete();
        String safe = (name == null || name.trim().isEmpty()) ? "file" : name.replaceAll("[\\\\/:*?\"<>|]+", "_");
        File out = new File(dir, safe);
        byte[] data = Base64.decode(b64, Base64.DEFAULT);
        FileOutputStream fo = new FileOutputStream(out);
        try { fo.write(data); } finally { fo.close(); }
        return FileProvider.getUriForFile(MainActivity.this, AUTH, out).toString();
      } catch (Exception e) {
        return "";
      }
    }

    @JavascriptInterface
    public boolean share(String title, String text, String url) {
      try {
        final Intent i = new Intent(Intent.ACTION_SEND);
        if (url != null && url.length() > 0) {
          Uri u = Uri.parse(url);
          String mime = getContentResolver().getType(u);
          i.setType(mime == null ? "application/octet-stream" : mime);
          i.putExtra(Intent.EXTRA_STREAM, u);
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
          i.setType("text/plain");
        }
        if (text != null && text.length() > 0) i.putExtra(Intent.EXTRA_TEXT, text);
        if (title != null && title.length() > 0) i.putExtra(Intent.EXTRA_SUBJECT, title);
        runOnUiThread(new Runnable() {
          public void run() {
            try { startActivity(Intent.createChooser(i, "PROTOCOL SAKHA")); }
            catch (Exception e) { Toast.makeText(MainActivity.this, "શેર થઈ શક્યું નહીં", Toast.LENGTH_SHORT).show(); }
          }
        });
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }
}
