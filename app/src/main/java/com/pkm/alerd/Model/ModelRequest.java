package com.pkm.alerd.Model;

import com.google.firebase.database.IgnoreExtraProperties;

@SuppressWarnings("unused") @IgnoreExtraProperties
public class ModelRequest {

    public String status;
    public String rID;
    public String detail;
    public double rLatitude;
    public double rLongitude;
    public double uLatitude;
    public double uLongitude;
    public int seconds;

    public ModelRequest() {}

    public ModelRequest(String status, String rID, String detail, double rLatitude, double rLongitude,
                        double uLatitude, double uLongitude, int seconds) {
        this.status = status;
        this.rID = rID;
        this.detail = detail;
        this.rLatitude = rLatitude;
        this.rLongitude = rLongitude;
        this.uLatitude = uLatitude;
        this.uLongitude = uLongitude;
        this.seconds = seconds;
    }
}
