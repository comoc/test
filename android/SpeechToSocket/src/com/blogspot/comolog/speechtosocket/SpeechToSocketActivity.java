package com.blogspot.comolog.speechtosocket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.sciss.net.*;

public class SpeechToSocketActivity extends Activity {
	private static final String TAG = "SpeechToSocketActivity";
	
	private static final int REQUEST_CODE = 0;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;

	private static final int OUTGOING_PORT = 57110;
	private static final int INCOMMING_PORT = 57111;
	
	private static final String PREF_KEY = "preferenceTest";
	private static final String KEY_TEXT = "text";
	private static final String DEFAULT_IP_ADDRESS = "192.168.0.2";
	final Object mSync = new Object();
	private int mMsgId = 0;
	private OSCServer mOsc;
	private InetSocketAddress mAddress;
	private BluetoothChat mChat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mChat = new BluetoothChat();
		mChat.create(this, mListener);
		
		Button button;
		button = (Button) findViewById(R.id.buttonStartFreeForm);
		button.setOnClickListener(mOnClickListener);
		button = (Button) findViewById(R.id.buttonStartWebSearch);
		button.setOnClickListener(mOnClickListener);
		button = (Button) findViewById(R.id.buttonConnect);
		button.setOnClickListener(mOnClickListener);
		
		SharedPreferences pref = getSharedPreferences(PREF_KEY, Activity.MODE_PRIVATE);
		EditText ip = (EditText)findViewById(R.id.editTextIpAddress);
		ip.setText(pref.getString(KEY_TEXT, DEFAULT_IP_ADDRESS));

		try {
			mOsc = OSCServer.newUsing(OSCServer.UDP, INCOMMING_PORT);
			mOsc.start();
			mOsc.addOSCListener(mOscListener);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		if (mOsc != null) {
			mOsc.removeOSCListener(mOscListener);
			try {
				mOsc.stop();
			} catch (IOException e) {
			}
			mOsc.dispose();
		}
		
		mChat.destroy();

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		mChat.resume();
		super.onResume();
	}

	@Override
	protected void onStart() {
		mChat.start();
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				String resultsString = "";
				List<String> results = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				for (int i = 0; i < results.size(); i++) {
					String res = results.get(i);
					
					SpeechToSocketActivity.this.mChat.sendMessage(res);

					resultsString += res + "\n";
					byte[] b64str = null;
					try {
						b64str = Base64.encode(res.getBytes("UTF-8"), Base64.DEFAULT);
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if (mOsc != null && mAddress != null && b64str != null) {
						try {
							mOsc.send(new OSCMessage("/notify", new Object[] {
									new Integer(mMsgId), new Integer(i), b64str}), mAddress);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
				}
				TextView edit = (TextView) findViewById(R.id.editTextResults);
				edit.clearComposingText();
				edit.setText(resultsString);
			}
			break;
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
            	mChat.connectDevice(data);
            }
            break;
        case BluetoothChat.REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
            	mChat.setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                this.finish();
            }
            break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		Log.v(TAG, "dispatchKeyEvent : keyCode:" + keyCode);
		
		if (true) {//action == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_CALL:
				Log.v(TAG, "KeyEvent.KEYCODE_CALL");
				return true;
			case KeyEvent.KEYCODE_ENDCALL:
				Log.v(TAG, "KeyEvent.KEYCODE_ENDCALL");
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				Log.v(TAG, "KeyEvent.KEYCODE_VOLUME_UP");
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				Log.v(TAG, "KeyEvent.KEYCODE_VOLUME_DOWN");
				return true;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				if (action == KeyEvent.ACTION_DOWN)
					startSR(true);
				return true;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:				
//			{
//				KeyEvent newev = new KeyEvent(
//						event.getDownTime(),
//						event.getEventTime(),
//						event.getAction(),
//						KeyEvent.KEYCODE_BACK,
//						event.getRepeatCount(),
//						event.getMetaState(),
//						event.getDeviceId(),
//						event.getScanCode(),
//						event.getFlags(),
//						event.getSource());
//				return super.dispatchKeyEvent(newev);
//			}
				break;
			}
		}
		
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKeyDown : keyCode:" + keyCode);
//		if (keyCode == 85) {
//			return true;
//		} else if (keyCode == 86) {
//			return true;
//		} else if (keyCode == 87) {
//			return true;
//		}
//		
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKeyLongPress : keyCode:" + keyCode);

		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		Log.v(TAG, "onKeyMultiple : keyCode:" + keyCode);

