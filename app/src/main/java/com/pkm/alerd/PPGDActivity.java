package com.pkm.alerd;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class PPGDActivity extends AppCompatActivity {

    private int pointer = 0;
    private int[] drawable = {R.drawable.capture1, R.drawable.capture2, R.drawable.capture3,
            R.drawable.capture4, R.drawable.capture5, R.drawable.capture6, R.drawable.capture7};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppgd);

        final ImageView imageView = findViewById(R.id.help_image);
        Button prev = findViewById(R.id.prev_button);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pointer > 0) {
                    pointer = pointer - 1;
                    imageView.setImageResource(drawable[pointer]);
                }
            }
        });
        Button next = findViewById(R.id.next_button);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pointer < 6) {
                    pointer = pointer + 1;
                    imageView.setImageResource(drawable[pointer]);
                }
            }
        });
    }
}
