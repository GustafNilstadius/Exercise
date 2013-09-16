package edu.buffalo.cse.cse486586.simpledynamo;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnPut1ClickListener implements OnClickListener {

	private static final String TAG = OnPut1ClickListener.class.getName();
	//private static final int TEST_CNT = 2;
	public static final String KEY_FIELD = "key";
	public static final String VERSION_FIELD = "version";
	public static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	private final ContentValues[] mContentValues;

	public OnPut1ClickListener(TextView _tv, ContentResolver _cr) {
		mTextView = _tv;
		mContentResolver = _cr;
		
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
		mContentValues = initTestValues();
	}

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	private ContentValues[] initTestValues() {
		ContentValues[] cv = new ContentValues[SimpleDynamoActivity.TEST_cnt];
		for (int i = 0; i < SimpleDynamoActivity.TEST_cnt; i++) {
			cv[i] = new ContentValues();
			cv[i].put(KEY_FIELD, Integer.toString(i));
			cv[i].put(VERSION_FIELD, i);
			cv[i].put(VALUE_FIELD, "Put1" + Integer.toString(i));
		}

		return cv;
	}

	@Override
	public void onClick(View v) {
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class Task extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (testInsert()) {
				publishProgress("Insert success\n");
			} else {
				publishProgress("Insert fail\n");
				return null;
			}
			
			return null;
		}
		
		protected void onProgressUpdate(String...strings) {
			mTextView.append(strings[0]);

			return;
		}

		private boolean testInsert() {
			try {
				for (int i = 0; i < SimpleDynamoActivity.TEST_cnt; i++) {
					mContentResolver.insert(mUri, mContentValues[i]);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				return false;
			}

			return true;
		}

	}


}
