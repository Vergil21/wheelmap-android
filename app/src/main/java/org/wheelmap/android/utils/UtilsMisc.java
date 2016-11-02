/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UtilsMisc {

    private static final String TAG = UtilsMisc.class.getSimpleName();

    public static float dbToPx(Resources r, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static void dumpCursorToLog(String tag, Cursor cursor) {
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            String row = DatabaseUtils.dumpCurrentRowToString(cursor);
            Log.d(tag, row);
            cursor.moveToNext();
        }

    }

    public static void dumpCursorCompare(String tag, Cursor oldCursor, Cursor newCursor) {
        Log.d(TAG, tag + " dumpCursorCompare cursor "
                + ((newCursor != null) ? newCursor.hashCode() : "null") + " count = "
                + ((newCursor != null) ? newCursor.getCount() : "null")
                + " isNewCursor = " + (newCursor != oldCursor));
    }

    public static void closeSilently(final InputStream inStream) {
        if (inStream == null) {
            return;
        }

        try {
            inStream.close();
        } catch (final IOException e) {
            // do nothing because we close silently
            Log.w(TAG, "cannot close inStream stream", e);
        }
    }

    public static String formatHtmlLink(String link, String text) {
        return String.format(Locale.US, "<a href=\"%s\">%s</a>", link, text);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setActivatedCompat(View view, boolean activated) {
        if (hasHoneycomb()) {
            view.setActivated(activated);
        }
    }

    public static boolean isGoogleTV(Context context) {
        return context.getPackageManager().hasSystemFeature(
                "com.google.android.tv");
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

	/*
     * Enable this if version 4.1 of android.jar is available in maven
	 * repository
	 * 
	 * public static boolean hasJellyBean() { return Build.VERSION.SDK_INT >=
	 * Build.VERSION_CODES.JELLY_BEAN; }
	 */

    public static boolean isTablet(Context context) {

        Configuration config = context.getApplicationContext().getResources().getConfiguration();

        boolean isLarge = (config.screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        int minDimension = (int) Math.min(
                dpHeight,
                dpWidth
        );

        boolean isTablet = isLarge && minDimension >= 540;
        Log.d(TAG, "isTablet: " + isTablet);
        Log.d(TAG, "minDimension: " + minDimension);
        if (!isTablet) {
            Log.d(TAG, "minDimension: " + minDimension);
            Log.d(TAG, "isLarge: " + isLarge);
        }
        return isTablet;
    }

    public static boolean isHoneycombTablet(Context context) {
        return hasHoneycomb() && isTablet(context);
    }

    public static int calcRotationOffset(Display display) {

        int rotation;
        try {
            Method method;
            if (hasFroyo()) {
                method = Display.class.getDeclaredMethod("getRotation",
                        new Class[0]);
            } else {
                method = Display.class.getDeclaredMethod("getOrientation",
                        new Class[0]);
            }
            rotation = (Integer) method.invoke(display, new Object[0]);
        } catch (NoSuchMethodException e) {
            return 0;
        } catch (IllegalArgumentException e) {
            return 0;
        } catch (IllegalAccessException e) {
            return 0;
        } catch (InvocationTargetException e) {
            return 0;
        }
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return -90;
            default:
                return 0;
        }
    }

    public static void showAsPopup(Activity activity) {
        activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        Display display = activity.getWindowManager().getDefaultDisplay();
        int width;
        if (android.os.Build.VERSION.SDK_INT < 13) {
            width = display.getWidth();
        } else {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
        }

        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            width *= 1d / 2d;
        } else {
            width *= (2d / 3d);
        }

        params.width = width < 600 ? 600 : width;
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        activity.getWindow().setAttributes(params);
    }

    public static File createImageFile(Context context) throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String imageFileName = "jpg_" + timeStamp + "_";

        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), imageFileName + ".jpg");
    }

    public static String getFilePathFromContentUri(Uri selectedVideoUri,
                                                   ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

}
