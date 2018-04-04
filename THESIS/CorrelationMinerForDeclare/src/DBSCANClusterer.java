import java.util.*;

/**
 * Created by volodymyr leno on 10.03.2018.
 */

//@Deprecated
public class DBSCANClusterer{

    double eps;
    int minPts;

    public HashMap<String, Double> attrMax = new HashMap<>();
    public HashMap<String, Double> attrMin = new HashMap<>();
    public HashMap<StringPair, Double> editDistances = new HashMap<>();

    private enum PointStatus {
        NOISE,
        PART_OF_CLUSTER
    }

    public DBSCANClusterer(double eps, int minPts){
        this.eps = eps;
        this.minPts = minPts;
    }

    public List<Cluster> clustering(List<FeatureVector> points){
        calculateRanges(points);
        computeEditDistances(points);

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
            if (point != neighbor && Clustering.computeDistance(neighbor, point, points) <= eps) {
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

    public void calculateRanges(List<FeatureVector> featureVectorsList){
        Clustering.computeRanges(featureVectorsList);
    }

    public void computeEditDistances(List<FeatureVector> featureVectorList){
        Clustering.computeEditDistances(featureVectorList);
        editDistances = Clustering.editDistances;
    }
}