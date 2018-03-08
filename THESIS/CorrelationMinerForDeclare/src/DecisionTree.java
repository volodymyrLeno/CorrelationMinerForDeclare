import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by volodymyr leno on 09.01.2018.
 */
public final class DecisionTree {

    public static List<List<FeatureVector>> partitions = new ArrayList<>();
    public static List<List<String>> rules = new ArrayList<>();

    public static List<Cluster> id3(List<FeatureVector> featureVectorList, Double supportThreshold){
        partitions.clear();
        rules.clear();

        List<Cluster> clusters = new ArrayList<>();
        partitioning(featureVectorList, new ArrayList<>(), featureVectorList.size(), supportThreshold);

        for(int i = 0; i < partitions.size(); i++){
            List<FeatureVector> temp = partitions.get(i);
            HashMap<String, Integer> labelsCounts = new HashMap<>();
            for(String label: temp.stream().map(fv -> fv.label).collect(Collectors.toList())){
                if(labelsCounts.containsKey(label))
                    labelsCounts.put(label, labelsCounts.get(label) + 1);
                else
                    labelsCounts.put(label, 1);
            }
            Integer max = labelsCounts.get(temp.get(0).label);
            String bestLabel = temp.get(0).label;
            for(String label: labelsCounts.keySet())
                if(labelsCounts.get(label) > max){
                    max = labelsCounts.get(label);
                    bestLabel = label;
                }
            clusters.add(new Cluster(bestLabel, rules.get(i), partitions.get(i), ""));
        }
        return clusters;
    }

    public static void partitioning(List<FeatureVector> featureVectorList, List<String> rule, Integer totalAmount, Double supportThreshold) {
        BestSplit bestSplit = getBestSplit(featureVectorList);
        if(computeInformationGain(featureVectorList, bestSplit.attribute, bestSplit.value) == 0){ //|| (double)(featureVectorList.size()/totalAmount) < supportThreshold){
            partitions.add(featureVectorList);
            rules.add(rule);
        }
        else{
            String attribute = bestSplit.attribute;
            String split = bestSplit.value;

            if(tryParseDouble(split)){
                Double value = Double.parseDouble(split);
                partitioning(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.from.get(attribute)) > value).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " > " + split)).
                        collect(Collectors.toList()), totalAmount, supportThreshold);
                partitioning(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.from.get(attribute)) <= value).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " <= " + split)).
                        collect(Collectors.toList()), totalAmount, supportThreshold);
            }
            else{
                partitioning(featureVectorList.stream().filter(fv -> fv.from.get(attribute).equals(split)).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " = " + split)).
                        collect(Collectors.toList()), totalAmount, supportThreshold);
                partitioning(featureVectorList.stream().filter(fv -> !fv.from.get(attribute).equals(split)).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " != " + split)).
                        collect(Collectors.toList()), totalAmount, supportThreshold);
            }
        }
    }

    public static HashMap<String, Double> getProbabilities(List<FeatureVector> featureVectorList){
        HashMap<String, Double> probabilities = new HashMap<>();
        for(String label: featureVectorList.stream().map(featureVector -> featureVector.label).collect(Collectors.toList())){
            if(!probabilities.containsKey(label))
                probabilities.put(label, 1.0);
            else
                probabilities.put(label, probabilities.get(label) + 1.0);
        }
        for(String label: probabilities.keySet())
            probabilities.put(label, probabilities.get(label)/featureVectorList.size());
        return probabilities;
    }

    public static double computeEntropy(List<FeatureVector> featureVectorList){
        if(featureVectorList == null) return 0;
        HashMap<String, Double> probabilities = getProbabilities(featureVectorList);
        Double entropy = 0.0;
        for(String label: probabilities.keySet())
            entropy += probabilities.get(label) * Math.log(probabilities.get(label));
        return -entropy;
    }

    public static double computeInformationGain(List<FeatureVector> featureVectorList, String attribute, String split){
        double Entropy = computeEntropy(featureVectorList);

        if(tryParseDouble(split)){
            Double value = Double.parseDouble(split);
            List<FeatureVector> part1 = featureVectorList.stream().filter(fv -> Double.parseDouble(fv.from.get(attribute)) > value).collect(Collectors.toList());
            List<FeatureVector> part2 = featureVectorList.stream().filter(fv -> Double.parseDouble(fv.from.get(attribute)) <= value).collect(Collectors.toList());
            return Entropy - (computeEntropy(part1) * part1.size() + computeEntropy(part2) * part2.size())/featureVectorList.size();
        }
        else{
            List<FeatureVector> part1 = featureVectorList.stream().filter(fv -> fv.from.get(attribute).equals(split)).collect(Collectors.toList());
            List<FeatureVector> part2 = featureVectorList.stream().filter(fv -> !fv.from.get(attribute).equals(split)).collect(Collectors.toList());
            return Entropy - (computeEntropy(part1) * part1.size() + computeEntropy(part2) * part2.size())/featureVectorList.size();
        }
    }

    public static BestSplit getBestSplit(List<FeatureVector> featureVectorList){
        HashMap<String, String> split = new HashMap<>();
        HashMap<String, Double> infGain = new HashMap<>();
        for(String attribute: featureVectorList.get(0).from.keySet()){
            List<String> values = featureVectorList.stream().map(fv -> fv.from.get(attribute)).distinct().collect(Collectors.toList());

        double bestSplit = computeInformationGain(featureVectorList, attribute, values.get(0));
            split.put(attribute, values.get(0));
            for(String value: values){
                if(bestSplit < computeInformationGain(featureVectorList, attribute, value)){
                    bestSplit = computeInformationGain(featureVectorList, attribute, value);
                    split.put(attribute, value);
                }
            }
            infGain.put(attribute, bestSplit);
        }

        String bestAttribute = Collections.max(infGain.entrySet(), Map.Entry.comparingByValue()).getKey();
        String bestThreshold = split.get(bestAttribute);

        return new BestSplit(bestAttribute, bestThreshold);
    }

    public static boolean tryParseDouble(String value){
        try{
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }
}

class BestSplit{
    String attribute;
    String value;

    public BestSplit(String attribute, String value){
        this.attribute = attribute;
        this.value = value;
    }

    public String toString(){
        return "attribute: " + attribute + ", threshold: " + value;
    }
}