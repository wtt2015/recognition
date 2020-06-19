package com.tycms.recognition.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import org.nelbds.nglite.exception.NGLiteException;
import org.nelbds.nglite.func.ObjectDetector;
import org.nelbds.nglite.func.Recognition;
import org.nelbds.nglite.util.ImageUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 检测管理
 */
public class DetectorManager {
    private final String TAG = "DetectorManager";

    private ObjectDetector detector;
//    private String TF_OD_API_MODEL_FILE = "detect.tflite";
//    private String TF_OD_API_LABELS_FILE = "labelmap0.txt";
    private String TF_OD_API_MODEL_FILE = "detect_0529_9.tflite";
    private String TF_OD_API_LABELS_FILE = "ty_labelmap.txt";

    private Matrix cropToFrameTransform;
    private ClassifierManager classifierManager;

    /**
     * 初始化
     * @param context
     */
    public void init(Context context) {
        try {
            detector = ObjectDetector.Builder.generateTFLiteFloatObjectDetector(context,
                    TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE);
        } catch (NGLiteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        classifierManager = new ClassifierManager();
        classifierManager.init(context, true);
    }

    public List<Recognition> detectionImage(Bitmap bitmap) {
//        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/images/";
//        BitmapUtil.savePicture(path,bitmap);
//        BitmapUtil.savePicture(path,convertPic(bitmap));

        List<Recognition> results = new ArrayList<>();
        try {
            results = detector.recognizeImage(convertPic(bitmap));
        } catch (NGLiteException e) {
            e.printStackTrace();
        }
        List<Recognition> mappedRecognitions = convertList(results);
        Log.i(TAG, mappedRecognitions.toString());

        List<Recognition> finalResults = new ArrayList<>();

        // 图像分类
        for (Recognition result : mappedRecognitions) {
            if (result.getLocation() == null) {
                continue;
            }

            // 识别到铲斗
            if (result.getTitle().startsWith("bucket")){
                Bitmap cropPic = cropPic(bitmap, result.getLocation());
                Recognition finalResult = classifierManager.processImage(cropPic);

                if (finalResult != null){
                    String id = result.getId();
                    String title = finalResult.getTitle();
                    Float confidence = result.getConfidence();
                    RectF location = result.getLocation();

                    Recognition bucket = new Recognition(id, title, confidence, location);
                    finalResults.add(bucket);
                }
            } else {
//                finalResults.add(result);
                // 识别到卡车
                if (result.getTitle().startsWith("Truck")){
                    finalResults.add(result);
                }
            }
        }

        return finalResults;
    }

    // 根据位置裁剪图片
    private Bitmap cropPic(Bitmap bitmap, RectF location) {
        int left = (int)location.left;
        int top = (int)location.top;
        int right = (int)location.right;
        int bottom = (int)location.bottom;

        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }
        if (right > bitmap.getWidth()) {
            right = bitmap.getWidth();
        }
        if (bottom > bitmap.getHeight()) {
            bottom = bitmap.getHeight();
        }

        int width = right - left;
        int height = bottom - top;

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);
        return croppedBitmap;
    }

    // 图片转换到指定尺寸
    private Bitmap convertPic(Bitmap bitmap) {
        int TF_OD_API_INPUT_SIZE = 300;
        int cropSize = TF_OD_API_INPUT_SIZE;
        int sensorOrientation = 0;
        boolean MAINTAIN_ASPECT = false;

        Matrix frameToCropTransform = ImageUtils.getTransformationMatrix(
                bitmap.getWidth(), bitmap.getHeight(),
                cropSize, cropSize,
                sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        Bitmap croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(bitmap, frameToCropTransform, null);

        return croppedBitmap;
    }

    // 将分析结果还原
    private List<Recognition> convertList(List<Recognition> results) {
        final List<Recognition> mappedRecognitions = new LinkedList<>();
        float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
        float MINIMUM_CONFIDENCE_TF_OD_API_TRUCK = 0.5f;

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        float minimumConfidenceTruck = MINIMUM_CONFIDENCE_TF_OD_API_TRUCK;
        for (final Recognition result : results) {
            final RectF location = result.getLocation();
//            if (location != null && result.getConfidence() >= minimumConfidence) {
//                cropToFrameTransform.mapRect(location);
//                result.setLocation(location);
//                mappedRecognitions.add(result);
//            }

            if (location != null && result.getConfidence() >= Math.min(minimumConfidence,minimumConfidenceTruck)) {
                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                if (result.getTitle().startsWith("bucket") && result.getConfidence() >= minimumConfidence) {
                    mappedRecognitions.add(result);
                } else if (result.getTitle().startsWith("Truck") && result.getConfidence() >= minimumConfidenceTruck) {
                    mappedRecognitions.add(result);
                }
            }
        }
        return mappedRecognitions;
    }
}