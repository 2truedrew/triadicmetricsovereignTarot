package com.apk;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class A extends Activity {
    WebView wv;
    TextView log;
    ScrollView sv;
    LinearLayout ll;
    StringBuilder logBuf = new StringBuilder();
    File weaveDir;
    static final int PICK_HTML = 1, REQ_PERM = 2;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setAllowFileAccess(true);
        wv.setWebViewClient(new WebViewClient());
        wv.setWebChromeClient(new WebChromeClient());
        ll.addView(wv, new LinearLayout.LayoutParams(-1, 0, 4));
        Button pick = new Button(this);
        pick.setText("Import HTML Files");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("text/html");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK_HTML);
        });
        ll.addView(pick);
        Button weave = new Button(this);
        weave.setText("Weave & Build");
        weave.setOnClickListener(v -> weaveFiles());
        ll.addView(weave);
        Button copyLog = new Button(this);
        copyLog.setText("Copy Error Log");
        copyLog.setOnClickListener(v -> {
            ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(logBuf.toString());
            Toast.makeText(this, "Log copied", Toast.LENGTH_SHORT).show();
        });
        ll.addView(copyLog);
        sv = new ScrollView(this);
        log = new TextView(this);
        log.setTextIsSelectable(true);
        sv.addView(log);
        ll.addView(sv, new LinearLayout.LayoutParams(-1, 0, 2));
        setContentView(ll);
        weaveDir = new File(Environment.getExternalStorageDirectory(), "Weave");
        weaveDir.mkdirs();
        checkPerms();
        wv.loadUrl("file:///android_asset/html/index.html");
    }

    void checkPerms() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager())
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:" + getPackageName())), REQ_PERM);
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM);
        }
    }

    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_HTML && res == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null)
                for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    uris.add(data.getClipData().getItemAt(i).getUri());
            else if (data.getData() != null) uris.add(data.getData());
            importFiles(uris);
        }
    }

    void importFiles(List<Uri> uris) {
        File htmlDir = new File(weaveDir, "html"); htmlDir.mkdirs();
        int count = 0;
        for (Uri uri : uris) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                File out = new File(htmlDir, new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date()) + count + ".html");
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[8192]; int n;
                while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                is.close(); fos.close();
                log("Imported: " + out.getName()); count++;
            } catch (Exception e) { log("Import error: " + e.getMessage()); }
        }
        Toast.makeText(this, "Imported " + count + " files", Toast.LENGTH_SHORT).show();
    }

    void weaveFiles() {
        try {
            File htmlDir = new File(weaveDir, "html");
            File[] files = htmlDir.listFiles((d, n) -> n.endsWith(".html"));
            if (files == null || files.length == 0) { log("No HTML files to weave"); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><meta charset=UTF-8><meta name=viewport content=\"width=device-width,initial-scale=1.0\"><title>Apperakadabra Weave</title>");
            sb.append("<style>:root{--p:#6366f1;--b:#2d2d5e;--r:12px}*{margin:0;padding:0;box-sizing:border-box}body{font-family:system-ui,sans-serif;background:#0f0f1a;color:#e2e8f0}.nav{background:#1a1a2e;padding:1rem;display:flex;gap:1rem;flex-wrap:wrap;position:sticky;top:0;z-index:100;border-bottom:1px solid var(--b)}.nav a{color:#a5b4fc;text-decoration:none;padding:.5rem 1rem;border-radius:8px;transition:.2s}.nav a:hover,.nav a.active{background:var(--b);color:#fff}.content{max-width:900px;margin:0 auto;padding:2rem}.card{background:#1a1a2e;border-radius:var(--r);padding:1.5rem;margin:1rem 0;border:1px solid var(--b)}@media(max-width:768px){.content{padding:1rem}.nav{justify-content:center}}</style></head><body>");
            sb.append("<nav class=nav><a href=# class=active>Home</a>");
            for (File f : files) {
                String name = f.getName();
                sb.append("<a href=\"#" + name + "\">" + name.replace(".html","").replace("_"," ") + "</a>");
            }
            sb.append("</nav><main class=content>");
            for (File f : files) {
                String c = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                if (c.contains("<body")) c = c.split("<body[^>]*>")[1].split("</body>")[0];
                sb.append("<div class=card id=\"" + f.getName() + "\">" + c + "</div>");
            }
            sb.append("</main>");
            sb.append("<script>document.querySelectorAll('.nav a').forEach(a=>{a.onclick=function(e){e.preventDefault();document.querySelectorAll('.nav a').forEach(x=>x.classList.remove('active'));this.classList.add('active');let t=document.querySelector(this.getAttribute('href'));if(t)t.scrollIntoView({behavior:'smooth',block:'start'})}})</script>");
            sb.append("</body></html>");
            File out = new File(weaveDir, "weaved.html");
            java.nio.file.Files.write(out.toPath(), sb.toString().getBytes());
            File assetsOut = new File(getFilesDir(), "weaved.html");
            java.nio.file.Files.write(assetsOut.toPath(), sb.toString().getBytes());
            wv.loadUrl("file://" + assetsOut.getAbsolutePath());
            log("Weaved " + files.length + " files -> " + out.getAbsolutePath());
            Toast.makeText(this, "Weaved " + files.length + " files", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { log("Weave error: " + e.getMessage()); }
    }

    void log(String msg) {
        logBuf.append("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
        log.setText(logBuf.toString());
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }
}
