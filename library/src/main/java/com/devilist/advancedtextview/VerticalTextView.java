/*
 * Copyright  2017  zengp
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

package com.devilist.advancedtextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Vibrator;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static android.content.Context.VIBRATOR_SERVICE;
import static android.view.MotionEvent.ACTION_MOVE;

/**
 * VerticalTextView ———— 实现文字竖排的TextView。
 * <p> 功能：
 * <p> 1.文字从上到下竖排。
 * <p> 2.文字阅读方向可选择 从右向左 和 从左向右。
 * <p> 3.长按可选择文本，并弹出自定义的可定制化的菜单ActionMenu
 * <p>
 * Created by zengpu on 2017/1/20.
 */
public class VerticalTextView extends TextView {

    private static String TAG = VerticalTextView.class.getSimpleName();

    private final int TRIGGER_LONGPRESS_TIME_THRESHOLD = 300;    // 触发长按事件的时间阈值
    private final int TRIGGER_LONGPRESS_DISTANCE_THRESHOLD = 10; // 触发长按事件的位移阈值

    private Context mContext;
    private int mScreenWidth;      // 屏幕宽度
    private int mScreenHeight;      // 屏幕高度

    // attrs
    private boolean isLeftToRight;        // 竖排方向，是否从左到右；默认从右到左
    private float mLineSpacingExtra;      // 行距 默认 6px
    private float mCharSpacingExtra;      // 字符间距 默认 6px
    private boolean isUnderLineText;      // 是否需要下划线，默认false
    private int mUnderLineColor;          // 下划线颜色 默认 Color.RED
    private float mUnderLineWidth;        // 下划线线宽 默认 1.5f
    private float mUnderLineOffset;       // 下划线偏移 默认 3px
    private boolean isShowActionMenu;     // 是否显示ActionMenu，默认true
    private int mTextHighlightColor;      // 选中文字背景高亮颜色 默认0x60ffeb3b

    // onMeasure相关
    private int[] mTextAreaRoughBound; // 粗略计算的文本最大显示区域(包含padding)，用于view的测量和不同Gravity情况下文本的绘制
    private int[] mMeasureMode; // 宽高的测量模式

    private SparseArray<Float[]> mLinesOffsetArray; // 记录每一行文字的X,Y偏移量
    private SparseArray<int[]> mLinesTextIndex;     // 记录每一行文字开始和结束字符的index
    private int mMaxTextLine = 0;                   // 最大行数

    private int mStatusBarHeight;   // 状态栏高度
    private int mActionMenuHeight;  // 弹出菜单ActionMenu高度
    private String mSelectedText;   // 选择的文字

    // onTouchEvent相关
    private float mTouchDownX = 0;
    private float mTouchDownY = 0;
    private float mTouchDownRawY = 0;
    private boolean isLongPress = false;               // 是否发触了长按事件
    private boolean isLongPressTouchActionUp = false;  // 长按事件结束后，标记该次事件，防止手指抬起后view没有重绘
    private boolean isVibrator = false;                // 是否触发过长按震动
    private boolean isActionSelectAll = false;         // 是否触发全选事件

    private int mStartLine;                            // 长按触摸事件 所选文字的起始行
    private int mCurrentLine;                          // 长按触摸事件 移动过程中手指所在行
    private float mStartTextOffset;                    // 长按触摸事件 所选文字开始位置的Y向偏移值
    private float mCurrentTextOffset;                  // 长按触摸事件 移动过程中所选文字结束位置的Y向偏移值

    private Vibrator mVibrator;
    private PopupWindow mActionMenuPopupWindow;     // 长按弹出菜单
    private ActionMenu mActionMenu = null;          // ActionMenu

    private OnClickListener mOnClickListener;
    private CustomActionMenuCallBack mCustomActionMenuCallBack;

    public VerticalTextView(Context context) {
        this(context, null);
    }

