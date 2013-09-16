package edu.buffalo.cse.cse486586.groupmessenger;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class OnSendClickListener implements OnClickListener {
	private final TextView mTextView;
	private final EditText mEditText;
	private SequencerTotal protocol;

	public OnSendClickListener(EditText _et, TextView _tv,SequencerTotal pt) {
		mEditText = _et;
		mTextView = _tv;
		protocol = pt;
	}

	@Override
	public void onClick(View v) {		
	    String message = mEditText.getText().toString();
		Log.d("sendMessage", message);
		// executeOnExecutor is parrallel,so replace it with execute
		
		new ClientTask().execute("send"+ message);
		mEditText.setText("");
		return;

	}
	
	

	private class ClientTask extends AsyncTask<String, String, Void> {

		protected Void doInBackground(String... msgs) {		
			protocol.to_multicast(msgs[0]);
			return null;
		}
	}
}
