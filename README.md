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
            app:forbiddenActionMenu="false"
            app:textJustify="false" />
``` 
