# AdvancedTextView

这是一个增强的TextView库。可以实现文字的两端对齐，文字竖排，以及自定义的弹出菜单。

具体介绍请移步博客：

https://blog.csdn.net/devilist/article/details/54911641

https://blog.csdn.net/devilist/article/details/79236665

本库目前提供两个控件 SelectableTextView 和 VerticalTexview。

# 1. SelectableTextView

![image](https://github.com/devilist/AdvancedTextView/raw/master/images/selectabletextview.gif)

在布局中引用：

``` 
 <com.devilist.advancedtextview.SelectableTextView
      android:id="@+id/ctv_content"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_margin="10dp"
      android:background="#FDFBF8"
      android:lineSpacingMultiplier="1.5"
      android:padding="5dp"
      android:textColor="#808080"
      android:textSize="16sp"
      app:forbiddenActionMenu="false"                     // 是否禁用自定义ActionMenu
      app:textHeightColor="@color/colorAccent"            // 文本高亮色
      app:textJustify="false" />                          // 是否启用两端对齐
``` 

也可以在代码中设置：

```
        selectableTextView.setTextJustify(true);                  // 是否启用两端对齐 默认启用 
        selectableTextView.setForbiddenActionMenu(false);         // 是否禁用自定义ActionMenu 默认启用
        selectableTextView.setTextHighlightColor(0xff48543e);     // 文本高亮色
```
注意：在代码中调用上述三个方法后需要 调用 inviladite() 或 postInviladite()方法通知View重绘

设置ActionMenu菜单点击监听：

```
selectableTextView.setCustomActionMenuCallBack(new CustomActionMenuCallBack() {
    @Override
        public boolean onCreateCustomActionMenu(ActionMenu menu) {
            menu.setActionMenuBgColor(0xff666666);                    // ActionMenu背景色
            menu.setMenuItemTextColor(0xffffffff);                   // ActionMenu文字颜色
            List<String> titleList = new ArrayList<>();
            titleList.add("翻译");
            titleList.add("分享");
            titleList.add("分享");
            menu.addCustomMenuItem(titleList);                       // 添加菜单
            return false;                                            // 返回false，保留默认菜单(全选/复制)；返回true，移除默认菜单
     }
     
    @Override
        public void onCustomActionItemClicked(String itemTitle, String selectedContent) {
            Toast.makeText(this, "ActionMenu: " + itemTitle, Toast.LENGTH_SHORT).show();
     }
});
```

# 2. VerticalTextView

![image](https://github.com/devilist/AdvancedTextView/raw/master/images/verticaltextview.gif)

在布局中引用：

```
<com.devilist.advancedtextview.VerticalTextView
            android:id="@+id/vtv_text_ltr"
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:background="#FDFBF8"
            android:gravity="center"
            android:padding="15dp"
            android:textColor="#808080"
            android:textSize="16sp"
            app:charSpacingExtra="2dp"                              // 字符间距
            app:lineSpacingExtra="15dp"                             // 行间距
            app:showActionMenu="true"                               // 是否开启ActionMenu，默认关闭
            app:textLeftToRight="true"                              // 文字是否从左向右排版，默认从右向左排版
            app:underLineText="true"                                // 是否显示下划线，默认不显示
            app:underLineColor="#CEAD53"                            // 下划线颜色
            app:underLineWidth="2.5"                                // 下划线线宽
            app:textHeightLightColor="@color/colorAccent"           // 选中文字高亮色
            app:underlineOffset="3dp" />                            // 下划线偏移量
```

在代码中设置：

```
    vtv_text_ltr.setLeftToRight(true)                  // 文字是否从左向右排版，默认从右向左排版
                .setLineSpacingExtra(10)               // 行间距
                .setCharSpacingExtra(2)                // 字符间距
                .setUnderLineText(true)                // 是否显示下划线，默认不显示
                .setShowActionMenu(true)               // 是否开启ActionMenu，默认关闭
                .setUnderLineColor(0xffCEAD53)         // 下划线颜色
                .setUnderLineWidth(1.0f)               // 下划线线宽
                .setUnderLineOffset(3)                 // 下划线偏移量
                .setTextHighlightColor(0xffCEAD53)     // 选中文字高亮色
                .setCustomActionMenuCallBack(this);    // ActionMenu菜单点击监听
```
注意：在代码中调用上述方法后需要 调用 requestLayout()方法通知View重新布局

设置ActionMenu菜单点击监听和SelectableTextView一样。


