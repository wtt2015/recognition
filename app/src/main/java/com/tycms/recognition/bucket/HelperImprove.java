package com.tycms.recognition.bucket;

import android.graphics.RectF;
import android.util.Log;

import org.nelbds.nglite.func.Recognition;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by WangTuan on 2020/9/10.
 */

public class HelperImprove {
    public HelperImprove() {
    }


    /**
     * 对集合进行排序，如果长度为2的话把挖斗放在0位，卡车放在1位
     *
     * @param list
     * @return
     */
    public static List<Recognition> sortList(List<Recognition> list) {
        if (list != null && list.size() == 2) {
            List<Recognition> newList = new ArrayList<>();
            Recognition bucket = null;
            Recognition truck = null;

            for (Recognition recognition : list) {
                String title = recognition.getTitle();
                if (title.startsWith("bucket")) {
                    bucket = recognition;
                } else if (title.startsWith("Truck")) {
                    truck = recognition;
                }
            }
            if (bucket != null) {
                newList.add(bucket);
            }
            if (truck != null) {
                newList.add(truck);
            }
            Log.i("sort", "list 0 is: " + newList.get(0).getTitle());
            Log.i("sort", "list 1 is: " + newList.get(1).getTitle());
            return newList;
        }
        return list;
    }

    /**
     * 判断集合里时候有挖斗竖直或水平状态
     *
     * @param list
     * @return 1竖直 0水平  -1无挖斗
     */
    public static int getVerticalOrHorizontal(List<Recognition> list) {
        for (Recognition recognition : list) {
            String title = recognition.getTitle();
            if (title.startsWith("bucket1")) {
                return 1;
            } else if (title.startsWith("bucket0")) {
                return 0;
            }
        }
        return -1;
    }

    /**
     * 铲斗是否在卡车正上方（精度更高）
     *
     * @return
     */
    public static boolean isInInternal(List<Recognition> list) {

        RectF bucketRectF = list.get(0).getLocation();
        float x0 = bucketRectF.left;
        float y0 = bucketRectF.top;
        float w0 = bucketRectF.right - x0;
        float h0 = bucketRectF.bottom - y0;

        RectF truckRectF = list.get(1).getLocation();
        float x1 = truckRectF.left;
        float y1 = truckRectF.top;
        float w1 = truckRectF.right - x1;
        float h1 = truckRectF.bottom - y1;

        return x0 >= x1 && x0 + w0 <= x1 + w1 && y0 + h0 / 2.0F <= y1 + h1 / 2.0F;
    }

    /**
     * 挖斗在卡车上方并且有交集
     */
    public static boolean isAboveAndHaveIntersection(List<Recognition> list) {

        RectF rect1 = list.get(0).getLocation();
        RectF rect2 = list.get(1).getLocation();

        float bucketCenterLine = (rect1.top + rect1.bottom) / 2f;
        float truckCenterLine = (rect2.top + rect2.bottom) / 2f;

        if (bucketCenterLine > truckCenterLine) {
            Log.i("HaveIntersection", "挖斗不在卡车上方");
            return false;
        }

        float leftColumnMax = Math.max(rect1.left, rect2.left);
        float rightColumnMin = Math.min(rect1.right, rect2.right);
        float upRowMax = Math.max(rect1.top, rect2.top);
        float downRowMin = Math.min(rect1.bottom, rect2.bottom);

        if (leftColumnMax >= rightColumnMin || downRowMin <= upRowMax) {
            Log.i("HaveIntersection", "挖斗在卡车上方 但无交集");
            return false;
        }

//        float rect1Width = rect1.right - rect1.left;
//        float rect1Height = rect1.bottom - rect1.top;
//
//        float rect2Width = rect2.right - rect2.left;
//        float rect2Height = rect2.bottom - rect2.top;

//        float s1 = rect1Width * rect1Height;
//        float s2 = rect2Width * rect2Height;
        float sCross = (downRowMin - upRowMax) * (rightColumnMin - leftColumnMax);
        Log.i("HaveIntersection", "挖斗在卡车上方且有交集 交集大小为：" + sCross);
        return sCross > 0;
    }

    /**
     * 铲斗上边缘高于卡车，下边缘低于卡车（精度居中）
     */
    public static boolean isInInternalTopBottom(List<Recognition> list) {
        RectF bucketRectF = list.get(0).getLocation();
        RectF truckRectF = list.get(1).getLocation();

        float bucketTop = bucketRectF.top;
        float bucketBottom = bucketRectF.bottom;
        float TruckTop = truckRectF.top;

        return bucketTop < TruckTop && bucketBottom > TruckTop;
    }

