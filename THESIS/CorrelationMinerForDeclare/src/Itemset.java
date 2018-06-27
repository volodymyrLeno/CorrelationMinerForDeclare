import java.util.ArrayList;
import java.util.List;

/**
 * Created by volodymyr leno on 26.12.2017.
 */

public class Itemset {
    List<String> items;
    private Integer frequency;

    Itemset(List<String> items, Integer frequency){
        this.items = new ArrayList<>(items);
        this.frequency = frequency;
    }

    public boolean isFrequent(Integer threshold){
        return frequency >= threshold;
    }

    public void increaseFrequency(){
        this.frequency += 1;
    }

    public String toString(){
        return "(" + items + "; frequency = " + frequency + ")";
    }
}