package com.tycms.recognition.util;

/**
 * Created by WangTuantuan on 2020/9/14.
 */
public class Constants {

    public static float CONFIDENCE_BUCKET_1 = 0.8f;
    public static float CONFIDENCE_BUCKET_0 = 0.4f;
    public static float CONFIDENCE_TRUCK = 0.4f;
    public static int VERTICAL_SUM = 5;
    public static int HORIZONTAL_SUM = 2;
    public static int BUCKET_INTERVAL = 7;


    public static final String INITIAL_STATE = "初始状态";
    public static final String DIGGING = "挖掘中";
    public static final String TRANSPORTING = "运送中";
    public static final String READY_TO_LOAD = "准备装车";
    public static final String LOADING = "装车";
    public static final String LOADING_FINISH = "真实装车";


}
