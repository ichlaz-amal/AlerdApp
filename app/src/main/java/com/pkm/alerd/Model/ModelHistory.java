package com.pkm.alerd.Model;

import com.google.firebase.database.IgnoreExtraProperties;

@SuppressWarnings("unused") @IgnoreExtraProperties
public class ModelHistory {

    public String doneDetail;
    public int seconds;

    public ModelHistory() {}

    public ModelHistory(String doneDetail, int seconds) {
        this.doneDetail = doneDetail;
        this.seconds = seconds;
    }
}