    /**
     * 铲斗是否在卡车上方（精度略低，只要求上方）
     *
     * @return
     */
    public static boolean isInTop(List<Recognition> list) {

        RectF bucketRectF = list.get(0).getLocation();
        RectF truckRectF = list.get(1).getLocation();

        float h0 = bucketRectF.bottom;
        float h1 = truckRectF.bottom;
        return h0 / 2.0F <= h1 / 2.0F;
    }

    /**
     * 铲斗上边缘低于卡车上边缘
     */
    public static boolean isInInternalTopTop(List<Recognition> list) {

        RectF bucketRectF = list.get(0).getLocation();
        RectF truckRectF = list.get(1).getLocation();

        float bucketTop = bucketRectF.top;
        float TruckTop = truckRectF.top;

        return bucketTop > TruckTop;
    }

    /**
     * 挖斗 竖直或水平
     *
     * @param recognitions
     * @return 1竖直 0水平  -1无挖斗
     */
    public static int verticalOrHorizontal(List<Recognition> recognitions) {
        return recognitions != null && !recognitions.isEmpty() ?
                getVerticalOrHorizontal(recognitions)
                : -1;
    }


    /**
     * 是否只有竖斗
     *
     * @param recognitions
     * @return
     */
    public static boolean isOnlyVertical(List<Recognition> recognitions) {
        return recognitions != null && recognitions.size() == 1 && recognitions.get(0).getTitle().startsWith("bucket1");
    }

    /**
     * 铲斗上线在卡车上线下面（此时可认为铲斗在卡车旁边）
     *
     * @param recognitions
     * @return
     */
    public static boolean isBucketTopTruckTop(List<Recognition> recognitions) {
        return recognitions != null && recognitions.size() == 2 && isInInternalTopTop(recognitions);
    }

    /**
     * 是否出现卡车
     *
     * @param recognitions
     * @return
     */
    public static boolean isHaveTruck(List<Recognition> recognitions) {
        for (Recognition recognition : recognitions) {
            if (recognition.getTitle().startsWith("Truck")) {
                return true;
            }
        }
        return false;
    }


    /**
     * 铲斗在卡车内部正上方
     *
     * @param recognitions
     * @return
     */
    public static int dumpingOrNot(List<Recognition> recognitions) {
        return recognitions != null && !recognitions.isEmpty() ?
                (recognitions.size() == 1 ? 0 : (recognitions.size() == 2 ? (isInInternal(recognitions) ? 1 : 0) : 0))
                : -1;
    }

    /**
     * 铲斗和卡车同时出现
     *
     * @param recognitions
     * @return
     */
    public static int dumpingOrNot1(List<Recognition> recognitions) {
        return recognitions != null && !recognitions.isEmpty() ?
                (recognitions.size() == 1 ? 0 : (recognitions.size() == 2 ? 1 : 0))
                : -1;
    }

    /**
     * 铲斗在卡车上方
     *
     * @param recognitions
     * @return
     */
    public static int dumpingOrNot2(List<Recognition> recognitions) {
        return recognitions != null && !recognitions.isEmpty() ?
                (recognitions.size() == 1 ? 0 : (recognitions.size() == 2 ? (isInTop(recognitions) ? 1 : 0) : 0))
                : -1;
    }

    /**
     * 铲斗上边缘高于卡车，下边缘低于卡车
     *
     * @param recognitions
     * @return
     */
    public static int dumpingOrNot3(List<Recognition> recognitions) {
        return recognitions != null && !recognitions.isEmpty() ?
                (recognitions.size() == 1 ? 0 : (recognitions.size() == 2 ? (isInInternalTopBottom(recognitions) ? 1 : 0) : 0))
                : -1;
    }


    /**
     * 挖斗在卡车上方 并且有交集
     *
     * @param recognitions
     * @return
     */
    public static int dumpingOrNot4(List<Recognition> recognitions) {
        return recognitions != null && !recognitions.isEmpty() ?
                (recognitions.size() == 1 ? 0 : (recognitions.size() == 2 ? ((isAboveAndHaveIntersection(recognitions) || isInInternal(recognitions) )? 1 : 0) : 0))
                : -1;
    }

}

