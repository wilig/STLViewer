package com.sihrc.stlviewer.util;

import android.graphics.PointF;
import android.view.MotionEvent;

/**
 * Created by sihrc on 1/31/15.
 */
public class GestureUtils {
    // for touch event handling
    public static final int TOUCH_NONE = 0;
    public static final int TOUCH_DRAG = 1;
    public static final int TOUCH_ZOOM = 2;
    public static final float TOUCH_SCALE_FACTOR = 180.0f / 320;

    /**
     * @param event
     * @return pinched distance
     */
    public static float getPinchDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return android.util.FloatMath.sqrt(x * x + y * y);
    }

    /**
     * @param event
     * @param pt    pinched point
     */
    public static void getPinchCenterPoint(MotionEvent event, PointF pt) {
        pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
        pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
    }
}
