package com.pkm.alerd;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pkm.alerd.Model.ModelAccount;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PhoneAuthActivity";
    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_BEFORE_INITIALIZED = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGN_IN_FAILED = 5;
    private static final int STATE_SIGN_IN_SUCCESS = 6;

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private View mProgressView;
    private ViewGroup mSignInViews;
    private ViewGroup mPhoneNumberViews;
    private ViewGroup mVerificationViews;

    private EditText mPhoneNumberField;
    private EditText mVerificationField;

    private String userStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // Assign views
        mProgressView = findViewById(R.id.login_progress);
        mSignInViews = findViewById(R.id.sign_in_fields);
        mPhoneNumberViews = findViewById(R.id.phone_number_field);
        mVerificationViews = findViewById(R.id.code_verification_field);

        mPhoneNumberField = findViewById(R.id.field_phone_number);
        mVerificationField = findViewById(R.id.field_verification_code);

        Button mUserButton = findViewById(R.id.button_sign_in_user);
        Button mResponderButton = findViewById(R.id.button_sign_in_responder);
        Button mStartButton = findViewById(R.id.button_start_verification);
        Button mVerifyButton = findViewById(R.id.button_verify);
        Button mResendButton = findViewById(R.id.button_resend);

        // Assign click listeners
        mUserButton.setOnClickListener(this);
        mResponderButton.setOnClickListener(this);
        mStartButton.setOnClickListener(this);
        mVerifyButton.setOnClickListener(this);
        mResendButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseDatabase.getInstance().getReference().child("accounts");

        // Initialize phone auth callbacks
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + phoneAuthCredential);
                mVerificationInProgress = false;

                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, phoneAuthCredential);
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                mVerificationInProgress = false;

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    mPhoneNumberField.setError("Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show();
                }
                // Show a message and update the UI
                updateUI(STATE_VERIFY_FAILED);
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // Update UI
                updateUI(STATE_CODE_SENT);
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            showProgress(true);
            DatabaseReference ref = mRef.child(currentUser.getUid());
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ModelAccount value = dataSnapshot.getValue(ModelAccount.class);
                    if (value != null) {
                        userStatus = value.status; checkPermission();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (mVerificationViews.getVisibility() == View.VISIBLE) {
            updateUI(STATE_INITIALIZED);
        } else if (mPhoneNumberViews.getVisibility() == View.VISIBLE) {
            updateUI(STATE_BEFORE_INITIALIZED);
        } else if (mSignInViews.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber, 60, TimeUnit.SECONDS, this, mCallbacks);
        mVerificationInProgress = true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber, PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber, 60, TimeUnit.SECONDS, this, mCallbacks, token);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            updateUI(STATE_SIGN_IN_SUCCESS, user);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mVerificationField.setError("Invalid code.");
                            }
                            // Update UI
                            updateUI(STATE_SIGN_IN_FAILED);
                        }
                    }
                });
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = "+62" + mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }

        return true;
    }

    private void updateUI(int uiState) {
        updateUI(uiState, mAuth.getCurrentUser(), null);
    }

    private void updateUI(int uiState, FirebaseUser user) {
        updateUI(uiState, user, null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState, null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        switch (uiState) {
            case STATE_BEFORE_INITIALIZED:
                // User type options
                user = null;
                mSignInViews.setVisibility(View.VISIBLE);
                mPhoneNumberViews.setVisibility(View.GONE);
                mVerificationViews.setVisibility(View.GONE);
                break;

            case STATE_INITIALIZED:
                // Initialized state, show only the phone number field and start button
                mSignInViews.setVisibility(View.GONE);
                mPhoneNumberViews.setVisibility(View.VISIBLE);
                mVerificationViews.setVisibility(View.GONE);
                break;

            case STATE_CODE_SENT:
                // Code sent state, show the verification field, the verification field
                mVerificationViews.setVisibility(View.VISIBLE);
                Toast.makeText(LoginActivity.this, R.string.status_code_sent,
                        Toast.LENGTH_LONG).show();
                break;

            case STATE_VERIFY_FAILED:
                // Verification has failed, show all options
                mVerificationViews.setVisibility(View.VISIBLE);
                Toast.makeText(LoginActivity.this, R.string.status_verification_failed,
                        Toast.LENGTH_LONG).show();
                break;

            case STATE_VERIFY_SUCCESS:
                // Verification has succeeded, proceed to sign in
                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        mVerificationField.setText(cred.getSmsCode());
                    } else {
                        mVerificationField.setText(R.string.instant_validation);
                    }
                } break;

            case STATE_SIGN_IN_FAILED:
                // No-op, handled by sign-in check
                mSignInViews.setVisibility(View.VISIBLE);
                Toast.makeText(LoginActivity.this, R.string.status_sign_in_failed,
                        Toast.LENGTH_LONG).show();
                break;

            case STATE_SIGN_IN_SUCCESS:
                // No-op, handled by sign-in check
                break;
        }
        showProgress(false);
        if (user != null) {
            showProgress(true);
            validateUser(user);
        }
    }

    private void validateUser(FirebaseUser user) {
        DatabaseReference ref = mRef.child(user.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ModelAccount value = dataSnapshot.getValue(ModelAccount.class);
                    if (value != null && value.status.equals(userStatus)) {
                        checkPermission();
                    } else if (value != null) {
                        mAuth.signOut(); updateUI(STATE_BEFORE_INITIALIZED);
                        String text = getText(R.string.user_status_problem) + " " + value.status;
                        Toast.makeText(LoginActivity.this, text, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Intent intent = new Intent(getApplicationContext(), RegisterFormActivity.class);
                    intent.putExtra("USER_STATUS", userStatus);
                    startActivity(intent); finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkPermission() {
        boolean getPermission = true;
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA, Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                getPermission = false;
            }
        } if (!getPermission) {
            ActivityCompat.requestPermissions(this, permissions,1);
        } else {
            goToMain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goToMain();
        } else {
            super.onBackPressed();
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

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }); if (show) {
            mSignInViews.setVisibility(View.GONE);
            mPhoneNumberViews.setVisibility(View.GONE);
            mVerificationViews.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sign_in_user:
                userStatus = "User";
                updateUI(STATE_INITIALIZED);
                break;

            case R.id.button_sign_in_responder:
                userStatus = "Responder";
                updateUI(STATE_INITIALIZED);
                break;

            case R.id.button_start_verification:
                if (!validatePhoneNumber()) {
                    return;
                } showProgress(true);
                startPhoneNumberVerification("+62" + mPhoneNumberField.getText().toString());
                break;

            case R.id.button_verify:
                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                } showProgress(true);
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;

            case R.id.button_resend:
                showProgress(true);
                resendVerificationCode(mPhoneNumberField.getText().toString(), mResendToken);
                break;
        }
    }
}
