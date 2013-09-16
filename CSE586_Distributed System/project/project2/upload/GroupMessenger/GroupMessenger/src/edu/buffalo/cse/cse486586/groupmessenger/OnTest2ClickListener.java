package edu.buffalo.cse.cse486586.groupmessenger;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnTest2ClickListener implements OnClickListener {
	private TextView mTextView;
	private SequencerTotal protocol;

	public OnTest2ClickListener(TextView _tv, SequencerTotal pt) {
		mTextView = _tv;
		protocol = pt;
	}

	@Override
	public void onClick(View v) {
		// executeOnExecutor is parrallel,so replace it with execute
		new ClientTask().execute("test2"+"0"+ protocol.myId + ":" + Integer.toString(0));
		//new ClientTask().execute("test2"+"0");

		return;

	}

	public class ClientTask extends AsyncTask<String, String, Void> {
		int i = 0;
		
		protected Void doInBackground(String... msgs) {
			
			String message;

			//message = msgs[0] + protocol.myId + ":" + Integer.toString(i);
			message =msgs[0];
			i++;
			Log.d("sendMessage", message);
			protocol.to_multicast(message);

			return null;
		}
	}

}
