package com.tycms.recognition.bucket;

/**
 * 挖斗装车过程状态
 * Created by WangTuantuan on 2020/9/21.
 */
public enum LoadingState {
    INITIAL_STATE,//初始状态
    DIGGING,//挖掘中
    TRANSPORTING,//运送中
    READY_TO_LOAD,//准备装车
    LOADING,//装车
}
