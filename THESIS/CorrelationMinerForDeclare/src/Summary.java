import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by volodymyr leno on 13.02.2018.
 */
public final class Summary {
    public static Integer getTotalNumber(List<Correlation> correlations){
        return correlations.size();
    }

    public static void antecedentInfo(List<Correlation> correlations){
        System.out.println();
        antecedentAttributes(correlations);
        System.out.println();
        antecedentLength(correlations);
    }

    public static void antecedentAttributes(List<Correlation> correlations){
        HashMap<String, Integer> attributeAppearence = new HashMap<>();
        for(Correlation correlation: correlations){
            for(String rule: correlation.antecedent){
                String[] params = rule.split("\\s[<!>=]+\\s");
                String attribute = params[0];
                if(attributeAppearence.containsKey(attribute))
                    attributeAppearence.put(attribute, attributeAppearence.get(attribute) + 1);
                else
                    attributeAppearence.put(attribute, 1);
            }
        }
        System.out.println("Attribute appearences in antecedent part:\n");

        attributeAppearence.entrySet().stream().sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
                .forEach(k -> System.out.println(k.getKey() + ": " + k.getValue()));
    }

    public static void antecedentLength(List<Correlation> correlations){
        List<Integer> lengths = correlations.stream().map(correlation -> correlation.antecedent.size()).collect(Collectors.toList());
        System.out.println("Max Length - " + Collections.max(lengths));
        System.out.println("Min Length - " + Collections.min(lengths));
        Integer sum = lengths.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Average Length - " + String.format("%.2f", (double)sum/correlations.size()));
    }

    public static void consequentAttributes(List<Correlation> correlations){
        HashMap<String, Integer> attributeAppearence = new HashMap<>();
        for(Correlation correlation: correlations){
            for(String rule: correlation.consequent){
                String[] params = rule.split("\\s[<!>=]+\\s");
                String attribute = params[0];
                if(attributeAppearence.containsKey(attribute))
                    attributeAppearence.put(attribute, attributeAppearence.get(attribute) + 1);
                else
                    attributeAppearence.put(attribute, 1);
            }
        }
        System.out.println("Attribute appearences in consequent part:\n");

        attributeAppearence.entrySet().stream().sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
                .forEach(k -> System.out.println(k.getKey() + ": " + k.getValue()));
    }

    public static void consequentLength(List<Correlation> correlations){
        List<Integer> lengths = correlations.stream().map(correlation -> correlation.consequent.size()).collect(Collectors.toList());
        System.out.println("Max Length - " + Collections.max(lengths));
        System.out.println("Min Length - " + Collections.min(lengths));
        Integer sum = lengths.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Average Length - " + String.format("%.2f", (double)sum/correlations.size()));
    }

    public static void patternLength(List<Correlation> correlations){
        List<Integer> lengthsAntecedent = correlations.stream().map(correlation -> correlation.antecedent.size()).collect(Collectors.toList());
        List<Integer> lengthsConsequent = correlations.stream().map(correlation -> correlation.consequent.size()).collect(Collectors.toList());
        List<Integer> lengths = new ArrayList<>();
        for(int i = 0; i < lengthsAntecedent.size(); i++)
            lengths.add(lengthsAntecedent.get(i) + lengthsConsequent.get(i));
        System.out.println("Max Length - " + Collections.max(lengths));
        System.out.println("Min Length - " + Collections.min(lengths));
        Integer sum = lengths.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Average Length - " + String.format("%.2f", (double)sum/correlations.size()));
    }

    public static void getCoverage(HashMap<String, List<Event>> cases, HashMap<Itemset, List<FeatureVector>> featureVectors, String ruleType){
        Integer coveredNumber = 0;
        for(Itemset itemset: featureVectors.keySet())
            coveredNumber += featureVectors.get(itemset).size();
        Integer totalNumber = 0;
        for(String caseID: cases.keySet()){
            List<String> activities = new ArrayList<>();
            cases.get(caseID).stream().filter(event -> !activities.contains(event.activityName)).
                    forEach(event -> activities.add(event.activityName));
            for(String activity1: activities)
                for(String activity2: activities)
                    totalNumber += getFrequency(cases.get(caseID), activity1, activity2, ruleType);
        }
        System.out.println("Total number: " + totalNumber);
        System.out.println("Covered number: " + coveredNumber);
    }

