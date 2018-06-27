import java.util.HashMap;
import java.util.List;

/**
 * Created by volodymyr leno on 26.12.2017.
 */

public class Event {
    String caseID;
    String activityName;
    private String timestamp;
    HashMap<String, String> payload;

    Event(List<String> attributes, String[] values){
        this.caseID = values[0];
        this.activityName = values[1];
        this.timestamp = values[2];
        payload = new HashMap<>();
        for(int i = 3; i < values.length; i++)
            if(!values[i].equals(""))
                payload.put(attributes.get(i), values[i]);
    }

    public Event(String caseID, String activityName, String timestamp) {
        this.caseID = caseID;
        this.activityName = activityName;
        this.timestamp = timestamp;
        payload = new HashMap<>();
    }

    public Event(Event event){
        this.caseID = event.caseID;
        this.activityName = event.activityName;
        this.timestamp = event.timestamp;
        this.payload = new HashMap<>(event.payload);
    }

    public String toString() {
        return "(" + this.caseID + ", " + this.activityName + ", " + this.timestamp + ", " + payload + ")";
    }
}