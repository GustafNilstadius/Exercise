package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnGetClickListener implements OnClickListener {

	private static final String TAG = OnPut1ClickListener.class.getName();
	//private static final int TEST_CNT = 2;
	public static final String KEY_FIELD = "key";
	public static final String VERSION_FIELD = "version";
	public static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	private final ContentValues[] mContentValues;

	public OnGetClickListener(TextView _tv, ContentResolver _cr) {
		mTextView = _tv;
		mContentResolver = _cr;
		
		mUri = buildUri("content",
				"edu.buffalo.cse.cse486586.simpledynamo.provider");
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
			if (!testQuery()) {
				publishProgress("Query fail\n");
			} 
			return null;
		}

		protected void onProgressUpdate(String... strings) {
			mTextView.append(strings[0]);

			return;
		}

		private boolean testQuery() {
			try {
				for (int i = 0; i < SimpleDynamoActivity.TEST_cnt; i++) {
					String key = (String) mContentValues[i].get(KEY_FIELD);
					Cursor resultCursor = mContentResolver.query(mUri, null,
							key, null, null);
					if (resultCursor == null) {
						Log.e(TAG, "Result null");
						throw new Exception();
					}

					int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
					int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
					if (keyIndex == -1 || valueIndex == -1) {
						Log.e(TAG, "Wrong columns");
						resultCursor.close();
						throw new Exception();
					}

					resultCursor.moveToFirst();

					if (!(resultCursor.isFirst() && resultCursor.isLast())) {
						Log.e(TAG, "Wrong number of rows");
						resultCursor.close();
						throw new Exception();
					}

					String returnKey = resultCursor.getString(keyIndex);
					String returnValue = resultCursor.getString(valueIndex);

					resultCursor.close();

					String output = "<" + returnKey + "," + returnValue + ">,";

					if(0==(i+1)%3)
						output = output+"\n";
					publishProgress(output);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			} catch (Exception e) {
				return false;
			}

			return true;
		}
	}

}
