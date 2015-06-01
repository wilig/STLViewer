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
import com.sihrc.stlviewer.util.GestureUtils;
import com.sihrc.stlviewer.util.IOUtils;


public class STLView extends GLSurfaceView {

    private int touchMode = GestureUtils.TOUCH_NONE;
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
            stlBytes = IOUtils.getSTLBytes(context, uri);
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
                    pinchStartDistance = GestureUtils.getPinchDistance(event);
                    pinchStartZ = stlRenderer.distanceZ;
                    if (pinchStartDistance > 50f) {
                        GestureUtils.getPinchCenterPoint(event, pinchStartPoint);
                        previousX = pinchStartPoint.x;
                        previousY = pinchStartPoint.y;
                        touchMode = GestureUtils.TOUCH_ZOOM;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float pinchScale;
                float pinchMoveX;
                float pinchMoveY;
                if (touchMode == GestureUtils.TOUCH_ZOOM && pinchStartDistance > 0) {
                    // on pinch
                    PointF pt = new PointF();

                    GestureUtils.getPinchCenterPoint(event, pt);
                    pinchMoveX = pt.x - previousX;
                    pinchMoveY = pt.y - previousY;
                    previousX = pt.x;
                    previousY = pt.y;

                    if (isRotate) {
                        stlRenderer.angleX += pinchMoveX * GestureUtils.TOUCH_SCALE_FACTOR;
                        stlRenderer.angleY += pinchMoveY * GestureUtils.TOUCH_SCALE_FACTOR;
                    } else {
                        // change view point
                        stlRenderer.positionX += pinchMoveX * GestureUtils.TOUCH_SCALE_FACTOR / 5;
                        stlRenderer.positionY += pinchMoveY * GestureUtils.TOUCH_SCALE_FACTOR / 5;
                    }
                    STLRenderer.requestRedraw();

                    pinchScale = GestureUtils.getPinchDistance(event) / pinchStartDistance;
                    changeDistance(pinchStartZ / pinchScale);
                    invalidate();
                }
                break;

            // end pinch
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (touchMode == GestureUtils.TOUCH_ZOOM) {
                    touchMode = GestureUtils.TOUCH_NONE;

                    pinchStartPoint.x = 0.0f;
                    pinchStartPoint.y = 0.0f;
                    invalidate();
                }
                break;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // start drag
            case MotionEvent.ACTION_DOWN:
                if (touchMode == GestureUtils.TOUCH_NONE && event.getPointerCount() == 1) {
                    touchMode = GestureUtils.TOUCH_DRAG;
                    previousX = event.getX();
                    previousY = event.getY();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (touchMode == GestureUtils.TOUCH_DRAG) {
                    float x = event.getX();
                    float y = event.getY();

                    float dx = x - previousX;
                    float dy = y - previousY;
                    previousX = x;
                    previousY = y;

                    if (isRotate) {
                        stlRenderer.angleX += dx * GestureUtils.TOUCH_SCALE_FACTOR;
                        stlRenderer.angleY += dy * GestureUtils.TOUCH_SCALE_FACTOR;
                    } else {
                        // change view point
                        stlRenderer.positionX += dx * GestureUtils.TOUCH_SCALE_FACTOR / 5;
                        stlRenderer.positionY += dy * GestureUtils.TOUCH_SCALE_FACTOR / 5;
                    }
                    STLRenderer.requestRedraw();
                    requestRender();
                }
                break;

            // end drag
            case MotionEvent.ACTION_UP:
                if (touchMode == GestureUtils.TOUCH_DRAG) {
                    touchMode = GestureUtils.TOUCH_NONE;
                    break;
                }
        }

        return true;
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
