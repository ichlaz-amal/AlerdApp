package com.pkm.alerd;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pkm.alerd.Model.ModelRequest;

@SuppressWarnings("ConstantConditions")
public class ResponderMapFragment extends Fragment implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ResponderMapFragment";
    private static OnGetCostumerListener mCallback;

    private boolean cameraOnce = false;
    private boolean costumerFound = false;
    private GoogleApiClient mGoogleApiClient;
    private MapView mapView;

    public static boolean repeat = true;
    public static GoogleMap mGoogleMap;
    public static LatLng latLngResponder;
    public static LatLng latLngUser;

    public ResponderMapFragment() {
        // Required empty public constructor
    }
    public static ResponderMapFragment newInstance() {
        return new ResponderMapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = rootView.findViewById(R.id.mapView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGetCostumerListener) {
            mCallback = (OnGetCostumerListener) context;
        } else {
            throw new RuntimeException(context.toString() + "must implement OnGetCostumerListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "not have permission");
            return;
        }
        buildGoogleMapApiClient();
        mGoogleMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleMapApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        latLngResponder = new LatLng(location.getLatitude(), location.getLongitude());
        if (!cameraOnce) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLngResponder));
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(16));
            cameraOnce = true;
        } if (!costumerFound) {
            getClosestCostumer();
            costumerFound = true;
        } if (ResponderMainActivity.onService) {
            submitLocation();
        }
    }

    public interface OnGetCostumerListener {
        void onProcessCostumer(String costumerID, double distance, boolean noValidation);
    }

    public static void getClosestCostumer() {
        getClosestCostumer("");
    }

    public static void getClosestCostumer(final String costumerException) {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference().child("requests");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (repeat) {
                    repeat = false;
                    String costumerID = "";
                    double closestDistance = 0;
                    boolean noValidation = false;

                    for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                        String key = postSnapshot.getKey();
                        ModelRequest value = postSnapshot.getValue(ModelRequest.class);
                        double distance = getDistance(value.uLatitude, value.uLongitude,
                                latLngResponder.latitude, latLngResponder.longitude);

                        if (value.status.equals("Taken") && value.rID.equals(ResponderMainActivity.mUser.getUid())) {
                            closestDistance = distance;
                            costumerID = postSnapshot.getKey();
                            noValidation = true; break;
                        } else if (value.status.equals("Waiting") && !key.equals(costumerException) &&
                                (closestDistance == 0 || distance < closestDistance)) {
                            closestDistance = distance;
                            costumerID = postSnapshot.getKey();
                        }
                    } if (!costumerID.equals("")) {
                        mCallback.onProcessCostumer(costumerID, closestDistance, noValidation);
                    } else {
                        repeat = true;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dP = Math.toRadians(lat2 - lat1);
        double dL = Math.toRadians(lng2 - lng1);

        double a = Math.pow(Math.sin(dP / 2), 2) + Math.cos(p1) * Math.cos(p2) *
                Math.pow(Math.sin(dL / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    public static void submitLocation() {
        ResponderMainActivity.mRef.child("rLatitude").setValue(latLngResponder.latitude);
        ResponderMainActivity.mRef.child("rLongitude").setValue(latLngResponder.longitude);
    }
}