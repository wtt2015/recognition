package com.tycms.recognition.bucket;

import android.util.Log;

import org.nelbds.nglite.func.Recognition;
import java.util.List;

/**
 * Created by kangweibo01 on 2020/5/8.
 */

public class BucketCounterV0  implements IBucketCounter {
    private String TAG = "wtt";
    private int verticalSum;
    private int horizontalSum;
    private int bucketInterval;
    private int bucketSum;

    public BucketCounterV0() {
        this.reset();
    }

    private void reset() {
        this.verticalSum = 0;
        this.horizontalSum = 0;
        this.bucketInterval = 0;
        this.bucketSum = 0;
    }

    public int feedRT(List<Recognition> recognition) {
        int ret = 0;
        recognition = Helper.filter(recognition);
        if(Helper.dumpingOrNot(recognition) != 0) {
//            if(this.verticalSum >= 3 && this.horizontalSum >= 3 && this.bucketInterval >= 7) {
               if(this.verticalSum >= 2 && this.horizontalSum >= 1 && this.bucketInterval >= 2) {
//            if(this.verticalSum >= 2 && this.horizontalSum >= 1 && this.bucketInterval >= 3) {
                ++this.bucketSum;
                Log.i(TAG,"bucketSum +1  bucketSum= " +bucketSum);
                this.verticalSum = 0;
                this.horizontalSum = 0;
                this.bucketInterval = 0;
                ret = 1;
            }
        } else {
            ++this.bucketInterval;
            Log.i(TAG,"bucketInterval +1  bucketInterval= " +bucketInterval);
        }

        if(Helper.verticalOrNot(recognition) == 1) {
            ++this.verticalSum;
            Log.i(TAG,"verticalSum +1  verticalSum= " +verticalSum);
        } else if(Helper.verticalOrNot(recognition) == 0) {
            ++this.horizontalSum;
            Log.i(TAG,"horizontalSum +1  horizontalSum= " +horizontalSum);
        }

        return ret;
    }

    public int feedOver(List<Recognition> recognitions) {
        this.reset();
        return 0;
    }

    public int feedOL(int[][] resultPro) {
        int horizontalSum = 0;
        int verticalSum = 0;
        int bucketSumF0 = 0;
        int bucketInterval = 0;
        int[][] var6 = resultPro;
        int var7 = resultPro.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            int[] verticalDumpPair = var6[var8];
            if(verticalDumpPair[1] != 0) {
                if(verticalSum >= 3 && horizontalSum >= 3 && bucketInterval >= 7) {
                    ++bucketSumF0;
                    verticalSum = 0;
                    horizontalSum = 0;
                    bucketInterval = 0;
                }
            } else {
                ++bucketInterval;
            }

            if(verticalDumpPair[0] == 1) {
                ++verticalSum;
            } else if(verticalDumpPair[0] == 0) {
                ++horizontalSum;
            }
        }

        return bucketSumF0;
    }

    public int getInternalCount() {
        return this.bucketSum;
    }
}

