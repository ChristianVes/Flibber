package christian.eilers.flibber.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class Article implements Serializable{

    private String key;
    private String name;
    private String userID;
    private boolean isPrivate;
    private Date timestamp;
    @Exclude
    private boolean isChecked; // Local Attribute (not added to the database)

    public Article(){}

    public Article(String key, String name, String userID, boolean isPrivate) {
        this.key = key;
        this.name = name;
        this.userID = userID;
        this.isPrivate = isPrivate;
        isChecked = false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
