package com.tycms.recognition.bucket;

import android.graphics.RectF;
import org.nelbds.nglite.func.Recognition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kangweibo01 on 2020/5/8.
 */

public class Helper{
    public Helper() {
    }

    public static float area(RectF rect) {
        float w = rect.right - rect.left;
        float h = rect.bottom - rect.top;
        return w * h;
    }

    public static List<Recognition> filter(List<Recognition> recognitions) {
        List<Recognition> list = new ArrayList();
        List<Recognition> buckets = new ArrayList();
        List<Recognition> trucks = new ArrayList();
        Iterator var4 = recognitions.iterator();

        Recognition maxTarget;
        while(var4.hasNext()) {
            maxTarget = (Recognition)var4.next();
            if(maxTarget.getTitle().startsWith("bucket")) {
                buckets.add(maxTarget);
            } else if(maxTarget.getTitle().startsWith("Truck")) {
                trucks.add(maxTarget);
            }
        }

        float maxArea = -1.0F;
        maxTarget = null;
        if(buckets.size() > 0) {
            Iterator var6 = buckets.iterator();

            Recognition truck;
            while(var6.hasNext()) {
                truck = (Recognition)var6.next();
                if(area(truck.getLocation()) > maxArea) {
                    maxArea = area(truck.getLocation());
                    maxTarget = truck;
                }
            }

            assert maxTarget != null;

            list.add(maxTarget);
            maxTarget = null;
            maxArea = -1.0F;
            if(trucks.size() > 0) {
                var6 = trucks.iterator();

                while(var6.hasNext()) {
                    truck = (Recognition)var6.next();
                    if(area(truck.getLocation()) > maxArea) {
                        maxArea = area(truck.getLocation());
                        maxTarget = truck;
                    }
                }

                assert maxTarget != null;

                list.add(maxTarget);
            }
        }

        return list;
    }

    /**
     * 铲斗是否在卡车上方（精度略低，只要求上方）
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isInTop(Recognition a, Recognition b) {
        float h0 = a.getLocation().bottom;
        float h1 = b.getLocation().bottom;
        return h0 / 2.0F <= h1 / 2.0F;
    }

    public static boolean isVertical(String label) {
        return label.startsWith("bucket1");
    }

    public static boolean isInInternal(Recognition a, Recognition b) {
        float x0 = a.getLocation().left;
        float y0 = a.getLocation().top;
        float w0 = a.getLocation().right - x0;
        float h0 = a.getLocation().bottom - y0;
        float x1 = b.getLocation().left;
        float y1 = b.getLocation().top;
        float w1 = b.getLocation().right - x1;
        float h1 = b.getLocation().bottom - y1;
        return x0 >= x1 && x0 + w0 <= x1 + w1 && y0 + h0 / 2.0F <= y1 + h1 / 2.0F;
    }

    public static int verticalOrNot(List<Recognition> recognitions) {
        return recognitions != null &&
                !recognitions.isEmpty()?(recognitions.size() == 1?(isVertical(((Recognition)recognitions.get(0)).getTitle())?1:0):(recognitions.size() == 2?(isVertical(((Recognition)recognitions.get(0)).getTitle())?1:0):0)):-1;
    }

    public static int dumpingOrNot(List<Recognition> recognitions) {
        return recognitions != null &&
                !recognitions.isEmpty()?(recognitions.size() == 1?0:(recognitions.size() == 2?(isInInternal((Recognition)recognitions.get(0), (Recognition)recognitions.get(1))?1:0):0)):-1;
    }

    /**
     * 铲斗在卡车上方
     *
     * @param recognitions
     * @return
     */
    public static int dumpingTopOrNot(List<Recognition> recognitions) {
        return recognitions != null &&
                !recognitions.isEmpty() ? (recognitions.size() == 1 ? 0 : (recognitions.size() == 2 ? (isInTop((Recognition) recognitions.get(0), (Recognition) recognitions.get(1)) ? 1 : 0) : 0)) : -1;
    }

}

