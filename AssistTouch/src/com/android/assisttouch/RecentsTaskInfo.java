package com.android.assisttouch;

import android.content.Intent;
import android.graphics.drawable.Drawable;

/**
 * @author by ouyang on  2017/11/16.
 */
public class RecentsTaskInfo {
    private String title;
    private String packageName;
    private String className;
    private Drawable icon;
    private Intent intent;
    
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	public Drawable getIcon() {
		return icon;
	}
	
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public Intent getIntent() {
		return intent;
	}
	
	public void setIntent(Intent intent) {
		this.intent = intent;
	}
	
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public String toString() {
		return "RecentsTaskInfo [title=" + title + ", packageName="
				+ packageName + ", className=" + className + ", icon=" + icon
				+ ", intent=" + intent + "]";
	}
}
