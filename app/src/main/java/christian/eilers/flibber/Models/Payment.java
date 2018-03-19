package christian.eilers.flibber.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class Payment implements Serializable{

    private String key, title, description, payerID, creatorID;
    private HashMap<String, Boolean> involvedIDs;
    private int price;
    private Date timestamp;

    public Payment(){}

    public Payment(String key, String title, String description, String payerID, String creatorID, HashMap<String, Boolean> involvedIDs, int price) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.payerID = payerID;
        this.creatorID = creatorID;
        this.involvedIDs = involvedIDs;
        this.price = price;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getPayerID() {
        return payerID;
    }

    public void setPayerID(String payerID) {
        this.payerID = payerID;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public HashMap<String, Boolean> getInvolvedIDs() {
        return involvedIDs;
    }

    public void setInvolvedIDs(HashMap<String, Boolean> involvedIDs) {
        this.involvedIDs = involvedIDs;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
