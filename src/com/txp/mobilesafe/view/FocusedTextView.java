package com.txp.mobilesafe.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class FocusedTextView extends TextView {

	/**
	 * 获取焦点的TextView
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	
	//有style样式的话，会走此方法
	public FocusedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	//有属性时会走此方法
	public FocusedTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	//用代码new对象时，走此方法
	public FocusedTextView(Context context) {
		super(context);
	}

	/**
	 * 表示有没有焦点
	 * 
	 * 跑马灯要运行，首先会调用此方法，检测当前TextView是否有焦点
	 * 所以我们强制设为有焦点
	 */
	@Override
	public boolean isFocused() {
		//强制设为有焦点
		return true;
	}
}