    public VerticalTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalTextView);
        mLineSpacingExtra = mTypedArray.getDimension(R.styleable.VerticalTextView_lineSpacingExtra, 6);
        mCharSpacingExtra = mTypedArray.getDimension(R.styleable.VerticalTextView_charSpacingExtra, 6);
        isLeftToRight = mTypedArray.getBoolean(R.styleable.VerticalTextView_textLeftToRight, false);
        isUnderLineText = mTypedArray.getBoolean(R.styleable.VerticalTextView_underLineText, false);
        mUnderLineColor = mTypedArray.getColor(R.styleable.VerticalTextView_underLineColor, Color.RED);
        mUnderLineWidth = mTypedArray.getFloat(R.styleable.VerticalTextView_underLineWidth, 1.5f);
        mUnderLineOffset = mTypedArray.getDimension(R.styleable.VerticalTextView_underlineOffset, 3);
        mTextHighlightColor = mTypedArray.getColor(R.styleable.VerticalTextView_textHeightLightColor, 0x60ffeb3b);
        isShowActionMenu = mTypedArray.getBoolean(R.styleable.VerticalTextView_showActionMenu, false);
        mTypedArray.recycle();

        mLineSpacingExtra = Math.max(6, mLineSpacingExtra);
        mCharSpacingExtra = Math.max(6, mCharSpacingExtra);
        if (isUnderLineText) {
            mUnderLineWidth = Math.abs(mUnderLineWidth);
            mUnderLineOffset = Math.min(Math.abs(mUnderLineOffset), Math.abs(mLineSpacingExtra) / 2);
        }

        init();
    }

    private void init() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mScreenWidth = wm.getDefaultDisplay().getWidth();
        mScreenHeight = wm.getDefaultDisplay().getHeight();
        setTextIsSelectable(false);
        mLinesOffsetArray = new SparseArray<>();
        mLinesTextIndex = new SparseArray<>();
        mTextAreaRoughBound = new int[]{0, 0};
        mStatusBarHeight = Utils.getStatusBarHeight(mContext);
        mActionMenuHeight = Utils.dp2px(mContext, 45);
        mVibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
    }

    public VerticalTextView setLeftToRight(boolean leftToRight) {
        isLeftToRight = leftToRight;
        return this;
    }

    public VerticalTextView setLineSpacingExtra(float lineSpacingExtra) {
        this.mLineSpacingExtra = Utils.dp2px(mContext, lineSpacingExtra);
        return this;
    }

    public VerticalTextView setCharSpacingExtra(float charSpacingExtra) {
        this.mCharSpacingExtra = Utils.dp2px(mContext, charSpacingExtra);
        return this;
    }

    public VerticalTextView setUnderLineText(boolean underLineText) {
        isUnderLineText = underLineText;
        return this;
    }

    public VerticalTextView setUnderLineColor(int underLineColor) {
        this.mUnderLineColor = underLineColor;
        return this;
    }

    public VerticalTextView setUnderLineWidth(float underLineWidth) {
        this.mUnderLineWidth = underLineWidth;
        return this;
    }

    public VerticalTextView setUnderLineOffset(float underLineOffset) {
        this.mUnderLineOffset = Utils.dp2px(mContext, underLineOffset);
        return this;
    }

    public VerticalTextView setShowActionMenu(boolean showActionMenu) {
        isShowActionMenu = showActionMenu;
        return this;
    }

    public VerticalTextView setTextHighlightColor(int color) {
        this.mTextHighlightColor = color;
        String color_hex = String.format("%08X", color);
        color_hex = "#40" + color_hex.substring(2);
        setHighlightColor(Color.parseColor(color_hex));
        return this;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
        if (null != l) {
            mOnClickListener = l;
        }
    }

    /**
     * 设置ActionMenu菜单内容监听
     *
     * @param callBack
     */
    public void setCustomActionMenuCallBack(CustomActionMenuCallBack callBack) {
        this.mCustomActionMenuCallBack = callBack;
    }

    /* ***************************************************************************************** */
    // view测量部分
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // view的初始测量宽高(包含padding)
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        Log.d(TAG, "widthSize " + widthSize);
        Log.d(TAG, "heightSize " + heightSize);
        // 粗略计算文字的最大宽度和最大高度，用于修正最后的测量宽高
        mTextAreaRoughBound = getTextRoughSize(heightSize == 0 ? mScreenHeight : heightSize,
                mLineSpacingExtra, mCharSpacingExtra);

        int measuredWidth;
        int measureHeight;

