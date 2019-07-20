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
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class DemoView1 extends View {
    private static final String TAG = "DemoView1";

    private static float MIN_POSITION_X = 20;

    private float mPageW, mPageH;  // 宽高
    private PointF mP0 = new PointF();  // 起始点点
    private PointF mPMove = new PointF(); // 移动点
    private PointF mPMoveCenter = new PointF(); // p0 pMove 中点
    // 两条边的中点
    private PointF mPEdge1Center = new PointF();
    private PointF mPEdge2Center = new PointF();
    // 边界点
    private PointF mPBoundary1 = new PointF();
    private PointF mPBoundary2 = new PointF();
    // 贝塞尔曲线中点
    private PointF mPBezier1Center = new PointF();
    private PointF mPBezier2Center = new PointF();
    // 垂直平分线与坐标轴交点，作为贝塞尔曲线控制点
    private PointF mPBezier1Control = new PointF();
    private PointF mPBezier2Control = new PointF();
    // 阴影
    private PointF mPMoveShader = new PointF(); // 移动点附近的阴影
    private PointF mPEdge1Shader = new PointF(); // x轴交点
    private PointF mPEdge2Shader = new PointF(); // y轴交点

    private Path mPathPage1 = new Path();
    private Path mPathPage2 = new Path();
    private Path mPathTmp = new Path();

    private int mTouchArea = 0;

    private PointF oriP = new PointF();
    TextPaint mPaintText;
    Paint mPaintShader;
    Paint mPaintLine;
    Bitmap bitmap, bitmap1;


    public DemoView1(Context context) {
        super(context);
        initView();
    }

    public DemoView1(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DemoView1(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mPaintText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setColor(Color.RED);
        mPaintText.setTextSize(46);
        mPaintShader = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mPaintShader.setColor(Color.BLUE);
        mPaintShader.setTextSize(16);

        mPaintLine = new Paint(Paint.ANTI_ALIAS_FLAG);

        bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg1);
        bitmap1 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mPageW = getWidth();
        mPageH = getHeight();
        oriP.set(event.getX(), event.getY());

        mPMove.set(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getY() <= Math.min(mPageW - MIN_POSITION_X, mPageH - (mPageW - MIN_POSITION_X))) {
                    mP0.set(mPageW, 0);
                    mTouchArea = 0;
                } else if (event.getY() >= Math.max(mPageW - MIN_POSITION_X, mPageH - (mPageW - MIN_POSITION_X))) {
                    mP0.set(mPageW, mPageH);
                    mTouchArea = 0;
                } else {
                    mTouchArea = 1;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        calPathAtArea();
        invalidate();
        return true;
    }

    private void calPathAtArea() {
        if (mTouchArea == 0) {
            calPath();
        }
        if (mTouchArea == 1) {
            calPathRect();
        }
    }

    private void calPath() {
        //判断触点位置是否在边界圆外面
        float r = (float) Math.sqrt((mPMove.x - MIN_POSITION_X) * (mPMove.x - MIN_POSITION_X)
                + (mPMove.y - mP0.y) * (mPMove.y - mP0.y));
        if (r > mPageW - MIN_POSITION_X) { // 调整move坐标
            float sin = Math.abs((mPMove.y - mP0.y) / r);
            mPMove.x = (mPageW - MIN_POSITION_X) * (float) Math.sqrt(1 - sin * sin);
            mPMove.y = mP0.y == 0 ? (mPageW - MIN_POSITION_X) * sin : (mP0.y - (mPageW - MIN_POSITION_X) * sin);
        }
        // 斜率
        float k = (mP0.y - mPMove.y) / (mP0.x - mPMove.x);
        // 中心点
        mPMoveCenter.set((mP0.x + mPMove.x) / 2, (mP0.y + mPMove.y) / 2);
        // 手指move点和中心点连线的中心点
        PointF pMove_MoveCenter = new PointF();
        pMove_MoveCenter.x = (mPMove.x + mPMoveCenter.x) / 2;
        pMove_MoveCenter.y = (mPMove.y + mPMoveCenter.y) / 2;
        // y = (-1/k)*(x- pMoveMoveCenter.x) + pMoveMoveCenter.y
        // 求出跟 x轴交点，这个点为边界位置
        float xmin = -k * (mP0.y - pMove_MoveCenter.y) + pMove_MoveCenter.x;
        if (xmin < MIN_POSITION_X) {
            Log.e(TAG, " xmin: " + xmin);
            // 越界了，按照边界值处理，需要矫正move位置
            // y = -(1/k)(x- MIN_POSITION_X) + mP0.y;
            // y = y = k*(x -mP0.x) + mP0.y
            pMove_MoveCenter.x = (k * k * mP0.x + MIN_POSITION_X) / (k * k + 1);
            pMove_MoveCenter.y = k * (pMove_MoveCenter.x - mP0.x) + mP0.y;
            mPMove.x = (4 * pMove_MoveCenter.x - mP0.x) / 3;
            mPMove.y = (4 * pMove_MoveCenter.y - mP0.y) / 3;
            mPMoveCenter.set((mP0.x + mPMove.x) / 2, (mP0.y + mPMove.y) / 2);
            k = (mP0.y - mPMove.y) / (mP0.x - mPMove.x);
        }

        // 垂直平分线 y = -(1/k)(x- xc) + yc;
        // 垂分线与x,y轴交点
        mPBezier1Control.x = mPMoveCenter.x - k * (mP0.y - mPMoveCenter.y);
        mPBezier1Control.y = mP0.y;
        mPBezier2Control.x = mP0.x;
        mPBezier2Control.y = -1 / k * (mP0.x - mPMoveCenter.x) + mPMoveCenter.y;

        // 两条边的中点
        mPEdge1Center.x = (mPMove.x + mPBezier1Control.x) / 2;
        mPEdge1Center.y = (mPMove.y + mPBezier1Control.y) / 2;
        mPEdge2Center.x = (mPMove.x + mPBezier2Control.x) / 2;
        mPEdge2Center.y = (mPMove.y + mPBezier2Control.y) / 2;
        // y = -(1/k)(x- xc) + yc;
        // 边界点
        mPBoundary1.x = -k * (mP0.y - pMove_MoveCenter.y) + pMove_MoveCenter.x;
        mPBoundary1.y = mP0.y;
        mPBoundary2.x = mP0.x;
        mPBoundary2.y = -1 / k * (mP0.x - pMove_MoveCenter.x) + pMove_MoveCenter.y;

        // 贝塞尔曲线中点
        mPBezier1Center.x = ((mPEdge1Center.x + mPBoundary1.x) / 2 + mPBezier1Control.x) / 2;
        mPBezier1Center.y = ((mPEdge1Center.y + mPBoundary1.y) / 2 + mPBezier1Control.y) / 2;
        mPBezier2Center.x = ((mPEdge2Center.x + mPBoundary2.x) / 2 + mPBezier2Control.x) / 2;
        mPBezier2Center.y = ((mPEdge2Center.y + mPBoundary2.y) / 2 + mPBezier2Control.y) / 2;

        // 阴影需要的点
        float shaderWidth = (float) Math.sqrt((pMove_MoveCenter.x - mPMoveCenter.x) * (pMove_MoveCenter.x - mPMoveCenter.x)
                + (pMove_MoveCenter.y - mPMoveCenter.y) * (pMove_MoveCenter.y - mPMoveCenter.y)) / 4;
        float k1 = (mPMove.y - mPBezier1Control.y) / (mPMove.x - mPBezier1Control.x);
        float deltaX1 = (float) (shaderWidth * Math.sin(Math.atan(Math.abs(k1))));
        float deltaY1 = (float) (shaderWidth * Math.cos(Math.atan(Math.abs(k1))));
        mPEdge1Shader.x = mPEdge1Center.x - deltaX1;
        mPEdge1Shader.y = mPEdge1Center.y + Math.signum(k1) * deltaY1;

        float k2 = (mPMove.y - mPBezier2Control.y) / (mPMove.x - mPBezier2Control.x);
        float deltaX2 = (float) (shaderWidth * Math.sin(Math.atan(Math.abs(k2))));
        float deltaY2 = (float) (shaderWidth * Math.cos(Math.atan(Math.abs(k2))));
        mPEdge2Shader.x = mPEdge2Center.x + (mP0.y == 0 ? -Math.signum(k2) * deltaX2 : Math.signum(k2) * deltaX2);
        mPEdge2Shader.y = mPEdge2Center.y + (mP0.y == 0 ? deltaY2 : -deltaY2);
        // y = k1*(x - mPEdge1Shader.x) + mPEdge1Shader.y
        // y = k2*(x -mPEdge2Shader.x) + mPEdge2Shader.y
        mPMoveShader.x = (mPEdge2Shader.y - mPEdge1Shader.y + k1 * mPEdge1Shader.x - k2 * mPEdge2Shader.x) / (k1 - k2);
        mPMoveShader.y = k1 * (mPMoveShader.x - mPEdge1Shader.x) + mPEdge1Shader.y;


        mPathPage1.reset();
        mPathPage1.moveTo(mPBoundary1.x, mPBoundary1.y);
        mPathPage1.quadTo(mPBezier1Control.x, mPBezier1Control.y, mPEdge1Center.x, mPEdge1Center.y);
        mPathPage1.lineTo(mPMove.x, mPMove.y);
        mPathPage1.lineTo(mPEdge2Center.x, mPEdge2Center.y);
        mPathPage1.quadTo(mPBezier2Control.x, mPBezier2Control.y, mPBoundary2.x, mPBoundary2.y);
        mPathPage1.lineTo(mP0.x, mP0.y);
        mPathPage1.lineTo(mPBoundary1.x, mPBoundary1.y);
        mPathPage1.close();

        mPathPage2.reset();
        mPathPage2.moveTo(mPBoundary1.x, mPBoundary1.y);
        mPathPage2.quadTo(mPBezier1Control.x, mPBezier1Control.y, mPEdge1Center.x, mPEdge1Center.y);
        mPathPage2.lineTo(mPMove.x, mPMove.y);
        mPathPage2.lineTo(mPEdge2Center.x, mPEdge2Center.y);
        mPathPage2.quadTo(mPBezier2Control.x, mPBezier2Control.y, mPBoundary2.x, mPBoundary2.y);
        mPathPage2.lineTo(mPBezier2Control.x, mPBezier2Control.y);
        mPathPage2.lineTo(mPBezier1Control.x, mPBezier1Control.y);
        mPathPage2.lineTo(mPBoundary1.x, mPBoundary1.y);
        mPathPage2.close();

    }

    private void calPathRect() {
        mPMoveCenter.set((mP0.x + mPMove.x) / 2, mP0.y);
        mPathPage1.reset();
        mPathPage1.moveTo(mPMove.x, mPageH);
        mPathPage1.lineTo(mPageW, mPageH);
        mPathPage1.lineTo(mPageW, 0);
        mPathPage1.lineTo(mPMove.x, 0);

        mPathPage2.reset();
        mPathPage2.moveTo(mPMove.x, mPageH);
        mPathPage2.lineTo(mPMoveCenter.x, mPageH);
        mPathPage2.lineTo(mPMoveCenter.x, 0);
        mPathPage2.lineTo(mPMove.x, 0);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 上面一页
        mPaintText.setColor(Color.RED);
        int ori = canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintText, Canvas.ALL_SAVE_FLAG);
        Matrix matrix = new Matrix();
        RectF oriRect = new RectF(0, 0, getWidth(), getHeight());
        RectF bmpRect = new RectF(0, 0, bitmap.getWidth(), getHeight());
        matrix.setRectToRect(bmpRect, oriRect, Matrix.ScaleToFit.FILL);
        canvas.drawBitmap(bitmap, matrix, mPaintText);
        canvas.drawText("锄禾日当午", getWidth() / 3, getHeight() / 3, mPaintText);
        canvas.restoreToCount(ori);


        if (mTouchArea == 0) {
            drawPageTurnFromCorner(ori, canvas);
            // 辅助线
//            drawLines(ori, canvas);
        }
        if (mTouchArea == 1) {
            // 下面一页
            mPaintText.setColor(Color.BLACK);
            canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintText, Canvas.ALL_SAVE_FLAG);
            canvas.clipPath(mPathPage1, Region.Op.INTERSECT);
            canvas.drawText("汗滴禾下土", 2 * getWidth() / 3, 3 * getHeight() / 4, mPaintText);
            // 背后阴影
            float x0 = (mPMove.x + mPMoveCenter.x) / 2;
            float y0 = mP0.y;
            float x1 = (mP0.x + mPMoveCenter.x) / 2;
            float y1 = mP0.y;
            LinearGradient half1 = new LinearGradient(x0, y0, x1, y1,
                    new int[]{0x44000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
            mPaintShader.setShader(half1);
            canvas.drawRect(new RectF(x0, 0, x1, mPageH), mPaintShader);
            mPaintShader.setShader(null);
            mPaintShader.setStyle(Paint.Style.FILL);
            mPaintShader.setColor(0xffffffff);
            canvas.clipPath(mPathPage2, Region.Op.INTERSECT);
            canvas.drawRect(new RectF(mPMove.x, 0, mPMoveCenter.x, mPageH), mPaintShader);
            canvas.restoreToCount(ori);

            // 前面阴影
            canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintShader, Canvas.ALL_SAVE_FLAG);
            canvas.clipPath(mPathPage1, Region.Op.DIFFERENCE);
            x1 = 2 * mPMove.x - x0;
            y1 = mP0.y;
            half1 = new LinearGradient(x0, y0, x1, y1,
                    new int[]{0x44000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
            mPaintShader.setShader(half1);
            canvas.drawRect(new RectF(x0, 0, x1, mPageH), mPaintShader);
            mPaintShader.setShader(null);
            canvas.restoreToCount(ori);
        }

    }

    private void drawPageTurnFromCorner(int ori, Canvas canvas) {
        // 下面一页
        mPaintText.setColor(Color.BLACK);
        canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintText, Canvas.ALL_SAVE_FLAG);
        canvas.clipPath(mPathPage1, Region.Op.INTERSECT);
        canvas.drawText("汗滴禾下土", 2 * getWidth() / 3, 3 * getHeight() / 4, mPaintText);

        // 背后阴影
        mPaintShader.setStyle(Paint.Style.FILL);
        float x0 = (mPMove.x + mPMoveCenter.x) / 2;
        float y0 = (mPMove.y + mPMoveCenter.y) / 2;
        LinearGradient half1 = new LinearGradient(x0, y0, mPMoveCenter.x, mPMoveCenter.y,
                new int[]{0x44000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        mPaintShader.setShader(half1);
        mPathTmp.reset();
        mPathTmp.moveTo(mPBoundary1.x, mPBoundary1.y);
        mPathTmp.lineTo(mPBoundary2.x, mPBoundary2.y);
        mPathTmp.lineTo(mPBezier2Control.x, mPBezier2Control.y);
        mPathTmp.lineTo(mPBezier1Control.x, mPBezier1Control.y);
        mPathTmp.close();
        canvas.drawPath(mPathTmp, mPaintShader);
        mPaintShader.setShader(null);

        // 翻页脚
        canvas.clipPath(mPathPage2, Region.Op.INTERSECT);
        mPathTmp.reset();
        mPathTmp.moveTo(mPMove.x, mPMove.y);
        mPathTmp.lineTo(mPBezier1Center.x, mPBezier1Center.y);
        mPathTmp.lineTo(mPBezier2Center.x, mPBezier2Center.y);
        mPathTmp.close();
        canvas.clipPath(mPathTmp, Region.Op.INTERSECT);

        mPaintShader.setStyle(Paint.Style.FILL);
        mPaintShader.setColor(0xffffffff);
        canvas.drawPath(mPathPage2, mPaintShader);
        canvas.restoreToCount(ori);

        canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintShader, Canvas.ALL_SAVE_FLAG);
        // 前面阴影
        canvas.clipPath(mPathPage1, Region.Op.DIFFERENCE);

        half1 = new LinearGradient(mPEdge1Center.x, mPEdge1Center.y, mPEdge1Shader.x, mPEdge1Shader.y,
                new int[]{0x11000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        LinearGradient half2 = new LinearGradient(mPEdge2Center.x, mPEdge2Center.y, mPEdge2Shader.x, mPEdge2Shader.y,
                new int[]{0x11000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        mPathTmp.reset();
        mPathTmp.moveTo(mPMoveShader.x, mPMoveShader.y);
        mPathTmp.lineTo(mPEdge1Shader.x, mPEdge1Shader.y);
        mPathTmp.quadTo(mPBezier1Control.x, mPBezier1Control.y, mPBoundary1.x, mPBoundary1.y);
        mPathTmp.lineTo(mPBezier1Control.x, mPBezier1Control.y);
        mPathTmp.lineTo(mPMove.x, mPMove.y);
        mPathTmp.lineTo(mPMoveShader.x, mPMoveShader.y);
        mPathTmp.close();
        mPaintShader.setShader(half1);
        canvas.drawPath(mPathTmp, mPaintShader);
        mPathTmp.reset();
        mPathTmp.moveTo(mPMoveShader.x, mPMoveShader.y);
        mPathTmp.lineTo(mPEdge2Shader.x, mPEdge2Shader.y);
        mPathTmp.quadTo(mPBezier2Control.x, mPBezier2Control.y, mPBoundary2.x, mPBoundary2.y);
        mPathTmp.lineTo(mPBezier2Control.x, mPBezier2Control.y);
        mPathTmp.lineTo(mPMove.x, mPMove.y);
        mPathTmp.lineTo(mPMoveShader.x, mPMoveShader.y);
        mPathTmp.close();
        mPaintShader.setShader(half2);
        canvas.drawPath(mPathTmp, mPaintShader);
        canvas.restoreToCount(ori);
    }

    private void drawLines(int ori, Canvas canvas) {
        canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintLine, Canvas.ALL_SAVE_FLAG);
        mPaintLine.setStrokeWidth(3);
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setPathEffect(new DashPathEffect(new float[]{30, 30}, 0));
        mPaintLine.setColor(Color.MAGENTA);
        canvas.drawCircle(MIN_POSITION_X, 0, mPageW - MIN_POSITION_X, mPaintLine);
        canvas.drawCircle(MIN_POSITION_X, mPageH, mPageW - MIN_POSITION_X, mPaintLine);
        canvas.drawLine(MIN_POSITION_X, 0, MIN_POSITION_X, mPageH, mPaintLine);
        canvas.drawLine(0, mPageW - MIN_POSITION_X, mPageW, mPageW - MIN_POSITION_X, mPaintLine);
        canvas.drawLine(0, mPageH - (mPageW - MIN_POSITION_X), mPageW, mPageH - (mPageW - MIN_POSITION_X), mPaintLine);

        canvas.drawLine(MIN_POSITION_X, mP0.y, oriP.x, oriP.y, mPaintLine);

        mPaintLine.setPathEffect(null);
        mPaintLine.setStrokeWidth(3);
        mPaintLine.setColor(Color.BLUE);
        canvas.drawLine(mP0.x, mP0.y, mPMove.x, mPMove.y, mPaintLine);
        mPaintLine.setColor(Color.GRAY);
        canvas.drawLine(mPBezier1Control.x, mPBezier1Control.y, mPBezier2Control.x, mPBezier2Control.y, mPaintLine);

        mPaintLine.setColor(Color.GREEN);
        canvas.drawLine(mPBoundary1.x, mPBoundary1.y, mPBoundary2.x, mPBoundary2.y, mPaintLine);

        mPaintLine.setColor(Color.LTGRAY);
        canvas.drawLine(mPBezier1Control.x, mPBezier1Control.y, mPMove.x, mPMove.y, mPaintLine);
        canvas.drawLine(mPBezier2Control.x, mPBezier2Control.y, mPMove.x, mPMove.y, mPaintLine);
        canvas.drawLine(mPBezier1Center.x, mPBezier1Center.y, mPBezier2Center.x, mPBezier2Center.y, mPaintLine);

        canvas.drawLine(mPMoveShader.x, mPMoveShader.y, mPEdge2Shader.x, mPEdge2Shader.y, mPaintLine);
        canvas.drawLine(mPMoveShader.x, mPMoveShader.y, mPEdge1Shader.x, mPEdge1Shader.y, mPaintLine);
        canvas.drawLine(mPEdge2Center.x, mPEdge2Center.y, mPEdge2Shader.x, mPEdge2Shader.y, mPaintLine);
        canvas.drawLine(mPEdge1Center.x, mPEdge1Center.y, mPEdge1Shader.x, mPEdge1Shader.y, mPaintLine);
        canvas.restoreToCount(ori);
    }
}
