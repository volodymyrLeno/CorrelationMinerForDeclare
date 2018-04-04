import java.util.HashMap;
import java.util.Objects;

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
        this.from = new HashMap<>();
        this.to = new HashMap<>();
        for(String attribute: from.keySet())
            this.from.put(attribute, from.get(attribute));
        if(to != null){
            for(String attribute: to.keySet())
                this.to.put(attribute, to.get(attribute));
        }
        else
            this.to = null;
    }

    public String toString(){
        return from + " => " + to + " label = " + label;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FeatureVector)) {
            return false;
        }
        FeatureVector fv = (FeatureVector) o;
        return fv.from.equals(this) && fv.to.equals(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}