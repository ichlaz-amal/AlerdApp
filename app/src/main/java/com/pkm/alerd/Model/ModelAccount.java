package com.pkm.alerd.Model;

import com.google.firebase.database.IgnoreExtraProperties;

@SuppressWarnings("unused") @IgnoreExtraProperties
public class ModelAccount {

    public String name;
    public String phone;
    public String status;
    public String institute;

    public ModelAccount() {}

    public ModelAccount(String name, String phone, String status, String institute) {
        this.name = name;
        this.phone = phone;
        this.status = status;
        this.institute = institute;
    }
}