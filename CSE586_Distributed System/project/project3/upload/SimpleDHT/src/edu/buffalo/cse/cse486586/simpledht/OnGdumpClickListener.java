package edu.buffalo.cse.cse486586.simpledht;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class OnGdumpClickListener implements OnClickListener {
	private static final String TAG = OnGdumpClickListener.class.getName();
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";
	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;

	public OnGdumpClickListener(TextView _tv, ContentResolver _cr) {
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", 
				"edu.buffalo.cse.cse486586.simpledht.provider");
		
	}

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	@Override
	public void onClick(View v) {
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class Task extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			testQuery();
				
			return null;
		}
		
		protected void onProgressUpdate(String...strings) {
			mTextView.append(strings[0]);

			return;
		}



		private boolean testQuery() {
			try {
				String output = new String();
				String[] projection = {"global"};
				Cursor resultCursor = mContentResolver.query(mUri, projection, null,
						null, null);
				if (resultCursor == null) {
					Log.e(TAG, "Result null");
					throw new Exception();
				}
				Log.d(TAG, Integer.toString(resultCursor.getCount()));

				output = output+"\n"+"Gdump begin";
				int i = 1;
				while (resultCursor.moveToNext()) {
					// Extract data.
					output = output
							+ resultCursor.getString(resultCursor
									.getColumnIndex(KEY_FIELD));
					output = output + ":";
					output = output
							+ resultCursor.getString(resultCursor
									.getColumnIndex(VALUE_FIELD));
					if (0 != (i % 3)) {
						output = output + ";";
					} else {
						output = output + "\n";
					}
					i++;
				}

				resultCursor.close();

				output = output+"\n"+"Gdump end";
				output = output+"\n"
						+ new SimpleDateFormat("HH:mm:ss").format(new Date());
				output = output + "\n";
				publishProgress(output);

			} catch (Exception e) {
				Log.e(TAG, "exception");
				return false;
			}

			return true;
		}
	}
}