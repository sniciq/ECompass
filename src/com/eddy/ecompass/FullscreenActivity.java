package com.eddy.ecompass;

import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.eddy.ecompass.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
	
//	private final String TAG = "FullscreenActivity";
	
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	private SensorManager sensorManager;
	private Sensor aSensor;//加速度传感器
	private Sensor mSensor;//磁力传感器
	
	float[] accelerometerValues=new float[3];  
    float[] magneticFieldValues=new float[3];  
    float[] values=new float[3];  
    float[] rotate=new float[9]; 
    private ECompassView compassView;
    private float mDirection = 0.0f;// 当前浮点方向
    private float mTargetDirection;
    private boolean mStopDrawing = false;
    
    private AccelerateInterpolator mInterpolator;// 动画从开始到结束，变化率是一个加速的过程,就是一个动画速率
    private final float MAX_ROATE_DEGREE = 1.0f;// 最多旋转一周，即360°
    protected final Handler mHandler = new Handler();
    private LinearLayout mDirectionLayout;
    private LinearLayout angleLayout;
    private SensorEventListener slistener;
    
    protected Runnable mCompassViewUpdater = new Runnable() {
		@Override
		public void run() {
			if (compassView != null && !mStopDrawing) {
				if (mDirection != mTargetDirection) {
					// calculate the short routine
					float to = mTargetDirection;
					if (to - mDirection > 180) {
						to -= 360;
					} else if (to - mDirection < -180) {
						to += 360;
					}

					// limit the max speed to MAX_ROTATE_DEGREE
					float distance = to - mDirection;
					if (Math.abs(distance) > MAX_ROATE_DEGREE) {
						distance = distance > 0 ? MAX_ROATE_DEGREE : (-1.0f * MAX_ROATE_DEGREE);
					}

					// need to slow down if the distance is short
					mDirection = normalizeDegree(mDirection
							+ ((to - mDirection) * mInterpolator.getInterpolation(Math.abs(distance) > MAX_ROATE_DEGREE ? 0.4f : 0.3f)));// 用了一个加速动画去旋转图片，很细致
					compassView.updateDirection(mDirection);// 更新指南针旋转
				}

				updateDirectionText();// 更新方向值

				mHandler.postDelayed(mCompassViewUpdater, 20);// 20毫米后重新执行自己，比定时器好
			}
		}
	};
	
	private void updateDirectionText() {
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		// 先移除layout中所有的view
		mDirectionLayout.removeAllViews();
		angleLayout.removeAllViews();

		// 下面是根据mTargetDirection，作方向名称图片的处理
		ImageView east = null;
		ImageView west = null;
		ImageView south = null;
		ImageView north = null;
		float direction = normalizeDegree(mTargetDirection * -1.0f);
		if (direction > 22.5f && direction < 157.5f) {
			// east
			east = new ImageView(this);
			east.setImageResource(R.drawable.e_cn);
			east.setLayoutParams(lp);
		} else if (direction > 202.5f && direction < 337.5f) {
			// west
			west = new ImageView(this);
			west.setImageResource(R.drawable.w_cn);
			west.setLayoutParams(lp);
		}

		if (direction > 112.5f && direction < 247.5f) {
			// south
			south = new ImageView(this);
			south.setImageResource(R.drawable.s_cn);
			south.setLayoutParams(lp);
		} else if (direction < 67.5 || direction > 292.5f) {
			// north
			north = new ImageView(this);
			north.setImageResource(R.drawable.n_cn);
			north.setLayoutParams(lp);
		}
		// 下面是根据系统使用语言，更换对应的语言图片资源
		// east/west should be before north/south
		if (east != null) {
			mDirectionLayout.addView(east);
		}
		if (west != null) {
			mDirectionLayout.addView(west);
		}
		if (south != null) {
			mDirectionLayout.addView(south);
		}
		if (north != null) {
			mDirectionLayout.addView(north);
		}
//		// 下面是根据方向度数显示度数图片数字
		int direction2 = (int) direction;
		boolean show = false;
		if (direction2 >= 100) {
			angleLayout.addView(getNumberImage(direction2 / 100));
			direction2 %= 100;
			show = true;
		}
		if (direction2 >= 10 || show) {
			angleLayout.addView(getNumberImage(direction2 / 10));
			direction2 %= 10;
		}
		angleLayout.addView(getNumberImage(direction2));
		// 下面是增加一个°的图片
		ImageView degreeImageView = new ImageView(this);
		degreeImageView.setImageResource(R.drawable.degree);
		degreeImageView.setLayoutParams(lp);
		angleLayout.addView(degreeImageView);
	}
	
	private View getNumberImage(int number) {
		ImageView image = new ImageView(this);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		switch (number) {
		case 0:
			image.setImageResource(R.drawable.number_0);
			break;
		case 1:
			image.setImageResource(R.drawable.number_1);
			break;
		case 2:
			image.setImageResource(R.drawable.number_2);
			break;
		case 3:
			image.setImageResource(R.drawable.number_3);
			break;
		case 4:
			image.setImageResource(R.drawable.number_4);
			break;
		case 5:
			image.setImageResource(R.drawable.number_5);
			break;
		case 6:
			image.setImageResource(R.drawable.number_6);
			break;
		case 7:
			image.setImageResource(R.drawable.number_7);
			break;
		case 8:
			image.setImageResource(R.drawable.number_8);
			break;
		case 9:
			image.setImageResource(R.drawable.number_9);
			break;
		}
		image.setLayoutParams(lp);
		return image;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fullscreen);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		compassView = (ECompassView) findViewById(R.id.compassView);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, compassView, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
					}
					controlsView.animate().translationY(visible ? 0 : mControlsHeight).setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
				}

				if (visible && AUTO_HIDE) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.
		compassView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
		
		slistener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
					magneticFieldValues = event.values;
				}
				if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					accelerometerValues = event.values;
				}
				
				SensorManager.getRotationMatrix(rotate, null, accelerometerValues, magneticFieldValues);  
	            SensorManager.getOrientation(rotate, values);  
	            
	            values[0] = (float) Math.toDegrees(values[0]); 
	            
	            mTargetDirection = values[0] * -1.0f;
//	            Log.i(TAG, mTargetDirection + " ");
			}
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);  
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(slistener, aSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(slistener, mSensor, SensorManager.SENSOR_DELAY_GAME);
		
		mInterpolator = new AccelerateInterpolator();// 实例化加速动画对象
		mDirectionLayout = (LinearLayout) findViewById(R.id.layout_direction);
		angleLayout = (LinearLayout) findViewById(R.id.layout_angle);
	}
	
	public void onPause(){  
		sensorManager.unregisterListener(slistener);  
        super.onPause();  
    } 
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(sensorManager != null && aSensor != null) {
			sensorManager.registerListener(slistener, aSensor, SensorManager.SENSOR_DELAY_GAME);
		}
		if(sensorManager != null && mSensor != null) {
			sensorManager.registerListener(slistener, mSensor, SensorManager.SENSOR_DELAY_GAME);
		}
		
		mStopDrawing = false;
		mHandler.postDelayed(mCompassViewUpdater, 20);
	}

	// 调整方向传感器获取的值
	private float normalizeDegree(float degree) {
		return (degree + 720) % 360;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
