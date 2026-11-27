package com.xaveco.videocutter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_VIDEOS = 41;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private File finalVideo;
    private TextView status, count;
    private Button pickButton, shareButton, saveButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 38, 28, 28);
        page.setBackgroundColor(0xFF090711);

        TextView crown = text("✦  VIDEO CUTTER PRO  ✦", 13, 0xFFFFD66B, true);
        crown.setGravity(Gravity.CENTER);
        page.addView(crown, match());

        TextView title = text("Seus vídeos.\nUm só resultado.", 30, 0xFFFFFFFF, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 24, 0, 10);
        page.addView(title, match());

        TextView subtitle = text("Recorte os 3 primeiros segundos de cada vídeo e una tudo em um MP4.", 16, 0xFFBEB7D1, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(8, 0, 8, 28);
        page.addView(subtitle, match());

        LinearLayout features = new LinearLayout(this);
        features.setOrientation(LinearLayout.HORIZONTAL);
        features.setGravity(Gravity.CENTER);
        features.addView(feature("✂", "Corte 3s"), new LinearLayout.LayoutParams(0, 112, 1));
        features.addView(feature("⇄", "Une tudo"), new LinearLayout.LayoutParams(0, 112, 1));
        features.addView(feature("HD", "MP4"), new LinearLayout.LayoutParams(0, 112, 1));
        page.addView(features, match());

        pickButton = new Button(this);
        pickButton.setText("＋  SELECIONAR VÍDEOS");
        pickButton.setTextSize(17);
        pickButton.setAllCaps(false);
        pickButton.setOnClickListener(v -> openPicker());
        page.addView(pickButton, buttonParams(64));

        count = text("Nenhum vídeo selecionado", 14, 0xFFE5DFFF, true);
        count.setGravity(Gravity.CENTER);
        count.setPadding(0, 20, 0, 8);
        page.addView(count, match());

        status = text("Escolha dois ou mais vídeos. A ordem selecionada será respeitada.", 14, 0xFFAAA2BD, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, 22);
        page.addView(status, match());

        shareButton = new Button(this);
        shareButton.setText("↗  COMPARTILHAR VÍDEO FINAL");
        shareButton.setAllCaps(false);
        shareButton.setEnabled(false);
        shareButton.setOnClickListener(v -> shareFinal());
        page.addView(shareButton, buttonParams(58));

        saveButton = new Button(this);
        saveButton.setText("↓  SALVAR NO APARELHO");
        saveButton.setAllCaps(false);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> saveFinal());
        page.addView(saveButton, buttonParams(58));

        TextView privacy = text("PROCESSAMENTO LOCAL  •  SEUS ORIGINAIS NÃO SÃO ALTERADOS", 10, 0xFF777087, true);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, 28, 0, 0);
        page.addView(privacy, match());
        setContentView(page);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }
    private LinearLayout feature(String symbol, String label) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
        card.setBackgroundColor(0xFF21152F); card.setPadding(6, 12, 6, 12);
        TextView a = text(symbol, 25, 0xFFE08AFF, true); a.setGravity(Gravity.CENTER); card.addView(a, match());
        TextView b = text(label, 12, 0xFFFFFFFF, true); b.setGravity(Gravity.CENTER); card.addView(b, match());
        return card;
    }
    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams buttonParams(int h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, h);
        p.setMargins(0, 10, 0, 0); return p;
    }
    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, PICK_VIDEOS);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_VIDEOS || resultCode != RESULT_OK || data == null) return;
        ArrayList<Uri> sources = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) sources.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) sources.add(data.getData());
        if (sources.isEmpty()) return;
        pickButton.setEnabled(false); shareButton.setEnabled(false); saveButton.setEnabled(false);
        count.setText(sources.size() + " vídeo(s) selecionado(s)");
        status.setText("Preparando os cortes e unindo tudo...");
        executor.execute(() -> {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VideoCutter3s");
            if (!dir.exists()) dir.mkdirs();
            ArrayList<File> clips = new ArrayList<>();
            String error = null;
            try {
                for (int n = 0; n < sources.size(); n++) {
                    File clip = new File(dir, "parte_" + n + "_" + System.currentTimeMillis() + ".mp4");
                    VideoTrimmer.trim(this, sources.get(n), clip, 3_000_000L);
                    clips.add(clip);
                    final int done = n + 1;
                    runOnUiThread(() -> status.setText("Recortando vídeos: " + done + "/" + sources.size()));
                }
                finalVideo = new File(dir, "video_unificado_" + System.currentTimeMillis() + ".mp4");
                VideoTrimmer.join(clips, finalVideo);
            } catch (Exception e) { error = e.getMessage(); }
            final String resultError = error;
            runOnUiThread(() -> {
                pickButton.setEnabled(true);
                if (resultError == null) {
                    status.setText("✓ Pronto! Todos os trechos estão em um único vídeo MP4.");
                    shareButton.setEnabled(true); saveButton.setEnabled(true);
                } else {
                    status.setText("Não foi possível unir os vídeos: " + resultError);
                    Toast.makeText(this, "Verifique se os vídeos têm formatos compatíveis.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    private Uri finalUri() {
        return FileProvider.getUriForFile(this, getPackageName() + ".provider", finalVideo);
    }
    private void shareFinal() {
        if (finalVideo == null || !finalVideo.exists()) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("video/mp4"); i.putExtra(Intent.EXTRA_STREAM, finalUri());
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Compartilhar vídeo unificado"));
    }
    private void saveFinal() {
        if (finalVideo == null || !finalVideo.exists()) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "VideoCutter_unificado.mp4");
                values.put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VideoCutter3s");
                Uri uri = getContentResolver().insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Não foi possível criar o arquivo.");
                try (java.io.InputStream in = new java.io.FileInputStream(finalVideo);
                     java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[8192]; int len; while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }
            } else {
                File target = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "VideoCutter3s");
                if (!target.exists()) target.mkdirs();
                java.nio.file.Files.copy(finalVideo.toPath(), new File(target, "VideoCutter_unificado.mp4").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Toast.makeText(this, "Vídeo salvo em Filmes/VideoCutter3s", Toast.LENGTH_LONG).show();
        } catch (Exception e) { Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }
}
