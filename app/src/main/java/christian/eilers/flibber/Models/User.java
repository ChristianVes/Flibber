package christian.eilers.flibber.Models;

public class User {

    private String name;
    private String userID;
    private String picPath;
    private double money;

    public User() {}

    public User(String name, String userID, String picPath, double money) {
        this.name = name;
        this.userID = userID;
        this.picPath = picPath;
        this.money = money;
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

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }
}
