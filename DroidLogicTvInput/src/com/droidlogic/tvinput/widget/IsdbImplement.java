package com.droidlogic.tvinput.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IsdbImplement {
    final String TAG = "IsdbImplement";

    int video_left;
    int video_right;
    int video_top;
    int video_bottom;
    int video_height;
    int video_width;
    double x_dimension;
    double y_dimension;
    double safe_area_left;
    double safe_area_top;

    double video_h_v_rate_on_screen;
    int video_h_v_rate_origin;

    Paint text_paint;
    Paint background_paint;

    IsdbImplement()
    {
        background_paint = new Paint();
        text_paint = new Paint();
    }

    void updateVideoPosition(String ratio, String screen_mode, String video_status)
    {
        try {
            String hs_str = video_status.split("VPP_hsc_startp 0x")[1].split("\\.")[0];
            String he_str = video_status.split("VPP_hsc_endp 0x")[1].split("\\.")[0];
            String vs_str = video_status.split("VPP_vsc_startp 0x")[1].split("\\.")[0];
            String ve_str = video_status.split("VPP_vsc_endp 0x")[1].split("\\.")[0];

            video_left = Integer.valueOf(hs_str, 16);
            video_right = Integer.valueOf(he_str, 16);
            video_top = Integer.valueOf(vs_str, 16);
            video_bottom = Integer.valueOf(ve_str, 16);
            video_height = video_bottom - video_top;
            video_width = video_right - video_left;
            //TODO:
            x_dimension = 960;
            y_dimension = 540;
            video_h_v_rate_on_screen = (double)(video_right - video_left) / (double)(video_bottom - video_top);
            safe_area_left = 0; //(video_right - video_left) * (1-0.8) / 2;
            safe_area_top = 0; //(video_bottom - video_top) * (1-0.8) / 2;
            Log.i(TAG, "position: "+ video_left + " " + video_right + " " + video_top +
                    " " + video_bottom + " " + video_h_v_rate_on_screen +
                    " " + x_dimension + " " + y_dimension + ratio + " " + screen_mode);
        } catch (Exception e) {
            Log.d(TAG, "position exception " + e.toString());
        }
        try {
            video_h_v_rate_origin = Integer.valueOf(ratio.split("0x")[1], 16);
//                Log.d(TAG, "ratio: " + video_h_v_rate_origin);
        } catch (Exception e) {
            //0x90 means 16:9 a fallback value
            video_h_v_rate_origin = 0x90;
            Log.d(TAG, "ratio exception " + e.toString());
        }
    }

    int fsize;
    int fscale;
    int fgcolor;
    int bgcolor;
    int rows;
    double font_actual_size;
    JSONArray row_array;
    int horizon_layout = 0;

    void draw(Canvas canvas, String jsonStr)
    {
        JSONObject ccObj;
        if (TextUtils.isEmpty(jsonStr)) {
            Log.e(TAG, "empty json");
            return;
        }
        try {
            ccObj = new JSONObject(jsonStr);
        } catch (Exception e) {
            return;
        }
        try {
            fsize = ccObj.getInt("fsize");
            fscale = ccObj.getInt("fscale");
            fgcolor = ccObj.getInt("fgcolor");
            bgcolor = ccObj.getInt("bgcolor");
            rows = ccObj.getInt("row_count");
            row_array = ccObj.getJSONArray("rows");
            horizon_layout = ccObj.getInt("horizon_layout");
            Log.e(TAG, fsize + " " + fscale + " " + fgcolor + " " + bgcolor + " " + rows);

            background_paint.setColor(Color.BLACK);
            text_paint.setColor(Color.WHITE);
            text_paint.setSubpixelText(true);
            font_actual_size = (video_height * fsize / y_dimension) / 2;
            text_paint.setTextSize((int)font_actual_size);
            text_paint.setTypeface(Typeface.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "get params failed: " + e.toString());
            return;
        }
        try {
            if (rows > 0) {
                for (int i = 0; i < rows; i++) {
                    int x,y;
                    JSONObject target_row = row_array.getJSONObject(i);
                    if (horizon_layout == 1) {
                        y = target_row.getInt("x");
                        x = target_row.getInt("y");
                    } else {
                        x = target_row.getInt("x");
                        y = target_row.getInt("y");
                    }
                    String str = target_row.getString("text");
                    Log.e(TAG, "x:"+x+" y:"+y +" str:"+str + " horizon? " + horizon_layout);

                    double str_left = x * video_width / x_dimension + safe_area_left;
                    double str_bottom = y * video_height / y_dimension + safe_area_top;
                    canvas.drawRect((float) str_left,
                            (float) str_bottom + text_paint.getFontMetrics().ascent,
                            (float) (str_left + text_paint.measureText(str)),
                            (float) str_bottom + text_paint.getFontMetrics().descent,
                            background_paint);
                    canvas.drawText(str, (float) str_left, (float)str_bottom, text_paint);
                    Log.e(TAG, "sleft " + str_left + " sright " + str_bottom);
//                    background_paint.setColor(Color.RED);
//                    canvas.drawLine(x-1000, x+1000, y, y, background_paint);
//                    canvas.drawLine(x, x, y-1000, y+ 1000, background_paint);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse rows detail failed "+e.toString());
            return;
        }
        Log.e(TAG, "Draw done");
    }
}
