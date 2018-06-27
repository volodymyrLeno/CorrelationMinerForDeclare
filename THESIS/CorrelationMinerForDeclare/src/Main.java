import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
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

        correlationMiner.findCorrelations(csvFile, rulesList, absoluteSupportThreshold, minDistance, minPts, supportThreshold, prunning);
    }
}