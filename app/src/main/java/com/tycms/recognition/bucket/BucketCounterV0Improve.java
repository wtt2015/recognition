package com.tycms.recognition.bucket;

import android.util.Log;

import com.tycms.recognition.util.Constants;

import org.nelbds.nglite.func.Recognition;

import java.util.List;

/**
 * Created by WangTuan on 2020/9/10.
 */

public class BucketCounterV0Improve implements IBucketCounter {
    private String TAG = "wtt";
    private int verticalSum;
    private int horizontalSum;
    private int bucketInterval;
    private int bucketSum;
    private boolean isNumEnough = false;//参数值是否都达到
    private boolean isHaveTruck = false;//是否有卡车出现
    private int verticalAddNum = 0;//卡车出现前竖斗每出现2次水平斗清零

    public BucketCounterV0Improve() {
        this.reset();
    }

    public void reset() {
        this.verticalSum = 0;
        this.horizontalSum = 0;
        this.bucketInterval = 0;
        this.bucketSum = 0;

        isNumEnough = false;
        isHaveTruck = false;
        verticalAddNum = 0;
    }


    @Override
    public int feedRT(List<Recognition> recognition) {
        int ret = 0;
        recognition = HelperImprove.sortList(recognition);
        if (!isHaveTruck) {
            isHaveTruck = HelperImprove.isHaveTruck(recognition);
        }

        if (HelperImprove.dumpingOrNot4(recognition) == 1) {
            if (this.verticalSum >= Constants.VERTICAL_SUM && this.horizontalSum >= Constants.HORIZONTAL_SUM && this.bucketInterval >= Constants.BUCKET_INTERVAL) {
                isNumEnough = true;
            }
        } else {
            ++this.bucketInterval;
            Log.i(TAG, "bucketInterval +1  bucketInterval= " + bucketInterval);
        }

        if (HelperImprove.verticalOrHorizontal(recognition) == 1) {
            ++this.verticalSum;
            verticalAddNum++;
            Log.i(TAG, "verticalSum +1  verticalSum= " + verticalSum);

            if (!isHaveTruck) {
                if (verticalAddNum == 2 && horizontalSum != 0) {
                    Log.i(TAG, "horizontalSum = " + horizontalSum + " 没有卡车 horizontalSum 清零");
                    this.horizontalSum = 0;
                }
            }

            if (verticalAddNum == 2) {
                verticalAddNum = 0;
            }


//                if (isNumEnough  && (HelperImprove.isOnlyVertical(recognition) || HelperImprove.isBucketTopTruckTop(recognition))) {
            if (isNumEnough  && (HelperImprove.isOnlyVertical(recognition) || !HelperImprove.isAboveAndHaveIntersection(recognition))) {
                ++this.bucketSum;
                Log.i(TAG, "bucketSum +1  bucketSum= " + bucketSum);
                this.verticalSum = 0;
                this.horizontalSum = 0;
                this.bucketInterval = 0;
                ret = 1;
                isNumEnough = false;
                isHaveTruck = false;
            }

        } else if (HelperImprove.verticalOrHorizontal(recognition) == 0) {
            ++this.horizontalSum;
            Log.i(TAG, "horizontalSum +1  horizontalSum= " + horizontalSum);
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

        for (int var8 = 0; var8 < var7; ++var8) {
            int[] verticalDumpPair = var6[var8];
            if (verticalDumpPair[1] != 0) {
                if (verticalSum >= 3 && horizontalSum >= 3 && bucketInterval >= 7) {
                    ++bucketSumF0;
                    verticalSum = 0;
                    horizontalSum = 0;
                    bucketInterval = 0;
                }
            } else {
                ++bucketInterval;
            }

            if (verticalDumpPair[0] == 1) {
                ++verticalSum;
            } else if (verticalDumpPair[0] == 0) {
                ++horizontalSum;
            }
        }

        return bucketSumF0;
    }

    public int getInternalCount() {
        return this.bucketSum;
    }
}

