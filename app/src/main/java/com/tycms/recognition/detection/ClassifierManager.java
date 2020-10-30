package com.tycms.recognition.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import org.nelbds.nglite.exception.NGLiteException;
import org.nelbds.nglite.func.Recognition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类管理
 */
class ClassifierManager {

    private final String TAG = "ClassifierManager";
//    private ImageClassifier mClassifier;

    /**
     * 初始化
     * @param context
     * @param isTFLite
     */
    public void init(Context context, boolean isTFLite) {
        if (isTFLite) {
            String tfFileName = "classify_model.tflite";
            String labelFileName = "ty_labels.txt";
            boolean isQuantized = false;

//            try {
//                mClassifier = ImageClassifier.Builder.generateTFLiteImageClassifier(context,
//                        tfFileName, labelFileName, isQuantized);
//            } catch (NGLiteException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else {
            String tfFileName = "mobilenet_quantized_scripted_925.pt";
            String labelFileName = "labels.txt";
            boolean isQuantized = true;
//            try {
//                mClassifier = ImageClassifier.Builder.generatePTMobileImageClassifierByTensor(context,
//                        tfFileName, labelFileName, isQuantized);
//            } catch (NGLiteException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    /**
     * 图像分类
     * @param bitmap
     */
    public Recognition processImage(Bitmap bitmap) {
//        if (mClassifier == null){
//            Log.d(TAG, "ClassifierManager 未初始化");
//            return null;
//        }

        long startTime = SystemClock.uptimeMillis();
        List<Recognition> results = new ArrayList<>();
//        try {
//            results = mClassifier.recognizeImage(bitmap);
//        } catch (NGLiteException e) {
//            e.printStackTrace();
//        }
        long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
//        Log.i(TAG, "results is: " + results.toString());
        if (results != null && results.size() > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("分类耗时： " + lastProcessingTimeMs + "ms" + "\n");

            for (Recognition result : results) {
                stringBuffer.append(result.getTitle() + "   " + result.getConfidence() + "\n");
            }

            Log.i(TAG, "results is: " + stringBuffer.toString());
            return results.get(0);
        }

        return null;
    }
}