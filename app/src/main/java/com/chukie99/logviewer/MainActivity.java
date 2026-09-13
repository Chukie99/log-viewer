package com.chukie99.logviewer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    TextView logView, status;
    EditText filterInput;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        logView = findViewById(R.id.logView);
        status = findViewById(R.id.status);
        filterInput = findViewById(R.id.filterInput);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnCatch = findViewById(R.id.btnCatch);
        Button btnOpen = findViewById(R.id.btnOpen);
        Button btnShare = findViewById(R.id.btnShare);

        btnClear.setOnClickListener(v -> {
            try {
                Runtime.getRuntime().exec("logcat -c").waitFor();
                status.setText("✅ Log dibersihkan — sekarang buka app yang FC, lalu kembali & Tangkap Crash");
                logView.setText("Log dibersihkan. Buka app target sampai FC, lalu kembali ke sini & tap Tangkap Crash.");
            } catch (Exception e) { status.setText("Gagal clear: "+e.getMessage()); logView.setText(e.toString()); }
        });

        btnCatch.setOnClickListener(v -> {
            String filter = filterInput.getText().toString().trim();
            try {
                // -d dump, *:E only errors, AndroidRuntime:E = crash stack
                Process p = Runtime.getRuntime().exec(new String[]{"logcat","-d","AndroidRuntime:E","*:S"});
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                int count=0;
                while ((line=br.readLine())!=null) {
                    if (filter.isEmpty() || line.toLowerCase().contains(filter.toLowerCase())) {
                        sb.append(line).append("\n"); count++;
                    }
                }
                br.close();
                // if empty, try broader *:E
                if (count==0) {
                    Process p2 = Runtime.getRuntime().exec(new String[]{"logcat","-d","*:E"});
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
                    StringBuilder sb2 = new StringBuilder();
                    while ((line=br2.readLine())!=null) {
                        if (filter.isEmpty() || line.toLowerCase().contains(filter.toLowerCase())) sb2.append(line).append("\n");
                    }
                    br2.close();
                    if (sb2.length()>0) sb = sb2;
                }
                String out = sb.toString();
                if (out.trim().isEmpty()) {
                    logView.setText("Log kosong / tidak ada FATAL.\n\nJika Android 13+: butuh izin 1x via PC:\n  adb shell pm grant com.chukie99.logviewer android.permission.READ_LOGS\n\nLalu ulang: Bersihkan → Buka App sampai FC → Tangkap Crash\n\nAtau coba kosongkan filter & tap lagi.");
                    status.setText("Log kosong — butuh izin READ_LOGS (lihat bawah)");
                } else {
                    // trim to last 600 lines
                    String[] lines = out.split("\n");
                    int start = Math.max(0, lines.length-600);
                    StringBuilder trimmed = new StringBuilder();
                    for (int i=start;i<lines.length;i++) trimmed.append(lines[i]).append("\n");
                    logView.setText(trimmed.toString());
                    status.setText("Dapat "+lines.length+" baris — copy/share & kirim ke dev");
                }
            } catch (Exception e) { logView.setText("Error: "+e.getMessage()+"\n"+e); status.setText("Gagal baca log"); }
        });

        btnOpen.setOnClickListener(v -> {
            String pkg = filterInput.getText().toString().trim();
            if (pkg.isEmpty()) { Toast.makeText(this,"Isi package dulu (cth: com.chukie99.posumkm)",Toast.LENGTH_SHORT).show(); return; }
            try {
                PackageManager pm = getPackageManager();
                Intent it = pm.getLaunchIntentForPackage(pkg);
                if (it!=null) { startActivity(it); status.setText("Membuka "+pkg+" — tunggu FC lalu kembali ke sini"); }
                else status.setText("Package tidak ketemu: "+pkg);
            } catch (Exception e){ status.setText("Gagal buka: "+e.getMessage()); }
        });

        btnShare.setOnClickListener(v -> {
            String txt = logView.getText().toString();
            if (txt.trim().isEmpty()) { Toast.makeText(this,"Log kosong",Toast.LENGTH_SHORT).show(); return; }
            try {
                ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("log", txt));
                Toast.makeText(this,"Copied ke clipboard",Toast.LENGTH_SHORT).show();
            } catch (Exception ignore){}
            try {
                Intent sh = new Intent(Intent.ACTION_SEND); sh.setType("text/plain"); sh.putExtra(Intent.EXTRA_TEXT, txt);
                startActivity(Intent.createChooser(sh, "Share log via WA"));
            } catch (Exception e){ Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }
}
