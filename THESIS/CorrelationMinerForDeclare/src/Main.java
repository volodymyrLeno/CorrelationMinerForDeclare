import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String csvFile = "C:/Volodymyr/PhD/TARTU/Simple Log.csv";
        HashMap<Integer, List<Event>> cases = readLog(csvFile);
        //System.out.println(cases);
        List<Itemset> itemsets = frequentItemsetsMining(cases, 4);
        //System.out.println(itemsets);
        HashMap<Itemset, List<FeatureVector>> featureVectors = extractFeatureVectors(cases, itemsets);

        getCorrelations(featureVectors);
    }

    public static HashMap<Integer, List<Event>> readLog(String path){
        HashMap<Integer, List<Event>> cases = new HashMap<>();
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

    public static List<Itemset> frequentItemsetsMining(HashMap<Integer, List<Event>> cases, Integer threshold){
        List<String> activities = new ArrayList<>();

        for(Integer key: cases.keySet()){
            cases.get(key).stream().filter(event -> !activities.contains(event.activityName)).
                    forEach(event -> activities.add(event.activityName));
        }

        List<Itemset> itemsets = new ArrayList<>();

        for(String activity: activities)
            itemsets.add(new Itemset(new ArrayList<>(Collections.singletonList(activity)), 0));

        Integer counter = 0;

        while(counter < 2){
            for(Itemset itemset: itemsets) {
                for (Integer key : cases.keySet()) {
                    List<Integer> idx1 = new ArrayList<>();
                    List<Integer> idx2 = new ArrayList<>();
                    Integer k = 0;
                    for (Event event : cases.get(key)) {
                        if (itemset.items.size() == 1) {
                            if (itemset.items.get(0).equals(event.activityName)) {
                                itemset.increaseFrequency();
                            }
                        } else {
                            if (itemset.items.get(0).equals(event.activityName)) idx1.add(k);
                            if (itemset.items.get(1).equals(event.activityName)) idx2.add(k);
                        }
                        k++;
                    }
                    if (idx1.size() > 0 && idx2.size() > 0) {
                        for(Integer i1: idx1){
                            for(Integer i2: idx2)
                                if(i2 > i1){
                                    itemset.increaseFrequency();
                                    idx2.remove(i2);
                                    break;
                                }
                        }
                    }
                }
            }

            itemsets.removeIf(itemset -> !itemset.isFrequent(threshold));
            counter++;

            List<Itemset> newItemsets = new ArrayList<>();

            for(int i = 0; i < itemsets.size(); i++)
            {
                for(int j = 0; j < itemsets.size(); j++)
                    if(i!=j){
                        List<String> merge = new ArrayList<>(itemsets.get(i).items);
                        merge.addAll(itemsets.get(j).items);
                        newItemsets.add(new Itemset(merge, 0));
                    }
            }
            if(counter < 2)
                itemsets = new ArrayList<>(newItemsets);
        }
        return itemsets;
    }

    public static HashMap<Itemset, List<FeatureVector>> extractFeatureVectors(HashMap<Integer, List<Event>> cases, List<Itemset> itemsets){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets){
            List<FeatureVector> pattern = new ArrayList<>();
            for(Integer key: cases.keySet()){
                List<Integer> idx1 = new ArrayList<>();
                List<Integer> idx2 = new ArrayList<>();
                Integer k = 0;
                for(Event event: cases.get(key)){
                    if (itemset.items.get(0).equals(event.activityName)) idx1.add(k);
                    if (itemset.items.get(1).equals(event.activityName)) idx2.add(k);
                    k++;
                }
                if(idx1.size() > 0 && idx2.size() > 0){
                    for(Integer i1: idx1){
                        for(Integer i2: idx2)
                            if(i2 > i1){
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                idx2.remove(i2);
                                break;
                            }
                    }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static void getCorrelations(HashMap<Itemset, List<FeatureVector>> featureVectorLists){
        for(Itemset itemset: featureVectorLists.keySet()){
            System.out.println("\n" + itemset.items + "\n");
            //System.out.println(featureVectorLists.get(itemset) + "\n");
            List<Cluster> clustersTo = Clustering.clustering(featureVectorLists.get(itemset),2);
            clustersTo.forEach(Cluster::giveLabels);
            for(Cluster cluster: clustersTo){
                System.out.println(cluster.rules);
                System.out.println(cluster.elements);
            }
            /*
            List<Cluster> clustersFrom = DecisionTree.id3(featureVectorLists.get(itemset));

            for (Cluster clusterTo : clustersTo) {
                clustersFrom.stream().filter(clusterFrom -> clusterTo.label.equals(clusterFrom.label)).
                        forEach(clusterFrom -> System.out.println(clusterFrom.rules + " =>" + clusterTo.rules));
            }
            */
        }
        return;
    }
}