package com.tycms.recognition.activity;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tycms.recognition.R;
import com.tycms.recognition.customview.AutoFitTextureView;
import com.tycms.recognition.util.DateTimeUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraFragment extends Fragment {
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Camera camera;
    private Camera.PreviewCallback imageListener;
    private Size desiredSize;
    /**
     * The layout identifier to inflate for this Fragment.
     */
    private int layout;
    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView textureView;

    private String mVideoName;
    private File mVecordFile;
    private MediaRecorder mMediaRecorder;
    private Timer mTimer = new Timer();

    private int getYUVByteSize(final int width, final int height) {
        // The luminance plane requires 1 byte per pixel.
        final int ySize = width * height;

        // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
        // Each 2x2 block takes 2 bytes to encode, one each for U and V.
        final int uvSize = ySize >> 1;

        return ySize + uvSize;
    }

    public AutoFitTextureView getAutoFitTextureView() {
        return textureView;
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
     * TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {

                    int index = getCameraIdForTianYuan();
                    camera = Camera.open(index);

                    try {
                        Camera.Parameters parameters = camera.getParameters();
                        List<String> focusModes = parameters.getSupportedFocusModes();
                        if (focusModes != null
                                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
                        Size[] sizes = new Size[cameraSizes.size()];
                        int i = 0;
                        for (Camera.Size size : cameraSizes) {
                            sizes[i++] = new Size(size.width, size.height);
                        }
                        Size previewSize =
                                chooseOptimalSize(sizes, desiredSize.getWidth(), desiredSize.getHeight());
                        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
                        camera.setDisplayOrientation(90);//摄像头方向手机90度；头盔0度 ；开发板 前90 后270
                        camera.setParameters(parameters);
                        camera.setPreviewTexture(texture);
                    } catch (IOException exception) {
                        camera.release();
                    }

                    camera.setPreviewCallbackWithBuffer(imageListener);
                    Camera.Size s = camera.getParameters().getPreviewSize();
                    camera.addCallbackBuffer(new byte[getYUVByteSize(s.height, s.width)]);

                    /**
                     * 需要全屏的话注释掉这句代码(目前全屏有问题 预览尺寸和显示尺寸一致问题)
                     */
                    textureView.setAspectRatio(s.height, s.width);
                    camera.startPreview();
                    mDetectInterface.onStartDetect();

                    startRecord();
                    long indexVideo = 1000 * 60 * 20;
                    mTimer.schedule(timerTask, indexVideo, indexVideo);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
                }
            };

    public interface DetectInterface {
        void onStartDetect();
    }

    private DetectInterface mDetectInterface;

    public void setDetectInterface(DetectInterface detectInterface) {
        mDetectInterface = detectInterface;
    }


    /**
     * 开始录制
     */
    private void startRecord() {
        //这是是判断视频文件有没有创建,如果没有就返回
        boolean creakOk = createRecordDir();
        if (!creakOk) {
            return;
        }

        try {
            setConfigRecord();
            mMediaRecorder.prepare();
            mMediaRecorder.start();


        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            Log.i("wtt", "startRecord: " + e.toString());
        }
    }

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Log.i("wtt", "定时到了");
            saveVideo();
            startRecord();
        }
    };

    private void saveVideo() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.i("wtt", "" + mVecordFile.toString());
            }
        } catch (Exception ex) {
            //Toast.makeText(getApplicationContext(),ex.getMessage(),1).show();
            Log.i("wtt", "saveVideo: " + ex.toString());
        }
    }


    private boolean createRecordDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(getContext(), "SD卡不存在!", Toast.LENGTH_SHORT).show();
            return false;
        }

        File sampleDir = new File("/sdcard/myVideo/");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        mVideoName = "VID_" + DateTimeUtil.getStringDate1() + "-" + SystemClock.uptimeMillis() + ".mp4";
        mVecordFile = new File(sampleDir, mVideoName);
        return true;
    }

    private void setConfigRecord() {
        mMediaRecorder = new MediaRecorder();
        camera.unlock();
        mMediaRecorder.setCamera(camera);

//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//1.设置采集声音
        //设置采集图像
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //2.设置视频，音频的输出格式 mp4
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        //3.设置音频的编码格式
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        //设置图像的编码格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        //录像角度
        mMediaRecorder.setOrientationHint(90);
        //使用SurfaceView预览
//        Surface surface = new Surface(textureView.getSurfaceTexture());
//        mMediaRecorder.setPreviewDisplay(surface);

//        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
//
//        mMediaRecorder.setAudioEncodingBitRate(44100);
//        if (mProfile.videoBitRate > 2 * 1024 * 1024) {
//            mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
//        } else {
//            mMediaRecorder.setVideoEncodingBitRate(1024 * 1024);
//        }
//        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
//        mMediaRecorder.setVideoSize(1280, 720);

        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

        mMediaRecorder.setAudioEncodingBitRate(10000000);
        if (mProfile.videoBitRate > 2 * 1024 * 1024) {
            mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
        } else {
            mMediaRecorder.setVideoEncodingBitRate(1024 * 1024);
        }
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setVideoSize(720, 480);

        mMediaRecorder.setOutputFile(mVecordFile.getAbsolutePath());

    }


    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    public void init(final Camera.PreviewCallback imageListener, final int layout, final Size desiredSize) {
        this.imageListener = imageListener;
        this.layout = layout;
        this.desiredSize = desiredSize;
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView = view.findViewById(R.id.texture);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (textureView.isAvailable()) {
            camera.startPreview();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        stopCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
        } catch (final InterruptedException e) {
        }
    }

    protected void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    private int getCameraId() {
        CameraInfo ci = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_BACK) return i;
        }
        return -1; // No camera found
    }

    /**
     * 天远设备目前只有一个摄像头，不分前后，获取到即使用
     *
     * @return
     */
    private int getCameraIdForTianYuan() {
        CameraInfo ci = new CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        if (cameraCount == 1) {
            return 0;
        }
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_BACK) return i;
        }
        return -1; // No camera found
    }

    private static final int MINIMUM_PREVIEW_SIZE = 320;

    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        if (exactSizeFound) {
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
