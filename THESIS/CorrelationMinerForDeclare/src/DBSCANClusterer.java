import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by volodymyr leno on 10.03.2018.
 */

//@Deprecated
public class DBSCANClusterer{

    private double eps;
    private int minPts;

    private HashMap<String, HashMap<String, Double>> attrMax;
    private HashMap<String, HashMap<String, Double>> attrMin;
    private HashMap<StringPair, Double> editDistances;
    private HashMap<String, HashMap<StringPair, Double>> rangesDistances;

    private enum PointStatus {
        NOISE,
        PART_OF_CLUSTER
    }

    DBSCANClusterer(double eps, int minPts){
        this.eps = eps;
        this.minPts = minPts;
        this.attrMin = new HashMap<>();
        this.attrMax = new HashMap<>();
        this.editDistances = new HashMap<>();
        this.rangesDistances = new HashMap<>();
    }

    public List<Cluster> clustering(List<FeatureVector> points){
        computeRanges(points);
        computeEditDistances(points);
        computeRangesDistances(points);
        List<Cluster> clusters = new ArrayList<>();
        final Map<FeatureVector, PointStatus> visited = new HashMap<>();

        for (FeatureVector point : points) {
            if (visited.get(point) != null) {
                continue;
            }
            final List<FeatureVector> neighbors = getNeighbors(point, points);
            if (neighbors.size() >= minPts) {
                Cluster cluster = new Cluster();
                clusters.add(expandCluster(cluster, point, neighbors, points, visited));
            } else {
                visited.put(point, PointStatus.NOISE);
            }
        }

        return clusters;
    }

    private Cluster expandCluster(Cluster cluster, FeatureVector point, List<FeatureVector> neighbors, List<FeatureVector> points, Map<FeatureVector, PointStatus> visited) {
        cluster.elements.add(point);
        visited.put(point, PointStatus.PART_OF_CLUSTER);

        List<FeatureVector> seeds = new ArrayList<>(neighbors);
        int index = 0;
        while (index < seeds.size()) {
            FeatureVector current = seeds.get(index);
            PointStatus pStatus = visited.get(current);
            if (pStatus == null) {
                List<FeatureVector> currentNeighbors = getNeighbors(current, points);
                if (currentNeighbors.size() >= minPts) {
                    seeds = merge(seeds, currentNeighbors);
                }
            }

            if (pStatus != PointStatus.PART_OF_CLUSTER) {
                visited.put(current, PointStatus.PART_OF_CLUSTER);
                cluster.elements.add(current);
            }

            index++;
        }
        return cluster;
    }

