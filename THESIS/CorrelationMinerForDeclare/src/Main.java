import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String csvFile = "C:/Volodymyr/PhD/TARTU/Log.csv";
        //String csvFile = "C:/Volodymyr/PhD/TARTU/B.csv";
        HashMap<String, List<Event>> cases = readLog(csvFile);

        //System.out.println(cases);

        //List<Itemset> itemsets = frequentItemsetsMining(cases, 4);
        //System.out.println(itemsets);
        //System.out.println("Total amount = " + itemsets.size());
        //HashMap<Itemset, List<FeatureVector>> featureVectors = extractFeatureVectors(cases, itemsets);

        //extractFeatureVectors(cases, 4, "chain precedence");
        HashMap<Itemset, List<FeatureVector>> featureVectors = extractFeatureVectors(cases, 1, "alternate precedence");

        for(Itemset itemset: featureVectors.keySet())
            System.out.println(itemset);

        getCorrelations(featureVectors, 2);
    }

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

    public static HashMap<Itemset, List<FeatureVector>> extractFeatureVectors(HashMap<String, List<Event>> cases, Integer threshold, String ruleType){
        List<String> activities = new ArrayList<>();

        for(String key: cases.keySet()){
            cases.get(key).stream().filter(event -> !activities.contains(event.activityName)).
                    forEach(event -> activities.add(event.activityName));
        }

        List<Itemset> itemsets = new ArrayList<>();
        for(String activity: activities)
            itemsets.add(new Itemset(new ArrayList<>(Collections.singletonList(activity)), 0));

        HashMap<Itemset, HashMap<String, List<Integer>>> index1 = new HashMap<>();
        HashMap<Itemset, HashMap<String, List<Integer>>> index2 = new HashMap<>();

        Integer counter = 0;

        while(counter < 2){
            for(Itemset itemset: itemsets) {
                HashMap<String, List<Integer>> idx1 = new HashMap();
                HashMap<String, List<Integer>> idx2 = new HashMap();
                for (String key : cases.keySet()) {
                    List<Integer> id1 = new ArrayList<>();
                    List<Integer> id2 = new ArrayList<>();
                    Integer k = 0;
                    for (Event event : cases.get(key)) {
                        if (itemset.items.size() == 1) {
                            if (itemset.items.get(0).equals(event.activityName)) {
                                itemset.increaseFrequency();
                            }
                        } else {
                            if (itemset.items.get(0).equals(event.activityName)) id1.add(k);
                            if (itemset.items.get(1).equals(event.activityName)) id2.add(k);
                        }
                        k++;
                    }
                    idx1.put(key, id1);
                    idx2.put(key, id2);

                    if(id1.size() > 0 && id2.size() > 0){
                        if(ruleType.equals("precedence") || ruleType.equals("chain precedence"))
                        {
                            for(Integer i2: id2)
                                for(Integer i1: id1)
                                    if((ruleType.equals("precedence") && i2 > i1) || (ruleType.equals("chain precedence") && (i2 - i1 == 1))){
                                        itemset.increaseFrequency();
                                        break;
                                    }
                        }
                        else if(ruleType.equals("response") || ruleType.equals("chain response")){
                            for(Integer i1: id1)
                                for(Integer i2: id2)
                                    if((ruleType.equals("response") && i2 > i1) || (ruleType.equals("chain response") && (i2 - i1 == 1))){
                                        itemset.increaseFrequency();
                                        break;
                                    }
                        }
                        else if(ruleType.equals("responded existence")){
                            for(Integer i1: id1)
                                for(Integer i2: id2){
                                    itemset.increaseFrequency();
                                    break;
                                }
                        }
                        else if(ruleType.equals("alternate response")){
                            for(Integer i1: id1)
                                for(Integer i2: id2)
                                    if(i2 > i1 && id1.stream().noneMatch(el -> el > i1 && el < i2)){
                                        itemset.increaseFrequency();
                                        break;
                                    }
                        }
                        else if(ruleType.equals("alternate precedence")){
                            for(Integer i2: id2)
                                for(Integer i1: id1)
                                    if(i2 > i1 && id2.stream().noneMatch(el -> el > i1 && el < i2)){
                                        itemset.increaseFrequency();
                                        break;
                                    }
                        }
                    }
                }
                index1.put(itemset, idx1);
                index2.put(itemset, idx2);
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
        switch (ruleType){
            case "response": return getResponse(cases, itemsets, index1, index2);
            case "chain response": return getChainResponse(cases, itemsets, index1, index2);
            case "precedence": return getPrecedence(cases, itemsets, index1, index2);
            case "chain precedence": return getChainPrecedence(cases, itemsets, index1, index2);
            case "responded existence": return getRespondedExistence(cases, itemsets, index1, index2);
            case "alternate response": return getAlternateResponse(cases, itemsets, index1, index2);
            case "alternate precedence": return getAlternatePrecedence(cases, itemsets, index1, index2);
        }
        return getChainPrecedence(cases, itemsets, index1, index2);
    }

    public static HashMap<Itemset, List<FeatureVector>> getChainPrecedence(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                      HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if (index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0) {
                    for (Integer i2: index2.get(itemset).get(key))
                        for (Integer i1: index1.get(itemset).get(key))
                            if (i2 - i1 == 1) {
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                break;
                            }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getPrecedence(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                           HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if (index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0) {
                    List<Integer> idx1 = index1.get(itemset).get(key);
                    Collections.reverse(idx1);
                    for (Integer i2: index2.get(itemset).get(key))
                        for (Integer i1: idx1)
                            if (i2 > i1) {
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                break;
                            }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getChainResponse(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                      HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if (index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0) {
                    for (Integer i1: index1.get(itemset).get(key))
                        for (Integer i2: index2.get(itemset).get(key))
                            if(i2 - i1 == 1) {
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                break;
                            }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getResponse(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                         HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if (index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0) {
                    for (Integer i1: index1.get(itemset).get(key))
                        for (Integer i2: index2.get(itemset).get(key))
                            if(i2 > i1) {
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                break;
                            }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getRespondedExistence(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                    HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if(index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0)
                    for(Integer i1: index1.get(itemset).get(key))
                        for(Integer i2: index2.get(itemset).get(key)){
                            pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                            break;
                        }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getAlternateResponse(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                              HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if (index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0) {
                    for (Integer i1: index1.get(itemset).get(key))
                        for (Integer i2: index2.get(itemset).get(key))
                            if(i2 > i1 && index1.get(itemset).get(key).stream().noneMatch(el -> el > i1 && el < i2)) {
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                break;
                            }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getAlternatePrecedence(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                             HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            for(String key: cases.keySet()){
                if (index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0) {
                    List<Integer> idx1 = index1.get(itemset).get(key);
                    Collections.reverse(idx1);
                    for (Integer i2: index2.get(itemset).get(key))
                        for (Integer i1: idx1)
                            if (i2 > i1 && index2.get(itemset).get(key).stream().noneMatch(el -> el > i1 && el < i2)) {
                                pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                                break;
                            }
                }
            }
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static void getCorrelations(HashMap<Itemset, List<FeatureVector>> featureVectorLists, Integer clusterSize){
        List<Correlation> correlations = new ArrayList<>();
        for(Itemset itemset: featureVectorLists.keySet()){
            System.out.println("\n" + itemset.items + "\n");
            /*
            for(FeatureVector fv: featureVectorLists.get(itemset))
                    System.out.println(fv);
            */
            System.out.println(featureVectorLists.get(itemset) + "\n");
            List<Cluster> clustersTo = Clustering.clustering(featureVectorLists.get(itemset),clusterSize);
            clustersTo.forEach(Cluster::giveLabels);

            /*
            for(Cluster cluster: clustersTo){
                System.out.println(cluster.rules);
                System.out.println(cluster.elements + "\n");
            }
            */

            List<Cluster> clustersFrom = DecisionTree.id3(featureVectorLists.get(itemset));

            /*
            for(Cluster cluster: clustersFrom){
                System.out.println(cluster.rules);
                System.out.println(cluster.elements + "\n");
            }
            */

            for(Cluster clusterTo: clustersTo)
                for(Cluster clusterFrom: clustersFrom){
                    if(clusterFrom.label.equals(clusterTo.label)){
                        Correlation correlation = new Correlation(itemset, clusterFrom.rules, clusterTo.rules);
                        //if(correlation.getConfidence(featureVectorLists.get(itemset)) > threshold){
                        correlations.add(correlation);
                        System.out.println(clusterFrom.rules + " => " + clusterTo.rules + " conf = " + correlation.getConfidence(featureVectorLists.get(itemset)));
                        //}
                    }
                }
        }
    }
}