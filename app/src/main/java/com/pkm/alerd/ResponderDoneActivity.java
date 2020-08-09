package com.pkm.alerd;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ResponderDoneActivity extends AppCompatActivity {

    private TextView doneText;
    private EditText detail;
    private Button submit;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.done_responder);

        final String costumerID = getIntent().getStringExtra("costumerID");

        doneText = findViewById(R.id.done_text);
        detail = findViewById(R.id.my_detail_done);
        progressBar = findViewById(R.id.done_progress_bar);

        submit = findViewById(R.id.submit_done);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress(true);
                final String detailText = detail.getText().toString();
                if (!detailText.isEmpty()) {
                    final DatabaseReference from = FirebaseDatabase.getInstance().getReference()
                            .child("requests").child(costumerID);
                    from.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            DatabaseReference to = FirebaseDatabase.getInstance().getReference()
                                    .child("history").child("requests").child(costumerID);
                            to.setValue(dataSnapshot.getValue());
                            to.child("doneDetail").setValue(detailText);

                            StorageReference pict = FirebaseStorage.getInstance().getReference()
                                    .child("Events").child(costumerID);
                            pict.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Intent intent = new Intent(getApplicationContext(), ResponderMainActivity.class);
                                    from.setValue(null); startActivity(intent); finish();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                    showProgress(false);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), ResponderMainActivity.class);
        startActivity(intent); finish();
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

        if (show) {
            doneText.setVisibility(View.GONE);
            detail.setVisibility(View.GONE);
            submit.setVisibility(View.GONE);
        } else {
            doneText.setVisibility(View.VISIBLE);
            detail.setVisibility(View.VISIBLE);
            submit.setVisibility(View.VISIBLE);
            detail.setError("Cannot be Empty");
        }
    }
}
