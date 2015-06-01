package com.sihrc.stlviewer.util;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class IOUtils {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Convert <code>input</code> stream into byte[].
     *
     * @param input
     * @return Array of Byte
     * @throws IOException
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    /**
     * Copy <code>length</code> size of <code>input</code> stream to <code>output</code> stream.
     * This method will NOT close input and output stream.
     *
     * @param input
     * @param output
     * @return long copied length
     * @throws IOException
     */
    private static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Copy <code>length</code> size of <code>input</code> stream to <code>output</code> stream.
     *
     * @param input
     * @param output
     * @return long copied length
     * @throws IOException
     */
    public static long copy(InputStream input, OutputStream output, int length) throws IOException {
        byte[] buffer = new byte[length];
        int count = 0;
        int n = 0;
        int max = length;
        while ((n = input.read(buffer, 0, max)) != -1) {
            output.write(buffer, 0, n);
            count += n;
            if (count > length) {
                break;
            }

            max -= n;
            if (max <= 0) {
                break;
            }
        }
        return count;
    }

    /**
     * Close <code>closeable</code> quietly.
     *
     * @param closeable
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Throwable e) {
            // do nothing
        }
    }

    /**
     * @param context
     * @return
     */
    public static byte[] getSTLBytes(Context context, Uri uri) {
        byte[] stlBytes = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            stlBytes = toByteArray(inputStream);
        } catch (IOException e) {
        } finally {
            closeQuietly(inputStream);
        }
        return stlBytes;
    }

    /**
     * checks 'text' in ASCII code
     *
     * @param bytes
     * @return
     */
    public static boolean isText(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0x0a || b == 0x0d || b == 0x09) {
                // white spaces
                continue;
            }
            if (b < 0x20 || (0xff & b) >= 0x80) {
                // control codes
                return false;
            }
        }
        return true;
    }

    public static float[] listToFloatArray(List<Float> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    public static String upOneDirectory(String directory){
        String[] dirs = directory.split("/");
        StringBuilder stringBuilder = new StringBuilder("");

        for(int i = 0; i < dirs.length - 1; i++)
            stringBuilder.append(dirs[i]).append('/');

        return stringBuilder.toString();
    }
}
