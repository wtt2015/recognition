package com.tycms.recognition.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by WangTuantuan on 2020/9/3.
 */
public class DateTimeUtil {

    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyy-MM-dd HH:mm
     */
    public static String getStringDate1() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    //时间转换类，将得到的音乐时间毫秒转换为时分秒格式
    public static  String formatTime(int length) {
        Date date = new Date(length);
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String totalTime = sdf.format(date);
        return totalTime;
    }
}
