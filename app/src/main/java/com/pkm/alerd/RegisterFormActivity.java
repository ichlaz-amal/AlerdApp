package com.pkm.alerd;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterFormActivity extends AppCompatActivity {

    private EditText nameField, ktpField, emailField, addressField, instituteField;
    private String userStatus, name, ktp, email, address, institute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_form);

        userStatus = getIntent().getStringExtra("USER_STATUS");

        nameField = findViewById(R.id.name_field);
        ktpField = findViewById(R.id.ktp_field);
        emailField = findViewById(R.id.email_field);
        addressField = findViewById(R.id.address_field);
        instituteField = findViewById(R.id.institute_field);

        if (userStatus.equals("User")) {
            instituteField.setVisibility(View.GONE);
            TextView instituteText = findViewById(R.id.institute_text);
            instituteText.setVisibility(View.GONE);
        }

        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = nameField.getText().toString();
                ktp = ktpField.getText().toString();
                email = emailField.getText().toString();
                address = addressField.getText().toString();
                institute = instituteField.getText().toString();

                if (name.isEmpty()) {
                    nameField.setError("Cannot be Empty");
                } else if (ktp.isEmpty()) {
                    ktpField.setError("Cannot be Empty");
                } else if (email.isEmpty()) {
                    emailField.setError("Cannot be Empty");
                } else if (address.isEmpty()) {
                    emailField.setError("Cannot be Empty");
                } else if (institute.isEmpty() && instituteField.getVisibility() == View.VISIBLE) {
                    instituteField.setError("Cannot be Empty");
                } else {
                    submitForm(); goToMain();
                }
            }
        });
    }

    private void submitForm() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("accounts").child(user.getUid());

            ref.child("name").setValue(name);
            ref.child("ktp").setValue(ktp);
            ref.child("email").setValue(email);
            ref.child("address").setValue(address);
            ref.child("phone").setValue(user.getPhoneNumber());
            ref.child("status").setValue(userStatus);

            if (userStatus.equals("Responder")) {
                ref.child("institute").setValue(institute);
            }
        }
    }

    private void goToMain() {
        if (userStatus.equals("User")) {
            Intent intent = new Intent(getApplicationContext(), UserMainActivity.class);
            startActivity(intent); finish();
        } else if (userStatus.equals("Responder")) {
            Intent intent = new Intent(getApplicationContext(), ResponderMainActivity.class);
            startActivity(intent); finish();
        }
    }
}
