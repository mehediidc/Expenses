package com.mhdigital.myexpenses.nativebridge;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class NativeBridge extends CordovaPlugin {
    private static final int REQ_SAVE_BACKUP = 4401;
    private CallbackContext pendingBackup;
    private String pendingJson;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if ("saveBackup".equals(action)) {
                JSONObject o = args.getJSONObject(0);
                pendingJson = o.getString("json");
                String filename = o.optString("filename", "MyExpenses_Backup.json");
                pendingBackup = callbackContext;

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                cordova.getActivity().startActivityForResult(intent, REQ_SAVE_BACKUP);
                return true;
            }

            if ("printHtml".equals(action)) {
                JSONObject o = args.getJSONObject(0);
                printHtml(o.optString("title","MyExpenses"), o.optString("html",""), callbackContext);
                return true;
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage() == null ? "Native operation failed" : e.getMessage());
            return true;
        }
        return false;
    }

    private void printHtml(final String title, final String html, final CallbackContext callback) {
        final Activity activity = cordova.getActivity();
        activity.runOnUiThread(() -> {
            try {
                final WebView printView = new WebView(activity);
                printView.getSettings().setJavaScriptEnabled(false);
                printView.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        try {
                            PrintManager pm = (PrintManager) activity.getSystemService(Activity.PRINT_SERVICE);
                            PrintAttributes attrs = new PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                    .build();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                pm.print(title, printView.createPrintDocumentAdapter(title), attrs);
                            } else {
                                pm.print(title, printView.createPrintDocumentAdapter(), attrs);
                            }
                            callback.success();
                        } catch (Exception e) {
                            callback.error(e.getMessage() == null ? "Could not open Android print service" : e.getMessage());
                            cleanup(activity, printView);
                        }
                    }
                });

                ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                printView.setAlpha(0.01f);
                root.addView(printView, lp);

                String css = "<style>" +
                        "body{font-family:Arial,sans-serif;padding:18px;color:#111}" +
                        "h2{text-align:center;margin:0 0 16px}" +
                        "table{width:100%;border-collapse:collapse;font-size:12px}" +
                        "th,td{border:1px solid #999;padding:7px 6px}" +
                        "th{background:#eee}" +
                        ".num{text-align:right}" +
                        ".summary-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}" +
                        "@media print{body{padding:0}}" +
                        "</style>";
                String doc = "<!doctype html><html><head><meta charset='utf-8'><title>" +
                        escape(title) + "</title>" + css + "</head><body><h2>" +
                        escape(title) + "</h2>" + html + "</body></html>";
                printView.loadDataWithBaseURL("file:///android_asset/www/", doc, "text/html", "UTF-8", null);
            } catch (Exception e) {
                callback.error(e.getMessage() == null ? "Print failed" : e.getMessage());
            }
        });
    }

    private void cleanup(Activity activity, WebView v) {
        activity.runOnUiThread(() -> {
            try {
                ViewGroup p=(ViewGroup)v.getParent();
                if(p!=null)p.removeView(v);
                v.destroy();
            } catch(Exception ignored){}
        });
    }

    private String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_SAVE_BACKUP || pendingBackup == null) return;
        CallbackContext cb = pendingBackup;
        pendingBackup = null;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            cb.error("cancelled");
            pendingJson = null;
            return;
        }
        try {
            Uri uri = data.getData();
            ContentResolver cr = cordova.getActivity().getContentResolver();
            try (OutputStream out = cr.openOutputStream(uri)) {
                if (out == null) throw new Exception("Cannot open selected file location");
                out.write(pendingJson.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            cb.success();
        } catch (Exception e) {
            cb.error(e.getMessage() == null ? "Could not save backup file" : e.getMessage());
        } finally {
            pendingJson = null;
        }
    }
}
