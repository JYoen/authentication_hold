package com.example.screen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;
import android.view.View;


public class MyDraw extends View {
    private static final String TAG = "MyDraw";
    private final ShapeDrawable drawable;

    public MyDraw(Context context, int x, int y) {
        super(context);
        y -= 150;
        int width = 170;
        int height = 170;
        setContentDescription(context.getResources().getString(
                R.string.draw));

        drawable = new ShapeDrawable(new OvalShape());
        // If the color isn't set, the shape uses black as the default.
        drawable.getPaint().setColor(0xff81d8cf);
        Log.w(TAG, "MyDraw: " + x + "###" + y);
        // If the bounds aren't set, the shape can't be drawn.
        drawable.setBounds(x - width, y - height, x + width, y + height);
    }

    protected void onDraw(Canvas canvas) {
        drawable.draw(canvas);
    }


}
