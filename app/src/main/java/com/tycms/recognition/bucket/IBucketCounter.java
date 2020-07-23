package com.tycms.recognition.bucket;

import org.nelbds.nglite.func.Recognition;
import java.util.List;

/**
 * Created by kangweibo01 on 2020/5/8.
 */

public interface IBucketCounter {
    int feedRT(List<Recognition> var1);

    int feedOver(List<Recognition> var1);

    int feedOL(int[][] var1);

    int feedRT20200706(List<Recognition> var1);

    int getInternalCount();
}
