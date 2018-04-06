package christian.eilers.flibber.Models;

import java.util.ArrayList;
import java.util.Date;

public class TaskModel {

    private String title;
    private long frequenz, points;
    private ArrayList<String> involvedIDs;
    private Date timestamp;
    private boolean ordered;

    public TaskModel() {}

    public TaskModel(String title, long frequenz, long points, ArrayList<String> involvedIDs, boolean ordered, Date timestamp) {
        this.title = title;
        this.frequenz = frequenz;
        this.points = points;
        this.involvedIDs = involvedIDs;
        this.ordered = ordered;
        this.timestamp = timestamp;
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

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
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
