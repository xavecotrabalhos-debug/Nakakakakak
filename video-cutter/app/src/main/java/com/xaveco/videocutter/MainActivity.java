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
    private final ArrayList<File> finishedClips = new ArrayList<>();
    private TextView status;
    private Button shareButton, pickButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL); page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(28,48,28,28); page.setBackgroundColor(0xFF101820);
        TextView title = new TextView(this); title.setText("VIDEO CUTTER 3s"); title.setTextSize(27); title.setTextColor(0xFFFFFFFF); title.setGravity(Gravity.CENTER); page.addView(title,match());
        TextView subtitle = new TextView(this); subtitle.setText("Corte automático dos 3 primeiros segundos de cada vídeo."); subtitle.setTextSize(16); subtitle.setTextColor(0xFFCFD8E3); subtitle.setGravity(Gravity.CENTER); subtitle.setPadding(0,18,0,28); page.addView(subtitle,match());
        pickButton = new Button(this); pickButton.setText("Selecionar vídeos"); pickButton.setOnClickListener(v -> openPicker()); page.addView(pickButton,match());
        status = new TextView(this); status.setText("Você pode selecionar vários vídeos de uma vez."); status.setTextColor(0xFFFFFFFF); status.setTextSize(15); status.setGravity(Gravity.CENTER); status.setPadding(0,24,0,24); page.addView(status,match());
        shareButton = new Button(this); shareButton.setText("Enviar vídeos recortados"); shareButton.setEnabled(false); shareButton.setOnClickListener(v -> shareClips()); page.addView(shareButton,match());
        TextView note = new TextView(this); note.setText("O corte é feito no próprio celular. Os vídeos originais não são alterados."); note.setTextColor(0xFF9EABB8); note.setTextSize(13); note.setGravity(Gravity.CENTER); note.setPadding(0,30,0,0); page.addView(note,match());
        setContentView(page);
    }
    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1,-2); }
    private void openPicker() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("video/*"); intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); startActivityForResult(intent,PICK_VIDEOS);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_VIDEOS||resultCode!=RESULT_OK||data==null) return;
        ArrayList<Uri> selected=new ArrayList<>();
        if(data.getClipData()!=null) for(int i=0;i<data.getClipData().getItemCount();i++) selected.add(data.getClipData().getItemAt(i).getUri());
        else if(data.getData()!=null) selected.add(data.getData());
        if(selected.isEmpty()) return;
        pickButton.setEnabled(false); shareButton.setEnabled(false); finishedClips.clear(); status.setText("Recortando "+selected.size()+" vídeo(s)...");
        executor.execute(() -> {
            ArrayList<File> outputs=new ArrayList<>(); int failures=0;
            File dir=new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),"VideoCutter3s"); if(!dir.exists()) dir.mkdirs();
            for(int i=0;i<selected.size();i++) try {
                File out=new File(dir,"clip_3s_"+System.currentTimeMillis()+"_"+i+".mp4"); VideoTrimmer.trim(this,selected.get(i),out,3_000_000L); outputs.add(out);
            } catch(Exception e) { failures++; }
            final int failed=failures;
            runOnUiThread(() -> {
                pickButton.setEnabled(true); finishedClips.clear(); finishedClips.addAll(outputs); shareButton.setEnabled(!outputs.isEmpty());
                String msg=outputs.size()+" vídeo(s) recortado(s) com sucesso."; if(failed>0) msg+=" "+failed+" falha(s)."; status.setText(msg);
                if(!outputs.isEmpty()) Toast.makeText(this,"Pronto! Toque em Enviar para escolher o destino.",Toast.LENGTH_LONG).show();
            });
        });
    }
    private void shareClips() {
        try {
            ArrayList<Uri> uris=new ArrayList<>(); for(File f:finishedClips) uris.add(FileProvider.getUriForFile(this,getPackageName()+".provider",f));
            Intent send=new Intent(Intent.ACTION_SEND_MULTIPLE); send.setType("video/mp4"); send.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris); send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send,"Enviar vídeos recortados para..."));
        } catch(Exception e) { Toast.makeText(this,"Erro ao compartilhar: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }
}
