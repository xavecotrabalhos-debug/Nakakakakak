package com.example.appteste;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int clicks = 0;
    private TextView counter;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32,32,32,32);
        layout.setBackgroundColor(Color.rgb(245,247,250));
        TextView title = new TextView(this);
        title.setText("Meu primeiro APK!"); title.setTextColor(Color.rgb(25,45,80)); title.setTextSize(28); title.setGravity(Gravity.CENTER);
        layout.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView desc = new TextView(this);
        desc.setText("Se você está vendo esta tela, o aplicativo foi instalado e abriu corretamente."); desc.setTextColor(Color.DKGRAY); desc.setTextSize(16); desc.setGravity(Gravity.CENTER); desc.setPadding(8,16,8,24);
        layout.addView(desc);
        counter = new TextView(this); counter.setText("Botão clicado: 0 vez(es)"); counter.setTextColor(Color.BLACK); counter.setTextSize(18); counter.setGravity(Gravity.CENTER); counter.setPadding(0,8,0,20); layout.addView(counter);
        Button button = new Button(this); button.setText("Testar botão"); button.setOnClickListener(v -> { clicks++; counter.setText("Botão clicado: " + clicks + " vez(es)"); }); layout.addView(button);
        setContentView(layout);
    }
}