//        if (widthSize == 0) {
//            // 当嵌套在HorizontalScrollView时，MeasureSpec.getSize(widthMeasureSpec)返回0，因此需要特殊处理
//            measuredWidth = mTextAreaRoughBound[0];
//        } else if (widthSize <= mScreenWidth) {
//            measuredWidth = mTextAreaRoughBound[0] <= mScreenWidth ?
//                    Math.max(widthSize, mTextAreaRoughBound[0]) : widthSize;
//        } else {
//            measuredWidth = mTextAreaRoughBound[0] <= mScreenWidth ?
//                    mScreenWidth : Math.min(widthSize, mTextAreaRoughBound[0]);
//        }

        if (widthSize == 0) {
            // 当嵌套在HorizontalScrollView时，MeasureSpec.getSize(widthMeasureSpec)返回0，因此需要特殊处理
            measuredWidth = mTextAreaRoughBound[0];
        } else {
            measuredWidth = widthMode == MeasureSpec.AT_MOST ? mTextAreaRoughBound[0] : widthSize;
        }

        if (heightSize == 0) {
            // 当嵌套在ScrollView时，MeasureSpec.getSize(widthMeasureSpec)返回0，因此需要特殊处理
            measureHeight = mScreenHeight;
        } else {
            measureHeight = heightMode == MeasureSpec.AT_MOST ? mTextAreaRoughBound[1] : heightSize;
        }
        setMeasuredDimension(measuredWidth, measureHeight);

        Log.d(TAG, "measuredWidth " + measuredWidth);
        Log.d(TAG, "measureHeight " + measureHeight);
    }

    /**
     * 粗略计算文本的宽度和高度(包含padding)，用于修正最后的测量宽高
     *
     * @param oriHeightSize    初始测量高度 必须大于0。当等于0时，用屏幕高度代替
     * @param lineSpacingExtra
     * @param charSpacingExtra
     * @return int[textWidth, textHeight]
     */
    private int[] getTextRoughSize(int oriHeightSize, float lineSpacingExtra,
                                   float charSpacingExtra) {

        // 将文本用换行符分隔，计算粗略的行数
        String[] subTextStr = getText().toString().split("\n");
        int textLines = 0;
        // 用于计算最大高度的目标子段落
        String targetSubPara = "";
        int tempLines = 1;
        float tempLength = 0;
        // 计算每个段落的行数，然后累加
        for (String aSubTextStr : subTextStr) {
            // 段落的粗略长度(字符间距也要考虑进去)
            float subParagraphLength = aSubTextStr.length() * (getTextSize() + charSpacingExtra);
            // 段落长度除以初始测量高度，得到粗略行数
            int subLines = (int) Math.ceil(subParagraphLength
                    / Math.abs(oriHeightSize - getPaddingTop() - getPaddingBottom()));
            if (subLines == 0)
                subLines = 1;
            textLines += subLines;
            // 如果所有子段落的行数都为1,则最大高度为长度最长的子段落长度；否则最大高度为oriHeightSize；
            if (subLines == 1 && tempLines == 1) {
                if (subParagraphLength > tempLength) {
                    tempLength = subParagraphLength;
                    targetSubPara = aSubTextStr;
                }
            }
            tempLines = subLines;
        }
        // 计算文本粗略高度，包括padding
        int textHeight = getPaddingTop() + getPaddingBottom();
        if (textLines > subTextStr.length)
            textHeight = oriHeightSize;
        else {
            // 计算targetSubPara长度作为高度
            for (int i = 0; i < targetSubPara.length(); i++) {
                String char_i = String.valueOf(getText().toString().charAt(i));
                // 区别标点符号 和 文字
                if (isUnicodeSymbol(char_i)) {
                    textHeight += 1.4f * getCharHeight(char_i, getTextPaint()) + charSpacingExtra;
                } else {
                    textHeight += getTextSize() + charSpacingExtra;
                }
            }
        }
        // 计算文本的粗略宽度，包括padding，
        int textWidth = getPaddingLeft() + getPaddingRight() +
                (int) ((textLines + 1) * getTextSize() + lineSpacingExtra * (textLines - 1));
        Log.d(TAG, "textRoughLines " + textLines);
        Log.d(TAG, "textRoughWidth " + textWidth);
        Log.d(TAG, "textRoughHeight " + textHeight);
        return new int[]{textWidth, textHeight};
    }

    /* ***************************************************************************************** */
    // 触摸事件部分。处理onclick事件，长按选择文字事件 和 弹出ActionMenu事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int currentLine; // 当前所在行
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN");
                // 每次按下时，创建ActionMenu菜单，创建不成功，屏蔽长按事件
                if (null == mActionMenu) {
                    mActionMenu = createActionMenu();
                }
                mTouchDownX = event.getX();
                mTouchDownY = event.getY();
                mTouchDownRawY = event.getRawY();
                isLongPress = false;
                isVibrator = false;
                isLongPressTouchActionUp = false;
                break;
            case ACTION_MOVE:
                Log.d(TAG, "ACTION_MOVE");
                // 先判断是否禁用了ActionMenu功能，以及ActionMenu是否创建失败，
                // 二者只要满足了一个条件，退出长按事件
                if (isShowActionMenu || mActionMenu.getChildCount() == 0) {
                    // 手指移动过程中的字符偏移
                    currentLine = getCurrentTouchLine(event.getX(), isLeftToRight);
                    float mWordOffset_move = event.getY();
                    // 判断是否触发长按事件 判断条件为：
                    // 1.超过时间阈值；2.且超过手指移动的最小位移阈值；3.且未超过边界，在padding内
                    boolean isTriggerTime = event.getEventTime() - event.getDownTime() >= TRIGGER_LONGPRESS_TIME_THRESHOLD;
                    boolean isTriggerDistance = Math.abs(event.getX() - mTouchDownX) < TRIGGER_LONGPRESS_DISTANCE_THRESHOLD
                            && Math.abs(event.getY() - mTouchDownY) < TRIGGER_LONGPRESS_DISTANCE_THRESHOLD;
                    int[] drawPadding = getDrawPadding(isLeftToRight);
                    boolean isInBound = event.getX() >= drawPadding[0] && event.getX() <= getWidth() - drawPadding[2]
                            && event.getY() >= drawPadding[1] && event.getY() <= getHeight() - drawPadding[3];
                    if (isTriggerTime && isTriggerDistance && isInBound) {
                        Log.d(TAG, "ACTION_MOVE 长按");
                        isLongPress = true;
                        isLongPressTouchActionUp = false;
                        mStartLine = currentLine;
                        mStartTextOffset = mWordOffset_move;
                        // 每次触发长按时，震动提示一次
                        if (!isVibrator) {
                            mVibrator.vibrate(60);
                            isVibrator = true;
                        }
                    }
                    if (isLongPress) {
                        mCurrentLine = currentLine;
                        mCurrentTextOffset = mWordOffset_move;
                        // 通知父布局不要拦截触摸事件
                        getParent().requestDisallowInterceptTouchEvent(true);
                        // 通知view绘制所选文字背景色
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP");
                // 处理长按事件
                if (isLongPress) {
                    currentLine = getCurrentTouchLine(event.getX(), isLeftToRight);
                    float mWordOffsetEnd = event.getY();
                    mCurrentLine = currentLine;
                    mCurrentTextOffset = mWordOffsetEnd;
                    // 手指抬起后选择文字
                    selectText(mStartTextOffset, mCurrentTextOffset, mStartLine, mCurrentLine, mCharSpacingExtra, isLeftToRight);
                    if (!TextUtils.isEmpty(mSelectedText)) {
                        // 计算菜单显示位置
                        int mPopWindowOffsetY = calculatorActionMenuYPosition((int) mTouchDownRawY, (int) event.getRawY());
                        // 弹出菜单
                        showActionMenu(mPopWindowOffsetY, mActionMenu);
                    }
                    isLongPressTouchActionUp = true;
                    isLongPress = false;

                } else if (event.getEventTime() - event.getDownTime() < TRIGGER_LONGPRESS_TIME_THRESHOLD) {
                    // 由于onTouchEvent最终返回了true,onClick事件会被屏蔽掉，因此在这里处理onClick事件
                    if (null != mOnClickListener)
                        mOnClickListener.onClick(this);
                }
                // 通知父布局继续拦截触摸事件
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return true;
    }

    /**
     * 计算触摸位置所在行,最小值为1
     *
     * @param offsetX
     * @param isLeftToRight
     * @return
     */
    private int getCurrentTouchLine(float offsetX, boolean isLeftToRight) {

        int currentLine = 1;
        float lineWidth = getTextSize() + mLineSpacingExtra;
        int[] drawPadding = getDrawPadding(isLeftToRight);
        if (isLeftToRight) {
            // 边界控制
            if (offsetX >= getWidth() - drawPadding[2])
                currentLine = mMaxTextLine;
            else
                currentLine = (int) Math.ceil((offsetX - drawPadding[0]) / lineWidth);
        } else {
            if (offsetX <= drawPadding[0])
                currentLine = mMaxTextLine;
            else
                currentLine = (int) Math.ceil((getWidth() - offsetX - drawPadding[2]) / lineWidth);
        }

        currentLine = currentLine <= 0 ? 1 : (currentLine > mMaxTextLine ? mMaxTextLine : currentLine);

        Log.d(TAG, "touch line is: " + currentLine);
        return currentLine;
    }

    /**
     * 选择选中的字符
     *
     * @param startOffsetY
     * @param endOffsetY
     * @param startLine
     * @param endLine
     * @param charSpacingExtra
     */
    private void selectText(float startOffsetY, float endOffsetY,
                            int startLine, int endLine, float charSpacingExtra, boolean isLeftToRight) {
        // 计算开始和结束的字符index
        int index_start = getSelectTextIndex(startOffsetY, startLine, charSpacingExtra, isLeftToRight);
        int index_end = getSelectTextIndex(endOffsetY, endLine, charSpacingExtra, isLeftToRight);
        if (index_start == index_end)
            mSelectedText = "";
        else {
            String textAll = getText().toString();
            if (TextUtils.isEmpty(textAll))
                mSelectedText = "";
            else
                mSelectedText = textAll.substring(Math.min(index_start, index_end),
                        Math.max(index_start, index_end));
        }
        Log.d(TAG, "mSelectedText  " + mSelectedText);
    }

    /**
     * 计算所选文字起始或结束字符对应的index
     *
     * @param offsetY
     * @param targetLine
     * @param charSpacingExtra 字符间距
     */
    private int getSelectTextIndex(float offsetY, int targetLine, float charSpacingExtra, boolean isLeftToRight) {
        int[] drawPadding = getDrawPadding(isLeftToRight);
        // 该行文字的起始和结束位置
        int[] lineIndex = mLinesTextIndex.get(targetLine);
        // 目标位置
        int targetIndex = lineIndex[1];
        float tempY = drawPadding[1];
        // 边界控制
        if (offsetY < drawPadding[1]) {
            return lineIndex[0];
        } else if (offsetY > getHeight() - drawPadding[3]) {
            return lineIndex[1];
        }
        /*
         * 循环累加每一个字符的高度，一直到tempY > offsetY 时停止，然后根据行首或行末计算index；
         * 如果循环完成后依然未触发tempY >= offsetY条件，返回该行的最后一个字符index，即lineIndex[1];
        */
        for (int i = lineIndex[0]; i < lineIndex[1]; i++) {
            String char_i = String.valueOf(getText().toString().charAt(i));
            // 区别换行符，标点符号 和 文字
            if (char_i.equals("\n")) {
                tempY = drawPadding[1];
            } else if (isUnicodeSymbol(char_i)) {
                tempY += 1.4f * getCharHeight(char_i, getTextPaint()) + charSpacingExtra;
            } else {
                tempY += getTextSize() + charSpacingExtra;
            }
            // 触发停止的条件
            if (tempY >= offsetY) {
                targetIndex = i;
                break;
            }
        }
        Log.d(TAG, "target index  " + targetIndex);
        return targetIndex;
    }

    /**
     * 计算弹出菜单相对于父布局的Y向偏移
     *
     * @param yOffsetStart 所选字符的起始位置相对屏幕的Y向偏移
     * @param yOffsetEnd   所选字符的结束位置相对屏幕的Y向偏移
     * @return
     */
    private int calculatorActionMenuYPosition(int yOffsetStart, int yOffsetEnd) {
        if (yOffsetStart > yOffsetEnd) {
            int temp = yOffsetStart;
            yOffsetStart = yOffsetEnd;
            yOffsetEnd = temp;
        }
        int actionMenuOffsetY;

        if (yOffsetStart < mActionMenuHeight * 3 / 2 + mStatusBarHeight) {
            if (yOffsetEnd > mScreenHeight - mActionMenuHeight * 3 / 2) {
                // 菜单显示在屏幕中间
                actionMenuOffsetY = mScreenHeight / 2 - mActionMenuHeight / 2;
            } else {
                // 菜单显示所选文字下方
                actionMenuOffsetY = yOffsetEnd + mActionMenuHeight / 2;
            }
        } else {
            // 菜单显示所选文字上方
            actionMenuOffsetY = yOffsetStart - mActionMenuHeight * 3 / 2;
        }
        return actionMenuOffsetY;
    }

    /* ***************************************************************************************** */
    // 创建ActionMenu部分

    /**
     * 创建ActionMenu菜单
     *
     * @return
     */
    private ActionMenu createActionMenu() {
        // 创建菜单
        ActionMenu actionMenu = new ActionMenu(mContext);
        // 是否需要移除默认item
        boolean isRemoveDefaultItem = false;
        if (null != mCustomActionMenuCallBack) {
            isRemoveDefaultItem = mCustomActionMenuCallBack.onCreateCustomActionMenu(actionMenu);
        }
        if (!isRemoveDefaultItem)
            actionMenu.addDefaultMenuItem(); // 添加默认item

        actionMenu.addCustomItem();  // 添加自定义item
        actionMenu.setFocusable(true); // 获取焦点
        actionMenu.setFocusableInTouchMode(true);

        if (actionMenu.getChildCount() != 0) {
            // item监听
            for (int i = 0; i < actionMenu.getChildCount(); i++) {
                actionMenu.getChildAt(i).setOnClickListener(mMenuClickListener);
            }
        }
        return actionMenu;
    }

    /**
     * 长按弹出菜单
     *
     * @param offsetY
     * @param actionMenu
     * @return 菜单创建成功，返回true
     */
    private void showActionMenu(int offsetY, ActionMenu actionMenu) {

        mActionMenuPopupWindow = new PopupWindow(actionMenu, WindowManager.LayoutParams.WRAP_CONTENT,
                mActionMenuHeight, true);
        mActionMenuPopupWindow.setFocusable(true);
        mActionMenuPopupWindow.setOutsideTouchable(false);
        mActionMenuPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mActionMenuPopupWindow.showAtLocation(this, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, offsetY);

        mActionMenuPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // 清理已选的文字
                clearSelectedTextBackground();
            }
        });
    }

    /**
     * 隐藏菜单
     */
    private void hideActionMenu() {
        if (null != mActionMenuPopupWindow) {
            mActionMenuPopupWindow.dismiss();
            mActionMenuPopupWindow = null;
        }
    }

    /**
     * 菜单点击事件监听
     */
    private OnClickListener mMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            String menuItemTitle = (String) v.getTag();

            if (menuItemTitle.equals(ActionMenu.DEFAULT_MENU_ITEM_TITLE_SELECT_ALL)) {
                //全选事件
                mSelectedText = getText().toString();
                int[] drawPadding = getDrawPadding(isLeftToRight);
                mStartLine = 1;
                mCurrentLine = mMaxTextLine;
                mStartTextOffset = drawPadding[1];
                mCurrentTextOffset = getHeight() - drawPadding[3];
                isLongPressTouchActionUp = true;
                invalidate();

            } else if (menuItemTitle.equals(ActionMenu.DEFAULT_MENU_ITEM_TITLE_COPY)) {
                // 复制事件
                Utils.copyText(mContext, mSelectedText);
                Toast.makeText(mContext, "复制成功！", Toast.LENGTH_SHORT).show();
                hideActionMenu();

            } else {
                // 自定义事件
                if (null != mCustomActionMenuCallBack) {
                    mCustomActionMenuCallBack.onCustomActionItemClicked(menuItemTitle, mSelectedText);
                }
                hideActionMenu();
            }
        }
    };

    /* ***************************************************************************************** */
    // 绘制部分

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制竖排文字
        drawVerticalText(canvas, mLineSpacingExtra, mCharSpacingExtra, isLeftToRight);
        // 绘制下划线
        drawTextUnderline(canvas, isLeftToRight, mUnderLineOffset, mCharSpacingExtra);
        // 绘制选中文字的背景，触发条件：
        // 1.长按事件 2.全选事件 3.手指滑动过快时，进入ACTION_UP事件后，可能会出现背景未绘制的情况
        if (isLongPress | isActionSelectAll | isLongPressTouchActionUp) {
            drawSelectedTextBackground(canvas, mStartLine, mCurrentLine,
                    mStartTextOffset, mCurrentTextOffset, mLineSpacingExtra, mCharSpacingExtra, isLeftToRight);
            isActionSelectAll = false;
            isLongPressTouchActionUp = false;
        }
    }

    /**
     * 绘制竖排文字
     *
     * @param canvas
     * @param lineSpacingExtra 行距
     * @param charSpacingExtra 字符间距
     * @param isLeftToRight    文字方向
     */
    private void drawVerticalText(Canvas canvas, float lineSpacingExtra,
                                  float charSpacingExtra, boolean isLeftToRight) {
        // 文字画笔
        TextPaint textPaint = getTextPaint();
        int textStrLength = getText().length();
        if (textStrLength == 0)
            return;
        // 每次绘制时初始化参数
        mMaxTextLine = 1;
        int currentLineStartIndex = 0; // 行首位置标记
        mLinesOffsetArray.clear();
        mLinesTextIndex.clear();
        int[] drawPadding = getDrawPadding(isLeftToRight); // 绘制文字的padding
        // 当前竖行的XY向偏移初始值
        float currentLineOffsetX = isLeftToRight ?
                drawPadding[0] : getWidth() - drawPadding[2] - getTextSize();
        float currentLineOffsetY = drawPadding[1] + getTextSize();
        for (int j = 0; j < textStrLength; j++) {
            String char_j = String.valueOf(getText().charAt(j));
            /* 换行条件为：
             * 1：遇到换行符；
             * 2：该竖行是否已经写满。
             *
             * 该竖行是否已经写满，判定条件为：
             * 1.y向剩余的空间已经不够填下一个文字；
             * 2.且当前要绘制的文字不是标点符号；
             * 3.或当前要绘制的文字是标点符号，但标点符号的高度大于y向剩余的空间
             * 注意：文字是从左下角开始向上绘制的
            */
            boolean isLineBreaks = char_j.equals("\n");
            boolean isCurrentLineFinish = currentLineOffsetY > getHeight() - drawPadding[3]
                    && (!isUnicodeSymbol(char_j) || (isUnicodeSymbol(char_j) &&
                    currentLineOffsetY + getCharHeight(char_j, textPaint) > getHeight() - drawPadding[3] + getTextSize()));

            if (isLineBreaks || isCurrentLineFinish) {
                // 记录记录偏移量,和行首行末字符的index；然后另起一行，
                mLinesOffsetArray.put(mMaxTextLine, new Float[]{currentLineOffsetX, currentLineOffsetY});
                mLinesTextIndex.put(mMaxTextLine, new int[]{currentLineStartIndex, j});
                // 另起一竖行，更新偏移量
                currentLineOffsetX = isLeftToRight ?
                        currentLineOffsetX + getTextSize() + lineSpacingExtra
                        : currentLineOffsetX - getTextSize() - lineSpacingExtra;
                currentLineOffsetY = drawPadding[1] + getTextSize();
                mMaxTextLine++;
            }
            // 判断是否是行首，记录行首字符位置；
            // 判断行首的条件为：currentLineOffsetY == drawPadding[1]+getTextSize()
            if (currentLineOffsetY == drawPadding[1] + getTextSize()) {
                currentLineStartIndex = j;
            }

            // 绘制第j个字符.
            if (isLineBreaks) {
                // 如果是换行符，do nothing
                //char_j = "";
                //canvas.drawText(char_j, currentLineOffsetX, currentLineOffsetY, textPaint);
            } else if (isUnicodeSymbol(char_j)) {
                // 如果是Y向需要补偿标点符号，加一个补偿 getTextSize() - getCharHeight.
                // 注意：如果该竖行第一个字符是标点符号的话，不加补偿;
                // 判断是否是第一个字符的条件为：offsetY == drawPadding[1] + getTextSize()
                float drawOffsetY = currentLineOffsetY;
                if (isSymbolNeedOffset(char_j))
                    drawOffsetY = drawOffsetY - (getTextSize() - 1.4f * getCharHeight(char_j, textPaint));
                // 文字从左向右，标点符号靠右绘制，竖排标点除外
                float drawOffsetX = currentLineOffsetX;
                if (isLeftToRight && !isVerticalSymbol(char_j))
                    drawOffsetX = drawOffsetX + getTextSize() / 2;

                canvas.drawText(char_j, drawOffsetX, drawOffsetY, textPaint);
                currentLineOffsetY += 1.4f * getCharHeight(char_j, textPaint) + charSpacingExtra;

            } else {
                canvas.drawText(char_j, currentLineOffsetX, currentLineOffsetY, textPaint);
                currentLineOffsetY += getTextSize() + charSpacingExtra;
            }

            // 最后一行的偏移量和行首行末字符的index；
            if (j == textStrLength - 1) {
                mLinesOffsetArray.put(mMaxTextLine, new Float[]{currentLineOffsetX, currentLineOffsetY});
                mLinesTextIndex.put(mMaxTextLine, new int[]{currentLineStartIndex, textStrLength});
            }
        }
        Log.d(TAG, "mMaxTextLine is : " + mMaxTextLine);
    }

    /**
     * 绘制下划线
     *
     * @param canvas
     * @param isLeftToRight    文字方向
     * @param underLineOffset  下划线偏移量 >0
     * @param charSpacingExtra
     */
    private void drawTextUnderline(Canvas canvas, boolean isLeftToRight, float underLineOffset,
                                   float charSpacingExtra) {

        if (!isUnderLineText || mUnderLineWidth == 0)
            return;

        // 下划线paint
        Paint underLinePaint = getPaint();
        underLinePaint.setColor(mUnderLineColor);
        underLinePaint.setAntiAlias(true);
        underLinePaint.setStyle(Paint.Style.FILL);
        underLinePaint.setStrokeWidth(mUnderLineWidth);

        int[] drawPadding = getDrawPadding(isLeftToRight); // 绘制文字的padding

        for (int i = 0; i < mMaxTextLine; i++) {
            // Y向开始和结束位置
            float yStart = drawPadding[1];
            float yEnd = mLinesOffsetArray.get(i + 1)[1] - getTextSize();
            // 如果end <= start 或者 该行字符为换行符，则不绘制下划线
            int[] lineIndex = mLinesTextIndex.get(i + 1);
            String lineText = getText().toString().substring(lineIndex[0], lineIndex[1]);
            if (yEnd <= yStart || (lineText.equals("\n")))
                continue;
            // Y向边界处理
            if (yEnd > getHeight() - drawPadding[3] - getTextSize())
                yEnd = getHeight() - drawPadding[3];
            // 首行缩进处理
            int spaceNum = getLineStartSpaceNumber(lineText);
            if (spaceNum > 0) {
                yStart = yStart + (getTextSize() + charSpacingExtra) * spaceNum;
            }

            // X向；注意不同的文字方向和下划线偏移
            float xStart = mLinesOffsetArray.get(i + 1)[0];
            if (isLeftToRight)
                xStart += getTextSize() + underLineOffset;
            else
                xStart -= underLineOffset;
            float xEnd = xStart;

            canvas.drawLine(xStart, yStart, xEnd, yEnd, underLinePaint);
        }
    }

    /**
     * 绘制所选文字高亮色
     *
     * @param canvas
     * @param startLine        开始行
     * @param endLine          结束行
     * @param startOffsetY     开始字符Y向偏移
     * @param endOffsetY       结束文字Y向偏移
     * @param lineSpacingExtra
     * @param charSpacingExtra
     * @param isLeftToRight
     */
    private void drawSelectedTextBackground(Canvas canvas, int startLine, int endLine,
                                            float startOffsetY, float endOffsetY,
                                            float lineSpacingExtra, float charSpacingExtra,
                                            boolean isLeftToRight) {

        if (startLine == endLine && Math.abs(endOffsetY - startOffsetY) == 0) {
            return;
        }
        // 文字背景高亮画笔
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setColor(mTextHighlightColor);
        highlightPaint.setAlpha(60);

        int[] drawPadding = getDrawPadding(isLeftToRight); // 绘制文字的padding

        // 预处理，如果startLine > endLine，交换二者
        if (startLine > endLine) {
            startLine = startLine + endLine;
            endLine = startLine - endLine;
            startLine = startLine - endLine;
            startOffsetY = startOffsetY + endOffsetY;
            endOffsetY = startOffsetY - endOffsetY;
            startOffsetY = startOffsetY - endOffsetY;
        }
        // 行宽
        int lineWidth = (int) (getTextSize() + lineSpacingExtra);
        // 开始行和结束行所选文字的y向偏移量
        int startLineOffsetY = getSelectTextPreciseOffsetY(startOffsetY, startLine, charSpacingExtra, true, isLeftToRight);
        int endLineOffsetY = getSelectTextPreciseOffsetY(endOffsetY, endLine, charSpacingExtra, false, isLeftToRight);
        // 围绕所选的文字创建一个Path闭合路径，一共八个点
        Path path_all = new Path();
        if (isLeftToRight) {
            // 往左偏移半个行距
            int offsetLeftPadding = (int) (drawPadding[0] - lineSpacingExtra / 2);
            path_all.moveTo(offsetLeftPadding + (startLine - 1) * lineWidth, startLineOffsetY);
            path_all.lineTo(offsetLeftPadding + startLine * lineWidth, startLineOffsetY);
            path_all.lineTo(offsetLeftPadding + startLine * lineWidth, drawPadding[1]);
            path_all.lineTo(offsetLeftPadding + endLine * lineWidth, drawPadding[1]);
            path_all.lineTo(offsetLeftPadding + endLine * lineWidth, endLineOffsetY);
            path_all.lineTo(offsetLeftPadding + (endLine - 1) * lineWidth, endLineOffsetY);
            path_all.lineTo(offsetLeftPadding + (endLine - 1) * lineWidth, getHeight() - drawPadding[3] + charSpacingExtra);
            path_all.lineTo(offsetLeftPadding + (startLine - 1) * lineWidth, getHeight() - drawPadding[3] + charSpacingExtra);
            path_all.close();
        } else {
            // 往右偏移半个行距
            int offsetRightPadding = (int) (getWidth() - drawPadding[2] + lineSpacingExtra / 2);
            path_all.moveTo(offsetRightPadding - (startLine - 1) * lineWidth, startLineOffsetY);
            path_all.lineTo(offsetRightPadding - startLine * lineWidth, startLineOffsetY);
            path_all.lineTo(offsetRightPadding - startLine * lineWidth, drawPadding[1]);
            path_all.lineTo(offsetRightPadding - endLine * lineWidth, drawPadding[1]);
            path_all.lineTo(offsetRightPadding - endLine * lineWidth, endLineOffsetY);
            path_all.lineTo(offsetRightPadding - (endLine - 1) * lineWidth, endLineOffsetY);
            path_all.lineTo(offsetRightPadding - (endLine - 1) * lineWidth, getHeight() - drawPadding[3] + charSpacingExtra);
            path_all.lineTo(offsetRightPadding - (startLine - 1) * lineWidth, getHeight() - drawPadding[3] + charSpacingExtra);
            path_all.close();
        }
        canvas.drawPath(path_all, highlightPaint);
        canvas.save();
        canvas.restore();
    }

    /**
     * 根据文本的Gravity计算文字绘制时的padding
     *
     * @param isLeftToRight 文字阅读方向
     * @return [left, top, right, bottom]
     */
    private int[] getDrawPadding(boolean isLeftToRight) {
        int textBoundWidth = mTextAreaRoughBound[0];
        int textBoundHeight = mTextAreaRoughBound[1];
        int left, right, top, bottom;
        int gravity;

        if (textBoundWidth < getWidth()) {
            // 先把水平方向的gravity解析出来
            gravity = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (gravity == Gravity.CENTER || gravity == Gravity.CENTER_HORIZONTAL) {
                left = getPaddingLeft() + (getWidth() - textBoundWidth) / 2;
                right = getPaddingRight() + (getWidth() - textBoundWidth) / 2;
            } else if (gravity == Gravity.RIGHT && isLeftToRight) {
                left = getPaddingLeft() + getWidth() - textBoundWidth;
                right = getPaddingRight();
            } else if (gravity == Gravity.LEFT && !isLeftToRight) {
                left = getPaddingLeft();
                right = getPaddingRight() + getWidth() - textBoundWidth;
            } else {
                left = isLeftToRight ? getPaddingLeft() : getPaddingLeft() + getWidth() - textBoundWidth;
                right = isLeftToRight ? getPaddingRight() + getWidth() - textBoundWidth : getPaddingRight();
            }
        } else {
            left = getPaddingLeft();
            right = getPaddingRight();
        }

        if (textBoundHeight < getHeight()) {
            // 先把垂直方向的gravity解析出来
            gravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            if (gravity == Gravity.CENTER || gravity == Gravity.CENTER_VERTICAL) {
                top = getPaddingTop() + (getHeight() - textBoundHeight) / 2;
                bottom = getPaddingBottom() + (getHeight() - textBoundHeight) / 2;
            } else if (gravity == Gravity.BOTTOM) {
                top = getPaddingTop() + getHeight() - textBoundHeight;
                bottom = getPaddingBottom();
            } else {
                top = getPaddingTop();
                bottom = getPaddingBottom() + getHeight() - textBoundHeight;
            }
        } else {
            top = getPaddingTop();
            bottom = getPaddingBottom();
        }

        return new int[]{left, top, right, bottom};
    }

    /**
     * 获取所选文字的精确Y向偏移
     *
     * @param offsetY
     * @param targetLine
     * @param charSpacingExtra
     * @return
     */
    private int getSelectTextPreciseOffsetY(float offsetY, int targetLine, float charSpacingExtra,
                                            boolean isStart, boolean isLeftToRight) {

        int[] drawPadding = getDrawPadding(isLeftToRight); // 绘制文字的padding
        // 该行文字的起始和结束位置
        int[] lineIndex = mLinesTextIndex.get(targetLine);
        // 目标位置
        int targetOffset = drawPadding[1];
        int tempY = drawPadding[1];
        // 边界控制
        if (offsetY < drawPadding[1]) {
            return drawPadding[1];
        } else if (offsetY > getHeight() - drawPadding[3]) {
            return getHeight() - drawPadding[3];
        }
        /*
         * 循环累加每一个字符的高度，一直到tempY > offsetY 时停止，然后根据行首或行末计算精确的偏移量；
         * 如果循环完成后依然未触发tempY >= offsetY条件，返回该行的最大长度;
        */
        for (int i = lineIndex[0]; i < lineIndex[1]; i++) {
            String char_i = String.valueOf(getText().toString().charAt(i));
            // 区别换行符，标点符号 和 文字
            if (char_i.equals("\n")) {
                tempY = drawPadding[1];
            } else if (isUnicodeSymbol(char_i)) {
                tempY += 1.4f * getCharHeight(char_i, getTextPaint()) + charSpacingExtra;
            } else {
                tempY += getTextSize() + charSpacingExtra;
            }
            if (tempY <= offsetY) {
                targetOffset = tempY;
            }
            // 触发暂停条件
            if (tempY > offsetY) {
                break;
            }
        }
        return Math.max(targetOffset, drawPadding[1]);
    }

    /**
     * 清除所选文字的背景
     */
    private void clearSelectedTextBackground() {
        mSelectedText = "";
        mStartLine = mCurrentLine = 0;
        mStartTextOffset = mCurrentTextOffset = 0;
        invalidate();
    }

    /**
     * 文字画笔
     *
     * @return
     */
    private TextPaint getTextPaint() {
        // 文字画笔
        TextPaint textPaint = getPaint();
        textPaint.setColor(getCurrentTextColor());
        return textPaint;
    }

    /**
     * 计算首行缩进的空格数
     *
     * @param lineText
     * @return
     */
    private int getLineStartSpaceNumber(String lineText) {
        if (lineText.startsWith("    ")) {
            return 4;
        } else if (lineText.startsWith("　　　") || lineText.startsWith("   ")) {
            return 3;
        } else if (lineText.startsWith("　　") || lineText.startsWith("  ")) {
            return 2;
        } else if (lineText.startsWith("　") || lineText.startsWith(" ")) {
            return 1;
        } else
            return 0;
    }

    /**
     * 获取一个字符的高度
     *
     * @param target_char
     * @param paint
     * @return
     */
    private float getCharHeight(String target_char, Paint paint) {
        Rect rect = new Rect();
        paint.getTextBounds(target_char, 0, 1, rect);
        return rect.height();
    }

    /**
     * 获取一个字符的宽度
     *
     * @param target_char
     * @param paint
     * @return
     */
    private float getCharWidth(String target_char, Paint paint) {
        Rect rect = new Rect();
        paint.getTextBounds(target_char, 0, 1, rect);
        return rect.width();
    }

    /**
     * 判断是否是标点符号
     * - - —— = + ~ 这几个不做判断
     *
     * @param str
     * @return
     */
    private boolean isUnicodeSymbol(String str) {
        String regex = ".*[_\"`!@#$%^&*()|{}':;,\\[\\].<>/?！￥…（）【】‘’；：”“。，、？︵ ︷︿︹︽﹁﹃︻︶︸﹀︺︾ˉ﹂﹄︼]$+.*";
        Matcher m = Pattern.compile(regex).matcher(str);
        return m.matches();
    }

    /**
     * 需要补偿的标点符号
     * - - —— = + ~ 这几个不做补偿
     *
     * @param str
     * @return
     */
    private boolean isSymbolNeedOffset(String str) {
        String regex = ".*[_!@#$%&()|{}:;,\\[\\].<>/?！￥…（）【】；：。，、？︵ ︷︿︹︽﹁﹃︻]$+.*";
        Matcher m = Pattern.compile(regex).matcher(str);
        return m.matches();
    }

    /**
     * 是否是竖排标点符号
     *
     * @param str
     * @return
     */
    private boolean isVerticalSymbol(String str) {
        String regex = ".*[︵ ︷︿︹︽﹁﹃︻︶︸﹀︺︾ˉ﹂﹄︼|]$+.*";
        Matcher m = Pattern.compile(regex).matcher(str);
        return m.matches();
    }

}
