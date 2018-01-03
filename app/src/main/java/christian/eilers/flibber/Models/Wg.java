package christian.eilers.flibber.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Wg {

    private String name;
    private String key;
    private String picPath;
    private Date timestamp;

    public Wg() {
    }

    public Wg(String name, String key, String picPath) {
        this.name = name;
        this.key = key;
        this.picPath = picPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
