package com.example.spring_seatreservation.common;


public class MyUser {
    long uid;
    String username;
    String password;
    long number;
    int type;

        // ✅ 无参构造（必须）
    public MyUser() {
    }

    // 有参构造
    public MyUser(long uid, String username, String password) {
        this.uid = uid;
        this.username = username;
        this.password = password;
    }
    public long getNumber() {
        return number;
    }

    public MyUser setNumber(long number) {
        this.number = number;
        return this;
    }

    public int getType() {
        return type;
    }

    public MyUser setType(int type) {
        this.type = type;
        return this;
    }

    public long getUid() {
        return uid;
    }

    public MyUser setUid(long uid) {
        this.uid = uid;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public MyUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public MyUser setPassword(String password) {
        this.password = password;
        return this;
    }
}
