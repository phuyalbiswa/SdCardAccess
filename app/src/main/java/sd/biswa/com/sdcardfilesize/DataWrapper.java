package sd.biswa.com.sdcardfilesize;

import java.io.Serializable;
import java.util.ArrayList;

public class DataWrapper implements Serializable{
    public ArrayList<PairData> mDataList;

    public DataWrapper(ArrayList<PairData> list){
        mDataList = list;
    }
}
