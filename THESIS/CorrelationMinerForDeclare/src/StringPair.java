import java.util.Comparator;
import java.util.Objects;

/**
 * Created by volodymyr leno on 08.03.2018.
 */
public class StringPair implements Comparator<StringPair> {
    private String value1;
    private String value2;

    StringPair(String v1, String v2){
        value1 = v1;
        value2 = v2;
    }

    public String toString(){
        return value1 + " " + value2;
    }

    public int compare(StringPair sp1, StringPair sp2){
        if(sp1.value1.compareTo(sp2.value1) == 0)
            return sp1.value2.compareTo(sp2.value2);
        else
            return sp1.value1.compareTo(sp2.value1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StringPair)) {
            return false;
        }
        StringPair  stringPair = (StringPair) o;
        return compare(this, stringPair) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value1, value2);
    }
}
