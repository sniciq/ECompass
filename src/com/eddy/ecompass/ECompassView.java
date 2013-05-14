package com.eddy.ecompass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ECompassView extends ImageView {
	
	private float directtion = 0.0f;
	private Drawable compass = null;
	

	public ECompassView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(compass == null) {
			compass = getDrawable();
			compass.setBounds(0, 0, getWidth(), getHeight());
		}
		
		canvas.save();
		canvas.rotate(directtion, getWidth()/2, getHeight()/2);
		compass.draw(canvas);
		canvas.restore();
	}

	public void updateDirection(float direction) {
		directtion = direction;
		invalidate();
	}
}
