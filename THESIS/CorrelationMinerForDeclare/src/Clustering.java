import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by volodymyr leno on 29.12.2017.
 */

public final class Clustering {

    public static List<List<FeatureVector>> partitions = new ArrayList<>();
    public static List<List<String>> rules = new ArrayList<>();

    public static HashMap<String, HashMap<String, Double>> attrMax = new HashMap<>();
    public static HashMap<String, HashMap<String, Double>> attrMin = new HashMap<>();
    public static HashMap<StringPair, Double> editDistances = new HashMap<>();
    public static HashMap<String, HashMap<StringPair, Double>> rangesDistances = new HashMap<>();

    public static List<Cluster> clusters = new ArrayList<>();

    public static void split(List<Cluster> clusterList, Double supportThreshold, Integer totalAmount){
        for(Cluster cluster: clusterList)
        {
            Attribute attribute = getBestAttribute(cluster.elements);
            Split split = getBestSplit(cluster.elements, attribute);

            if((double)cluster.elements.size()/totalAmount > supportThreshold && split.gain > 0.0){
                cluster.clusterType = "-";
                if(tryParseDouble(split.value)){
                    Double value = Double.parseDouble(split.value);

                    List<FeatureVector> subset1 = cluster.elements.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute.value)) <= value).
                            collect(Collectors.toList());
                    List<FeatureVector> subset2 = cluster.elements.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute.value)) > value).
                            collect(Collectors.toList());
                    clusters.add(new Cluster(Integer.toString(Integer.parseInt((clusters.stream().max(Comparator.comparing(c -> c.label))).get().label) + 1),
                            Stream.concat(cluster.rules.stream(), Stream.of(attribute.value + " <= " + split.value)).collect(Collectors.toList()), subset1, "Leaf"));
                    clusters.add(new Cluster(Integer.toString(Integer.parseInt((clusters.stream().max(Comparator.comparing(c -> c.label))).get().label) + 1),
                            Stream.concat(cluster.rules.stream(), Stream.of(attribute.value + " > " + split.value)).collect(Collectors.toList()), subset2, "Leaf"));
                }
                else{
                    List<FeatureVector> subset1 = cluster.elements.stream().filter(fv -> fv.to.get(attribute.value).equals(split.value)).collect(Collectors.toList());
                    List<FeatureVector> subset2 = cluster.elements.stream().filter(fv -> !fv.to.get(attribute.value).equals(split.value)).collect(Collectors.toList());

                    clusters.add(new Cluster(Integer.toString(Integer.parseInt((clusters.stream().max(Comparator.comparing(c -> c.label))).get().label) + 1),
                            Stream.concat(cluster.rules.stream(), Stream.of(attribute.value + " = " + split.value)).collect(Collectors.toList()), subset1, "Leaf"));
                    clusters.add(new Cluster(Integer.toString(Integer.parseInt((clusters.stream().max(Comparator.comparing(c -> c.label))).get().label) + 1),
                            Stream.concat(cluster.rules.stream(), Stream.of(attribute.value + " != " + split.value)).collect(Collectors.toList()), subset2, "Leaf"));
                }
            }
            else{
                cluster.clusterType = "Closed Leaf";
            }
        }
    }

    public static List<Correlation> getCorrelations(Itemset itemset, List<FeatureVector> fulfillments, List<FeatureVector> violations, Double supportThreshold, String ruleType, Boolean prunning){
        clusters.clear();
        computeEditDistances(fulfillments);
        computeRangesDistances(fulfillments);
        System.out.println(rangesDistances);
        List<Correlation> correlations = new ArrayList<>();
        clusters.add(new Cluster("1", new ArrayList<>(), fulfillments, "Leaf"));
        while(clusters.stream().anyMatch(cluster -> cluster.clusterType.equals("Leaf"))){
            split(clusters.stream().filter(cluster -> cluster.clusterType.equals("Leaf")).collect(Collectors.toList()), supportThreshold, (fulfillments.size() + violations.size()));
            List<Cluster> clustersTo = clusters.stream().filter(cl -> cl.clusterType.equals("Leaf") || cl.clusterType.equals("Closed Leaf")).
                    collect(Collectors.toList());

            List<String> violationRule = new ArrayList<>(Collections.singletonList("-"));
            clustersTo.add(new Cluster("-", violationRule, violations, "Closed Leaf"));
            clustersTo.forEach(Cluster::giveLabels);
            List<FeatureVector> featureVectorsList = new ArrayList<FeatureVector>() { { addAll(fulfillments); addAll(violations); } };
            List<Cluster> clustersFrom = DecisionTree.id3(featureVectorsList, 0.05, prunning);
            for(Cluster clusterTo: clustersTo)
                for(Cluster clusterFrom: clustersFrom)
                if(clusterFrom.label.equals(clusterTo.label)){
                    if(ruleType.equals("precedence") || ruleType.equals("chain precedence") || ruleType.equals("alternate precedence")){
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
        }
        return getDistinctCorrelations(correlations);
    }


    /********* Experimental ********/
    /*
    public static List<Cluster> clustering(List<FeatureVector> featureVectorList, Double supportThreshold){
        partitions.clear();
        rules.clear();

        List<Cluster> clusters = new ArrayList<>();
        List<String> attributes = new ArrayList<String>() { { addAll(featureVectorList.get(0).from.keySet()); addAll(featureVectorList.get(0).to.keySet()); } };
        partitioning(featureVectorList, new ArrayList<>(), supportThreshold, featureVectorList.size(), attributes);
        for(int i = 0; i < partitions.size(); i++)
            clusters.add(new Cluster(String.valueOf(i), rules.get(i), partitions.get(i), ""));
        return clusters;
    }

    public static List<Correlation> createCorrelations(List<FeatureVector> featureVectorList, List<Cluster> clusters){
        List<Correlation> correlations = new ArrayList<>();
        for(Cluster cluster: clusters)
        {
            List<String> rulesTo = new ArrayList<>();
            List<String> rulesFrom = new ArrayList<>();
            for(String rule: cluster.rules){
                String[] params = rule.split("\\s[<!>=]+\\s");
                String attribute = params[0];
                if(featureVectorList.get(0).from.containsKey(attribute))
                    rulesFrom.add(rule);
                if(featureVectorList.get(0).to.containsKey(attribute))
                    rulesTo.add(rule);
            }
            correlations.add(new Correlation("SubmitLoanApplication", "AssessApplication", rulesFrom, rulesTo));
        }
        return correlations;
    }

    public static void partitioning(List<FeatureVector> featureVectorList, List<String> rule, Double supportThreshold, Integer totalAmount, List<String> attributes){
        String attribute = getBestAttribute(featureVectorList, attributes);
        String split = getBestSplit(featureVectorList, attribute);

        partitions.add(featureVectorList);
        rules.add(rule);

        System.out.println(attribute + ": " + split + ", H = " + computeGain(featureVectorList, attribute, split));

        if((double)featureVectorList.size()/totalAmount > supportThreshold && computeGain(featureVectorList, attribute, split) > 0.0){
            if(tryParseDouble(split)){
                Double value = Double.parseDouble(split);

                if(featureVectorList.get(0).to.containsKey(attribute)){
                    List<FeatureVector> subset1 = featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute)) <= value).
                            collect(Collectors.toList());
                    List<FeatureVector> subset2 = featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute)) > value).
                            collect(Collectors.toList());

                    partitioning(subset1, Stream.concat(rule.stream(), Stream.of(attribute + " <= " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                    partitioning(subset2, Stream.concat(rule.stream(), Stream.of(attribute + " > " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                }
                else{
                    List<FeatureVector> subset1 = featureVectorList.stream().filter(fv -> Double.parseDouble(fv.from.get(attribute)) <= value).
                            collect(Collectors.toList());
                    List<FeatureVector> subset2 = featureVectorList.stream().filter(fv -> Double.parseDouble(fv.from.get(attribute)) > value).
                            collect(Collectors.toList());

                    partitioning(subset1, Stream.concat(rule.stream(), Stream.of(attribute + " <= " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                    partitioning(subset2, Stream.concat(rule.stream(), Stream.of(attribute + " > " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                }

            }
            else{
                if(featureVectorList.get(0).to.containsKey(attribute)){
                    List<FeatureVector> subset1 = featureVectorList.stream().filter(fv -> fv.to.get(attribute).equals(split)).
                            collect(Collectors.toList());
                    List<FeatureVector> subset2 = featureVectorList.stream().filter(fv -> !fv.to.get(attribute).equals(split)).
                            collect(Collectors.toList());

                    partitioning(subset1, Stream.concat(rule.stream(), Stream.of(attribute + " = " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                    partitioning(subset2, Stream.concat(rule.stream(), Stream.of(attribute + " != " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                }
                else{
                    List<FeatureVector> subset1 = featureVectorList.stream().filter(fv -> fv.from.get(attribute).equals(split)).
                            collect(Collectors.toList());
                    List<FeatureVector> subset2 = featureVectorList.stream().filter(fv -> !fv.from.get(attribute).equals(split)).
                            collect(Collectors.toList());

                    partitioning(subset1, Stream.concat(rule.stream(), Stream.of(attribute + " = " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                    partitioning(subset2, Stream.concat(rule.stream(), Stream.of(attribute + " != " + split)).collect(Collectors.toList()),
                            supportThreshold, totalAmount, attributes.stream().filter(attr -> !attr.equals(attribute)).collect(Collectors.toList()));
                }

            }
        }
        else{
            rules.add(rule);
            partitions.add(featureVectorList);
        }
    }
    */


    public static double[][] constructDistanceMatrix(List<FeatureVector> featureVectorsList){
        attrMax.clear();
        attrMin.clear();

        computeRanges(featureVectorsList);

        Integer N = featureVectorsList.size();
        double[][] distanceMatrix = new double[N][N];
        for(int i = 0; i < N; i++)
            for(int j = 0; j < N; j++){
                if(i == j)
                    distanceMatrix[i][j] = 0;
                else if(j > i)
                    distanceMatrix[i][j] = computeDistance(featureVectorsList.get(i), featureVectorsList.get(j), featureVectorsList);
                else
                    distanceMatrix[i][j] = distanceMatrix[j][i];
            }
        return distanceMatrix;
    }

    public static double[][] computeSimilarityMatrix(double[][] distanceMatrix){
        Integer N = distanceMatrix.length;
        double[][] similarityMatrix = new double[N][N];
        for(int i = 0; i < N; i++)
            for(int j = 0; j < N; j++)
            {
                if(i == j)
                    similarityMatrix[i][j] = 1;
                else if(j > i){
                    double value = 1 - distanceMatrix[i][j]/getMaxValue(distanceMatrix);
                    if(value >= 0.0 && value <= 1.0)
                        similarityMatrix[i][j] = value;
                    else
                        similarityMatrix[i][j] = 0;
                }
                else
                    similarityMatrix[i][j] = similarityMatrix[j][i];
            }
        return similarityMatrix;
    }

    public static double computeInhomogeneity(List<FeatureVector> featureVectorsList, String attribute) {
        double H = 0.0;

        double[][] fullSimilarityMatrix = computeSimilarityMatrix(constructDistanceMatrix(featureVectorsList));
        List<FeatureVector> reducedFeatureVectorList = new ArrayList<>();
        for (FeatureVector fv : featureVectorsList) {
            HashMap<String, String> from = new HashMap<>();
            HashMap<String, String> to = new HashMap<>();
            fv.from.keySet().stream().filter(k -> !k.equals(attribute)).forEach(k -> from.put(k, fv.from.get(k)));
            fv.to.keySet().stream().filter(k -> !k.equals(attribute)).forEach(k -> to.put(k, fv.to.get(k)));

            reducedFeatureVectorList.add(new FeatureVector(from, to));
        }
        double[][] reducedSimilarityMatrix = computeSimilarityMatrix(constructDistanceMatrix(reducedFeatureVectorList));
        for (int i = 0; i < fullSimilarityMatrix.length; i++)
            for (int j = 0; j < fullSimilarityMatrix.length; j++) {
                H += fullSimilarityMatrix[i][j] * (1 - reducedSimilarityMatrix[i][j]) + reducedSimilarityMatrix[i][j] * (1 - fullSimilarityMatrix[i][j]);
            }
        return -H;
    }

    public static Attribute getBestAttribute(List<FeatureVector> featureVectorList){

        HashMap<String, Double> inhomogeneities = new HashMap<>();

        for(String key: featureVectorList.get(0).to.keySet()){
            inhomogeneities.put(key, computeInhomogeneity(featureVectorList, key));
            System.out.println(key + ": " + inhomogeneities.get(key));
        }

        Map.Entry<String, Double> max = null;
            for (Map.Entry<String, Double> entry : inhomogeneities.entrySet()) {
                if (max == null || max.getValue() < entry.getValue()) {
                    max = entry;
                }
            }

        return new Attribute(max.getKey(), max.getValue());
    }

    public static Split getBestSplit(List<FeatureVector> featureVectorList, Attribute attribute){
        double deltaH = 0.0;
        List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute.value)).distinct().collect(Collectors.toList());
        String bestSplit = values.get(0);
        for(String value: values){
            System.out.println(value);
            if(deltaH < computeGain(featureVectorList, attribute, value)){
                deltaH = computeGain(featureVectorList, attribute, value);
                bestSplit = value;
            }
        }
        return new Split(bestSplit, deltaH);
    }

    public static double computeGain(List<FeatureVector> featureVectorList, Attribute attribute, String split){
        double Hd = attribute.inhomogeneity;
        if(tryParseDouble(split)){
            double H1 = computeInhomogeneity(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute.value)) > Double.parseDouble(split)).collect(Collectors.toList()), attribute.value);
            double H2 = computeInhomogeneity(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute.value)) <= Double.parseDouble(split)).collect(Collectors.toList()), attribute.value);
            return H1 + H2 - Hd;
        }
        else{
            double H1 = computeInhomogeneity(featureVectorList.stream().filter(fv -> fv.to.get(attribute.value).equals(split)).collect(Collectors.toList()), attribute.value);
            double H2 = computeInhomogeneity(featureVectorList.stream().filter(fv -> !fv.to.get(attribute.value).equals(split)).collect(Collectors.toList()), attribute.value);
            return H1 + H2 - Hd;
        }
    }

    public static double computeDistance(FeatureVector fv1, FeatureVector fv2, List<FeatureVector> featureVectorList){
        //System.out.println(fv1 + " => " + fv2);
        double distance = 0.0;

        List<String> attributesFrom = new ArrayList<String>(){{addAll(fv1.from.keySet()); addAll(fv2.from.keySet());}};
        List<String> attributesTo = new ArrayList<String>() {{addAll(fv1.to.keySet()); addAll(fv2.to.keySet());}};

        attributesFrom = attributesFrom.stream().distinct().collect(Collectors.toList());
        attributesTo = attributesTo.stream().distinct().collect(Collectors.toList());

        for(String key: attributesFrom){
            if(fv1.from.containsKey(key) && fv2.from.containsKey(key)){
                if(tryParseDouble(fv1.from.get(key))){
                    if(attrMax.get("from").get(key) == null || attrMin.get("from").get(key) == null)
                        continue;
                    else
                        distance += computeDistance(Double.parseDouble(fv1.from.get(key)), Double.parseDouble(fv2.from.get(key)), attrMax.get("from").get(key), attrMin.get("from").get(key));
                }
                else if(fv1.from.get(key).equalsIgnoreCase("true") || fv1.from.get(key).equalsIgnoreCase("false"))
                    distance += computeDistance(Boolean.valueOf(fv1.from.get(key)), Boolean.valueOf(fv2.from.get(key)));
                else if(isRange(fv1.from.get(key))){
                    StringPair pair = new StringPair(fv1.from.get(key), fv2.from.get(key));
                    distance += rangesDistances.get(key).get(pair);
                }
                else{
                    StringPair pair = new StringPair(fv1.from.get(key), fv2.from.get(key));
                    distance += editDistances.get(pair);
                }
                //System.out.println(distance);
            }
            else
                distance += 1.0;
        }
        for(String key: attributesTo){
            if(fv1.to.containsKey(key) && fv2.to.containsKey(key)){
                if(tryParseDouble(fv1.to.get(key))){
                    if(attrMax.get("to").get(key) == null || attrMin.get("to").get(key) == null)
                        continue;
                    else
                        distance += computeDistance(Double.parseDouble(fv1.to.get(key)), Double.parseDouble(fv2.to.get(key)), attrMax.get("to").get(key), attrMin.get("to").get(key));
                }
                else if(fv1.to.get(key).equalsIgnoreCase("true") || fv1.to.get(key).equalsIgnoreCase("false"))
                    distance += computeDistance(Boolean.valueOf(fv1.to.get(key)), Boolean.valueOf(fv2.to.get(key)));
                else if(isRange(fv1.to.get(key))){
                    StringPair pair = new StringPair(fv1.to.get(key), fv2.to.get(key));
                    distance += rangesDistances.get(key).get(pair);
                }
                else
                {
                    StringPair pair = new StringPair(fv1.to.get(key), fv2.to.get(key));
                    distance += editDistances.get(pair);
                }
            }
            else
                distance += 1.0;
        }
        return distance/(fv1.from.size() + fv1.to.size());
    }

    public static double computeDistance(double value1, double value2, double max, double min){
        if(Double.isNaN(value1) || Double.isNaN(value2))
            return 1.0;
        double value = (Math.abs(value1 - value2))/(Math.abs(max - min));
        if(Double.isNaN(value))
            return 0.0;
        else
            return value;
    }

    public static double computeDistance(Boolean value1, Boolean value2){
        if(value1.equals(value2)) return 0.0;
        else return 1.0;
    }

    public static double computeDistance(String value1, String value2){
        if(value1 == null)
            value1 = "";
        if(value2 == null)
            value2 = "";
        Integer maxEditDistance = Math.max(value1.length(), value2.length());
        double value = (double)calculateEditDistance(value1, value2)/maxEditDistance;
        if(Double.isNaN(value))
            return 0.0;
        else
            return value;
    }

    public static void computeRangesDistances(List<FeatureVector> featureVectorList){
        for(String attribute: featureVectorList.get(0).to.keySet()){
            if(isRange(featureVectorList.get(0).to.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute)).distinct().collect(Collectors.toList());
                HashMap<String, Double> means = computeRangesMeans(values);
                Double min = Collections.min(means.values());
                Double max = Collections.max(means.values());
                HashMap<StringPair, Double> distances = new HashMap<>();
                for(int i = 0; i < values.size(); i++)
                    for(int j = 0; j < values.size(); j++)
                        distances.put(new StringPair(values.get(i), values.get(j)),(Math.abs(means.get(values.get(i)) - means.get(values.get(j)))/(max-min)));
                rangesDistances.put(attribute, distances);
            }
        }
        for(String attribute: featureVectorList.get(0).from.keySet()){
            if(isRange(featureVectorList.get(0).from.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.from.get(attribute)).distinct().collect(Collectors.toList());
                HashMap<String, Double> means = computeRangesMeans(values);
                Double min = Collections.min(means.values());
                Double max = Collections.max(means.values());
                HashMap<StringPair, Double> distances = new HashMap<>();
                for(int i = 0; i < values.size(); i++)
                    for(int j = 0; j < values.size(); j++)
                        distances.put(new StringPair(values.get(i), values.get(j)),(Math.abs(means.get(values.get(i)) - means.get(values.get(j)))/(max-min)));
                rangesDistances.put(attribute, distances);
            }
        }
    }

    /*
    public static double computeRangesDistance(String value1, String value2, List<FeatureVector> featureVectorList, String attribute){
        List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute)).distinct().collect(Collectors.toList());
        HashMap<String, Double> means = computeRangesMeans(values);
        Double min = Collections.min(means.values());
        Double max = Collections.max(means.values());
        return Math.abs(means.get(value1) - means.get(value2))/(max-min);
    }
    */

    public static HashMap<String, Double> computeRangesMeans(List<String> ranges){
        HashMap<String, Double> rangesMeans = new HashMap<>();
        for(String range: ranges){
            String[] values = range.split("-");
            rangesMeans.put(range,(Double.parseDouble(values[0]) + Double.parseDouble(values[1]))/2);
        }
        return rangesMeans;
    }

    public static Integer calculateEditDistance(String value1, String value2){
        if(value1.equals(value2))
            return 0;
        else{
            int edits[][]=new int[value1.length()+1][value2.length()+1];
            for(int i=0;i<=value1.length();i++)
                edits[i][0]=i;
            for(int j=1;j<=value2.length();j++)
                edits[0][j]=j;
            for(int i=1;i<=value1.length();i++){
                for(int j=1;j<=value2.length();j++){
                    int u=(value1.charAt(i-1)==value2.charAt(j-1)?0:1);
                    edits[i][j]=Math.min(
                            edits[i-1][j]+1,
                            Math.min(
                                    edits[i][j-1]+1,
                                    edits[i-1][j-1]+u
                            )
                    );
                }
            }
            return edits[value1.length()][value2.length()];
        }
    }

    public static double getMaxValue(double[][] matrix){
        double maxValue = matrix[0][0];
        for (double[] aMatrix : matrix) {
            for (double anAMatrix : aMatrix) {
                if (anAMatrix > maxValue) {
                    maxValue = anAMatrix;
                }
            }
        }
        return maxValue;
    }

    public static boolean tryParseDouble(String value){
        try{
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    public static boolean isRange(String value){
        String[] values = value.split("-");
        return values.length == 2 && tryParseDouble(values[0]) && tryParseDouble(values[1]);
    }

    public static void printMatrix(double[][] matrix){
        for (double[] aMatrix : matrix) {
            for (int j = 0; j < matrix.length; j++)
                System.out.print(aMatrix[j] + " ");
            System.out.println();
        }
    }

    public static List<Correlation> getDistinctCorrelations(List<Correlation> correlations){
        List<Correlation> distinctCorrelations = new ArrayList<>();
        for(Correlation correlation: correlations){
            if(distinctCorrelations.stream().noneMatch(cor -> cor.antecedent.equals(correlation.antecedent) && cor.consequent.equals(correlation.consequent)))
                distinctCorrelations.add(correlation);
        }
        return distinctCorrelations;
    }

    public static void computeEditDistances(List<FeatureVector> featureVectorList){
        for(String attribute: featureVectorList.get(0).from.keySet()){
            if(!tryParseDouble(featureVectorList.get(0).from.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.from.get(attribute)).distinct().collect(Collectors.toList());
                for(int i = 0; i < values.size(); i++)
                    for(int j = 0; j < values.size(); j++)
                        editDistances.put(new StringPair(values.get(i), values.get(j)), computeDistance(values.get(i), values.get(j)));
            }
        }
        for(String attribute: featureVectorList.get(0).to.keySet()){
            if(!tryParseDouble(featureVectorList.get(0).to.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute)).distinct().collect(Collectors.toList());
                for(int i = 0; i < values.size(); i++)
                    for(int j = 0; j < values.size(); j++)
                        editDistances.put(new StringPair(values.get(i), values.get(j)), computeDistance(values.get(i), values.get(j)));
            }
        }
    }

    public static void computeRanges(List<FeatureVector> featureVectorsList){
        if(featureVectorsList.size() > 0){
            HashMap<String, Double> fromMax = new HashMap<>();
            HashMap<String, Double> fromMin = new HashMap<>();
            for(String attribute: featureVectorsList.get(0).from.keySet()){
                if(tryParseDouble(featureVectorsList.get(0).from.get(attribute))){
                    fromMax.put(attribute,Collections.max(featureVectorsList.stream().map(fv -> Double.parseDouble(fv.from.get(attribute))).
                            collect(Collectors.toList())));
                    fromMin.put(attribute, Collections.min(featureVectorsList.stream().map(fv -> Double.parseDouble(fv.from.get(attribute))).
                            collect(Collectors.toList())));
                }
            }
            attrMax.put("from",fromMax);
            attrMin.put("from",fromMin);

            HashMap<String, Double> toMax = new HashMap<>();
            HashMap<String, Double> toMin = new HashMap<>();
            for(String attribute: featureVectorsList.get(0).to.keySet()){
                if(tryParseDouble(featureVectorsList.get(0).to.get(attribute))){
                    toMax.put(attribute, Collections.max(featureVectorsList.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).
                            collect(Collectors.toList())));
                    toMin.put(attribute, Collections.min(featureVectorsList.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).
                            collect(Collectors.toList())));
                }
            }
            attrMax.put("to",toMax);
            attrMin.put("to",toMin);
        }
    }
}