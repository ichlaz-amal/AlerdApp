package com.pkm.alerd;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pkm.alerd.Model.ModelHistory;

public class UserDoneActivity extends AppCompatActivity {

    private boolean complete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.done_user);

        final TextView emergencyAction = findViewById(R.id.emergency_action);
        final TextView emergencyTime = findViewById(R.id.emergency_time);

        String costumerID = getIntent().getStringExtra("costumerID");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("history").child("requests").child(costumerID);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ModelHistory value = dataSnapshot.getValue(ModelHistory.class);
                    if (value != null) {
                        int minute = (value.seconds % 3600) / 60;
                        String detailText = "Kondisi sekarang: " + value.doneDetail;
                        String minuteText = "Waktu response: " + minute + " menit";
                        emergencyAction.setText(detailText);
                        emergencyTime.setText(minuteText);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Button button = findViewById(R.id.complete);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserMainActivity.class);
                startActivity(intent); finish(); complete = true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (complete) {
            super.onBackPressed();
        }
    }
}
