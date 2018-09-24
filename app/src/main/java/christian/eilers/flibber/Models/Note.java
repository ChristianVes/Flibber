package christian.eilers.flibber.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Note implements Serializable{

    private String title;
    private String description;
    private String userID;
    private String imagePath;
    private String key;
    private long commentsCount;
    private Date timestamp;
    private ArrayList<String> subscribers;

    public Note() {

    }

    public Note(String title, String description, String userID, String imagePath, String key, ArrayList<String> subscribers) {
        this.title = title;
        this.description = description;
        this.userID = userID;
        this.imagePath = imagePath;
        this.key = key;
        this.subscribers = subscribers;
        this.commentsCount = 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getKey() {
        return key;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ArrayList<String> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(ArrayList<String> subscribers) {
        this.subscribers = subscribers;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