		return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKeyUp : keyCode:" + keyCode);
		return super.onKeyUp(keyCode, event);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
        	mChat.startDeviceListActivity(REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
        	mChat.ensureDiscoverable();
            return true;        
        }
        return false;
    }
	
	private void startSR(boolean isFree) {
		if (mOsc != null && mAddress != null) {
			try {
				mOsc.send(new OSCMessage("/start_sr"), mAddress);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		
		try {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			if (isFree)
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
						RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			else
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
						RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
			startActivityForResult(intent, REQUEST_CODE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(SpeechToSocketActivity.this, e.toString(),
					Toast.LENGTH_SHORT).show();
		}		
	}
	
	private OnClickListener mOnClickListener = new OnClickListener() {

		public void onClick(View v) {
			if (v.getId() == R.id.buttonConnect) {
				EditText ip = (EditText) findViewById(R.id.editTextIpAddress);
				if (ip == null)
					return;
				String s = ip.getText().toString();
				if (s.isEmpty())
					return;
				SharedPreferences pref = getSharedPreferences(PREF_KEY, Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = pref.edit();
				editor.putString(KEY_TEXT, s);
				editor.commit();

				String ipAddr = SpeechToSocketActivity.this.getIpAddress();
				byte[] b64str = null;
				try {
					b64str = Base64.encode(ipAddr.getBytes("UTF-8"), Base64.DEFAULT);
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				if (!s.isEmpty()) {
					mAddress = new InetSocketAddress(s, OUTGOING_PORT);
					
					if (b64str != null) {
						try {
							mOsc.send(new OSCMessage("/connect", new Object[] {b64str}), mAddress);
							String str = getString(R.string.connected) + ": " + s + ":" + OUTGOING_PORT;
							Toast.makeText(SpeechToSocketActivity.this, str,
									Toast.LENGTH_SHORT).show();					
						} catch (IOException e) {
						}
					}
				}
				return;
			}
			
			int id = v.getId(); 

			if (id == R.id.buttonStartFreeForm)
				startSR(true);
			else if (id == R.id.buttonStartWebSearch)
				startSR(false);
		}
	};

	private OSCListener mOscListener = new OSCListener() {

		public void messageReceived(OSCMessage arg0, SocketAddress arg1,
				long arg2) {
			Log.v(TAG, "messageReceived:" + arg0.toString());
			if (arg0.getName().equals("/n_end")) {
				synchronized (mSync) {
					mSync.notifyAll();
				}
			} else if (arg0.getName().equals("/kick_free")) {
				startSR(true);
			} else if (arg0.getName().equals("/kick_web")) {
				startSR(false);
			}
		}
	};
	
    private String getIpAddress() {
        Enumeration<NetworkInterface> netIFs;
        try {
            netIFs = NetworkInterface.getNetworkInterfaces();
            while( netIFs.hasMoreElements() ) {
                NetworkInterface netIF = netIFs.nextElement();
                Enumeration<InetAddress> ipAddrs = netIF.getInetAddresses();
                while( ipAddrs.hasMoreElements() ) {
                    InetAddress ip = ipAddrs.nextElement();
                    if( ! ip.isLoopbackAddress() ) {
                        return ip.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private BluetoothChat.Listener mListener = new BluetoothChat.Listener() {

		@Override
		public void recieveMessage(String str) {
			// TODO Auto-generated method stub
			if (str.charAt(0) == 'f')
				startSR(true);
			else if (str.charAt(0) == 'w')
				startSR(false);
			Log.v(TAG, str);
		}
    	
    };
}