    private List<FeatureVector> getNeighbors(FeatureVector point, List<FeatureVector> points) {
        List<FeatureVector> neighbors = new ArrayList<>();
        for (FeatureVector neighbor : points) {
            if (point != neighbor && computeDistance(neighbor, point) <= eps) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    private List<FeatureVector> merge(List<FeatureVector> one, List<FeatureVector> two) {
        Set<FeatureVector> oneSet = new HashSet<>(one);
        for (FeatureVector item : two) {
            if (!oneSet.contains(item)) {
                one.add(item);
            }
        }
        return one;
    }

    private double computeDistance(FeatureVector fv1, FeatureVector fv2){
        double distance = 0.0;

        List<String> attributesFrom = new ArrayList<>(){{addAll(fv1.from.keySet()); addAll(fv2.from.keySet());}};
        List<String> attributesTo = new ArrayList<>() {{addAll(fv1.to.keySet()); addAll(fv2.to.keySet());}};

        attributesFrom = attributesFrom.stream().distinct().collect(Collectors.toList());
        attributesTo = attributesTo.stream().distinct().collect(Collectors.toList());

        for(String key: attributesFrom){
            if(fv1.from.containsKey(key) && fv2.from.containsKey(key)){
                if(correlationMiner.tryParseDouble(fv1.from.get(key))){
                    if (attrMax.get("from").get(key) != null && attrMin.get("from").get(key) != null) {
                        distance += computeDistance(Double.parseDouble(fv1.from.get(key)), Double.parseDouble(fv2.from.get(key)), attrMax.get("from").get(key), attrMin.get("from").get(key));
                    }
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
            }
            else
                distance += 1.0;
        }
        for(String key: attributesTo){
            if(fv1.to.containsKey(key) && fv2.to.containsKey(key)){
                if(correlationMiner.tryParseDouble(fv1.to.get(key))){
                    if (attrMax.get("to").get(key) != null && attrMin.get("to").get(key) != null) {
                        distance += computeDistance(Double.parseDouble(fv1.to.get(key)), Double.parseDouble(fv2.to.get(key)), attrMax.get("to").get(key), attrMin.get("to").get(key));
                    }
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

    private double computeDistance(double value1, double value2, double max, double min){
        if(Double.isNaN(value1) || Double.isNaN(value2))
            return 1.0;
        double value = (Math.abs(value1 - value2))/(Math.abs(max - min));
        if(Double.isNaN(value))
            return 0.0;
        else
            return value;
    }

    private double computeDistance(Boolean value1, Boolean value2){
        if(value1.equals(value2)) return 0.0;
        else return 1.0;
    }

    private double computeDistance(String value1, String value2){
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

    private void computeRangesDistances(List<FeatureVector> featureVectorList){
        for(String attribute: featureVectorList.get(0).to.keySet()){
            if(isRange(featureVectorList.get(0).to.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute)).distinct().collect(Collectors.toList());
                HashMap<String, Double> means = computeRangesMeans(values);
                Double min = Collections.min(means.values());
                Double max = Collections.max(means.values());
                HashMap<StringPair, Double> distances = new HashMap<>();
                for(int i = 0; i < values.size(); i++)
                    for (String value : values)
                        distances.put(new StringPair(values.get(i), value), (Math.abs(means.get(values.get(i)) - means.get(value)) / (max - min)));
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
                    for (String value : values)
                        distances.put(new StringPair(values.get(i), value), (Math.abs(means.get(values.get(i)) - means.get(value)) / (max - min)));
                rangesDistances.put(attribute, distances);
            }
        }
    }

    private HashMap<String, Double> computeRangesMeans(List<String> ranges){
        HashMap<String, Double> rangesMeans = new HashMap<>();
        for(String range: ranges){
            String[] values = range.split("-");
            rangesMeans.put(range,(Double.parseDouble(values[0]) + Double.parseDouble(values[1]))/2);
        }
        return rangesMeans;
    }

    private Integer calculateEditDistance(String value1, String value2){
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

    private boolean isRange(String value){
        String[] values = value.split("-");
        return values.length == 2 && correlationMiner.tryParseDouble(values[0]) && correlationMiner.tryParseDouble(values[1]);
    }

    private void computeEditDistances(List<FeatureVector> featureVectorList){
        for(String attribute: featureVectorList.get(0).from.keySet()){
            if(!correlationMiner.tryParseDouble(featureVectorList.get(0).from.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.from.get(attribute)).distinct().collect(Collectors.toList());
                for(int i = 0; i < values.size(); i++)
                    for (String value : values)
                        editDistances.put(new StringPair(values.get(i), value), computeDistance(values.get(i), value));
            }
        }
        for(String attribute: featureVectorList.get(0).to.keySet()){
            if(!correlationMiner.tryParseDouble(featureVectorList.get(0).to.get(attribute))){
                List<String> values = featureVectorList.stream().map(fv -> fv.to.get(attribute)).distinct().collect(Collectors.toList());
                for(int i = 0; i < values.size(); i++)
                    for (String value : values)
                        editDistances.put(new StringPair(values.get(i), value), computeDistance(values.get(i), value));
            }
        }
    }

    private void computeRanges(List<FeatureVector> featureVectorsList){
        if(featureVectorsList.size() > 0){
            HashMap<String, Double> fromMax = new HashMap<>();
            HashMap<String, Double> fromMin = new HashMap<>();
            for(String attribute: featureVectorsList.get(0).from.keySet()){
                if(correlationMiner.tryParseDouble(featureVectorsList.get(0).from.get(attribute))){
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
                if(correlationMiner.tryParseDouble(featureVectorsList.get(0).to.get(attribute))){
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