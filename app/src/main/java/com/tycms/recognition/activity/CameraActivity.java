package com.tycms.recognition.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

import com.tycms.recognition.R;
import com.tycms.recognition.bucket.BucketCounterV0Improve;
import com.tycms.recognition.bucket.BucketCounterV0Improve;
import com.tycms.recognition.bucket.BucketCounterV0StateControl;
import com.tycms.recognition.customview.OverlayView;
import com.tycms.recognition.detection.DetectorManagerMerge;
import com.tycms.recognition.tracking.MultiBoxTrackerNgLite;
import com.tycms.recognition.util.BitmapUtil;
import com.tycms.recognition.util.Constants;
import com.tycms.recognition.util.RxCode;
import com.tycms.recognition.util.VideoUtil;

import org.nelbds.nglite.func.Recognition;

import java.util.List;

import gorden.rxbus2.RxBus;
import gorden.rxbus2.Subscribe;
import gorden.rxbus2.ThreadMode;

public class CameraActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private TextView mAddTipTv, mStateTipTv;

    private final String TAG = "recognize";

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_READ = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String PERMISSION_WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private Handler handler;
    private HandlerThread handlerThread;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private boolean isProcessingFrame = false;
    private int[] rgbBytes = null;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private Integer sensorOrientation;
    private CameraFragment mCameraFragment;

    //    private DetectorManager detectorManager;
    private DetectorManagerMerge detectorManager;


    private MultiBoxTrackerNgLite tracker;
    private OverlayView trackingOverlay;
    private TextView txt_time;
    private TextView txt_count;

    public static BucketCounterV0StateControl bucketCounter;
    private int mLastDouShu = 0;
    private boolean isFirstIn = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_camera);
        initView();

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        createDetector();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Constants.isVideotape) {
            mCameraFragment.getVideoUtil().saveVideo();
            mCameraFragment.stopTimer();
        }
    }

    private void initView() {
        mAddTipTv = findViewById(R.id.mAddTipTv);
        mStateTipTv = findViewById(R.id.mStateTipTv);
        txt_time = findViewById(R.id.txt_time);
        txt_count = findViewById(R.id.txt_count);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        tracker = new MultiBoxTrackerNgLite(this);
        trackingOverlay.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
            }
        });
    }


    private void setStateTipText(long millisecondIndex, String newTip) {
        String add = "→";
        String currentTip = mStateTipTv.getText().toString();
        String oldTip = currentTip;
        if (currentTip.contains(add)) {
            currentTip = currentTip.substring(currentTip.indexOf(add) + 1);
        }
        String tip = currentTip + add + newTip;
        if (!TextUtils.equals(oldTip, tip)) {
            addResultStringBuilder(millisecondIndex, "state:" + tip);
        }
        mStateTipTv.setText(tip);

    }

    private void addResultStringBuilder(long millisecondIndex, String content) {
        if (!TextUtils.isEmpty(VideoUtil.mReStringBuilder.toString())) {
            VideoUtil.mReStringBuilder.append("\n");
        }
        VideoUtil.mReStringBuilder.append(millisecondIndex + ":" + content);
    }

    private Handler mViewHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            mAddTipTv.setVisibility(View.GONE);
        }
    };

    private void createDetector() {
//        detectorManager = new DetectorManager();
        detectorManager = new DetectorManagerMerge();
        detectorManager.init(this);

        bucketCounter = new BucketCounterV0StateControl();
        bucketCounter.setBucketCounterInterface(new BucketCounterV0StateControl.BucketCounterInterface() {
            @Override
            public void updateState(long millisecond, String state) {
                setStateTipText(millisecond - VideoUtil.mStartTimeMillis, state);
            }
        });
    }

    /**
     * 摄像头帧数据回调
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            return;
        }
        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            return;
        }

        isProcessingFrame = true;

        postInferenceCallback = new Runnable() {
            @Override
            public void run() {
                camera.addCallbackBuffer(bytes);
                isProcessingFrame = false;
            }
        };

        imageConverter = new Runnable() {
            @Override
            public void run() {
                BitmapUtil.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
            }
        };

        processImage();
    }

    private Handler mDetectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isProcessingFrame) {
                return;
            }
            isProcessingFrame = true;
            processImage();
        }
    };

    // 处理图像
    protected void processImage() {
        runInBackground(new Runnable() {
            @Override
            public void run() {
                // todo 图像识别工作

                if (Constants.isVideotape) {
                    //开启录像 从控件获取图像
                    recognizeFromTextureView();
                } else {
                    //不开启录像 从摄像头获取图片
                    recognize();
                }


                // 识别完成后，请求下一帧图像
                if (postInferenceCallback != null) {
                    postInferenceCallback.run();
                }
            }
        });
    }

    // 图像识别
    protected void recognize() {
        final long startTime = SystemClock.uptimeMillis();

        // TODO：考虑将此操作放入lib中执行，调用端传入顺时针旋转角度即可，避免每次都重新分配内存
        Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        Matrix matrix = new Matrix();
        matrix.setRotate(sensorOrientation);
        // 进行旋转
        rgbFrameBitmap = Bitmap.createBitmap(rgbFrameBitmap, 0, 0,
                rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), matrix, false);

//        final List<Recognition> results = detectionImage(rgbFrameBitmap);

        Bitmap bitmap = mCameraFragment.getAutoFitTextureView().getBitmap();
        final List<Recognition> results = detectionImage(bitmap);

        long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        // 数斗
        bucketCounter.feedRT(results);
//        bucketCounter.feedRT20200706(results);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int currentDouShu = bucketCounter.getInternalCount();
                txt_time.setText("时间：" + lastProcessingTimeMs + "ms");
                txt_count.setText("斗数：" + currentDouShu);

                if (currentDouShu > mLastDouShu) {
                    mAddTipTv.setText(String.valueOf(currentDouShu));
                    mAddTipTv.setVisibility(View.VISIBLE);
                    mViewHandler.sendEmptyMessageDelayed(000, 1000);
                    mLastDouShu = currentDouShu;
                }
                isProcessingFrame = false;
                mDetectHandler.sendEmptyMessage(11111);
            }
        });
    }


    // 图像识别
    protected void recognizeFromTextureView() {
        final long startTime = SystemClock.uptimeMillis();

        Bitmap bitmap = mCameraFragment.getAutoFitTextureView().getBitmap();
        final List<Recognition> results = detectionImage(bitmap);

        long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        // 数斗
        bucketCounter.feedRT(results);
//        bucketCounter.feedRT20200706(results);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int currentDouShu = bucketCounter.getInternalCount();
                txt_time.setText("时间：" + lastProcessingTimeMs + "ms");
                txt_count.setText("斗数：" + currentDouShu);

                if (currentDouShu > mLastDouShu) {
                    mAddTipTv.setText(String.valueOf(currentDouShu));
                    mAddTipTv.setVisibility(View.VISIBLE);
                    mViewHandler.sendEmptyMessageDelayed(000, 1000);
                    mLastDouShu = currentDouShu;
                    addResultStringBuilder(System.currentTimeMillis() - VideoUtil.mStartTimeMillis, "douShu:" + currentDouShu);
                }
                isProcessingFrame = false;
                mDetectHandler.sendEmptyMessage(11111);
            }
        });
    }

    private List<Recognition> detectionImage(Bitmap bitmap) {
        trackingOverlay.postInvalidate();

        int sensorOrientation = 0;
        tracker.setFrameConfiguration(bitmap.getWidth(), bitmap.getHeight(), sensorOrientation);

        List<Recognition> results = detectorManager.detectionImage(CameraActivity.this, bitmap);
        Log.i(TAG, results.toString());
        if (results != null && results.size() > 0) {
            addResultStringBuilder(System.currentTimeMillis() - VideoUtil.mStartTimeMillis, "result:" + results.toString());
        }

        tracker.trackResults(results, 0);
        trackingOverlay.postInvalidate();

        return results;
    }


    public void onPreviewSizeChosen(final Size size, final int rotation) {
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        sensorOrientation = rotation - getScreenOrientation();
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        isProcessingFrame = false;
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        if (isFirstIn) {
            isFirstIn = false;
        } else {
            isProcessingFrame = false;
            mDetectHandler.sendEmptyMessage(11111);
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException ignored) {
        }
    }


    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    protected void setFragment() {
        mCameraFragment = new CameraFragment();
        mCameraFragment.init(this, R.layout.camera_connection_fragment, getDesiredPreviewFrameSize());

        getSupportFragmentManager().beginTransaction().replace(R.id.container, mCameraFragment).commit();


        //录像模式开启以下功能
        if (Constants.isVideotape) {
            mCameraFragment.setDetectInterface(new CameraFragment.DetectInterface() {
                @Override
                public void onStartDetect() {
                    mDetectHandler.sendEmptyMessage(11111);
                }
            });
        }

    }

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    /*********************************许可相关**********************************/
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(PERMISSION_WRITE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(PERMISSION_READ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(this, "Camera permission is required for this demo", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_WRITE, PERMISSION_READ}, PERMISSIONS_REQUEST);
        }
    }
    /*********************************许可相关**********************************/
}
