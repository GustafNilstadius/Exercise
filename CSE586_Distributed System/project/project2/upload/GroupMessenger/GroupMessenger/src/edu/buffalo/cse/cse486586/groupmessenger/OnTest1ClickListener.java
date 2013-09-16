package edu.buffalo.cse.cse486586.groupmessenger;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnTest1ClickListener implements OnClickListener {
	private TextView mTextView;
	private SequencerTotal protocol;

	public OnTest1ClickListener(TextView _tv, SequencerTotal pt) {
		mTextView = _tv;
		protocol = pt;
	}

	@Override
	public void onClick(View v) {	
		// executeOnExecutor is parrallel,so replace it with execute
		new ClientTask().execute("test1");

		return;

	}

	private class ClientTask extends AsyncTask<String, String, Void> {

		protected Void doInBackground(String... msgs) {
			int i;
			String message;
			
			for (i = 0; i < 5; i++) {
				message = msgs[0]+protocol.myId + ":" + Integer.toString(i);
				Log.d("sendMessage", message);
				protocol.to_multicast(message);
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

}
