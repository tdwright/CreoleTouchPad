package co.uk.tdwright.creole;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import co.uk.tdwright.creole.CreoleConnectionService.LocalBinder;

import com.immersion.uhl.Device;
import com.immersion.uhl.EffectHandle;
import com.immersion.uhl.ImmVibe;
import com.immersion.uhl.MagSweepEffectDefinition;

public class CreoleTouchPadActivity extends Activity {
	
	private static final boolean FULLSCREEN = true;
	private TextView statusText;
    
    private CreoleConnectionService connService;
    boolean connServiceBound = false;
    
    private int screenHeight, screenWidth; 

	public static final String TAG = "CreoleTouchPad";
	private static final String STATUS_BROADCAST = "co.uk.tdwright.creole.STATUS_BROADCAST";
	private static final String INTENSITY_BROADCAST = "co.uk.tdwright.creole.INTENSITY_BROADCAST";
	
	// For the vibration
	private EffectHandle m_hEffectMagSweep = null;
	private MagSweepEffectDefinition m_defMagSweep = new MagSweepEffectDefinition(
			ImmVibe.VIBE_TIME_INFINITE,		// duration
			0,								// magnitude
			ImmVibe.VIBE_DEFAULT_STYLE,		// style
			0,								// attackTime
			0,								// attackLevel
			0,								// fadeTime
			0,								// fadeLevel
			0								// actuatorIndex
		);
	private Device m_device;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FULLSCREEN) {
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        
        setContentView(R.layout.main);
        this.setStatusText("Initialising...");
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        
        m_device = Device.newDevice(getApplicationContext());
        
        bindService(new Intent(CreoleTouchPadActivity.this, CreoleConnectionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	final int action = event.getAction();
    	switch (action & MotionEvent.ACTION_MASK) {
    	case MotionEvent.ACTION_DOWN:
    		try
    		{
    			sendCoords(event.getX(), event.getY());
    			m_defMagSweep.setStyle(ImmVibe.VIBE_STYLE_SHARP);
	    		m_hEffectMagSweep = m_device.playMagSweepEffect(m_defMagSweep);
    		}
    		catch (RuntimeException re) {}
    		break;
    	case MotionEvent.ACTION_MOVE:
    		try
    		{
    			sendCoords(event.getX(), event.getY());
    			m_defMagSweep.setStyle(ImmVibe.VIBE_STYLE_STRONG);
	    		m_hEffectMagSweep.modifyPlayingMagSweepEffect(m_defMagSweep);
    		}
    		catch (RuntimeException re) {}
    		break;
    	case MotionEvent.ACTION_UP:
    		try
    		{
    			m_hEffectMagSweep.stop();
    			sendCoords(0,0,true);
    		}
    		catch (RuntimeException re) {}
    		break;
    	}
    	return false;
    }

	private void sendCoords(float X, float Y)  {
		this.sendCoords(X, Y, false);
	}
    
	private void sendCoords(float X, float Y, boolean up)
	{
		String coordString;
		if(connServiceBound)
		{
			if(up)
			{
				coordString="finger___up";
				Log.v(TAG, "Finger raised");
			}
			else
			{
				float xProportion = X / (float)this.screenWidth;
				float yProportion = Y / (float)this.screenHeight;
				coordString = String.format("%.3f;%.3f", xProportion, yProportion);
				Log.v(TAG, "Pointer at: " + coordString);
			}
			connService.sendCoords(coordString);
		}
	}
	
    private void setIntensity(String intensity)
    {
    	try {
    		double proportion;
    		if(intensity == "000")
    		{
    			proportion = 0d;
    		}
    		else
    		{
	    		int thousandths = Integer.parseInt(intensity);
//	        	Log.v(TAG, Integer.toString(thousandths));
	        	proportion = (double)thousandths / 1000d;
    		}
        	int vibePower = (int)Math.floor(proportion * (double)ImmVibe.VIBE_MAX_MAGNITUDE);
        	Log.v(TAG, "Vibe power: " + Integer.toString(vibePower));
        	if (vibePower > ImmVibe.VIBE_MAX_MAGNITUDE) vibePower = ImmVibe.VIBE_MAX_MAGNITUDE;
        	m_defMagSweep.setMagnitude(vibePower);
        	if(m_hEffectMagSweep!=null) {
	        	if(m_hEffectMagSweep.isPlaying()) {
	        		m_hEffectMagSweep.modifyPlayingMagSweepEffect(m_defMagSweep);
	        	} // else wait for a touch event
    		}
    	} catch (NumberFormatException nfe) {
    		Log.d(TAG, "Received non-numerical intensity value.");
    	}
    }
    
    private void setStatusText(String status)
    {
    	final TextView statusView = (TextView)findViewById(R.id.status);
    	statusView.setText(status);
    }
	
	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			connService = binder.getService();
			connServiceBound = true;
		}

		public void onServiceDisconnected(ComponentName arg0) {
			connServiceBound = false;
		}
	};
	
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			if(intent.getAction()==INTENSITY_BROADCAST) {
//				Log.v(TAG, extras.getString("intensity"));
				setIntensity(extras.getString("intensity"));
			} else if (intent.getAction()==STATUS_BROADCAST) {
				setStatusText(extras.getString("status"));
				if(extras.getBoolean("connected")==false)
				{
		        	if(m_hEffectMagSweep!=null) {
			        	if(m_hEffectMagSweep.isPlaying()) {
			        		m_hEffectMagSweep.stop();
			        		m_defMagSweep.setMagnitude(0);
			        	}
		        	}
				}
			}
		}
   };

    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(STATUS_BROADCAST);
        filter.addAction(INTENSITY_BROADCAST);
        this.registerReceiver(this.broadcastReceiver, filter);
    }

    public void onPause() {
        super.onPause();
        this.unregisterReceiver(this.broadcastReceiver);
    }

}