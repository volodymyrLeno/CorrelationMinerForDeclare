import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    static HashMap<Itemset, List<FeatureVector>> fulfillments = new HashMap<>();
    static HashMap<Itemset, List<FeatureVector>> violations = new HashMap<>();

    public static void main(String[] args) {

        String csvFile = args[0];
        String[] rules = args[1].split("[,;]");
        List<String> rulesList = new ArrayList<>();
        Collections.addAll(rulesList, rules);
        Integer absoluteSupportThreshold = Integer.parseInt(args[2]);
        Double minDistance = Double.parseDouble(args[3]);
        Integer minPts = Integer.parseInt(args[4]);
        Double supportThreshold = Double.parseDouble(args[5]);
        Boolean prunning = Boolean.parseBoolean(args[6]);

        HashMap<String, List<Event>> cases = readLog(csvFile);

        for(String rule: rulesList) {
            long start = System.currentTimeMillis();

            long startTime = System.currentTimeMillis();
            fulfillments = extractFeatureVectors(cases, absoluteSupportThreshold, rule);
            long stopTime = System.currentTimeMillis();
            double fetureVectorsExtractionTime = (double) (stopTime - startTime) / 1000;
            List<Itemset> itemsets = fulfillments.keySet().stream().collect(Collectors.toList());


            violations = extractViolations(cases, itemsets, rule);

            for(Itemset itemset: itemsets)
                System.out.println(itemset);

            double clusteringTime = 0.0;
            double classificationTime = 0.0;


            //boolean perform = false;

            for (Itemset itemset : fulfillments.keySet())
                if (fulfillments.get(itemset).get(0).from.size() > 0 && fulfillments.get(itemset).get(0).to.size() > 0) {


                    List<FeatureVector> featureVectors = fulfillments.get(itemset).stream().map(fv -> new FeatureVector(fv.from, fv.to)).collect(Collectors.toList());

                    System.out.println("\n" + rule + " " + itemset.items + "\n");

                    startTime = System.currentTimeMillis();

                    DBSCANClusterer dbscanClusterer = new DBSCANClusterer(minDistance, minPts);
                    List<Cluster> clusters = dbscanClusterer.clustering(featureVectors);

                    stopTime = System.currentTimeMillis();
                    clusteringTime += (double) (stopTime - startTime) / 1000;


                    int k = 0;

                    for(Cluster cluster: clusters){
                        if(cluster.elements.size() > 0 && cluster.label == null){
                            HashMap<String, String> summary = getSummary(cluster);
                            cluster.label = Integer.toString(k);
                            cluster.giveLabels();
                            for(FeatureVector fv: cluster.elements){
                                for(String attribute: fv.to.keySet())
                                    fv.to.put(attribute,summary.get(attribute));
                            }
                            List<String> constraint = new ArrayList<>();
                            for(String attribute: summary.keySet())
                                constraint.add(attribute + " = " + summary.get(attribute));
                            cluster.rules = constraint;
                            k++;
                            //System.out.println(cluster.elements.size() + ", " + summary + ": " + cluster.elements);
                        }
                    }

                    List<String> violationRule = new ArrayList<>(Collections.singletonList("-"));
                    clusters.add(new Cluster("-", violationRule, violations.get(itemset), "Closed Leaf"));

                    List<FeatureVector> featureVectorsList = new ArrayList<FeatureVector>() {
                        {
                            addAll(featureVectors.stream().filter(fv -> fv.label!=null).collect(Collectors.toList()));
                            addAll(violations.get(itemset));
                        }
                    };

                    if(featureVectorsList.size() > 0)
                    {
                        startTime = System.currentTimeMillis();

                        List<Cluster> clustersFrom = DecisionTree.id3(featureVectorsList, supportThreshold, prunning);

                        stopTime = System.currentTimeMillis();
                        classificationTime += (double) (stopTime - startTime) / 1000;

                        List<Correlation> correlations = new ArrayList<>();

                        for(Cluster clusterTo: clusters)
                            for(Cluster clusterFrom: clustersFrom)
                                if(clusterFrom.label == null)
                                {
                                    if(rule.equals("precedence") || rule.equals("chain precedence") || rule.equals("alternate precedence")){
                                        Collections.reverse(itemset.items);
                                        Correlation correlation = new Correlation(itemset, clusterFrom.rules, clusterTo.rules);
                                        correlations.add(new Correlation(correlation.from, correlation.to,
                                                correlation.simplifyConstraint(correlation.antecedent), correlation.simplifyConstraint(correlation.consequent)));
                                        Collections.reverse(itemset.items);
                                    }
                                    else{
                                        Correlation correlation = new Correlation(itemset, clusterFrom.rules, clusterTo.rules);
                                        correlations.add(new Correlation(correlation.from, correlation.to,
                                                correlation.simplifyConstraint(correlation.antecedent), correlation.simplifyConstraint(correlation.consequent)));
                                    }
                                }
                                else if(clusterFrom.label.equals(clusterTo.label)){
                                    if(rule.equals("precedence") || rule.equals("chain precedence") || rule.equals("alternate precedence")){
                                        Collections.reverse(itemset.items);
                                        Correlation correlation = new Correlation(itemset, clusterFrom.rules, clusterTo.rules);
                                        correlations.add(new Correlation(correlation.from, correlation.to,
                                                correlation.simplifyConstraint(correlation.antecedent), correlation.simplifyConstraint(correlation.consequent)));
                                        Collections.reverse(itemset.items);
                                    }
                                    else{
                                        Correlation correlation = new Correlation(itemset, clusterFrom.rules, clusterTo.rules);
                                        correlations.add(new Correlation(correlation.from, correlation.to,
                                                correlation.simplifyConstraint(correlation.antecedent), correlation.simplifyConstraint(correlation.consequent)));
                                    }
                                }

                        for(Correlation correlation: correlations)
                        {
                            double support = Summary.getRelativeSupport(cases, featureVectorsList, correlation);
                            double confidence = Summary.getConfidence(cases, featureVectorsList, correlation);
                            //if(support > 0.1 && confidence > 0.6)
                                System.out.println(correlation + ", sup = " + support + ", conf = " + confidence);
                        }
                        /*
                           System.out.println(correlation + ", sup = " + Summary.getRelativeSupport(cases, featureVectorsList, correlation)
                        + ", conf = " + Summary.getConfidence(cases, featureVectorsList, correlation));
                        */
                    }
            }
            long stop = System.currentTimeMillis();
            double TotalTime = (double) (stop - start) / 1000;

            System.out.println("Feature Vectors Extraction time - " + fetureVectorsExtractionTime);
            System.out.println("Clustering Time - " + clusteringTime);
            System.out.println("Classification Time - " + classificationTime);
            System.out.println("Total Time - " + TotalTime);
        }

    }

    public static HashMap<String, String> getSummary(Cluster cluster){
        HashMap<String, String> summary = new HashMap<>();
        for(String attribute: cluster.elements.get(0).to.keySet())
        {
            if(Clustering.tryParseDouble(cluster.elements.get(0).to.get(attribute))){
                Double max = Collections.max(cluster.elements.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).
                        collect(Collectors.toList()));
                Double min = Collections.min(cluster.elements.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).
                        collect(Collectors.toList()));
                summary.put(attribute, min + "-" + max);
            }
            else{
                HashMap<String, Integer> frequencies = new HashMap<>();
                for(int i = 0; i < cluster.elements.size(); i++){
                    String value = cluster.elements.get(i).to.get(attribute);
                    if(frequencies.containsKey(value))
                        frequencies.put(value, frequencies.get(value) + 1);
                    else
                        frequencies.put(value, 1);
                }
                Integer max = frequencies.get(cluster.elements.get(0).to.get(attribute));
                String bestLabel = cluster.elements.get(0).to.get(attribute);
                for(String value: frequencies.keySet())
                    if(frequencies.get(value) > max){
                        max = frequencies.get(value);
                        bestLabel = value;
                    }
                summary.put(attribute,bestLabel);
            }
        }
        return summary;
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

    public static HashMap<Itemset, List<FeatureVector>> extractViolations(HashMap<String, List<Event>> cases, List<Itemset> itemsets, String ruleType){
        HashMap<Itemset, List<FeatureVector>> violations = new HashMap<>();
        for(Itemset itemset: itemsets){
            List<FeatureVector> pattern = new ArrayList<>();
            for(String caseID: cases.keySet()){
                List<Integer> id1 = new ArrayList<>();
                List<Integer> id2 = new ArrayList<>();
                int k = 0;
                for(Event event: cases.get(caseID)){
                    if (itemset.items.get(0).equals(event.activityName)) id1.add(k);
                    if (itemset.items.get(1).equals(event.activityName)) id2.add(k);
                    k++;
                }
                switch (ruleType){
                    case "response":
                        for(Integer i: id1)
                            if(id2.stream().noneMatch(el -> el > i))
                                pattern.add(new FeatureVector(cases.get(caseID).get(i).payload, null));
                        break;
                    case "chain response":
                        for(Integer i: id1)
                            if(id2.stream().noneMatch(el -> el - i == 1))
                                pattern.add(new FeatureVector(cases.get(caseID).get(i).payload, null));
                        break;
                    case "precedence":
                        for(Integer i: id2)
                            if(id1.stream().noneMatch(el -> el < i))
                                pattern.add(new FeatureVector(cases.get(caseID).get(i).payload, null));
                        break;
                    case "chain precedence":
                        for(Integer i: id2)
                            if(id1.stream().noneMatch(el -> i - el == 1))
                                pattern.add(new FeatureVector(cases.get(caseID).get(i).payload, null));
                        break;
                    case "responded existence":
                        if(id2.size() == 0)
                            for(Integer i: id1)
                                pattern.add(new FeatureVector(cases.get(caseID).get(i).payload, null));
                        break;
                }
            }
            violations.put(itemset, pattern);
        }
        return violations;
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
                        switch (ruleType) {
                            case "precedence":
                            case "chain precedence":
                                for (Integer i2 : id2)
                                    for (Integer i1 : id1)
                                        if ((ruleType.equals("precedence") && i2 > i1) || (ruleType.equals("chain precedence") && (i2 - i1 == 1))) {
                                            itemset.increaseFrequency();
                                            break;
                                        }
                                break;
                            case "response":
                            case "chain response":
                                for (Integer i1 : id1)
                                    for (Integer i2 : id2)
                                        if ((ruleType.equals("response") && i2 > i1) || (ruleType.equals("chain response") && (i2 - i1 == 1))) {
                                            itemset.increaseFrequency();
                                            break;
                                        }
                                break;
                            case "responded existence":
                                for (Integer i1 : id1)
                                    for (Integer i2 : id2) {
                                        itemset.increaseFrequency();
                                        break;
                                    }
                                break;
                            case "alternate response":
                                for (Integer i1 : id1)
                                    for (Integer i2 : id2)
                                        if (i2 > i1 && id1.stream().noneMatch(el -> el > i1 && el < i2)) {
                                            itemset.increaseFrequency();
                                            break;
                                        }
                                break;
                            case "alternate precedence":
                                for (Integer i2 : id2)
                                    for (Integer i1 : id1)
                                        if (i2 > i1 && id2.stream().noneMatch(el -> el > i1 && el < i2)) {
                                            itemset.increaseFrequency();
                                            break;
                                        }
                                break;
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
                for (Itemset itemset : itemsets) {
                    List<String> merge = new ArrayList<>(itemsets.get(i).items);
                    merge.addAll(itemset.items);
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
                                pattern.add(new FeatureVector(cases.get(key).get(i2), cases.get(key).get(i1)));
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
            cases.keySet().stream().filter(key -> index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0).forEach(key -> {
                List<Integer> idx1 = index1.get(itemset).get(key);
                Collections.reverse(idx1);
                for (Integer i2 : index2.get(itemset).get(key))
                    for (Integer i1 : idx1)
                        if (i2 > i1) {
                            pattern.add(new FeatureVector((cases.get(key).get(i2)), cases.get(key).get(i1)));
                            //pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                            break;
                        }
            });
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getChainResponse(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                      HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            cases.keySet().stream().filter(key -> index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0).forEach(key -> {
                for (Integer i1 : index1.get(itemset).get(key))
                    for (Integer i2 : index2.get(itemset).get(key))
                        if (i2 - i1 == 1) {
                            pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                            break;
                        }
            });
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getResponse(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                         HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            cases.keySet().stream().filter(key -> index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0).forEach(key -> {
                for (Integer i1 : index1.get(itemset).get(key))
                    for (Integer i2 : index2.get(itemset).get(key))
                        if (i2 > i1) {
                            pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                            break;
                        }
            });
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getRespondedExistence(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                    HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            cases.keySet().stream().filter(key -> index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0).forEach(key -> {
                for (Integer i1 : index1.get(itemset).get(key))
                    for (Integer i2 : index2.get(itemset).get(key)) {
                        pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                        break;
                    }
            });
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getAlternateResponse(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                              HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            cases.keySet().stream().filter(key -> index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0).forEach(key -> {
                for (Integer i1 : index1.get(itemset).get(key))
                    for (Integer i2 : index2.get(itemset).get(key))
                        if (i2 > i1 && index1.get(itemset).get(key).stream().noneMatch(el -> el > i1 && el < i2)) {
                            pattern.add(new FeatureVector(cases.get(key).get(i1), cases.get(key).get(i2)));
                            break;
                        }
            });
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }

    public static HashMap<Itemset, List<FeatureVector>> getAlternatePrecedence(HashMap<String, List<Event>> cases, List<Itemset> itemsets,
                                                                             HashMap<Itemset, HashMap<String, List<Integer>>> index1, HashMap<Itemset, HashMap<String, List<Integer>>> index2){
        HashMap<Itemset, List<FeatureVector>> featureVectors = new HashMap<>();
        for(Itemset itemset: itemsets) {
            List<FeatureVector> pattern = new ArrayList<>();
            cases.keySet().stream().filter(key -> index1.get(itemset).get(key).size() > 0 && index2.get(itemset).get(key).size() > 0).forEach(key -> {
                List<Integer> idx1 = index1.get(itemset).get(key);
                Collections.reverse(idx1);
                for (Integer i2 : index2.get(itemset).get(key))
                    for (Integer i1 : idx1)
                        if (i2 > i1 && index2.get(itemset).get(key).stream().noneMatch(el -> el > i1 && el < i2)) {
                            pattern.add(new FeatureVector(cases.get(key).get(i2), cases.get(key).get(i1)));
                            break;
                        }
            });
            featureVectors.put(itemset, pattern);
        }
        return featureVectors;
    }
}