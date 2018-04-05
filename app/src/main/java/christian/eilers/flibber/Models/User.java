package christian.eilers.flibber.Models;

import java.io.Serializable;

public class User implements Serializable {

    private String name;
    private String email;
    private String userID;
    private String picPath;
    private String deviceToken;
    private long money;
    private long points;

    public User() {}

    public User(String name, String email, String userID, String picPath, String deviceToken, long money, long points) {
        this.name = name;
        this.email = email;
        this.userID = userID;
        this.picPath = picPath;
        this.deviceToken = deviceToken;
        this.money = money;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
