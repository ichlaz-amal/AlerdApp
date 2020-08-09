package com.pkm.alerd;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pkm.alerd.Model.ModelAccount;
import com.pkm.alerd.Model.ModelRequest;

import java.io.ByteArrayOutputStream;

public class UserMainActivity extends AppCompatActivity implements View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener {

    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_VALIDATE = 2;
    private static final int STATE_WAITING = 3;
    private static final int STATE_TAKEN = 4;
    private static final int STATE_SUBMIT_CANCELLED = 5;

    private FloatingActionButton fab;
    private TextView responderInfo;
    private Uri mFile;

    private View appBarMain;
    private View waitingUser;
    private View progressBar;
    private View infoPanel;

    private TextView timeMainView;
    private TextView timeWaitingView;

    private TextView textInfo;
    private ImageView myCapture;
    private EditText myDetail;
    private Button submit;

    private String responderName;
    private String responderPhone;
    private String responderInstitute;

    private int seconds;
    private boolean startRun = false;
    private boolean taken = false;
    private boolean takenDone = false;

    public static boolean locationChanged = false;
    public static boolean emergencySubmitted = false;

    public static DatabaseReference mDatabaseReference;
    public static StorageReference mStorageReference;
    public static FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            seconds = savedInstanceState.getInt("seconds");
            startRun = savedInstanceState.getBoolean("startRun");
        }

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("requests").child(mUser.getUid());
        mStorageReference = FirebaseStorage.getInstance().getReference()
                .child("Events").child(mUser.getUid());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        appBarMain = findViewById(R.id.app_bar_main);
        waitingUser = findViewById(R.id.waiting_user);
        progressBar = findViewById(R.id.user_progress_bar);

        textInfo = findViewById(R.id.capture_text_info);
        myCapture = findViewById(R.id.my_capture);
        myDetail = findViewById(R.id.my_detail);

        infoPanel = findViewById(R.id.info_panel);
        responderInfo = findViewById(R.id.costumer_info);

        fab = findViewById(R.id.fab);
        submit = findViewById(R.id.submit_emergency);
        Button call = findViewById(R.id.call_button);
        Button message = findViewById(R.id.message_button);

        fab.setOnClickListener(this);
        submit.setOnClickListener(this);
        call.setOnClickListener(this);
        message.setOnClickListener(this);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, UserMapFragment.newInstance()).commit();

        timeMainView = findViewById(R.id.time_main);
        timeWaitingView = findViewById(R.id.time_waiting_user);

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                String time = hours + " : " + minutes + " : " + secs;
                timeMainView.setText(time);
                timeWaitingView.setText(time);
                if (startRun) {
                    seconds++;
                    if (emergencySubmitted) {
                        mDatabaseReference.child("seconds").setValue(seconds);
                    }
                    timeMainView.setVisibility(View.VISIBLE);
                    timeWaitingView.setVisibility(View.VISIBLE);
                } handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle saveInstanceState){
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putInt("seconds", seconds);
        saveInstanceState.putBoolean("startRun", startRun);
    }

    @Override
    public void onStart() {
        super.onStart();
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 3) {
                    ModelRequest value = dataSnapshot.getValue(ModelRequest.class);
                    if (value != null && value.status.equals("Taken")) {
                        UserMapFragment.latLngResponder = new LatLng(value.rLatitude, value.rLongitude);
                        UserMapFragment.mGoogleMap.clear();
                        UserMapFragment.mGoogleMap.addMarker(new MarkerOptions()
                                .position(UserMapFragment.latLngResponder));
                        if (!taken) {
                            seconds = value.seconds; emergencySubmitted = true;
                            startRun = true; taken = true; getResponderInfo(value.rID);
                        }
                    }
                } else if (!dataSnapshot.exists() && takenDone){
                    resetAttribute();
                    Intent intent = new Intent(getApplicationContext(), UserDoneActivity.class);
                    intent.putExtra("costumerID", mUser.getUid());
                    startActivity(intent); finish();
                } else if (takenDone) {
                    updateUI(STATE_SUBMIT_CANCELLED);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void resetAttribute() {
        seconds = 0; startRun = false;
        taken = false; takenDone = false;
        locationChanged = false; emergencySubmitted = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            assert extras != null; Bitmap bitmap = (Bitmap) extras.get("data");

            assert bitmap != null; mFile = getImageUri(getApplicationContext(), bitmap);
            Glide.with(this).load(mFile).into(myCapture);
            updateUI(STATE_VALIDATE);
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(),
                bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void submitEmergency(final String detail) {
        mStorageReference.putFile(mFile).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mDatabaseReference.child("status").setValue("Waiting");
                mDatabaseReference.child("detail").setValue(detail);
                emergencySubmitted = true;
                UserMapFragment.submitUserLatLng();
            }
        });
    }

    private void getResponderInfo(String ID) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("accounts").child(ID);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelAccount value = dataSnapshot.getValue(ModelAccount.class);
                if (value != null) {
                    responderName = value.name;
                    responderPhone = value.phone;
                    responderInstitute = value.institute;
                    updateUI(STATE_TAKEN);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (waitingUser.getVisibility() == View.VISIBLE) {
            updateUI(STATE_SUBMIT_CANCELLED);
            updateUI(STATE_INITIALIZED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
    }

    private void updateUI(int uiState) {
        showProgress(false);
        switch (uiState) {
            case STATE_INITIALIZED:
                appBarMain.setVisibility(View.VISIBLE);
                waitingUser.setVisibility(View.GONE);
                infoPanel.setVisibility(View.GONE);
                break;
            case STATE_VALIDATE:
                appBarMain.setVisibility(View.GONE);
                waitingUser.setVisibility(View.VISIBLE);
                infoPanel.setVisibility(View.GONE);

                textInfo.setText(R.string.enter_submit_detail);
                myCapture.setVisibility(View.VISIBLE);
                myDetail.setVisibility(View.VISIBLE);
                submit.setVisibility(View.VISIBLE);
                break;
            case STATE_WAITING:
                appBarMain.setVisibility(View.GONE);
                waitingUser.setVisibility(View.VISIBLE);
                infoPanel.setVisibility(View.GONE);

                textInfo.setText(R.string.waiting_responder);
                myCapture.setVisibility(View.GONE);
                myDetail.setVisibility(View.GONE);
                submit.setVisibility(View.GONE);
                showProgress(true);
                break;
            case STATE_TAKEN:
                String text = "Responder: " + responderName + " (" + responderInstitute + ")";
                responderInfo.setText(text);

                appBarMain.setVisibility(View.VISIBLE);
                waitingUser.setVisibility(View.GONE);
                infoPanel.setVisibility(View.VISIBLE);
                fab.setVisibility(View.GONE);
                takenDone = true;
                break;
            case STATE_SUBMIT_CANCELLED:
                mStorageReference.delete();
                emergencySubmitted = false; startRun = false;
                timeMainView.setVisibility(View.GONE);
                timeWaitingView.setVisibility(View.GONE);
                mDatabaseReference.setValue(null);
                break;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            // Handle the camera action
        } else if (id == R.id.nav_info) {
            Intent intent = new Intent(getApplicationContext(), PPGDActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_hospital) {

        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_logout) {
            if (!taken) {
                updateUI(STATE_SUBMIT_CANCELLED);
            } FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent); finish();
            Toast.makeText(UserMainActivity.this, R.string.logout_success,
                    Toast.LENGTH_LONG).show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.fab:
                if (locationChanged) {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);
                } break;
            case R.id.submit_emergency:
                String detail = myDetail.getText().toString();
                if (!detail.isEmpty()) {
                    updateUI(STATE_WAITING);
                    seconds = 0; startRun = true;
                    submitEmergency(detail);
                } else {
                    myDetail.setError("Cannot be Empty");
                } break;
            case R.id.call_button:
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + responderPhone));
                startActivity(intent); break;
            case R.id.message_button:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + responderPhone));
                startActivity(intent); break;
        }
    }
}
