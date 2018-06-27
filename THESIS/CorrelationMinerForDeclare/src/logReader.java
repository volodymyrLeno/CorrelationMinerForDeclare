import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class logReader {
    public static HashMap<String, List<Event>> readLog(String path){
        HashMap<String, List<Event>> cases = new HashMap<>();
        List<Event> events = new ArrayList();
        List<String> attributes = new ArrayList();
        int counter = 0;
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((line = br.readLine()) != null) {

                String[] row = line.split("[,;]");
                if(counter == 0) {
                    counter++;
                    Collections.addAll(attributes, row);
                }
                else {
                    events.add(new Event(attributes, row));
                    counter++;
                }
            }
            for(Event event: events)
                if (!cases.containsKey(event.caseID)) {
                    List<Event> list = new ArrayList<>();
                    list.add(event);
                    cases.put(event.caseID, list);
                } else cases.get(event.caseID).add(event);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return cases;
    }
}
