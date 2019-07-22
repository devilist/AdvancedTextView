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
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import android.text.Html;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class DemoView1 extends View {
    private static final String TAG = "DemoView1";

    private static float MIN_POSITION_X = 20;
    private static float THRESHOLD_MOTION_PAGE_TURN = 10;

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

    private PointF mPMoveOpt = new PointF();  // 修正
    private PointF mPMoveOri = new PointF();

    private Path mPathPage1 = new Path();
    private Path mPathPage2 = new Path();
    private Path mPathTmp = new Path();

    private TextPaint mPaintText;
    private Paint mPaintShader;
    private Paint mPaintLine;

    private PageTurn mTurnManager;

    private Camera mCamera;

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
        mPaintText.setTextSize(52);
        mPaintShader = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mPaintShader.setColor(Color.BLUE);
        mPaintShader.setTextSize(16);

        mPaintLine = new Paint(Paint.ANTI_ALIAS_FLAG);

        bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg1);
        bitmap1 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg2);
        mTurnManager = new PageTurn();
        mTurnManager.reset();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) { // 4.1.2
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        mCamera = new Camera();
    }

    // 翻页操作管理
    private class PageTurn {
        // 触摸位置
        final class Area {
            static final int none = -1;
            static final int top_left = 0;
            static final int top_right = 1;
            static final int bottom_left = 2;
            static final int bottom_right = 3;
            static final int center_left = 4;
            static final int center_right = 5;
        }

        // 翻页方向
        final class Direction {
            static final int none = -1;
            static final int left_to_right = 0;
            static final int right_to_left = 1;
        }

        // 翻页的起始点
        final class Anchor {
            static final int none = -1;
            static final int top = 0;
            static final int center = 1;
            static final int bottom = 2;
        }

        boolean isValid = false;
        boolean isCalFinish = false;

        int touchArea = Area.none;
        int turnDirection = Direction.none;
        int turnAnchor = Anchor.none;

        private PointF touchDown = new PointF();

        void reset() {
            isValid = false;
            isCalFinish = false;
            touchArea = Area.none;
            turnDirection = Direction.none;
            turnAnchor = Anchor.none;
            touchDown.set(-1, -1);
        }

        void calTouchArea(float downX, float downY) {
            this.touchDown.set(downX, downY);
            if (downY <= Math.min(mPageW - MIN_POSITION_X, mPageH - (mPageW - MIN_POSITION_X))) {
                if (downX >= mPageW / 2) touchArea = Area.top_right;
                else touchArea = Area.top_left;
            } else if (downY >= Math.max(mPageW - MIN_POSITION_X, mPageH - (mPageW - MIN_POSITION_X))) {
                if (downX >= mPageW / 2) touchArea = Area.bottom_right;
                else touchArea = Area.bottom_left;
            } else {
                if (downX >= mPageW / 2) touchArea = Area.center_right;
                else touchArea = Area.center_left;
            }
        }

        void calTurnMode(float moveX, float moveY) {
            if (isCalFinish) return;
            float d = (float) Math.sqrt((moveX - touchDown.x) * (moveX - touchDown.x)
                    + (moveY - touchDown.y) * (moveY - touchDown.y));
            // 滑动距离太小
            if (d < THRESHOLD_MOTION_PAGE_TURN) {
                isValid = false;
                return;
            }

            isCalFinish = true;

            // 竖向滑动
            if (Math.abs(moveY - touchDown.y) >= 1.732 * Math.abs(moveX - touchDown.x)) {
                isValid = false;
                return;
            }

            turnDirection = moveX - touchDown.x >= 0 ? Direction.left_to_right : Direction.right_to_left;

            if (touchArea == Area.center_left || touchArea == Area.center_right) {
                turnAnchor = Anchor.center;
            } else if (touchArea == Area.top_right) {
                turnAnchor = Anchor.top;
            } else if (touchArea == Area.bottom_right) {
                turnAnchor = Anchor.bottom;
            } else if (touchArea == Area.top_left) {
                if (turnDirection == Direction.left_to_right) {
                    turnAnchor = Anchor.bottom;
                } else {
                    turnAnchor = Anchor.top;
                }
            } else if (touchArea == Area.bottom_left) {
                if (turnDirection == Direction.left_to_right) {
                    turnAnchor = Anchor.top;
                } else {
                    turnAnchor = Anchor.bottom;
                }
            }
            isValid = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mPageW = getWidth();
        mPageH = getHeight();
        mPMoveOri.set(event.getX(), event.getY());


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPMove.set(event.getX(), event.getY());
                mTurnManager.reset();
                mTurnManager.calTouchArea(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                mTurnManager.calTurnMode(event.getX(), event.getY());
                if (mTurnManager.isCalFinish && mTurnManager.isValid) {
                    if (mTurnManager.turnAnchor == PageTurn.Anchor.top) {
                        mP0.set(mPageW, 0);
                    } else if (mTurnManager.turnAnchor == PageTurn.Anchor.bottom) {
                        mP0.set(mPageW, mPageH);
                    } else if (mTurnManager.turnAnchor == PageTurn.Anchor.center) {
                        mP0.set(mPageW, mPageH / 2);
                    }
                    mPMove.set(event.getX(), event.getY());
                    calPathAtArea();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (mTurnManager.isCalFinish && mTurnManager.isValid) {
                    mPMove.set(event.getX(), event.getY());
                    calPathAtArea();
                }
                break;
        }
        invalidate();
        return true;
    }

    private void calPathAtArea() {
        if (mTurnManager.turnAnchor == PageTurn.Anchor.center) {
            calPathRect();
        } else {
            calPath();
        }
    }

    private void calPath() {
        //判断触点位置是否在边界圆外面.要根据翻页方向分情况处理
        // 向右翻页，触摸点在左半部分，则矫正点的位置，让翻页效果更真实
//        if (mTurnManager.turnDirection == PageTurn.Direction.left_to_right
//                && (mTurnManager.touchArea == PageTurn.Area.top_left
//                || mTurnManager.touchArea == PageTurn.Area.bottom_left)) {
//            Log.e(TAG, "修正");
//        }
        // 调整x的变化率
        // xFinal = -a*x*x/W/W + b*W -W ; b-a =2 ,a>0
        mPMove.x = -1.2f * (float) Math.pow(mPMove.x, 3) / (float) Math.pow(mPageW, 2) + 3.2f * mPMove.x - mPageW;
        if (mP0.y == 0) {
            mPMove.y = mPMove.y >= mPageH / 2 ? (mPageH - mPMove.y) : mPMove.y;
        } else {
            mPMove.y = mPMove.y <= mPageH / 2 ? (mPageH - mPMove.y) : mPMove.y;
        }

        mPMoveOpt.set(mPMove.x, mPMove.y);

        float r = (float) Math.sqrt((mPMove.x - MIN_POSITION_X) * (mPMove.x - MIN_POSITION_X)
                + (mPMove.y - mP0.y) * (mPMove.y - mP0.y));
        if (r > mPageW - MIN_POSITION_X) { // 调整move坐标
            float sin = Math.abs((mPMove.y - mP0.y) / r);
            mPMove.x = MIN_POSITION_X + (mPageW - MIN_POSITION_X) * (float) Math.sqrt(1 - sin * sin)
                    * Math.signum(mPMove.x - MIN_POSITION_X);
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
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        // 上面一页 和背景
        mPaintText.setAlpha(255);
        mPaintText.setColor(Color.RED);
        int ori = canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintText, Canvas.ALL_SAVE_FLAG);
        drawBg(canvas);
        drawText(canvas, StringUtil.str_1);
        canvas.restoreToCount(ori);

        if (!mTurnManager.isCalFinish || !mTurnManager.isValid) {
            return;
        }
        if (mTurnManager.turnAnchor == PageTurn.Anchor.center) {
            // 下面一页
            canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintText, Canvas.ALL_SAVE_FLAG);
            mPaintText.setColor(Color.BLACK);
            mPaintText.setAlpha(255);
            canvas.clipPath(mPathPage1, Region.Op.INTERSECT);
            drawBg(canvas);
            drawText(canvas, StringUtil.str_2);
            // 背后阴影
            float x0 = (mPMove.x + mPMoveCenter.x) / 2;
            float y0 = mP0.y;
            float x1 = (mP0.x + mPMoveCenter.x) / 2;
            float y1 = mP0.y;
            LinearGradient half1 = new LinearGradient(x0, y0, x1, y1,
                    new int[]{0x99000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
            mPaintShader.setShader(half1);
            canvas.drawRect(new RectF(x0, 0, x1, mPageH), mPaintShader);
            mPaintShader.setShader(null);
            mPaintShader.setStyle(Paint.Style.FILL);
            mPaintShader.setColor(0xffffffff);
            canvas.clipPath(mPathPage2, Region.Op.INTERSECT);
            canvas.drawRect(new RectF(mPMove.x, 0, mPMoveCenter.x, mPageH), mPaintShader);

            // 翻页文字
            mCamera.save();
            canvas.translate(mPMoveCenter.x, mPMoveCenter.y);
            mCamera.rotateY(180);
            mCamera.applyToCanvas(canvas);
            canvas.translate(-mPMoveCenter.x, -mPMoveCenter.y);
            mCamera.restore();
            mPaintText.setColor(Color.RED);
            mPaintText.setAlpha(20);
            drawText(canvas, StringUtil.str_1);

            canvas.restoreToCount(ori);

            // 前面阴影
            canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintShader, Canvas.ALL_SAVE_FLAG);
            canvas.clipPath(mPathPage1, Region.Op.DIFFERENCE);
            x1 = 2 * mPMove.x - x0;
            y1 = mP0.y;
            half1 = new LinearGradient(x0, y0, x1, y1,
                    new int[]{0x99000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
            mPaintShader.setShader(half1);
            canvas.drawRect(new RectF(x0, 0, x1, mPageH), mPaintShader);
            mPaintShader.setShader(null);
            canvas.restoreToCount(ori);
        } else {

            drawPageTurnFromCorner(ori, canvas);
            // 辅助线
//            drawLines(ori, canvas);
        }

    }

    private void drawBg(Canvas canvas) {
        Matrix matrix = new Matrix();
        RectF oriRect = new RectF(0, 0, getWidth(), getHeight());
        RectF bmpRect = new RectF(0, 0, bitmap.getWidth(), getHeight());
        matrix.setRectToRect(bmpRect, oriRect, Matrix.ScaleToFit.FILL);
        canvas.drawBitmap(bitmap, matrix, mPaintText);
    }

    private void drawPageTurnFromCorner(int ori, Canvas canvas) {
        // 下面一页 页面文字
        mPaintText.setColor(Color.BLACK);
        mPaintText.setAlpha(255);
        canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintText, Canvas.ALL_SAVE_FLAG);
        canvas.clipPath(mPathPage1, Region.Op.INTERSECT);
        drawBg(canvas);
        drawText(canvas, StringUtil.str_2);

        // 翻页垂分线处阴影
        mPaintShader.setStyle(Paint.Style.FILL);
        float x0 = (mPMove.x + mPMoveCenter.x) / 2;
        float y0 = (mPMove.y + mPMoveCenter.y) / 2;
        LinearGradient half1 = new LinearGradient(x0, y0, mPMoveCenter.x, mPMoveCenter.y,
                new int[]{0x99000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        mPaintShader.setShader(half1);
        mPathTmp.reset();
        mPathTmp.moveTo(mPBoundary1.x, mPBoundary1.y);
        mPathTmp.lineTo(mPBoundary2.x, mPBoundary2.y);
        mPathTmp.lineTo(mPBezier2Control.x, mPBezier2Control.y);
        mPathTmp.lineTo(mPBezier1Control.x, mPBezier1Control.y);
        mPathTmp.close();
        canvas.drawPath(mPathTmp, mPaintShader);
        mPaintShader.setShader(null);
        /////////////////////////////////////////////////////////////////////////////////////////
        // 翻页的脚
        canvas.clipPath(mPathPage2, Region.Op.INTERSECT); // 与运算,先把整个角裁出来
        mPathTmp.reset();
        mPathTmp.moveTo(mPMove.x, mPMove.y);
        mPathTmp.lineTo(mPBezier1Center.x, mPBezier1Center.y);
        mPathTmp.lineTo(mPBezier2Center.x, mPBezier2Center.y);
        mPathTmp.close();
        canvas.clipPath(mPathTmp, Region.Op.INTERSECT); // 与运算,裁去多余的两段贝塞尔曲线
//        mPaintShader.setStyle(Paint.Style.FILL);
//        mPaintShader.setColor(0xffffffff);
//        canvas.drawPath(mPathPage2, mPaintShader);
        canvas.drawColor(Color.WHITE);

        // 绘制页面背后反向的文字
        double deg = Math.atan(Math.abs(mPMove.y - mP0.y) / Math.abs(mPMove.x - mP0.x));
        deg = 90 - Math.toDegrees(deg);
        deg = deg * (mP0.y == 0 ? -1 : 1);
        // camera的绘制是倒着来的....
        mCamera.save();
        canvas.translate(mPMoveCenter.x, mPMoveCenter.y);
        canvas.rotate(-(float) deg);
        mCamera.rotateX(180);
        mCamera.applyToCanvas(canvas);
        canvas.rotate((float) deg);
        canvas.translate(-mPMoveCenter.x, -mPMoveCenter.y);
        mCamera.restore();
        mPaintText.setColor(Color.RED);
        mPaintText.setAlpha(20);
        drawText(canvas, StringUtil.str_2);

        canvas.restoreToCount(ori);

        // 前面尖角处的阴影
        canvas.saveLayer(0, 0, getWidth(), getHeight(), mPaintShader, Canvas.ALL_SAVE_FLAG);
        canvas.clipPath(mPathPage1, Region.Op.DIFFERENCE);
        half1 = new LinearGradient(mPEdge1Center.x, mPEdge1Center.y, mPEdge1Shader.x, mPEdge1Shader.y,
                new int[]{0x44000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
        LinearGradient half2 = new LinearGradient(mPEdge2Center.x, mPEdge2Center.y, mPEdge2Shader.x, mPEdge2Shader.y,
                new int[]{0x44000000, Color.TRANSPARENT}, new float[]{0, 1}, Shader.TileMode.CLAMP);
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

    private void drawText(Canvas canvas, String text) {

        float rowH = mPaintText.getTextSize() * 1.3f;
        float currentHeight = getPaddingTop() + rowH;
        float currentPos = getPaddingLeft();

        for (int i = 0; i < text.length(); i++) {
            String s = String.valueOf(text.charAt(i));
            if (s.equals("\n") || currentPos > getWidth() - getPaddingRight()) {
                if (currentHeight > getHeight() - getPaddingBottom())
                    break;
                currentPos = getPaddingLeft();
                currentHeight += rowH;
            }
            float sWidth = StaticLayout.getDesiredWidth(s, 0, 1, mPaintText);
            canvas.drawText(s, currentPos, currentHeight, mPaintText);
            currentPos = currentPos + sWidth + 5;
        }

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

        canvas.drawLine(MIN_POSITION_X, mP0.y, mPMoveOri.x, mPMoveOri.y, mPaintLine);

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

        mPaintLine.setStyle(Paint.Style.FILL);
        mPaintLine.setColor(Color.BLUE);
        canvas.drawCircle(mPMoveOpt.x, mPMoveOpt.y, 10, mPaintLine);
        mPaintLine.setColor(Color.RED);
        canvas.drawCircle(mPMove.x, mPMove.y, 10, mPaintLine);
        canvas.restoreToCount(ori);
    }
}
