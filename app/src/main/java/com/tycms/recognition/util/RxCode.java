package com.tycms.recognition.util;

public class RxCode {
    /**
     * 初始状态
     */
    public static final int STATE_INITIAL_STATE = 1000;

    /**
     * 挖掘中
     */
    public static final int STATE_DIGGING = 1001;

    /**
     * 运送中
     */
    public static final int STATE_TRANSPORTING = 1002;

    /**
     * 准备装车
     */
    public static final int STATE_READY_TO_LOAD = 1003;

    /**
     * 装车
     */
    public static final int STATE_LOADING = 1004;

    /**
     * 真实装车
     */
    public static final int STATE_LOADING_FINISH = 1005;


}
