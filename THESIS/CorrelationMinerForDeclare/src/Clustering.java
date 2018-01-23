import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by volodymyr leno on 29.12.2017.
 */

public final class Clustering {

    public static List<List<FeatureVector>> partitions = new ArrayList<>();
    public static List<List<String>> rules = new ArrayList<>();

    public static List<Cluster> clustering(List<FeatureVector> featureVectorList, Integer n){
        partitions.clear();
        rules.clear();

        List<Cluster> clusters = new ArrayList<>();
        partitioning(featureVectorList, new ArrayList<>(), n);
        for(int i = 0; i < partitions.size(); i++)
            clusters.add(new Cluster(String.valueOf(i), rules.get(i), partitions.get(i)));
        return clusters;
    }

    public static void partitioning(List<FeatureVector> featureVectorList, List<String> rule, Integer n){
        if(featureVectorList.size() > n){
            String attribute = getBestAttribute(featureVectorList);
            String split = getBestSplit(featureVectorList, attribute);
            if(tryParseDouble(split)){
                Double value = Double.parseDouble(split);

                partitioning(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute)) <= value).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " <= " + split)).
                        collect(Collectors.toList()), n);

                partitioning(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute)) > value).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " > " + split)).
                        collect(Collectors.toList()), n);
            }
            else{
                partitioning(featureVectorList.stream().filter(fv -> fv.to.get(attribute).equals(split)).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " = " + split)).
                        collect(Collectors.toList()), n);

                partitioning(featureVectorList.stream().filter(fv -> !fv.to.get(attribute).equals(split)).
                        collect(Collectors.toList()), Stream.concat(rule.stream(), Stream.of(attribute + " != " + split)).
                        collect(Collectors.toList()), n);
            }
        }
        else{
            partitions.add(featureVectorList);
            rules.add(rule);
        }
    }

    public static double[][] constructDistanceMatrix(List<FeatureVector> featureVectorsList){
        Integer N = featureVectorsList.size();
        double[][] distanceMatrix = new double[N][N];
        for(int i = 0; i < N; i++)
            for(int j = 0; j < N; j++)
                distanceMatrix[i][j] = computeDistance(featureVectorsList.get(i).to, featureVectorsList.get(j).to, featureVectorsList);
        return distanceMatrix;
    }

    public static double[][] computeSimilarityMatrix(double[][] distanceMatrix){
        Integer N = distanceMatrix.length;
        double[][] similarityMatrix = new double[N][N];
        for(int i = 0; i < N; i++)
            for(int j = 0; j < N; j++)
            {
                double value = 1 - distanceMatrix[i][j]/getMaxValue(distanceMatrix);
                if(value >= 0.0 && value <= 1.0)
                    similarityMatrix[i][j] = value;
                else
                    similarityMatrix[i][j] = 0;
            }
        return similarityMatrix;
    }

    public static double computeInhomogeneity(List<FeatureVector> featureVectorsList, String attribute){
        double[][] fullSimilarityMatrix = computeSimilarityMatrix(constructDistanceMatrix(featureVectorsList));
        List<FeatureVector> reducedFeatureVectorList = new ArrayList<>();
        for(FeatureVector fv: featureVectorsList)
        {
            HashMap<String,String> to = new HashMap<>();
            fv.to.keySet().stream().filter(k -> !k.equals(attribute)).forEach(k -> to.put(k, fv.to.get(k)));
            reducedFeatureVectorList.add(new FeatureVector(fv.from, to));
        }
        double[][] reducedSimilarityMatrix = computeSimilarityMatrix(constructDistanceMatrix(reducedFeatureVectorList));
        double H = 0.0;
        for(int i = 0; i < fullSimilarityMatrix.length; i++)
            for(int j = 0; j < fullSimilarityMatrix.length; j++)
                H += fullSimilarityMatrix[i][j] * (1 - reducedSimilarityMatrix[i][j]) + reducedSimilarityMatrix[i][j] * (1 - fullSimilarityMatrix[i][j]);
        return -H;
    }

    public static String getBestAttribute(List<FeatureVector> featureVectorList){
        HashMap<String, Double> inhomogeneities = new HashMap<>();
        for(String key: featureVectorList.get(0).to.keySet()){
            inhomogeneities.put(key, computeInhomogeneity(featureVectorList, key));
            //System.out.println(key + "  " + computeInhomogeneity(featureVectorList, key));
        }

        Map.Entry<String, Double> max = null;
        for (Map.Entry<String, Double> entry : inhomogeneities.entrySet()) {
            if (max == null || max.getValue() < entry.getValue()) {
                max = entry;
            }
        }
        return max.getKey();
    }

    public static String getBestSplit(List<FeatureVector> featureVectorList, String attribute){

        double Hd = computeInhomogeneity(featureVectorList, attribute);
        double deltaH = 0.0;

        if(tryParseDouble(featureVectorList.get(0).to.get(attribute))){
            List<Double> values = featureVectorList.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).distinct().collect(Collectors.toList());
            double bestSplit = values.get(0);
            for(double value: values){
                double H1 = computeInhomogeneity(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute)) > value).collect(Collectors.toList()), attribute);
                double H2 = computeInhomogeneity(featureVectorList.stream().filter(fv -> Double.parseDouble(fv.to.get(attribute)) <= value).collect(Collectors.toList()), attribute);
                if(deltaH < (H1 + H2 - Hd)){
                    deltaH = H1 + H2 - Hd;
                    bestSplit = value;
                }
            }
            return String.valueOf(bestSplit);
        }
        else{
            List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute)).distinct().collect(Collectors.toList());
            String bestSplit = values.get(0);
            for(String value: values){
                double H1 = computeInhomogeneity(featureVectorList.stream().filter(fv -> fv.to.get(attribute).equals(value)).collect(Collectors.toList()), attribute);
                double H2 = computeInhomogeneity(featureVectorList.stream().filter(fv -> !fv.to.get(attribute).equals(value)).collect(Collectors.toList()), attribute);
                //System.out.println(H1 + H2 - Hd + "  " + "value: " + value);
                if(deltaH < (H1 + H2 - Hd)){
                    deltaH = H1 + H2 - Hd;
                    bestSplit = value;
                }
            }
            return bestSplit;
        }
    }

    public static double computeDistance(HashMap<String, String> payload1, HashMap<String, String> payload2, List<FeatureVector> featureVectorList){
        double distance = 0.0;
        for(String key: payload1.keySet()){
            if(tryParseDouble(payload1.get(key)))
                distance += computeDistance(Double.parseDouble(payload1.get(key)), Double.parseDouble(payload2.get(key)), getMaxValue(featureVectorList, key), getMinValue(featureVectorList, key));
            else if(payload1.get(key).equalsIgnoreCase("true") || payload2.get(key).equalsIgnoreCase("false"))
                distance += computeDistance(Boolean.valueOf(payload1.get(key)), Boolean.valueOf(payload2.get(key)));
            else
                distance += computeDistance(payload1.get(key), payload2.get(key));
        }
        return distance/payload1.size();
    }

    public static double computeDistance(double value1, double value2, double max, double min){
        return (Math.abs(value1 - value2))/(max - min);
    }

    public static double computeDistance(Boolean value1, Boolean value2){
        if(value1 == value2) return 0.0;
        else return 1.0;
    }

    public static double computeDistance(String value1, String value2){
        Integer maxEditDistance = Math.max(value1.length(), value2.length());
        return (double)calculateEditDistance(value1, value2)/maxEditDistance;
    }

    public static Integer calculateEditDistance(String value1, String value2){
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

    public static double getMaxValue(double[][] matrix){
        double maxValue = matrix[0][0];
        for (int j = 0; j < matrix.length; j++) {
            for (int i = 0; i < matrix[j].length; i++) {
                if (matrix[j][i] > maxValue) {
                    maxValue = matrix[j][i];
                }
            }
        }
        return maxValue;
    }

    public static double getMaxValue(List<FeatureVector> featureVectorList, String attribute){
        List<Double> values = featureVectorList.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).collect(Collectors.toList());
        return Collections.max(values);
    }

    public static double getMinValue(List<FeatureVector> featureVectorList, String attribute){
        List<Double> values = featureVectorList.stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).collect(Collectors.toList());
        return Collections.min(values);
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