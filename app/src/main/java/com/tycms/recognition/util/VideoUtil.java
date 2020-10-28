package com.tycms.recognition.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tycms.recognition.activity.CameraActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by WangTuantuan on 2020/10/28.
 */
public class VideoUtil {

    private Context mContext;
    private Camera mCamera;

    private String mVideoName;
    private File mVideoFile;
    private MediaRecorder mMediaRecorder;

    public static StringBuilder mReStringBuilder = new StringBuilder();
    ;
    private String mResultName;
    private File mResultFile;

    private String mRootPath = "/sdcard/ShuDouVideo/";
    public static long mStartTimeMillis = System.currentTimeMillis();


    //    public static long mVideoDuration = 1000 * 60 * 20;//录制视频时长
    public static final long mVideoDuration = 1000 * 60;//录制视频时长


    public VideoUtil(Context context, Camera camera) {
        this.mContext = context;
        this.mCamera = camera;
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        CameraActivity.bucketCounter.reset();
        //这是是判断视频文件有没有创建,如果没有就返回
        boolean creakOk = createRecordDir();
        if (!creakOk) {
            return;
        }

        Log.i("wtt", "startRecord");
        try {
            setConfigRecord();
            mMediaRecorder.prepare();
            mStartTimeMillis = System.currentTimeMillis();
            mMediaRecorder.start();

        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            Log.i("wtt", "startRecord: " + e.toString());
        }
    }


    private boolean createRecordDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(mContext, "SD卡不存在!", Toast.LENGTH_SHORT).show();
            return false;
        }

        File sampleDir = new File(mRootPath);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        String dataTime = DateTimeUtil.getStringDate1() + "_" + DateTimeUtil.timeAdd(mVideoDuration);
        mVideoName = "VID_" + dataTime + ".mp4";
        mResultName = "result_" + dataTime + ".txt";
        mVideoFile = new File(sampleDir, mVideoName);
        mResultFile = new File(sampleDir, mResultName);
        return true;
    }


    private void setConfigRecord() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

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

        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

    }


    public void saveVideo() {
        try {
            saveTxt();
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.i("wtt", "" + mVideoFile.toString());
            }
        } catch (Exception ex) {
            //Toast.makeText(getApplicationContext(),ex.getMessage(),1).show();
            Log.i("wtt", "saveVideo: " + ex.toString());
        }
        resetData();
    }

    private void saveTxt() {
        try {
            FileWriter fw = new FileWriter(mResultFile);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(mReStringBuilder.toString());
            bw.flush();
            bw.close();
            fw.close();

            String txtPath = mResultFile.getPath();
            String newPath = txtPath.substring(0, txtPath.lastIndexOf("."));
            newPath += ("_" + CameraActivity.bucketCounter.getInternalCount() + "斗" + ".txt");

            renameFile(txtPath, newPath);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * oldPath 和 newPath必须是新旧文件的绝对路径
     */
    private boolean renameFile(String oldPath, String newPath) {
        if (TextUtils.isEmpty(oldPath)) {
            return false;
        }

        if (TextUtils.isEmpty(newPath)) {
            return false;
        }
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        boolean b = oldFile.renameTo(newFile);
        return b;
    }

    private void resetData(){
        mReStringBuilder = new StringBuilder();
        mStartTimeMillis = System.currentTimeMillis();
    }

}
