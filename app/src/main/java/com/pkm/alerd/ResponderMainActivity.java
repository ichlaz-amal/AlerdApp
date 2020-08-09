package com.pkm.alerd;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pkm.alerd.Model.ModelAccount;
import com.pkm.alerd.Model.ModelRequest;

import java.util.Objects;

public class ResponderMainActivity extends AppCompatActivity implements View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener, ResponderMapFragment.OnGetCostumerListener {

    private static final int STATE_WAITING = 1;
    private static final int STATE_VALIDATION = 2;
    private static final int STATE_FIND_AGAIN = 3;
    private static final int STATE_ON_SERVICE = 4;

    private double mDistance;
    private String mCostumerID;

    private Button yesButton;
    private Button noButton;
    private Button doneButton;

    private View appBarMain;
    private View waitingResponder;
    private View progressBar;
    private View infoPanel;

    private ImageView emergencyCapture;
    private TextView emergencyDistance;
    private TextView textInfo;
    private TextView userInfo;

    private TextView timeMainView;
    private TextView timeWaitingView;

    private String userName;
    private String userPhone;
    private String userDetail;

    private boolean startRun = false;
    private int seconds;

    public static DatabaseReference mRef;
    public static FirebaseUser mUser;
    public static boolean onService = false;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            seconds = savedInstanceState.getInt("seconds");
            startRun = savedInstanceState.getBoolean("startRun");
        }

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.responder_progress_bar);
        appBarMain = findViewById(R.id.app_bar_main);
        waitingResponder = findViewById(R.id.waiting_responder);

        emergencyCapture = findViewById(R.id.emergency_capture);
        emergencyDistance = findViewById(R.id.emergency_distance);
        textInfo = findViewById(R.id.user_text_info);

        yesButton = findViewById(R.id.yes_button);
        noButton = findViewById(R.id.no_button);
        doneButton = findViewById(R.id.done_button);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);
        doneButton.setOnClickListener(this);

        infoPanel = findViewById(R.id.info_panel);
        userInfo = findViewById(R.id.costumer_info);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        Button call = findViewById(R.id.call_button);
        Button message = findViewById(R.id.message_button);
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
                .replace(R.id.fragment_container, ResponderMapFragment.newInstance()).commit();

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("accounts");
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        String token = Objects.requireNonNull(task.getResult()).getToken();
                        dbRef.child(mUser.getUid()).child("token").setValue(token);
                    }
                });

        timeMainView = findViewById(R.id.time_main);
        timeWaitingView = findViewById(R.id.time_waiting_responder);
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
        if (!onService) {
            updateUI(STATE_WAITING);
        }
    }

    @Override
    public void onProcessCostumer(String costumerID, double distance, final boolean noValidation) {
        mCostumerID = costumerID;
        mDistance = distance;

        mRef = FirebaseDatabase.getInstance().getReference().child("requests").child(mCostumerID);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelRequest value = dataSnapshot.getValue(ModelRequest.class);
                if (value != null) {
                    if (noValidation) {
                        ResponderMapFragment.latLngUser = new LatLng(value.uLatitude, value.uLongitude);
                        userDetail = value.detail;
                        getResponderInfo(mCostumerID);

                        seconds = value.seconds; startRun = true;
                        timeMainView.setVisibility(View.VISIBLE);
                    } else if (value.status.equals("Taken")) {
                        ResponderMapFragment.repeat = true;
                    } else {
                        ResponderMapFragment.latLngUser = new LatLng(value.uLatitude, value.uLongitude);
                        userDetail = value.detail;
                        updateUI(STATE_VALIDATION);

                        seconds = value.seconds; startRun = true;
                        timeMainView.setVisibility(View.VISIBLE);
                        timeWaitingView.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getResponderInfo(String ID) {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference()
                .child("accounts").child(ID);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelAccount value = dataSnapshot.getValue(ModelAccount.class);
                if (value != null) {
                    userName = value.name;
                    userPhone = value.phone;
                    updateUI(STATE_ON_SERVICE);
                    onService = true;
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
        switch(uiState) {
            case STATE_WAITING:
                appBarMain.setVisibility(View.GONE);
                waitingResponder.setVisibility(View.VISIBLE);

                emergencyCapture.setVisibility(View.GONE);
                emergencyDistance.setVisibility(View.GONE);
                yesButton.setVisibility(View.GONE);
                noButton.setVisibility(View.GONE);
                showProgress(true);
                break;
            case STATE_VALIDATION:
                StorageReference pict = FirebaseStorage.getInstance().getReference()
                        .child("Events").child(mCostumerID);
                Glide.with(this).load(pict).apply(RequestOptions.skipMemoryCacheOf(true))
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .into(emergencyCapture);
                emergencyCapture.setVisibility(View.VISIBLE);

                @SuppressLint("DefaultLocale")
                String vText = String.format("Distance: %.2f km (%s)", mDistance, userDetail);
                emergencyDistance.setText(vText);
                textInfo.setText(R.string.got_costumer);
                emergencyDistance.setVisibility(View.VISIBLE);

                yesButton.setVisibility(View.VISIBLE);
                noButton.setVisibility(View.VISIBLE);
                showProgress(false);
                break;
            case STATE_FIND_AGAIN:
                emergencyCapture.setVisibility(View.GONE);
                emergencyDistance.setVisibility(View.GONE);
                textInfo.setText(R.string.getting_costumer);

                yesButton.setVisibility(View.GONE);
                noButton.setVisibility(View.GONE);
                showProgress(true);
                break;
            case STATE_ON_SERVICE:
                ResponderMapFragment.mGoogleMap.addMarker(new MarkerOptions()
                        .position(ResponderMapFragment.latLngUser));
                appBarMain.setVisibility(View.VISIBLE);
                waitingResponder.setVisibility(View.GONE);
                infoPanel.setVisibility(View.VISIBLE);
                doneButton.setVisibility(View.VISIBLE);

                String uText = "Pemanggil: " + userName + " (" + userDetail + ")";
                userInfo.setText(uText);
                showProgress(false);
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

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.yes_button:
                ResponderMapFragment.submitLocation();
                mRef.child("rID").setValue(mUser.getUid());
                mRef.child("status").setValue("Taken");
                getResponderInfo(mCostumerID);
                break;
            case R.id.no_button:
                updateUI(STATE_FIND_AGAIN);
                ResponderMapFragment.getClosestCostumer(mCostumerID);
                break;
            case R.id.call_button:
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + userPhone));
                startActivity(intent);
                break;
            case R.id.message_button:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + userPhone));
                startActivity(intent);
                break;
            case R.id.done_button:
                onService = false;
                intent = new Intent(getApplicationContext(), ResponderDoneActivity.class);
                intent.putExtra("costumerID", mCostumerID);
                startActivity(intent); finish();
                break;
        }
    }
}
