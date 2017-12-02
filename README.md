# AdvancedTextView

这是一个增强的TextView库。可以实现文字的两端对齐，文字竖排，以及自定义的弹出菜单。

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

也可以在代码中引用：

```
        selectableTextView.setTextJustify(true);                  // 是否启用两端对齐 默认启用 
        selectableTextView.setForbiddenActionMenu(false);         // 是否禁用自定义ActionMenu 默认启用
        selectableTextView.setTextHighlightColor(0xff48543e);     // 文本高亮色
```

设置ActionMenu监听：

```
selectableTextView.setCustomActionMenuCallBack(new CustomActionMenuCallBack() {
    @Override
        public boolean onCreateCustomActionMenu(ActionMenu menu) {
            menu.setActionMenuBgColor(0xff666666);  // ActionMenu背景色
            menu.setMenuItemTextColor(0xffffffff);  // ActionMenu文字颜色
            List<String> titleList = new ArrayList<>();
            titleList.add("翻译");
            titleList.add("分享");
            titleList.add("分享");
            menu.addCustomMenuItem(titleList);  // 添加菜单
            return false; // 返回false，保留默认菜单(全选/复制)；返回true，移除默认菜单
     }
     
    @Override
        public void onCustomActionItemClicked(String itemTitle, String selectedContent) {
            Toast.makeText(this, "ActionMenu: " + itemTitle, Toast.LENGTH_SHORT).show();
     }
});
```
