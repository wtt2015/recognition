package com.tycms.recognition.bucket;

import android.util.Log;

import com.tycms.recognition.util.Constants;
import com.tycms.recognition.util.RxCode;

import org.nelbds.nglite.func.Recognition;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by WangTuan on 2020/9/21.
 */

public class BucketCounterV0StateControl {

    private String TAG_ShuDou = "shudoulog";
    private int bucketSum;
    private int verticalAddNum = 0;//竖斗增加的数量
    private int horizontalAddNum = 0;//平斗增加的数量
    private int noTruckNum = 0;//发现没有卡车的数量

    private int mDistanceComparisonCount = 0;//对比挖斗远离/靠近卡车次数
    private List<Recognition> mList = new ArrayList<>();

    private LoadingState mLoadingState = LoadingState.INITIAL_STATE;


    public BucketCounterV0StateControl() {
        this.reset();
    }

    public interface BucketCounterInterface {
        void updateState(long millisecond, String state);
    }

    private BucketCounterInterface mBucketCounterInterface;

    public void setBucketCounterInterface(BucketCounterInterface bucketCounterInterface) {
        this.mBucketCounterInterface = bucketCounterInterface;
    }

    public void reset() {
        this.bucketSum = 0;
        shuDouReset();
    }

    public int feedRT(List<Recognition> recognition) {
        int ret = 0;
        recognition = HelperImprove.sortList(recognition);

//        if (mLoadingState == LoadingState.TRANSPORTING || mLoadingState == LoadingState.READY_TO_LOAD) {
//
//            if (recognition != null && recognition.size() == 2) {
//                if (HelperImprove.isHaveIntersection(recognition)) {
//                    if (mLoadingState == LoadingState.TRANSPORTING) {
//                        Log.i(TAG_ShuDou, "运送中出现交集 →准备装车");
//                    } else if (mLoadingState == LoadingState.READY_TO_LOAD) {
//                        Log.i(TAG_ShuDou, "准备装车中出现交集 →准备装车");
//                    }
//                    mLoadingState = LoadingState.READY_TO_LOAD;
//                    RxBus.get().send(RxCode.STATE_READY_TO_LOAD, Constants.READY_TO_LOAD);
//                } else {
//                    mDistanceComparisonCount++;
//                    if (mDistanceComparisonCount == 1) {
//                        mList.addAll(recognition);
//                    } else if (mDistanceComparisonCount == 2) {
//                        if (HelperImprove.getFarAwayOrCloseTo(mList, recognition) == -1) {
//                            mLoadingState = LoadingState.READY_TO_LOAD;
//                            RxBus.get().send(RxCode.STATE_READY_TO_LOAD, Constants.READY_TO_LOAD);
//                            Log.i(TAG_ShuDou, "运送中 距离靠近 →准备装车");
//                        }
//                        mDistanceComparisonCount = 0;
//                        mList.clear();
//                    }
//                }
//            }
//        }


        if (mLoadingState == LoadingState.TRANSPORTING || mLoadingState == LoadingState.READY_TO_LOAD) {

            if (recognition != null && recognition.size() == 2) {

                if (mLoadingState == LoadingState.TRANSPORTING) {
                    Log.i(TAG_ShuDou, "运送中发现卡车和挖斗 →准备装车");
                    mLoadingState = LoadingState.READY_TO_LOAD;
//                    RxBus.get().send(RxCode.STATE_READY_TO_LOAD, Constants.READY_TO_LOAD);
                    updateState(Constants.READY_TO_LOAD);
                } else if (mLoadingState == LoadingState.READY_TO_LOAD) {
                    if (HelperImprove.isHaveIntersection(recognition)) {
                        Log.i(TAG_ShuDou, "准备装车中出现交集 →准备装车");
                        mLoadingState = LoadingState.READY_TO_LOAD;
//                        RxBus.get().send(RxCode.STATE_READY_TO_LOAD, Constants.READY_TO_LOAD);
                        updateState(Constants.READY_TO_LOAD);
                    } else {
                        mDistanceComparisonCount++;
                        if (mDistanceComparisonCount == 1) {
                            mList.addAll(recognition);
                        } else if (mDistanceComparisonCount == 2) {
                            if (HelperImprove.getFarAwayOrCloseTo(mList, recognition) == -1) {
                                mLoadingState = LoadingState.READY_TO_LOAD;
//                                RxBus.get().send(RxCode.STATE_READY_TO_LOAD, Constants.READY_TO_LOAD);
                                updateState(Constants.READY_TO_LOAD);
                                Log.i(TAG_ShuDou, "准备装车中 距离靠近 →准备装车");
                            }
                            mDistanceComparisonCount = 0;
                            mList.clear();
                        }
                    }
                }

            }
        }

        if (mLoadingState == LoadingState.READY_TO_LOAD) {
            if (recognition != null && recognition.size() == 2) {

                mDistanceComparisonCount++;
                if (mDistanceComparisonCount == 1) {
                    mList.addAll(recognition);
                } else if (mDistanceComparisonCount == 2) {
                    if (HelperImprove.getFarAwayOrCloseTo(mList, recognition) == 1) {
                        mLoadingState = LoadingState.LOADING;
//                        RxBus.get().send(RxCode.STATE_LOADING, Constants.LOADING);
                        updateState(Constants.LOADING);
                        Log.i(TAG_ShuDou, "准备装车中 距离远离 →装车后状态");
                    }
                    mDistanceComparisonCount = 0;
                    mList.clear();
                }

            } else {
                if (recognition != null) {
                    boolean isHaveTruck = HelperImprove.isHaveTruck(recognition);
                    if (!isHaveTruck) {
                        noTruckNum++;
                        if (noTruckNum >= 2) {
                            mLoadingState = LoadingState.LOADING;
//                            RxBus.get().send(RxCode.STATE_LOADING, Constants.LOADING);
                            updateState(Constants.LOADING);
                            Log.i(TAG_ShuDou, "准备装车中 发现2次无卡车 →装车后状态");
                            noTruckNum = 0;
                        }
                    }
                }
            }
        }


        if (HelperImprove.verticalOrHorizontal(recognition) == 1) {
            verticalAddNum++;
            /**
             * 初始状态或者挖掘中出现2次竖斗 置为挖掘状态
             */
            if (mLoadingState == LoadingState.INITIAL_STATE) {
                if (verticalAddNum >= 2) {
                    mLoadingState = LoadingState.DIGGING;
                    Log.i(TAG_ShuDou, "初始态→挖掘状态");
//                    RxBus.get().send(RxCode.STATE_DIGGING, Constants.DIGGING);
                    updateState(Constants.DIGGING);
                }
            }

            /**
             * 运送状态中出现2次竖斗 判断为卸货 置为初始状态
             */
            if (mLoadingState == LoadingState.TRANSPORTING) {
                if (verticalAddNum >= 2) {
                    mLoadingState = LoadingState.INITIAL_STATE;
                    Log.i(TAG_ShuDou, "运送状态中出现2次竖斗 判断为卸货 →初始状态");
//                    RxBus.get().send(RxCode.STATE_INITIAL_STATE, Constants.INITIAL_STATE);
                    updateState(Constants.INITIAL_STATE);
                }
            }

            if (mLoadingState == LoadingState.LOADING) {
                if (verticalAddNum >= 2) {
                    ++this.bucketSum;
//                    RxBus.get().send(RxCode.STATE_LOADING_FINISH, Constants.LOADING_FINISH);
                    updateState(Constants.LOADING_FINISH);
                    Log.i(TAG_ShuDou, "装车后出现2次竖斗 判断为装车" + "bucketSum +1  bucketSum= " + bucketSum + " →初始状态");
                    ret = 1;
                    shuDouReset();
                }
            }

            if (verticalAddNum >= 2) {
                verticalAddNum = 0;
            }

        } else if (HelperImprove.verticalOrHorizontal(recognition) == 0) {

            /**
             * 挖掘中和装车后出现两次平斗视为运送中
             */
            if (mLoadingState == LoadingState.DIGGING || mLoadingState == LoadingState.LOADING) {
                horizontalAddNum++;
                if (horizontalAddNum >= 2) {
                    if (mLoadingState == LoadingState.DIGGING) {
                        Log.i(TAG_ShuDou, "挖掘中中出现2次平斗 →运送中");
                    } else if (mLoadingState == LoadingState.LOADING) {
                        Log.i(TAG_ShuDou, "疑似装车后出现2次平斗 →运送中");
                    }
                    mLoadingState = LoadingState.TRANSPORTING;
//                    RxBus.get().send(RxCode.STATE_TRANSPORTING, Constants.TRANSPORTING);
                    updateState(Constants.TRANSPORTING);
                    horizontalAddNum = 0;
                }
            }

        }
        return ret;
    }


    public int getInternalCount() {
        return this.bucketSum;
    }

    private void shuDouReset() {
        verticalAddNum = 0;
        horizontalAddNum = 0;
        mDistanceComparisonCount = 0;
        noTruckNum = 0;
        mList.clear();
        mLoadingState = LoadingState.INITIAL_STATE;
    }

    private void updateState( String state) {
        if (mBucketCounterInterface != null) {
            long millisecond = System.currentTimeMillis();
            mBucketCounterInterface.updateState(millisecond, state);
        }
    }

}

