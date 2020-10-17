package com.nouserinterface.entities;

/**
 * Created by Jhh on 2020-09-08.
 */
public class User {
    public long id;
    public String name;
    public String screenName;
    public  String location;
    public String description;
    public boolean isProtected;
    public long followersCount;
    public long friendsCount;
    public long statusesCount;


    public User(){

    }

    @Override
    public String toString(){
        return id + " " + name;
    }
}
