import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Correlation(String from, String to, List<String> antecedent, List<String> consequent){
        this.from = from;
        this.to = to;
        this.antecedent = new ArrayList<>(antecedent);
        this.consequent = new ArrayList<>(consequent);

    }

    public List<String> simplifyConstraint(List<String> constraint){
        if(constraint.size() == 0 || constraint.get(0).equals("-"))
            return constraint;
        else {
            HashMap<String, List<String>> simplifiedConstraints = new HashMap<>();
            List<String> constraints = new ArrayList<>();
            for (String rule : constraint) {
                String[] params = rule.split("\\s[<!>=]+\\s");
                String attribute = params[0];

                if (!simplifiedConstraints.containsKey(attribute))
                    simplifiedConstraints.put(attribute, new ArrayList<>());
            }

            for (String attribute : simplifiedConstraints.keySet()) {
                HashMap<String, List<String>> operators = new HashMap<>();
                for (String rule : constraint) {
                    Pattern pattern = Pattern.compile("[<!>=]+");
                    Matcher matcher = pattern.matcher(rule);
                    String operator = "";
                    if (matcher.find())
                        operator = matcher.group();

                    String[] params = rule.split("\\s[<!>=]+\\s");
                    String attr = params[0];
                    String value = params[1];

                    if (attr.equals(attribute)) {
                        if (operators.containsKey(operator))
                            operators.get(operator).add(value);
                        else
                            operators.put(operator, new ArrayList<String>(Collections.singletonList(value)));
                    }
                }
                for (String operator : operators.keySet()) {
                    if (operator.equals(">"))
                        simplifiedConstraints.get(attribute).add(attribute + " > " + operators.get(operator).stream().max(String::compareTo).get());
                    else if (operator.equals("<="))
                        simplifiedConstraints.get(attribute).add(attribute + " <= " + operators.get(operator).stream().min(String::compareTo).get());
                    else if (operator.equals("!=")){
                        if(operators.containsKey("="))
                            simplifiedConstraints.get(attribute).add(attribute + " = " + operators.get("=").get(0));
                        else{
                            for(int i = 0; i < operators.get("!=").size(); i++)
                                simplifiedConstraints.get(attribute).add(attribute + " != " + operators.get(operator).get(i));
                        }
                    }
                    else{
                        for(int i = 0; i < operators.get(operator).size(); i++)
                            simplifiedConstraints.get(attribute).add(attribute + " " + operator + " " + operators.get(operator).get(i));
                    }
                }

                for (String rule : simplifiedConstraints.get(attribute))
                    constraints.add(rule);
            }
            return constraints;
        }
    }

    public String toString(){
        return from + ": " + antecedent + " => " + to + ": " + consequent;
    }
}