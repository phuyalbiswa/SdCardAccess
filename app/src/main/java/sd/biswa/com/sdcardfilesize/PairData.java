package sd.biswa.com.sdcardfilesize;

import java.io.Serializable;

public class PairData implements Serializable{
    public String name;
    public Object value;

    public PairData(String key, Object v){
        name = key;
        value = v;
    }
}
