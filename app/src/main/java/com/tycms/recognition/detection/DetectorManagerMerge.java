package com.tycms.recognition.detection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import org.nelbds.nglite.exception.NGLiteException;
import org.nelbds.nglite.func.ObjectDetector;
import org.nelbds.nglite.func.Recognition;
import org.nelbds.nglite.util.ImageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 检测管理（检测和分类合并）
 */
public class DetectorManagerMerge {
    private final String TAG = "DetectorManagerMerge";
    private String TF_OD_API_MODEL_FILE = "detectOneModel.tflite";
    private String TF_OD_API_LABELS_FILE = "ty_labelmap_onemodel.txt";

    private ObjectDetector detector;
    private Matrix cropToFrameTransform;

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        try {
            detector = ObjectDetector.Builder.generateTFLiteObjectDetector(context,
                    TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, false);
        } catch (NGLiteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<Recognition> detectionImage(Context context,Bitmap bitmap) {

        List<Recognition> results = new ArrayList<>();
        try {
//            String picName = "00001f7bd1054ca32b9077edd36182e579aa0.jpg";
//            Bitmap bitmap1 = loadImage(context,picName);
            final long startTime = SystemClock.uptimeMillis();
            results = detector.recognizeImage(convertPic(bitmap));
            long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            Log.i("shuDou","实际耗时："+lastProcessingTimeMs+"ms");
        } catch (NGLiteException e) {
            Log.i(TAG, "识别异常");
            results = new ArrayList<>();
            e.printStackTrace();
        } catch (Exception e) {
            Log.i("识别异常", "识别异常");
            results = new ArrayList<>();
            e.printStackTrace();
        }
        List<Recognition> mappedRecognitions = convertList(results);
        bitmap.recycle();
        Log.i(TAG, mappedRecognitions.toString());

        return mappedRecognitions;
    }

    // 根据位置裁剪图片
    private Bitmap cropPic(Bitmap bitmap, RectF location) {
        int left = (int) location.left;
        int top = (int) location.top;
        int right = (int) location.right;
        int bottom = (int) location.bottom;

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

        float minimumConfidence = 0.8f;
        float minimumConfidenceTruck = 0.6f;
        for (final Recognition result : results) {
            final RectF location = result.getLocation();
//            if (location != null && result.getConfidence() >= minimumConfidence) {
//                cropToFrameTransform.mapRect(location);
//                result.setLocation(location);
//                mappedRecognitions.add(result);
//            }

            if (location != null && result.getConfidence() >= Math.min(minimumConfidence, minimumConfidenceTruck)) {
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


    public static Bitmap loadImage(Context context, String fileName) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(fileName);
        } catch (IOException e) {
            Log.e("Test", "Cannot load image from assets");
        }
        return BitmapFactory.decodeStream(inputStream);
    }
}