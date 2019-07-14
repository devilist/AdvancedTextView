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

package com.devilist.readview.core;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ReadView extends View {

    private ReadManager mManager;

    private TextEditor mEditor;

    public ReadView(Context context) {
        super(context);
    }

    public ReadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void loadText(String text) {
        mManager.loadText(text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mEditor.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mEditor.onDraw(canvas);
    }
}
