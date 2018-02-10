import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by volodymyr leno on 23.01.2018.
 */
public class Correlation {
    String from;
    String to;
    List<String> antecedent;
    List<String> consequent;

    public Correlation(Itemset itemset, List<String> antecedent, List<String> consequent){
        this.from = itemset.items.get(0);
        this.to = itemset.items.get(1);
        this.antecedent = new ArrayList<>(antecedent);
        this.consequent = new ArrayList<>(consequent);
    }

    public Double getConfidence(List<FeatureVector> featureVectorList){
        List<FeatureVector> coverSet = featureVectorList.stream().filter(fv -> satisfyAntecedent(fv)).collect(Collectors.toList());
        return (double)coverSet.stream().filter(fv -> satisfyConsequent(fv)).count()/coverSet.size();
    }

    public boolean satisfyAntecedent(FeatureVector featureVector){
        for(String rule: antecedent){
            Pattern pattern = Pattern.compile("[<!>=]+");
            Matcher matcher = pattern.matcher(rule);
            String operator = "";
            if(matcher.find())
                operator = matcher.group();

            String[] params = rule.split("\\s[<!>=]+\\s");
            String attribute = params[0];
            String value = params[1];

            switch(operator){
                case "=": if(!featureVector.from.get(attribute).equals(value)) return false; break;
                case "!=": if(featureVector.from.get(attribute).equals(value)) return  false; break;
                case ">": if(Double.parseDouble(featureVector.from.get(attribute)) <= Double.parseDouble(value)) return false; break;
                case "<=": if(Double.parseDouble(featureVector.from.get(attribute)) > Double.parseDouble((value))) return false; break;
                default: break;
            }
        }
        return true;
    }

    public boolean satisfyConsequent(FeatureVector featureVector){
        for(String rule: consequent){

            Pattern pattern = Pattern.compile("[<!>=]+");
            Matcher matcher = pattern.matcher(rule);
            String operator = "";
            if(matcher.find())
                operator = matcher.group();

            String[] params = rule.split("\\s[<!>=]+\\s");
            String attribute = params[0];
            String value = params[1];

            switch(operator){
                case "=": if(!featureVector.to.get(attribute).equals(value)) return false; break;
                case "!=": if(featureVector.to.get(attribute).equals(value)) return  false; break;
                case ">": if(Double.parseDouble(featureVector.to.get(attribute)) <= Double.parseDouble(value)) return false; break;
                case "<=": if(Double.parseDouble(featureVector.to.get(attribute)) > Double.parseDouble((value))) return false; break;
                default: break;
            }
        }
        return true;
    }

    public String toString(){
        return from + ": " + antecedent + " => " + to + ": " + consequent;
    }
}