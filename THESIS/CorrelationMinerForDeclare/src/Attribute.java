/**
 * Created by volodymyr leno on 08.03.2018.
 */
public class Attribute {
    String value;
    Double inhomogeneity;

    Attribute(String value, Double inhomogeneity){
        this.value = value;
        this.inhomogeneity = inhomogeneity;
    }

    public String toString(){
        return value + ": " + inhomogeneity;
    }
}
