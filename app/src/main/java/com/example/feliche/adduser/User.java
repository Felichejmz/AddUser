package com.example.feliche.adduser;

/**
 * Created by feliche on 14/12/15.
 */
public class User {
    private String account, password, userName, birthday, email, imei;

    public User() {
        account = null;
        password = null;
        userName = null;
        birthday = null;
        email = null;
        imei = null;
    }

    public User(String account, String password, String userName, String birthday, String email, String imei) {
        this.account = account;
        this.password = password;
        this.userName = userName;
        this.birthday = birthday;
        this.email = email;
        this.imei = imei;
    }

    public String getAccount() {
        return this.account;
    }

    public String getPassword() {
        return this.password;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public String getEmail() {
        return this.email;
    }

    public String getImei() {
        return this.imei;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }
}
