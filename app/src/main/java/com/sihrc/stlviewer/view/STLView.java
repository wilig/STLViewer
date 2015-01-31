package com.sihrc.stlviewer.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.widget.Toast;

import com.sihrc.stlviewer.R;
import com.sihrc.stlviewer.object.STLObject;
import com.sihrc.stlviewer.renderer.STLRenderer;
import com.sihrc.stlviewer.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;


public class STLView extends GLSurfaceView {

    // for touch event handling
    private static final int TOUCH_NONE = 0;
    private int touchMode = TOUCH_NONE;
    private static final int TOUCH_DRAG = 1;
    private static final int TOUCH_ZOOM = 2;
    private static final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private STLRenderer stlRenderer;
    private Uri uri;
    private float previousX;
    private float previousY;
    private boolean isRotate = true;
    private PointF pinchStartPoint = new PointF();
    private float pinchStartZ = 0.0f;
    private float pinchStartDistance = 0.0f;

    public STLView(Context context, Uri uri) {
        super(context);

        this.uri = uri;

        byte[] stlBytes = null;
        try {
            stlBytes = getSTLBytes(context, uri);
        } catch (Exception ignored) {
        }

        if (stlBytes == null) {
            Toast.makeText(context, context.getString(R.string.error_fetch_data), Toast.LENGTH_LONG).show();
            return;
        }

        // Data loading.
        STLObject stlObject = new STLObject(stlBytes, context);

        SharedPreferences colorConfig = context.getSharedPreferences("colors", Activity.MODE_PRIVATE);
        STLRenderer.red = colorConfig.getFloat("red", 0.75f);
        STLRenderer.green = colorConfig.getFloat("green", 0.75f);
        STLRenderer.blue = colorConfig.getFloat("blue", 0.75f);
        STLRenderer.alpha = colorConfig.getFloat("alpha", 0.5f);

        // render: stlObject as null
        setRenderer(stlRenderer = new STLRenderer(stlObject));
        STLRenderer.requestRedraw();
    }

    /**
     * @param context
     * @return
     */
    private static byte[] getSTLBytes(Context context, Uri uri) {
        byte[] stlBytes = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            stlBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return stlBytes;
    }

    public boolean isRotate() {
        return isRotate;
    }

    public void setRotate(boolean isRotate) {
        this.isRotate = isRotate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // starts pinch
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    pinchStartDistance = getPinchDistance(event);
                    pinchStartZ = stlRenderer.distanceZ;
                    if (pinchStartDistance > 50f) {
                        getPinchCenterPoint(event, pinchStartPoint);
                        previousX = pinchStartPoint.x;
                        previousY = pinchStartPoint.y;
                        touchMode = TOUCH_ZOOM;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float pinchScale;
                float pinchMoveX;
                float pinchMoveY;
                if (touchMode == TOUCH_ZOOM && pinchStartDistance > 0) {
                    // on pinch
                    PointF pt = new PointF();

                    getPinchCenterPoint(event, pt);
                    pinchMoveX = pt.x - previousX;
                    pinchMoveY = pt.y - previousY;
                    previousX = pt.x;
                    previousY = pt.y;

                    if (isRotate) {
                        stlRenderer.angleX += pinchMoveX * TOUCH_SCALE_FACTOR;
                        stlRenderer.angleY += pinchMoveY * TOUCH_SCALE_FACTOR;
                    } else {
                        // change view point
                        stlRenderer.positionX += pinchMoveX * TOUCH_SCALE_FACTOR / 5;
                        stlRenderer.positionY += pinchMoveY * TOUCH_SCALE_FACTOR / 5;
                    }
                    STLRenderer.requestRedraw();

                    pinchScale = getPinchDistance(event) / pinchStartDistance;
                    changeDistance(pinchStartZ / pinchScale);
                    invalidate();
                }
                break;

            // end pinch
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (touchMode == TOUCH_ZOOM) {
                    touchMode = TOUCH_NONE;

                    pinchStartPoint.x = 0.0f;
                    pinchStartPoint.y = 0.0f;
                    invalidate();
                }
                break;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // start drag
            case MotionEvent.ACTION_DOWN:
                if (touchMode == TOUCH_NONE && event.getPointerCount() == 1) {
                    touchMode = TOUCH_DRAG;
                    previousX = event.getX();
                    previousY = event.getY();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (touchMode == TOUCH_DRAG) {
                    float x = event.getX();
                    float y = event.getY();

                    float dx = x - previousX;
                    float dy = y - previousY;
                    previousX = x;
                    previousY = y;

                    if (isRotate) {
                        stlRenderer.angleX += dx * TOUCH_SCALE_FACTOR;
                        stlRenderer.angleY += dy * TOUCH_SCALE_FACTOR;
                    } else {
                        // change view point
                        stlRenderer.positionX += dx * TOUCH_SCALE_FACTOR / 5;
                        stlRenderer.positionY += dy * TOUCH_SCALE_FACTOR / 5;
                    }
                    STLRenderer.requestRedraw();
                    requestRender();
                }
                break;

            // end drag
            case MotionEvent.ACTION_UP:
                if (touchMode == TOUCH_DRAG) {
                    touchMode = TOUCH_NONE;
                    break;
                }
        }

        return true;
    }

    /**
     * @param event
     * @return pinched distance
     */
    private static float getPinchDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return android.util.FloatMath.sqrt(x * x + y * y);
    }

    /**
     * @param event
     * @param pt    pinched point
     */
    private static void getPinchCenterPoint(MotionEvent event, PointF pt) {
        pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
        pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
    }

    private void changeDistance(float distance) {
        stlRenderer.distanceZ = distance;
        STLRenderer.requestRedraw();
        requestRender();
    }

    public Uri getUri() {
        return uri;
    }

}
