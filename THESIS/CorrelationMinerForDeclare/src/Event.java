import java.util.HashMap;
import java.util.List;

/**
 * Created by volodymyr leno on 26.12.2017.
 */

public class Event {
    Integer caseID;
    String activityName;
    String timestamp;
    HashMap<String, String> payload;

    public Event(List<String> attributes, String[] values){
        this.caseID = Integer.parseInt(values[0]);
        this.activityName = values[1];
        this.timestamp = values[2];
        payload = new HashMap<>();
        for(int i = 3; i < values.length; i++)
            payload.put(attributes.get(i), values[i]);
    }

    public Event(Integer caseID, String activityName, String timestamp) {
        this.caseID = caseID;
        this.activityName = activityName;
        this.timestamp = timestamp;
        payload = new HashMap<>();
    }

    public Event(Event event){
        this.caseID = event.caseID;
        this.activityName = event.activityName;
        this.timestamp = event.timestamp;
        payload = new HashMap<>();
    }

    public String toString() {
        return "(" + this.caseID + ", " + this.activityName + ", " + this.timestamp + ", " + payload + ")";
    }
}