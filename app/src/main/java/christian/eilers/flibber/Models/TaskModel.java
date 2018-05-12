package christian.eilers.flibber.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class TaskModel implements Serializable {

    private String key;
    private String title;
    private long frequenz;
    private ArrayList<String> involvedIDs;
    private Date timestamp;
    private boolean ordered;

    public TaskModel() {}

    public TaskModel(String key, String title, long frequenz, ArrayList<String> involvedIDs, boolean ordered, Date timestamp) {
        this.key = key;
        this.title = title;
        this.frequenz = frequenz;
        this.involvedIDs = involvedIDs;
        this.ordered = ordered;
        this.timestamp = timestamp;
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

    public long getFrequenz() {
        return frequenz;
    }

    public void setFrequenz(long frequenz) {
        this.frequenz = frequenz;
    }

    public ArrayList<String> getInvolvedIDs() {
        return involvedIDs;
    }

    public void setInvolvedIDs(ArrayList<String> involvedIDs) {
        this.involvedIDs = involvedIDs;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
}
