package christian.eilers.flibber.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class TaskModel implements Serializable {

    private long frequenz;
    private boolean ordered, allDay, fixedDay;
    private String key, title, description;
    private ArrayList<String> involvedIDs;
    private Date timestamp;


    public TaskModel() {}

    public TaskModel(String key, String title, String description, long frequenz, ArrayList<String> involvedIDs,
                     boolean ordered, boolean allDay, boolean fixedDay, Date timestamp) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.frequenz = frequenz;
        this.involvedIDs = involvedIDs;
        this.ordered = ordered;
        this.allDay = allDay;
        this.fixedDay = fixedDay;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    public boolean isFixedDay() {
        return fixedDay;
    }

    public void setFixedDay(boolean fixedDay) {
        this.fixedDay = fixedDay;
    }
}
