import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class correlationMiner {

    public static void findCorrelations(String csvFile, List<String> rulesList, Integer absoluteSupportThreshold, Double minDistance, Integer minPts, Double supportThreshold, Boolean prunning){
        HashMap<String, List<Event>> cases = logReader.readLog(csvFile);

        for(String rule: rulesList) {
            HashMap<Itemset, List<FeatureVector>> fulfillments = rulesExtractor.extractFeatureVectors(cases, absoluteSupportThreshold, rule);
            List<Itemset> itemsets = new ArrayList<>(fulfillments.keySet());

            HashMap<Itemset, List<FeatureVector>> violations = rulesExtractor.extractViolations(cases, itemsets, rule);

            for(Itemset itemset: itemsets)
                System.out.println(itemset);

            for (Itemset itemset : fulfillments.keySet())
                if (fulfillments.get(itemset).get(0).from.size() > 0 && fulfillments.get(itemset).get(0).to.size() > 0) {


                    List<FeatureVector> featureVectors = fulfillments.get(itemset).stream().map(fv -> new FeatureVector(fv.from, fv.to)).collect(Collectors.toList());

                    System.out.println("\n" + rule + " " + itemset.items + "\n");

                    DBSCANClusterer dbscanClusterer = new DBSCANClusterer(minDistance, minPts);
                    List<Cluster> clusters = dbscanClusterer.clustering(featureVectors);

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
                        }
                    }

                    List<String> violationRule = new ArrayList<>(Collections.singletonList("-"));
                    clusters.add(new Cluster("-", violationRule, violations.get(itemset), "Closed Leaf"));

                    List<FeatureVector> featureVectorsList = new ArrayList<>() {
                        {
                            addAll(featureVectors.stream().filter(fv -> fv.label!=null).collect(Collectors.toList()));
                            addAll(violations.get(itemset));
                        }
                    };

                    if(featureVectorsList.size() > 0)
                    {
                        List<Cluster> clustersFrom = DecisionTree.id3(featureVectorsList, supportThreshold, prunning);

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

                        for(Correlation correlation: correlations) {
                            double support = Summary.getRelativeSupport(cases, featureVectorsList, correlation);
                            double confidence = Summary.getConfidence(cases, featureVectorsList, correlation);
                            //if(support > 0.1 && confidence > 0.6)
                                System.out.println(correlation + ", sup = " + support + ", conf = " + confidence);
                        }
                    }
                }
        }
    }

    private static HashMap<String, String> getSummary(Cluster cluster){
        HashMap<String, String> summary = new HashMap<>();
        for(String attribute: cluster.elements.get(0).to.keySet())
        {
            if(tryParseDouble(cluster.elements.get(0).to.get(attribute))){
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

    static boolean tryParseDouble(String value){
        try{
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }
}
