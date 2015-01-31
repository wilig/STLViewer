package com.sihrc.stlviewer.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by sihrc on 1/31/15.
 */
public class GLUtils {
    public static FloatBuffer getFloatBufferFromList(List<Float> vertexList) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertexList.size() * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer triangleBuffer = vbb.asFloatBuffer();
        float[] array = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            array[i] = vertexList.get(i);
        }
        triangleBuffer.put(array);
        triangleBuffer.position(0);
        return triangleBuffer;
    }

    public static FloatBuffer getFloatBufferFromArray(float[] vertexArray) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertexArray.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer triangleBuffer = vbb.asFloatBuffer();
        triangleBuffer.put(vertexArray);
        triangleBuffer.position(0);
        return triangleBuffer;
    }

    public static void drawGrids(GL10 gl) {
        List<Float> lineList = new ArrayList<>(85);

        for (int x = -100; x <= 100; x += 5) {
            lineList.add((float) x);
            lineList.add(-100f);
            lineList.add(0f);
            lineList.add((float) x);
            lineList.add(100f);
            lineList.add(0f);
        }
        for (int y = -100; y <= 100; y += 5) {
            lineList.add(-100f);
            lineList.add((float) y);
            lineList.add(0f);
            lineList.add(100f);
            lineList.add((float) y);
            lineList.add(0f);
        }

        FloatBuffer lineBuffer = getFloatBufferFromList(lineList);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, lineBuffer);

        gl.glLineWidth(1f);
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, new float[]{0.5f, 0.5f, 0.5f, 1.0f}, 0);
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, new float[]{0.5f, 0.5f, 0.5f, 1.0f}, 0);
        gl.glDrawArrays(GL10.GL_LINES, 0, lineList.size() / 3);
    }
}
