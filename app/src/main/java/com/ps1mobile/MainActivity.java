package com.ps1mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_GAME = 41;
    private final ArrayList<String> names = new ArrayList<>();
    private LinearLayout library;
    private TextView status;

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1769AA));
        return b;
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        build();
    }
    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(16));
        root.setBackgroundColor(0xFF071426);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.addView(label("PS1  MOBILE", 23, Color.WHITE, true),
            new LinearLayout.LayoutParams(0, -2, 1));
        Button store = button("Loja"), libraryTab = button("Biblioteca"), settings = button("Configurações");
        nav.addView(store); nav.addView(libraryTab); nav.addView(settings);
        root.addView(nav);

        TextView hero = label("SEUS CLÁSSICOS. SUA BIBLIOTECA.", 26, Color.WHITE, true);
        hero.setPadding(0, dp(28), 0, dp(5)); root.addView(hero);
        root.addView(label("Organize seus jogos de PlayStation em um só lugar.", 14, 0xFFB9C9DC, false));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button add = button("+  Adicionar jogo");
        add.setOnClickListener(v -> pickGame());
        actions.addView(add);
        status = label("  Nenhum jogo adicionado", 13, 0xFFB9C9DC, false);
        actions.addView(status);
        actions.setPadding(0, dp(18), 0, dp(12)); root.addView(actions);

        root.addView(label("BIBLIOTECA", 16, Color.WHITE, true));
        library = new LinearLayout(this); library.setOrientation(LinearLayout.VERTICAL);
        TextView empty = label("Sua coleção aparecerá aqui.\\nUse “Adicionar jogo” para escolher um arquivo CUE, BIN, CHD ou ISO.", 14, 0xFF8FA8C4, false);
        empty.setPadding(dp(16), dp(18), dp(16), dp(18)); empty.setBackgroundColor(0xFF10243D);
        library.addView(empty);
        root.addView(library, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView foot = label("PS1 MOBILE  •  PROJETO EM DESENVOLVIMENTO  •  v0.1.0", 11, 0xFF7890AA, false);
        foot.setPadding(0, dp(10), 0, 0); root.addView(foot);
        setContentView(root);
        store.setOnClickListener(v -> Toast.makeText(this, "O catálogo será implementado depois.", Toast.LENGTH_SHORT).show());
        libraryTab.setOnClickListener(v -> Toast.makeText(this, "Biblioteca local", Toast.LENGTH_SHORT).show());
        settings.setOnClickListener(v -> Toast.makeText(this, "As opções do emulador entram junto do núcleo de emulação.", Toast.LENGTH_LONG).show());
    }
    private void pickGame() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        startActivityForResult(i, PICK_GAME);
    }
    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != PICK_GAME || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) {}
        String name = getName(uri), low = name.toLowerCase(Locale.ROOT);
        if (!(low.endsWith(".cue") || low.endsWith(".bin") || low.endsWith(".chd") ||
              low.endsWith(".iso") || low.endsWith(".img") || low.endsWith(".pbp") ||
              low.endsWith(".m3u") || low.endsWith(".ccd"))) {
            Toast.makeText(this, "Formato não reconhecido. Escolha um arquivo de jogo PS1.", Toast.LENGTH_LONG).show();
            return;
        }
        names.add(name); refresh();
        Toast.makeText(this, "Arquivo adicionado à biblioteca. Ainda não inicia jogos: o núcleo de emulação será integrado na próxima etapa.", Toast.LENGTH_LONG).show();
    }
    private String getName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) return c.getString(ix);
            }
        } catch (Exception ignored) {}
        return "Jogo PS1";
    }
    private void refresh() {
        library.removeAllViews();
        for (String n : names) {
            TextView item = label("▣   " + n + "\\n      Arquivo adicionado", 15, Color.WHITE, true);
            item.setPadding(dp(16), dp(13), dp(16), dp(13)); item.setBackgroundColor(0xFF10243D);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.bottomMargin = dp(7); library.addView(item, p);
        }
        status.setText("  " + names.size() + (names.size() == 1 ? " jogo" : " jogos"));
    }
}
