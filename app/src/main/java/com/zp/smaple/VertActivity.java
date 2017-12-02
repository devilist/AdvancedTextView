package com.zp.smaple;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.devilist.advancedtextview.ActionMenu;
import com.devilist.advancedtextview.CustomActionMenuCallBack;
import com.devilist.advancedtextview.VerticalTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zengp on 2017/12/2.
 */

public class VertActivity extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener, CustomActionMenuCallBack {

    private RadioGroup rg_text_orient, rg_text_underline;

    private VerticalTextView vtv_text_ltr;
    private HorizontalScrollView scroll_rtl;

    public static void start(Context context) {
        Intent starter = new Intent(context, VertActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vert);
        init();
    }

    private void init() {
        scroll_rtl = (HorizontalScrollView) findViewById(R.id.scroll_rtl);
        vtv_text_ltr = (VerticalTextView) findViewById(R.id.vtv_text_ltr);
        vtv_text_ltr.setText(Html.fromHtml(StringContentUtil.str_cbf).toString());

        vtv_text_ltr.setLeftToRight(true)
                .setLineSpacingExtra(10)
                .setCharSpacingExtra(2)
                .setUnderLineText(true)
                .setShowActionMenu(true)
                .setUnderLineColor(0xffCEAD53)
                .setUnderLineWidth(1.0f)
                .setUnderLineOffset(3)
                .setTextHighlightColor(0xffCEAD53)
                .setCustomActionMenuCallBack(this);

        vtv_text_ltr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(VertActivity.this, "onClick事件", Toast.LENGTH_SHORT).show();
            }
        });

        rg_text_orient = findViewById(R.id.rg_text_orient);
        rg_text_underline = findViewById(R.id.rg_text_underline);
        rg_text_orient.setOnCheckedChangeListener(this);
        rg_text_underline.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_ltr:
                vtv_text_ltr.setLeftToRight(true);
                vtv_text_ltr.requestLayout();
                scroll_rtl.fullScroll(View.FOCUS_LEFT);
                break;
            case R.id.rb_rtl:
                vtv_text_ltr.setLeftToRight(false);
                vtv_text_ltr.requestLayout();
                scroll_rtl.fullScroll(View.FOCUS_RIGHT);
                break;
            case R.id.rb_show:
                vtv_text_ltr.setUnderLineText(true);
                vtv_text_ltr.requestLayout();
                break;
            case R.id.rb_hidden:
                vtv_text_ltr.setUnderLineText(false);
                vtv_text_ltr.requestLayout();
                break;
        }
    }

    @Override
    public boolean onCreateCustomActionMenu(ActionMenu menu) {
        List<String> titleList = new ArrayList<>();
        titleList.add("翻译");
        titleList.add("分享");
        menu.addCustomMenuItem(titleList);
        return false;
    }

    @Override
    public void onCustomActionItemClicked(String itemTitle, String selectedContent) {
        Toast.makeText(this, "ActionMenu: " + itemTitle, Toast.LENGTH_SHORT).show();
    }
}
