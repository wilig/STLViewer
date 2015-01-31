package com.sihrc.stlviewer.object;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.sihrc.stlviewer.R;
import com.sihrc.stlviewer.renderer.STLRenderer;
import com.sihrc.stlviewer.util.IOUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class STLObject {
    public float maxX;
    public float maxY;
    public float maxZ;
    public float minX;
    public float minY;
    public float minZ;
    byte[] stlBytes = null;
    List<Float> normalList;
    FloatBuffer triangleBuffer;

    public STLObject(byte[] stlBytes, Context context) {
        this.stlBytes = stlBytes;

        processSTL(stlBytes, context);
    }

    /**
     *
     * @param stlBytes
     * @param context
     * @return
     */
    private boolean processSTL(byte[] stlBytes, final Context context) {
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        maxZ = Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;

        normalList = new ArrayList<Float>();

        final ProgressDialog progressDialog = prepareProgressDialog(context);

        final AsyncTask<byte[], Integer, List<Float>> task = new AsyncTask<byte[], Integer, List<Float>>() {

            @Override
            protected List<Float> doInBackground(byte[]... stlBytes) {
                List<Float> processResult = null;
                try {
                    if (IOUtils.isText(stlBytes[0])) {
                        processResult = processText(new String(stlBytes[0]));
                    } else {
                        processResult = processBinary(stlBytes[0]);
                    }
                } catch (Exception ignored) {
                }
                if (processResult != null && processResult.size() > 0 && normalList != null && normalList.size() > 0) {
                    return processResult;
                }

                return new ArrayList<>(0);
            }

            List<Float> processText(String stlText) throws Exception {
                normalList.clear();
                String[] stlLines = stlText.split("\n");
                progressDialog.setMax(stlLines.length);

                List<Float> vertexList = new ArrayList<>();
                for (int i = 0; i < stlLines.length; i++) {
                    String string = stlLines[i].trim();
                    if (string.startsWith("facet normal ")) {
                        string = string.replaceFirst("facet normal ", "");
                        String[] normalValue = string.split(" ");
                        normalList.add(Float.parseFloat(normalValue[0]));
                        normalList.add(Float.parseFloat(normalValue[1]));
                        normalList.add(Float.parseFloat(normalValue[2]));
                    }
                    if (string.startsWith("vertex ")) {
                        string = string.replaceFirst("vertex ", "");
                        String[] vertexValue = string.split(" ");
                        float x = Float.parseFloat(vertexValue[0]);
                        float y = Float.parseFloat(vertexValue[1]);
                        float z = Float.parseFloat(vertexValue[2]);
                        adjustMaxMin(x, y, z);
                        vertexList.add(x);
                        vertexList.add(y);
                        vertexList.add(z);
                    }

                    if (i % (stlLines.length / 50) == 0) {
                        publishProgress(i);
                    }
                }

                return vertexList;
            }

            List<Float> processBinary(byte[] stlBytes) throws Exception {
                List<Float> vertexList = new ArrayList<>();
                normalList.clear();

                int vectorSize = getIntWithLittleEndian(80);

                progressDialog.setMax(vectorSize);
                for (int i = 0; i < vectorSize; i++) {
                    normalList.add(Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50)));
                    normalList.add(Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 4)));
                    normalList.add(Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 8)));

                    float x = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 12));
                    float y = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 16));
                    float z = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 20));
                    adjustMaxMin(x, y, z);
                    vertexList.add(x);
                    vertexList.add(y);
                    vertexList.add(z);

                    x = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 24));
                    y = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 28));
                    z = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 32));
                    adjustMaxMin(x, y, z);
                    vertexList.add(x);
                    vertexList.add(y);
                    vertexList.add(z);

                    x = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 36));
                    y = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 40));
                    z = Float.intBitsToFloat(getIntWithLittleEndian(84 + i * 50 + 44));
                    adjustMaxMin(x, y, z);
                    vertexList.add(x);
                    vertexList.add(y);
                    vertexList.add(z);

                    if (i % (vectorSize / 50) == 0) {
                        publishProgress(i);
                    }
                }

                return vertexList;
            }

            @Override
            protected void onPostExecute(List<Float> vertexList) {

                if (normalList.size() < 1 || vertexList.size() < 1) {
                    Toast.makeText(context, context.getString(R.string.error_fetch_data), Toast.LENGTH_LONG).show();

                    progressDialog.dismiss();
                    return;
                }

                float[] vertexArray = IOUtils.listToFloatArray(vertexList);
                ByteBuffer vbb = ByteBuffer.allocateDirect(vertexArray.length * 4);
                vbb.order(ByteOrder.nativeOrder());
                triangleBuffer = vbb.asFloatBuffer();
                triangleBuffer.put(vertexArray);
                triangleBuffer.position(0);

                STLRenderer.requestRedraw();

                progressDialog.dismiss();
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                progressDialog.setProgress(values[0]);
            }
        };

        try {
            task.execute(stlBytes);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private static ProgressDialog prepareProgressDialog(Context context) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.stl_load_progress_title);
        progressDialog.setMax(0);
        progressDialog.setMessage(context.getString(R.string.stl_load_progress_message));
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        progressDialog.show();

        return progressDialog;
    }

    private void adjustMaxMin(float x, float y, float z) {
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (z > maxZ) {
            maxZ = z;
        }
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (z < minZ) {
            minZ = z;
        }
    }

    private int getIntWithLittleEndian(int offset) {
        return (0xff & stlBytes[offset]) | ((0xff & stlBytes[offset + 1]) << 8) | ((0xff & stlBytes[offset + 2]) << 16) | ((0xff & stlBytes[offset + 3]) << 24);
    }

    public void draw(GL10 gl) {
        if (normalList == null || triangleBuffer == null) {
            return;
        }
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, triangleBuffer);

        for (int i = 0; i < normalList.size() / 3; i++) {
            gl.glNormal3f(normalList.get(i * 3), normalList.get(i * 3 + 1), normalList.get(i * 3 + 2));
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, i * 3, 3);
        }

    }
}
