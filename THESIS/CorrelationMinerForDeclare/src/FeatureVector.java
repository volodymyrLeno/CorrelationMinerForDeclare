import java.util.HashMap;

/**
 * Created by volodymyr leno on 27.12.2017.
 */

public class FeatureVector {
    HashMap<String, String> from;
    HashMap<String,String> to;
    String label;

    public FeatureVector(Event from, Event to){
        this.from = from.payload;
        this.to = to.payload;
        this.label = null;
    }

    public FeatureVector(HashMap<String, String> from, HashMap<String,String> to){
        this.from = from;
        this.to = to;
    }

    public String toString(){
        return from + " => " + to + " label = " + label;
    }
}