package com.blogspot.comolog.speechtosocket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.sciss.net.*;

public class SpeechToSocketActivity extends Activity {
	private static final int REQUEST_CODE = 0;
	private static final int PORT = 57110;
	final Object mSync = new Object();
	private int mMsgId = 0;
	private OSCClient mClient;

	// final OSCBundle bndl1, bndl2;
	// final Integer nodeID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Button button;
		button = (Button) findViewById(R.id.buttonStartFreeForm);
		button.setOnClickListener(mOnClickListener);
		button = (Button) findViewById(R.id.buttonStartWebSearch);
		button.setOnClickListener(mOnClickListener);
		button = (Button) findViewById(R.id.buttonConnect);
		button.setOnClickListener(mOnClickListener);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
			String resultsString = "";
			List<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			for (int i = 0; i < results.size(); i++) {
				String res = results.get(i);
				resultsString += res + "\n";
				byte[] b64str = null;
				try {
					b64str = Base64.encode(res.getBytes("UTF-8"), Base64.DEFAULT);
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (mClient != null || b64str != null) {
					try {
						mClient.send(new OSCMessage("/notify", new Object[] {
								new Integer(mMsgId), new Integer(i), b64str}));
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
		super.onActivityResult(requestCode, resultCode, data);
	}

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.buttonConnect) {
				if (mClient != null) {
					try {
						mClient.stop();
					} catch (IOException e) {
					}
					mClient.dispose();
				}
				EditText ip = (EditText) findViewById(R.id.editTextIpAddress);
				if (ip == null)
					return;
				String s = ip.getText().toString();
				if (s.isEmpty())
					return;
				
				try {
					mClient = OSCClient.newUsing(OSCClient.UDP);
					mClient.setTarget(new InetSocketAddress(s, PORT));
					mClient.start();
					mClient.addOSCListener(mOscListener);
					mClient.send(new OSCMessage("/start"));
					String str = getString(R.string.connected) + ": " + s + ":" + PORT;
					Toast.makeText(SpeechToSocketActivity.this, str,
							Toast.LENGTH_SHORT).show();
					
				} catch (IOException e) {
				}

				return;
			}

			try {
				// 音声認識用インテント生成
				Intent intent;
				intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				if (v.getId() == R.id.buttonStartFreeForm)
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				else
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

				// intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				// R.string.check_pronounce_text);
				startActivityForResult(intent, REQUEST_CODE);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(SpeechToSocketActivity.this, e.toString(),
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	private OSCListener mOscListener = new OSCListener() {

		@Override
		public void messageReceived(OSCMessage arg0, SocketAddress arg1,
				long arg2) {
			if (arg0.getName().equals("/n_end")) {
				synchronized (mSync) {
					mSync.notifyAll();
				}
			}
		}
	};
}
