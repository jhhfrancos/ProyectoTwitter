package com.nouserinterface.entities;

/**
 * Created by Jhh on 2020-09-08.
 */
public class Tweet {
    public String createdAt;
    public long id;
    public String text;
    public boolean isRetweeted;
    public long retweetCount;
    public boolean isPossiblySensitive;
    public String lang;
    public Tweet retweetedStatus;
    public Tweet quotedStatus;
    public HashTag[] hashtagEntities;
    public User user;
    public int tweetsProfile;
    public int hashTagsUsed;

    public Tweet(){
        tweetsProfile = 0;
        hashTagsUsed= 0;
        hashtagEntities = null;
    }

    public class HashTag{
        public String text;
        public int start;
        public int end;
    }

    @Override
    public String toString(){
        return id +" "+ text;
    }
}

