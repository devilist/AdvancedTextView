/*
 * Copyright  2019  zengp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devilist.readdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Created by zengpu on 2019/7/15
 */
public class DemoView extends View {

    TextPaint paint0, paint1, paint2;
    Paint paint00;

    Bitmap bitmap;

    private float touchDownX = 0;
    private float transX = 0;
    private float lastTransX = 0;

    public DemoView(Context context) {
        super(context);
        initView();
    }

    public DemoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DemoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        paint0 = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint0.setColor(Color.RED);
        paint0.setTextSize(46);
        paint1 = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint1.setColor(Color.BLACK);
        paint1.setTextSize(48);
        paint2 = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint2.setColor(Color.BLUE);
        paint2.setTextSize(16);

        paint00 = new Paint(Paint.ANTI_ALIAS_FLAG);

        bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg1);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                transX = lastTransX + event.getX() - touchDownX;
                break;
            case MotionEvent.ACTION_UP:
                lastTransX = transX;
                break;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Matrix matrix = new Matrix();
        RectF oriRect = new RectF(0, 0, getWidth(), getHeight());
        RectF bmpRect = new RectF(0, 0, bitmap.getWidth(), getHeight());
        matrix.setRectToRect(bmpRect, oriRect, Matrix.ScaleToFit.FILL);
        canvas.drawBitmap(bitmap, matrix, paint00);
        int ori = canvas.saveLayer(0, 0, getWidth(), getHeight(), paint1, Canvas.ALL_SAVE_FLAG);
        paint1.setColor(Color.RED);
        drawText(canvas, StringUtil.str_2, 0);
        canvas.restoreToCount(ori);

        canvas.saveLayer(getWidth() + transX - 5, -5, getWidth() + transX + 50, getHeight() + 5, paint00, Canvas.ALL_SAVE_FLAG);
        LinearGradient half1 = new LinearGradient(getWidth() + transX, getHeight() / 2,
                getWidth() + transX + 50, getHeight() / 2,
                new int[]{0x66000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        paint00.setShader(half1);
        canvas.drawRect(getWidth() + transX - 5, -5, getWidth() + transX + 50, getHeight() + 5, paint00);
        canvas.restoreToCount(ori);

        canvas.saveLayer(transX - 50, -5, transX + 5, getHeight() + 5, paint00, Canvas.ALL_SAVE_FLAG);
        LinearGradient half2 = new LinearGradient(transX, getHeight() / 2,
                transX - 50, getHeight() / 2,
                new int[]{0x66000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        paint00.setShader(half2);
        canvas.drawRect(transX - 50, -5, transX + 5, getHeight() + 5, paint00);
        canvas.restoreToCount(ori);

        canvas.saveLayer(transX, 0, getWidth() + transX, getHeight(), paint00, Canvas.ALL_SAVE_FLAG);
        paint00.setShader(half1);
        oriRect = new RectF(transX, 0, getWidth() + transX, getHeight());
        matrix.reset();
        matrix.setRectToRect(bmpRect, oriRect, Matrix.ScaleToFit.FILL);
        canvas.drawBitmap(bitmap, matrix, paint00);
        paint1.setColor(Color.BLACK);
        drawText(canvas, StringUtil.str_1, transX);
        canvas.restoreToCount(ori);
    }

    private void drawText(Canvas canvas, String text, float transX) {

        float rowH = paint1.getTextSize() * 1.3f;
        float currentHeight = getPaddingTop() + rowH;
        float currentPos = getPaddingLeft() + transX;

        for (int i = 0; i < text.length(); i++) {
            String s = String.valueOf(text.charAt(i));
            if (s.equals("\n") ||currentPos > getWidth() - getPaddingRight() + transX) {
                if (currentHeight > getHeight() - getPaddingBottom())
                    break;
                currentPos = getPaddingLeft() + transX;
                currentHeight += rowH;
            }
            float sWidth = StaticLayout.getDesiredWidth(s, 0, 1, paint1);
            canvas.drawText(s, currentPos, currentHeight, paint1);
            currentPos = currentPos + sWidth + 5;
        }

    }
}
