package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	public static final String CONTENT_URI = "content://edu.buffalo.cse.cse486_586.simpledynamo.provider";
	public static final String AUTHORITY = "edu.buffalo.cse.cse486_586.simpledynamo.provider";
	public static final String SCHEME = "content";
	Uri myuri;
	public static final String KEY_FIELD = "key";
	public static final String VERSION_FIELD = "version";
	public static final String VALUE_FIELD = "value";

	public static final String TABLE = "content_provider_tutorial";

	public static final int MATCH_ALL = 1;
	public static final int MATCH_ID = 2;

	private SQLiteOpenHelper sqliteOpenHelper;
	private UriMatcher uriMatcher;

	DynamoAlgorithm dynamo;
	String myid;
	// only for debugging
	boolean distributed = true;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		int match = this.uriMatcher.match(uri);

		// Get the database
		SQLiteDatabase db = this.sqliteOpenHelper.getWritableDatabase();

		int rowsDeleted = 0;
		switch (match) {
		case MATCH_ALL:
			rowsDeleted = db.delete(TABLE, selection, selectionArgs);
			break;

		case MATCH_ID:
			String id = uri.getLastPathSegment();
			if (selection == null || selection.trim().length() == 0) {
				String whereClause = "id=" + id;
				rowsDeleted = db.delete(TABLE, whereClause, null);
			} else {
				String whereClause = selection + "and" + "id=" + id;
				rowsDeleted = db.delete(TABLE, whereClause, selectionArgs);
			}
			break;
		}

		return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return "tutorial/content/provider";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (!distributed)
			return insert_local(uri, values);
		else {
			String key = values.getAsString("key");
			String value = values.getAsString("value");
			dynamo.insert2coordinator(key, value);
			return null;
		}
	}

	private Uri insert_local(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub

		// Log.e("insert key:", values.getAsString("key"));
		// Log.e("insert value:", values.getAsString("value"));
		// Get an instance of writable database
		SQLiteDatabase db = this.sqliteOpenHelper.getWritableDatabase();

		// Insert the data into the database
		// long newId = db.insert(TABLE, null, values);

		long newId = db.insertWithOnConflict(TABLE, null, values, 5);// CONFLICT_REPLACE

		// Make sure data is successfully added
		if (newId < 0) {
			throw new SQLException("Insert Failure on: " + uri);
		}

		// Setup the newUri to identify this newly created object
		Uri newUri = ContentUris.withAppendedId(uri, newId);

		// notify the system that a change has been made to the underlying data
		this.getContext().getContentResolver().notifyChange(newUri, null);

		return newUri;
		// return null;
	}

	@Override
	public boolean onCreate() {
		// Open the database
		this.sqliteOpenHelper = new DBHelper(this.getContext(), "tutorialdb",
				null, 1);
		// this.sqliteOpenHelper.onCreate();

		// Setup the UriMatcher
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "objects", MATCH_ALL);
		this.uriMatcher.addURI(AUTHORITY, "object/#", MATCH_ID);

		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(AUTHORITY);
		uriBuilder.scheme(SCHEME);
		myuri = uriBuilder.build();

		dynamo = new DynamoAlgorithm(this.getContext());
		myid = dynamo.myid;
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (!distributed)
			return local_query(uri, projection, selection, selectionArgs,
					sortOrder);
		else {
			if (null == selection) {
				return local_query(uri, null, selection, selectionArgs,
						sortOrder);
			} else {
				// get from coordinator machine
				String value = dynamo.query2coordinator(selection);
				MatrixCursor c = new MatrixCursor(new String[] { KEY_FIELD,
						VALUE_FIELD });
				if (value != null) {
					c.newRow().add(selection).add(value);
					return c;
				} else {
					return null;
				}
			}
		}
	}

	private Cursor local_query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Query building helper class
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		Cursor cursor;

		// Set the table to query
		builder.setTables(TABLE);

		// execute the query
		if (null != selection) {
			cursor = builder.query(this.sqliteOpenHelper.getReadableDatabase(),
					projection, "key" + "=?", new String[] { selection }, null,
					null, sortOrder, null);
		} else {
			cursor = builder.query(this.sqliteOpenHelper.getReadableDatabase(),
					projection, null, null, null, null, sortOrder, null);
		}

		// sets up data watch so that the cursor is notified if data changes
		// after it is returned
		cursor.setNotificationUri(this.getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (this.uriMatcher.match(uri) != MATCH_ID) {
			// no object designated for update
			return 0;
		}

		// Get an instance of writable database
		SQLiteDatabase db = this.sqliteOpenHelper.getWritableDatabase();

		// Find the id of the object being updated
		String id = uri.getLastPathSegment();
		int rowsUpdated = 0;
		if (selection == null || selection.trim().length() == 0) {
			rowsUpdated = db.update(TABLE, values, "id=" + id, null);
		} else {
			String whereClause = selection + "and" + "id=" + id;
			rowsUpdated = db.update(TABLE, values, whereClause, selectionArgs);
		}

		return rowsUpdated;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	protected class DynamoAlgorithm {
		public class process implements Comparable<process> {
			private String id;
			private InetSocketAddress socket;

			public process(String id, InetSocketAddress socket) {
				if (id == null || socket == null)
					throw new NullPointerException();
				this.id = id;
				this.socket = socket;
			}

			public String id() {
				return id;
			}

			public InetSocketAddress socket() {
				return socket;
			}

			public boolean equals(Object o) {
				if (!(o instanceof process))
					return false;
				process p = (process) o;
				return p.id.equals(id) && p.socket.equals(socket);
			}

			public int hashCode() {
				return 31 * id.hashCode() + socket.hashCode();
			}

			public String toString() {
				return id + " " + socket.toString();
			}

			public int compareTo(process p) {
				try {
					int lastCmp = genHash(id).compareTo(genHash(p.id));
					return lastCmp;
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0;
			}
		}

		final int QUORUM_N = 3, QUORUM_R = 2, QUORUM_W = 2;
		final int SOCKET_TIMEOUT_MILISECOND = 200;

		final String SUCCESS = "sucess";
		final String FAIL = "fail";
		final String INSERT2COORDINATOR = "insert2coordinator";
		final String INSERT2SUCCESSORS = "insert2successors";
		final String QUERY2COORDINATOR = "query2coordinator";
		final String QUERY2SUCCESSORS = "query2successors";
		final String RECOVERY_REQUEST = "recovery_request";

		final String IPADDR = "10.0.2.2";
		public String myid;

		process myprocess;
		List<process> processes;

		public DynamoAlgorithm(Context context) {
			final int SERVER_PORT = 10000;

			ServerSocket welcomeSocket;

			// fork server thread
			try {
				welcomeSocket = new ServerSocket(SERVER_PORT);
				Log.d("onCreate", "build server success");
				new ServerTask().executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR, welcomeSocket);
			} catch (IOException e) {
				Log.e("MainActivity", "create server socket error");
				e.printStackTrace();
			}

			TelephonyManager tel = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			myid = tel.getLine1Number().substring(
					tel.getLine1Number().length() - 4);

			if (0 == myid.compareTo("5554")) {
				myprocess = new process(myid, new InetSocketAddress(IPADDR,
						11108));
			} else if (0 == myid.compareTo("5556")) {
				myprocess = new process(myid, new InetSocketAddress(IPADDR,
						11112));
			} else if (0 == myid.compareTo("5558")) {
				myprocess = new process(myid, new InetSocketAddress(IPADDR,
						11116));
			} else {
				myid = null;
				myprocess = null;
				Log.e("MainActivity", "Avd wrong");
			}

			process processArray[] = {
					new process("5554", new InetSocketAddress(IPADDR, 11108)),
					new process("5556", new InetSocketAddress(IPADDR, 11112)),
					new process("5558", new InetSocketAddress(IPADDR, 11116)) };

			processes = Arrays.asList(processArray);
			Collections.sort(processes);
			System.out.println(processes);

			// when a node recovery, copy key-version-value pairs from its
			// successors, and write the newest version to local
			// ContentProvider.onCreate() can't be blocked, so fork another
			// thread run this
			new ClientTask().execute();

			return;
		}

		private class ClientTask extends AsyncTask<String, String, Void> {
			protected Void doInBackground(String... msgs) {
				process_recovery();
				return null;
			}
		}

		private void process_recovery() {
			ArrayList<process> successors = get_successors(myprocess,
					QUORUM_N - 1);

			for (process p : successors) {
				try {
					Socket socket = new Socket();
					socket.setSoTimeout(SOCKET_TIMEOUT_MILISECOND);
					socket.connect(p.socket(), SOCKET_TIMEOUT_MILISECOND);

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));

					out.print(RECOVERY_REQUEST + "\n");
					out.flush();
					String key, version, value;

					while (null != (key = reader.readLine())) {
						version = reader.readLine();
						value = reader.readLine();
						recv_recovery(key, Integer.parseInt(version), value);
					}

					socket.close();
					Log.e(myid, "process_recovery end");
				} catch (SocketTimeoutException e) {
					Log.e(myid, "timeout");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(myid, "client socket error");
					e.printStackTrace();
				}
			}

			return;

		}

		private process get_coordinator(String key) {
			try {
				for (process p : processes) {
					if (genHash(key).compareTo(genHash(p.id())) <= 0) {
						return p;
					}
				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return processes.get(0);
		}

		private ArrayList<process> get_successors(process p, int number) {
			// return QUORUM_N-1
			int index = processes.indexOf(p);
			if (-1 == index) {
				Log.e(myid, "error! should not go here");
				return null;
			} else {
				ArrayList<process> successors = new ArrayList<process>();
				index++;
				while ((number > 0) && (index < processes.size())) {
					successors.add(processes.get(index));
					index++;
					number--;
				}
				// start over from beginning
				index = 0;
				while (number > 0) {
					successors.add(processes.get(index));
					index++;
					number--;
				}
				return successors;
			}
		}

		public String query2coordinator(String key) {
			process coordinator1 = get_coordinator(key);
			process coordinator2 = (get_successors(coordinator1, 1)).get(0);
			process coordinators[] = { coordinator1, coordinator2 };
			String ret = null;
			// at most ask for 2 times
			for (int i = 0; i < 2; i++) {
				try {
					Socket socket = new Socket();
					socket.setSoTimeout(QUORUM_N * SOCKET_TIMEOUT_MILISECOND);
					socket.connect(coordinators[i].socket(),
							SOCKET_TIMEOUT_MILISECOND);

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));

					out.print(QUERY2COORDINATOR + "\n" + key + "\n");
					out.flush();

					String result = reader.readLine();
					// block at here
					if ((null != result) && (0 == result.compareTo(SUCCESS))) {
						ret = reader.readLine();
						Log.d(myid, "return from coordinator" + ret);
					} else {
						ret = null;
						Log.d(myid, "query fail from coordinator");
					}
					socket.close();

					if (null != ret)// success
						break;
				} catch (SocketTimeoutException e) {
					Log.e(myid, "timeout");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(myid, "client socket error");
					e.printStackTrace();
				}
			}

			return ret;
		}

		public boolean insert2coordinator(String key, String value) {
			process coordinator1 = get_coordinator(key);
			process coordinator2 = (get_successors(coordinator1, 1)).get(0);
			process coordinators[] = { coordinator1, coordinator2 };
			boolean success = false;

			// at most ask for 2 times
			for (int i = 0; i < 2; i++) {
				try {
					Socket socket = new Socket();
					socket.setSoTimeout(QUORUM_N * SOCKET_TIMEOUT_MILISECOND);
					socket.connect(coordinators[i].socket(),
							SOCKET_TIMEOUT_MILISECOND);

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));

					out.print(INSERT2COORDINATOR + "\n" + key + "\n" + value
							+ "\n");
					out.flush();

					// block at here
					String result = reader.readLine();

					if ((null != result) && (0 == result.compareTo(SUCCESS))) {
						success = true;
						Log.d(myid, "insert success from coordinator");
					} else {
						success = false;
						Log.d(myid, "insert fail from coordinator");
					}
					socket.close();
					// if success,ask the successor of the designated
					// coordinator
					if (success)
						break;
				} catch (SocketTimeoutException e) {
					Log.e(myid, "timeout");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(myid, "client socket error");
					e.printStackTrace();
				}
			}

			return success;
		}

		private boolean insert2successors(String key, int version, String value) {
			ArrayList<process> successors = get_successors(myprocess,
					QUORUM_N - 1);
			int success_num = 0;
			for (process p : successors) {
				try {
					Socket socket = new Socket();
					socket.setSoTimeout(SOCKET_TIMEOUT_MILISECOND);
					socket.connect(p.socket(), SOCKET_TIMEOUT_MILISECOND);

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);

					out.print(INSERT2SUCCESSORS + "\n" + key + "\n"
							+ Integer.toString(version) + "\n" + value + "\n");
					out.flush();
					socket.close();

					success_num++;
				} catch (SocketTimeoutException e) {
					Log.e(myid, "timeout");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(myid, "client socket error");
					e.printStackTrace();
				}
			}

			if (success_num >= QUORUM_W - 1)
				return true;
			else
				return false;
		}

		private void successor_recv_insert(String key, int version, String value) {
			ContentValues content = new ContentValues();
			content.put(KEY_FIELD, key);
			content.put(VERSION_FIELD, version);
			content.put(VALUE_FIELD, value);
			insert_local(myuri, content);
			return;
		}

		private boolean coordinator_recv_insert(String key, String value) {
			// check if the key exists in coordinator
			Cursor cursor;
			int version;
			ContentValues content = new ContentValues();

			cursor = local_query(myuri, null, key, null, null);
			if (0 == cursor.getCount()) {

				version = 0;
				content.put(KEY_FIELD, key);
				content.put(VERSION_FIELD, version);
				content.put(VALUE_FIELD, value);
				insert_local(myuri, content);
			} else {
				cursor.moveToNext();
				version = cursor.getInt((cursor.getColumnIndex(VERSION_FIELD)));
				version++;
			}
			cursor.close();
			content.put(KEY_FIELD, key);
			content.put(VERSION_FIELD, version);
			content.put(VALUE_FIELD, value);
			insert_local(myuri, content);

			return insert2successors(key, version, value);
		}

		private void recv_recovery(String key, int version, String value) {
			// check if the key exists in coordinator
			Cursor cursor;
			int old_version;
			boolean update = false;
			ContentValues content = new ContentValues();

			cursor = local_query(myuri, null, key, null, null);
			if (0 == cursor.getCount()) {

				version = 0;
				update = true;
			} else {
				cursor.moveToNext();
				old_version = cursor.getInt((cursor
						.getColumnIndex(VERSION_FIELD)));
				if (old_version < version) {
					update = true;
				}
			}
			cursor.close();

			if (true == update) {
				content.put(KEY_FIELD, key);
				content.put(VERSION_FIELD, version);
				content.put(VALUE_FIELD, value);
				insert_local(myuri, content);
			}

			return;
		}

		private String coordinator_recv_query(String key) {
			Cursor cursor;
			int version, ret_version = -1;
			String value = null, ret_value = null;

			cursor = local_query(myuri, null, key, null, null);
			if (0 != cursor.getCount()) {
				cursor.moveToNext();
				version = cursor.getInt((cursor.getColumnIndex(VERSION_FIELD)));
				value = cursor.getString((cursor.getColumnIndex(VALUE_FIELD)));
				if (ret_version < version) {
					ret_version = version;
					ret_value = value;
				}
			}
			cursor.close();

			// query to the successors
			ArrayList<process> successors = get_successors(myprocess,
					QUORUM_N - 1);
			int success_num = 0;
			for (process p : successors) {
				try {
					Socket socket = new Socket();
					socket.setSoTimeout(SOCKET_TIMEOUT_MILISECOND);
					socket.connect(p.socket(), SOCKET_TIMEOUT_MILISECOND);

					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));

					out.print(QUERY2SUCCESSORS + "\n" + key + "\n");
					out.flush();

					String result;
					result = in.readLine();
					if (null != result) { //the peer still alive
						version = Integer.parseInt(result);
						value = in.readLine();					

						if (ret_version < version) {
							ret_version = version;
							ret_value = value;
						}

						if (version >= 0) // real exist in that successor, if
							success_num++; // version == -1, not exist
					}
					socket.close();

				} catch (SocketTimeoutException e) {
					Log.e(myid, "timeout");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(myid, "client socket error");
					e.printStackTrace();
				}
			}

			if (success_num >= (QUORUM_R - 1))
				return ret_value;
			else
				return null;
		}

		private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
			protected Void doInBackground(ServerSocket... sockets) {
				ServerSocket serverSocket = sockets[0];
				Socket socket;
				String protocol = null;

				try {
					while (true) {
						socket = serverSocket.accept();
						BufferedReader in = new BufferedReader(
								new InputStreamReader(socket.getInputStream()));
						PrintWriter out = new PrintWriter(
								socket.getOutputStream(), true);

						if (null != (protocol = in.readLine())) {
							if (0 == protocol.compareTo(INSERT2COORDINATOR)) {
								Log.d(myid, INSERT2COORDINATOR);
								String key = in.readLine();
								String value = in.readLine();
								boolean ret = coordinator_recv_insert(key,
										value);
								if (true == ret) {
									Log.d(myid, INSERT2COORDINATOR + SUCCESS);
									out.print(SUCCESS + "\n");
								} else {
									Log.d(myid, INSERT2COORDINATOR + FAIL);
									out.print(FAIL + "\n");
								}
								out.flush();
							} else if (0 == protocol
									.compareTo(INSERT2SUCCESSORS)) {
								Log.d(myid, INSERT2SUCCESSORS);
								String key = in.readLine();
								int version = Integer.parseInt(in.readLine());
								String value = in.readLine();
								successor_recv_insert(key, version, value);
							} else if (0 == protocol
									.compareTo(QUERY2COORDINATOR)) {
								Log.d(myid, "recv " + QUERY2COORDINATOR);
								String key = in.readLine();
								Log.d(myid, "key is " + key);
								String value = coordinator_recv_query(key);
								if (null != value) {
									Log.d(myid, "found " + value);
									out.print(SUCCESS + "\n");
									out.print(value + "\n");
								} else {
									Log.d(myid, "not found " + value);
									out.print(FAIL + "\n");
								}
								out.flush();
							} else if (0 == protocol
									.compareTo(QUERY2SUCCESSORS)) {
								Log.d(myid, "recv " + QUERY2SUCCESSORS);
								String key = in.readLine();
								Log.d(myid, "key is " + key);

								Cursor cursor;
								int version;
								String value = null;
								cursor = local_query(myuri, null, key, null,
										null);
								if (0 != cursor.getCount()) {
									cursor.moveToNext();
									version = cursor.getInt((cursor
											.getColumnIndex(VERSION_FIELD)));
									value = cursor.getString((cursor
											.getColumnIndex(VALUE_FIELD)));
								} else {
									version = -1;
									value = "not found";
								}
								cursor.close();

								out.print(version + "\n");
								out.print(value + "\n");
								out.flush();
							} else if (0 == protocol
									.compareTo(RECOVERY_REQUEST)) {
								Log.d(myid, "recv " + RECOVERY_REQUEST);

								Cursor cursor;
								String key;
								int version;
								String value = null;
								cursor = local_query(myuri, null, null, null,
										null);
								if (0 != cursor.getCount()) {
									while (cursor.moveToNext()) {
										key = cursor.getString((cursor
												.getColumnIndex(KEY_FIELD)));
										version = cursor
												.getInt((cursor
														.getColumnIndex(VERSION_FIELD)));
										value = cursor.getString((cursor
												.getColumnIndex(VALUE_FIELD)));

										out.print(key + "\n");
										out.print(version + "\n");
										out.print(value + "\n");
									}
								}

								cursor.close();

								out.flush();
							} else {
								// throw exception
								Log.e("ServerTask.doInBackground",
										"wrong protocol");
							}
						}
						socket.close();
					}
				} catch (IOException e) {
					Log.e("ServerTask::doInBackground",
							"server socket accept error");
					e.printStackTrace();
				}

				return null;
			}

			protected void onProgressUpdate(String... strings) {
				return;
			}
		}

	}
}
