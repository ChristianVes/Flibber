package christian.eilers.flibber.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class StockProduct implements Serializable{

    private String key;
    private String name;
    private long price;
    private ArrayList<String> purchaserIDs;
    private ArrayList<String> involvedIDs;
    private Date timestamp;

    public StockProduct(){}

    public StockProduct(String key, String name, long price, ArrayList<String> purchaserIDs, ArrayList<String> involvedIDs) {
        this.key = key;
        this.name = name;
        this.price = price;
        this.purchaserIDs = purchaserIDs;
        this.involvedIDs = involvedIDs;
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

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public ArrayList<String> getPurchaserIDs() {
        return purchaserIDs;
    }

    public void setPurchaserIDs(ArrayList<String> purchaserIDs) {
        this.purchaserIDs = purchaserIDs;
    }

    public ArrayList<String> getInvolvedIDs() {
        return involvedIDs;
    }

    public void setInvolvedIDs(ArrayList<String> involvedIDs) {
        this.involvedIDs = involvedIDs;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
