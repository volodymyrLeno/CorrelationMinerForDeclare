import java.util.ArrayList;
import java.util.List;

/**
 * Created by volodymyr leno on 15.01.2018.
 */
public class Cluster {
    String label;
    List<String> rules;
    List<FeatureVector> elements;

    public Cluster(String label, List<String> rules, List<FeatureVector> elements){
        this.label = label;
        this.rules = new ArrayList<>(rules);
        this.elements = new ArrayList<>(elements);
    }

    public void giveLabels(){
        for(FeatureVector element: elements)
            element.label = label;
    }
}