    static Integer getFrequency(List<Event> eventList, String from, String to, String ruleType) {
        Integer frequency = 0;
        List<Integer> id1 = new ArrayList<>();
        List<Integer> id2 = new ArrayList<>();
        for (int i = 0; i < eventList.size(); i++) {
            if (eventList.get(i).activityName.equals(from)) id1.add(i);
            if (eventList.get(i).activityName.equals(to)) id2.add(i);
        }
        if(id1.size() > 0 && id2.size() > 0){
            switch (ruleType) {
                case "precedence":
                case "chain precedence":
                    for (Integer i2 : id2)
                        for (Integer i1 : id1)
                            if ((ruleType.equals("precedence") && i2 > i1) || (ruleType.equals("chain precedence") && (i2 - i1 == 1))) {
                                frequency++;
                                break;
                            }
                    break;
                case "response":
                case "chain response":
                    for (Integer i1 : id1)
                        for (Integer i2 : id2)
                            if ((ruleType.equals("response") && i2 > i1) || (ruleType.equals("chain response") && (i2 - i1 == 1))) {
                                frequency++;
                                break;
                            }
                    break;
                case "responded existence":
                    for (Integer i1 : id1)
                        for (Integer i2 : id2) {
                            frequency++;
                            break;
                        }
                    break;
                case "alternate response":
                    for (Integer i1 : id1)
                        for (Integer i2 : id2)
                            if (i2 > i1 && id1.stream().noneMatch(el -> el > i1 && el < i2)) {
                                frequency++;
                                break;
                            }
                    break;
                case "alternate precedence":
                    for (Integer i2 : id2)
                        for (Integer i1 : id1)
                            if (i2 > i1 && id2.stream().noneMatch(el -> el > i1 && el < i2)) {
                                frequency++;
                                break;
                            }
                    break;
            }
        }
        return frequency;
    }

    static double getRelativeSupport(HashMap<String, List<Event>> cases, List<FeatureVector> featureVectorList, Correlation correlation){
        Integer activationFrequency = 0;
        for(String caseID: cases.keySet()){
            for(Event event: cases.get(caseID))
                if(event.activityName.equals(correlation.from))
                    activationFrequency++;
        }
        if(correlation.consequent.get(0).equals("-"))
            return(double)featureVectorList.stream().filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) && fv.to == null).collect(Collectors.toList()).size()/activationFrequency;
        else
            return (double)featureVectorList.stream().filter(fv -> fv.from != null && fv.to != null).filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) &&
                    ruleSatisfaction(fv.to, correlation.consequent)).collect(Collectors.toList()).size()/activationFrequency;
    }

    static double getConfidence(HashMap<String, List<Event>> cases, List<FeatureVector> featureVectorList, Correlation correlation){
        List<Event> activations = new ArrayList<>();
        for(String caseID: cases.keySet()){
            for(Event event: cases.get(caseID))
                if(event.activityName.equals(correlation.from))
                    activations.add(event);
        }
        Integer coverage = activations.stream().filter(fv -> ruleSatisfaction(fv.payload, correlation.antecedent)).collect(Collectors.toList()).size();
        Integer ruleFrequency = 0;
        if(correlation.consequent.get(0).equals("-"))
            ruleFrequency = featureVectorList.stream().filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) &&
                    fv.to == null).collect(Collectors.toList()).size();
        else
            ruleFrequency = featureVectorList.stream().filter(fv -> fv.from != null && fv.to != null).filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) &&
                    ruleSatisfaction(fv.to, correlation.consequent)).collect(Collectors.toList()).size();

        return (double)ruleFrequency/coverage;
    }

    static boolean ruleSatisfaction(HashMap<String, String> payload, List<String> rules){
        for(String rule: rules){
            Pattern pattern = Pattern.compile("[<!>=]+");
            Matcher matcher = pattern.matcher(rule);
            String operator = "";
            if(matcher.find())
                operator = matcher.group();

            String[] params = rule.split("\\s[<!>=]+\\s");
            String attribute = params[0];
            String value = params[1];
            switch(operator){
                case "=": {
                    if (Clustering.isRange(value)){
                        String[] values1 = value.split("-");
                        String[] values2 = payload.get(attribute).split("-");
                        if(Double.parseDouble(values1[0]) > Double.parseDouble(values2[0]) || Double.parseDouble(values1[1]) < Double.parseDouble(values2[1]))
                            return false;
                    }
                    else if(!payload.get(attribute).equals(value)) return false;
                } break;
                case "!=": if(payload.get(attribute).equals(value)) return  false; break;
                case ">": if(Double.parseDouble(payload.get(attribute)) <= Double.parseDouble(value)) return false; break;
                case "<=": if(Double.parseDouble(payload.get(attribute)) > Double.parseDouble((value))) return false; break;
                default: break;
            }
        }
        return true;
    }
}