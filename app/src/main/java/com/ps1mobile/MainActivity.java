
package com.ps1mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_GAME = 41;
    private final ArrayList<String> names = new ArrayList<>();
    private LinearLayout root, content, nav;
    private TextView status;

    private static final String[][] CATALOG = {
        {"RESIDENT EVIL 3", "NEMESIS • SURVIVAL HORROR", "RACCOON CITY", "0xFF6D2630"},
        {"METAL GEAR SOLID", "TACTICAL ESPIONAGE", "SHADOW MOSES", "0xFF344B59"},
        {"FINAL FANTASY VII", "JAPANESE RPG", "MIDGAR", "0xFF42604C"},
        {"CRASH BANDICOOT", "PLATFORMER", "N. SANITY ISLAND", "0xFFB66A2B"},
        {"SILENT HILL", "PSYCHOLOGICAL HORROR", "SILENT HILL", "0xFF59616A"},
        {"TEKKEN 3", "FIGHTING", "KING OF IRON FIST", "0xFF76513D"}
    };

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(0xFF1769AA)
        );
        return b;
    }

    private GradientDrawable panel(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), 0xFF29415B);
        return d;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        buildShell();
        showStore();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(12), dp(22), dp(12));
        root.setBackgroundColor(0xFF071426);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        header.addView(
            label("PS1 MOBILE", 22, Color.WHITE, true),
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        header.addView(
            label("PLAYSTATION CLASSICS", 11, 0xFF7898B8, true)
        );

        root.addView(header);

        nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(8), 0, dp(8));

        Button store = button("Loja");
        Button library = button("Biblioteca");
        Button settings = button("Configurações");

        nav.addView(store);
        nav.addView(library);
        nav.addView(settings);

        root.addView(nav);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        root.addView(
            content,
            new LinearLayout.LayoutParams(-1, 0, 1)
        );

        setContentView(root);

        store.setOnClickListener(v -> showStore());
        library.setOnClickListener(v -> showLibrary());
        settings.setOnClickListener(v -> showSettings());
    }

    private void showStore() {
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(6), 0, dp(12));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(17), dp(20), dp(17));
        hero.setBackground(panel(0xFF142D49));

        hero.addView(
            label("DESTAQUE DA SEMANA", 11, 0xFF70B9FF, true)
        );

        TextView title = label(
            "RESIDENT EVIL 3", 27, Color.WHITE, true
        );

        title.setPadding(0, dp(6), 0, dp(2));
        hero.addView(title);

        hero.addView(
            label(
                "NEMESIS  •  PLAYSTATION  •  1999",
                12, 0xFFC2D3E5, true
            )
        );

        TextView desc = label(
            "Volte a Raccoon City nesta vitrine de clássicos do PS1.",
            13, 0xFFB0C2D5, false
        );

        desc.setPadding(0, dp(8), 0, dp(8));
        hero.addView(desc);

        Button featured = button("Selecionar arquivo do jogo");
        featured.setOnClickListener(v -> pickGame());
        hero.addView(featured);

        page.addView(hero);

        TextView heading = label(
            "CATÁLOGO DE CLÁSSICOS", 17, Color.WHITE, true
        );

        heading.setPadding(0, dp(17), 0, dp(8));
        page.addView(heading);

        page.addView(
            label(
                "Explore a vitrine e importe os jogos que você já possui.",
                12, 0xFF9EB2C8, false
            )
        );

        for (int i = 0; i < CATALOG.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(8), 0, 0);

            row.addView(
                catalogCard(i),
                new LinearLayout.LayoutParams(0, dp(154), 1)
            );

            if (i + 1 < CATALOG.length) {
                row.addView(
                    new View(this),
                    new LinearLayout.LayoutParams(dp(10), 1)
                );

                row.addView(
                    catalogCard(i + 1),
                    new LinearLayout.LayoutParams(0, dp(154), 1)
                );
            }

            page.addView(row);
        }

        TextView note = label(
            "Esta loja é um catálogo visual. Não vende nem baixa jogos. " +
            "Use “Selecionar arquivo” para importar uma cópia sua do aparelho.",
            11, 0xFF8199B2, false
        );

        note.setPadding(dp(4), dp(15), dp(4), dp(8));
        page.addView(note);

        scroll.addView(page);

        content.addView(
            scroll,
            new LinearLayout.LayoutParams(-1, -1)
        );
    }

    private View catalogCard(int index) {
        String[] item = CATALOG[index];

        // CORRIGIDO: converte a cor hexadecimal 0xAARRGGBB.
        int color = (int) Long.parseLong(item[3].substring(2), 16);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(8));
        card.setBackground(panel(0xFF10243A));

        TextView cover = label(item[0], 16, Color.WHITE, true);
        cover.setGravity(Gravity.CENTER);
        cover.setPadding(dp(8), dp(8), dp(8), dp(8));
        cover.setBackground(panel(color));

        card.addView(
            cover,
            new LinearLayout.LayoutParams(-1, 0, 1)
        );

        TextView genre = label(
            item[1], 9, 0xFF8DB8E2, true
        );

        genre.setPadding(0, dp(6), 0, 0);
        card.addView(genre);

        card.addView(
            label(item[2], 10, 0xFFD4DFEA, true)
        );

        card.setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle(item[0])
                .setMessage(
                    item[1] + "\n\n" +
                    item[2] +
                    "\n\nEsta ficha é apenas um catálogo. " +
                    "Para jogar, selecione seu arquivo local."
                )
                .setPositiveButton(
                    "Importar arquivo",
                    (dialog, which) -> pickGame()
                )
                .setNegativeButton("Fechar", null)
                .show()
        );

        return card;
    }

    private void showLibrary() {
        content.removeAllViews();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(10), 0, 0);

        page.addView(
            label("MINHA BIBLIOTECA", 21, Color.WHITE, true)
        );

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        Button add = button("+ Adicionar jogo");
        add.setOnClickListener(v -> pickGame());
        actions.addView(add);

        status = label(
            "  " + names.size() +
            (names.size() == 1 ? " jogo" : " jogos"),
            13, 0xFFB9C9DC, false
        );

        actions.addView(status);
        actions.setPadding(0, dp(12), 0, dp(12));
        page.addView(actions);

        if (names.isEmpty()) {
            TextView empty = label(
                "Sua coleção ainda está vazia.\n\n" +
                "Abra a Loja ou toque em Adicionar jogo para " +
                "importar um arquivo CUE, BIN, CHD ou ISO.",
                14, 0xFF9EB2C8, false
            );

            empty.setPadding(
                dp(16), dp(20), dp(16), dp(20)
            );

            empty.setBackground(panel(0xFF10243D));
            page.addView(empty);

        } else {
            for (String name : names) {
                TextView item = label(
                    "▣   " + name +
                    "\n      Arquivo adicionado • PS1",
                    14, Color.WHITE, true
                );

                item.setPadding(
                    dp(15), dp(14), dp(15), dp(14)
                );

                item.setBackground(panel(0xFF10243D));

                LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(-1, -2);

                p.bottomMargin = dp(7);
                page.addView(item, p);
            }
        }

        content.addView(
            page,
            new LinearLayout.LayoutParams(-1, -1)
        );
    }

    private void showSettings() {
        content.removeAllViews();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(12), 0, 0);

        page.addView(
            label("CONFIGURAÇÕES", 21, Color.WHITE, true)
        );

        TextView info = label(
            "Emulador: núcleo PCSX-ReARMed planejado\n\n" +
            "Vídeo: opções disponíveis após integrar o núcleo\n\n" +
            "Controles: controles virtuais serão adicionados " +
            "na etapa de emulação\n\n" +
            "BIOS: suporte para selecionar seu próprio arquivo " +
            "será integrado depois.",
            14, 0xFFB9C9DC, false
        );

        info.setPadding(
            dp(16), dp(18), dp(16), dp(18)
        );

        info.setBackground(panel(0xFF10243D));
        page.addView(info);
        content.addView(page);
    }

    private void pickGame() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");

        startActivityForResult(i, PICK_GAME);
    }

    @Override
    protected void onActivityResult(
        int req, int result, Intent data
    ) {
        super.onActivityResult(req, result, data);

        if (req != PICK_GAME ||
            result != RESULT_OK ||
            data == null ||
            data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        String name = getName(uri);
        String low = name.toLowerCase(Locale.ROOT);

        if (!(low.endsWith(".cue") ||
              low.endsWith(".bin") ||
              low.endsWith(".chd") ||
              low.endsWith(".iso") ||
              low.endsWith(".img") ||
              low.endsWith(".pbp") ||
              low.endsWith(".m3u") ||
              low.endsWith(".ccd"))) {

            Toast.makeText(
                this,
                "Formato não reconhecido. Escolha um arquivo de jogo PS1.",
                Toast.LENGTH_LONG
            ).show();

            return;
        }

        names.add(name);

        Toast.makeText(
            this,
            "Jogo adicionado à biblioteca. A execução depende do núcleo de emulação.",
            Toast.LENGTH_LONG
        ).show();

        showLibrary();
    }

    private String getName(Uri uri) {
        try (Cursor c = getContentResolver().query(
            uri, null, null, null, null
        )) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                );

                if (ix >= 0) {
                    return c.getString(ix);
                }
            }
        } catch (Exception ignored) {}

        return "Jogo PS1";
    }
}
