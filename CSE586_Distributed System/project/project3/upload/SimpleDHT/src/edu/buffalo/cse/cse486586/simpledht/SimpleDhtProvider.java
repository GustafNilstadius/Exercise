package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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

public class SimpleDhtProvider extends ContentProvider {
	public static final String CONTENT_URI = "content://edu.buffalo.cse.cse486586.simpledht.provider";
	public static final String AUTHORITY = "edu.buffalo.cse.cse486586.simpledht.provider";
	public static final String SCHEME = "content";
	Uri myuri;
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String VALUE = "value";
	public static final String TABLE = "content_provider_tutorial";

	public static final int MATCH_ALL = 1;
	public static final int MATCH_ID = 2;

	private SQLiteOpenHelper sqliteOpenHelper;
	private UriMatcher uriMatcher;

	DhtAlgorithm dht;
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
			if (dht.is_local(key))
				return insert_local(uri, values);
			else {
				dht.insert2remote(key, value);
				return null;
			}
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
		long newId = db.insertWithOnConflict(TABLE, null, values, 5);

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

		dht = new DhtAlgorithm(this.getContext());
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
				if (0 == projection[0].compareTo("local")) {
					return local_query(uri, null, selection, selectionArgs,
							sortOrder);
				} else if (0 == projection[0].compareTo("global")) {
					// get local
					// forward the message
					// wait for response
					HashMap<String, String> map = dht.query_global();
					MatrixCursor c = new MatrixCursor(new String[] { KEY_FIELD,
							VALUE_FIELD });
					for (Map.Entry<String, String> entry : map.entrySet()) {
						c.newRow().add(entry.getKey()).add(entry.getValue());
					}
					return c;
				}
			} else if (dht.is_local(selection)) {
				return local_query(uri, projection, selection, selectionArgs,
						sortOrder);
			} else {
				// the key in the remote machine
				String value = dht.query_single(dht.saddr, selection);
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
		return null;
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

	protected class DhtAlgorithm {
		final String PROT_JOIN = "join";
		final String UPDATE_PRED = "update_pred";
		final String UPDATE_SUCC = "update_succ";
		final String INSERT = "insert";
		final String QUERY_SINGLE_REQUEST = "query_single_request";
		final String QUERY_SINGLE_REPLY = "query_single_reply";
		final String QUERY_GLOBAL_REQUEST = "query_global_request";
		final String QUERY_GLOBAL_REQUEST_END = "query_global_request_end";
		final String QUERY_GLOBAL_REPLY = "query_global_reply";
		final String FOUND = "found";
		final String NOTFOUND = "not found";

		final String IPADDR = "10.0.2.2";
		InetSocketAddress leader_addr;
		public InetSocketAddress pred_sadr, succ_sadr;
		public InetSocketAddress saddr;
		public String mynodeid;
		public String pred_nodeid;

		private TreeMap<String, InetSocketAddress> RingTop;
		private boolean isLeader = false;

		public boolean orign_receive_query_single = false;
		public boolean orign_receive_query_global_all = false;
		public String query_single_value;
		public HashMap<String, String> global_query_result;

		public DhtAlgorithm(Context context) {
			RingTop = new TreeMap<String, InetSocketAddress>();
			final int SERVER_PORT = 10000;

			leader_addr = new InetSocketAddress(IPADDR, 11108);
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
			String portStr = tel.getLine1Number().substring(
					tel.getLine1Number().length() - 4);

			if (0 == portStr.compareTo("5554")) {
				isLeader = true;
				saddr = new InetSocketAddress(IPADDR, 11108);

			} else if (0 == portStr.compareTo("5556")) {
				saddr = new InetSocketAddress(IPADDR, 11112);

			} else if (0 == portStr.compareTo("5558")) {
				saddr = new InetSocketAddress(IPADDR, 11116);

			} else {
				portStr = null;
				saddr = null;
				Log.e("MainActivity", "Avd wrong");

			}
			if (null != portStr) {
				try {
					mynodeid = genHash(portStr);
					Log.d(portStr, mynodeid);
					if (!isLeader) {
						join(mynodeid, saddr);
					} else {
						RingTop.put(mynodeid, saddr);
					}
					pred_nodeid = mynodeid;
					pred_sadr = saddr;
					succ_sadr = saddr;

				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			global_query_result = new HashMap<String, String>();
			return;
		}

		public boolean is_local(String key) {
			String hashedkey;
			boolean local = false;
			int i = 0;
			try {
				hashedkey = genHash(key);
				if (0 == mynodeid.compareTo(pred_nodeid)) // only one node //
															// node
				{
					local= true;
				} else if (mynodeid.compareTo(pred_nodeid) < 0) // the node is
																// at the
																// beginning
				{
					if (mynodeid.compareTo(hashedkey) >= 0) {
						local= true;
					} else if ((i = hashedkey.compareTo(pred_nodeid)) > 0) {
						local = true;
					} else {
						local= false;
					}
				} else {
					if ((mynodeid.compareTo(hashedkey) >= 0)
							&& (hashedkey.compareTo(pred_nodeid) > 0)) {
						local= true;
					}
				}
				Log.d("is_local mynodeid:",mynodeid);
				Log.d("is_local pred_nodeid:",pred_nodeid);
				if(local)
					Log.d("is_local",key+" "+hashedkey + " local " + "true");
				else
					Log.d("is_local",key+" "+hashedkey + " local " + "false");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			return local;

		}

		public void insert2remote(String key, String value) {
			try {
				// Socket socket = new
				// Socket(succ_sadr.getHostName(),succ_sadr.getPort());
				Socket socket = new Socket(succ_sadr.getAddress()
						.getHostAddress(), succ_sadr.getPort());
				// Log.d("B_mutlicast", "client socket build success");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())), true);
				out.print(INSERT + "\n" + key + "\n" + value);
				out.flush();
				socket.close();
			} catch (IOException e) {
				Log.e("send join", "client socket error");
				e.printStackTrace();
			}

			return;

		}
/*
		public HashMap<String, String> query_global() {
			global_query_result.clear();
			Cursor cursor = local_query(myuri, null, null, null, null);

			if (cursor != null) {
				Log.d("query_global", Integer.toString(cursor.getCount()));
				while (cursor.moveToNext()) {
					// Extract data.
					global_query_result.put(cursor.getString(cursor
							.getColumnIndex(KEY_FIELD)), cursor
							.getString(cursor.getColumnIndex(VALUE_FIELD)));
				}
				cursor.close();
			}

			if (!succ_sadr.equals(saddr)) {
				try {
					Socket socket = new Socket(succ_sadr.getAddress(),
							succ_sadr.getPort());
					// Log.d("B_mutlicast", "client socket build success");
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.print(QUERY_GLOBAL_REQUEST + "\n"
							+ saddr.getAddress().getHostAddress() + "\n"
							+ saddr.getPort());
					out.flush();
					socket.close();
				} catch (IOException e) {
					Log.e("query_global", "client socket error");
					e.printStackTrace();
				}
				orign_receive_query_global_all = false;
				while (!orign_receive_query_global_all) {
				}
			}

			return global_query_result;
		}*/

		public HashMap<String, String> query_global() {
			global_query_result.clear();
			recv_global_request(saddr);
			return global_query_result;
		}
		private void recv_global_request(InetSocketAddress org_saddr) {
			Socket socket;
			PrintWriter out;

			Cursor cursor = local_query(myuri, null, null, null, null);

			if (cursor != null) {
				if (0 != cursor.getCount()) {
					try {
						socket = new Socket(org_saddr.getAddress(),
								org_saddr.getPort());
						// Log.d("B_mutlicast", "client socket build success");
						out = new PrintWriter(
								new BufferedWriter(new OutputStreamWriter(
										socket.getOutputStream())), true);
						out.print(QUERY_GLOBAL_REPLY + "\n");

						while (cursor.moveToNext()) {
							out.print(cursor.getString(cursor
									.getColumnIndex(KEY_FIELD)) + "\n");
							out.print(cursor.getString(cursor
									.getColumnIndex(VALUE_FIELD)) + "\n");
						}
						out.flush();
						socket.close();
					} catch (IOException e) {
						Log.e("query_global", "client socket error");
						e.printStackTrace();
					}
				}
				cursor.close();
			}	

			//forward or end
			try {
				socket = new Socket(succ_sadr.getAddress(), succ_sadr.getPort());
				// Log.d("B_mutlicast", "client socket build success");
				out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())), true);
				if (!succ_sadr.equals(org_saddr)) {
					// relay
					out.print(QUERY_GLOBAL_REQUEST + "\n"
							+ org_saddr.getAddress().getHostAddress() + "\n"
							+ org_saddr.getPort());
				} else {
					// game over
					out.print(QUERY_GLOBAL_REQUEST_END + "\n");
				}
				out.flush();
				socket.close();

				
			} catch (IOException e) {
				Log.e("query_global", "client socket error");
				e.printStackTrace();
			}
			
			// first node
			if (saddr.equals(org_saddr)) {
				orign_receive_query_global_all = false;
				while (!orign_receive_query_global_all) {
				}
				
			}

		}

		public String query_single(InetSocketAddress org_saddr, String key) {
			try {
				Socket socket = new Socket(succ_sadr.getAddress(),
						succ_sadr.getPort());
				// Log.d("B_mutlicast", "client socket build success");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())), true);
				out.print(QUERY_SINGLE_REQUEST + "\n"
						+ org_saddr.getAddress().getHostAddress() + "\n"
						+ org_saddr.getPort() + "\n" + key);
				out.flush();
				socket.close();
			} catch (IOException e) {
				Log.e("send join", "client socket error");
				e.printStackTrace();
			}

			if (org_saddr.getPort() == saddr.getPort()) {
				orign_receive_query_single = false;
				while (!orign_receive_query_single) {
				}
				
				return query_single_value;
			}

			return null;
		}

		private void recv_query_single(InetSocketAddress org_saddr, String key) {
			Cursor cursor;
			boolean found = false;
			String value;
			if (is_local(key)) {
				cursor = local_query(myuri, null, key, null, null);
				if (0 == cursor.getCount()) {
					found = false;
					value = null;
				} else {
					found = true;
					cursor.moveToNext();
					value = cursor
							.getString(cursor.getColumnIndex(VALUE_FIELD));
					cursor.close();
				}

				{
					// send back to the origin
					try {
						Socket socket = new Socket(org_saddr.getAddress(),
								org_saddr.getPort());
						// Log.d("B_mutlicast", "client socket build success");
						PrintWriter out = new PrintWriter(
								new BufferedWriter(new OutputStreamWriter(
										socket.getOutputStream())), true);
						if (found) {
							out.print(QUERY_SINGLE_REPLY + "\n" + FOUND + "\n"
									+ value);
						} else {
							out.print(QUERY_SINGLE_REPLY + "\n" + NOTFOUND);
						}

						out.flush();
						socket.close();
					} catch (IOException e) {
						Log.e("send join", "client socket error");
						e.printStackTrace();
					}
				}

			} else {
				// the key in the remote machine
				// query_single(saddr, key);
				query_single(org_saddr, key);
			}
		}

		private void recv_insert(String key, String value) {
			if (dht.is_local(key)) {
				ContentValues content = new ContentValues();
				content.put(OnTestClickListener.KEY_FIELD, key);
				content.put(OnTestClickListener.VALUE_FIELD, value);
				insert_local(myuri, content);
			} else {
				dht.insert2remote(key, value);
			}
		}

		private void join(String mynodeid, InetSocketAddress myself) {
			// String msg = PROT_JOIN + "\n" + mynodeid + "\n"+
			// myself.getHostName() + "\n" + Integer.toString(myself.getPort());
			String hostaddress = myself.getAddress().getHostAddress();
			String msg = PROT_JOIN + "\n" + mynodeid + "\n"
					+ myself.getAddress().getHostAddress() + "\n"
					+ Integer.toString(myself.getPort());
			new ClientTask().execute(msg);
			return;
		}

		public class ClientTask extends AsyncTask<String, String, Void> {
			protected Void doInBackground(String... msgs) {
				Socket socket;
				try {
					socket = new Socket(leader_addr.getAddress(),
							leader_addr.getPort());

					Log.d("join", "client socket build success");
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.print(msgs[0]);
					out.flush();
					socket.close();
				} catch (IOException e) {
					Log.e("join", "io exception");
					e.printStackTrace();
				}
				return null;
			}
		}

		private void update_pred(InetSocketAddress self,
				InetSocketAddress pred, String pred_node_id) {
			try {
				Socket socket = new Socket(self.getAddress(), self.getPort());
				// Log.d("B_mutlicast", "client socket build success");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())), true);
				// out.print(UPDATE_PRED + "\n" + pred.getHostName() + "\n"+
				// Integer.toString(pred.getPort()) + "\n"+ pred_node_id);
				out.print(UPDATE_PRED + "\n"
						+ pred.getAddress().getHostAddress() + "\n"
						+ Integer.toString(pred.getPort()) + "\n"
						+ pred_node_id);
				out.flush();
				socket.close();
			} catch (IOException e) {
				Log.e("send join", "client socket error");
				e.printStackTrace();
			}
			return;
		}

		private void update_succ(InetSocketAddress self, InetSocketAddress succ) {
			try {
				Socket socket = new Socket(self.getAddress(), self.getPort());
				// Log.d("B_mutlicast", "client socket build success");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())), true);
				out.print(UPDATE_SUCC + "\n" + succ.getHostName() + "\n"
						+ Integer.toString(succ.getPort()));
				out.flush();
				socket.close();
			} catch (IOException e) {
				Log.e("send join", "client socket error");
				e.printStackTrace();
			}
			return;
		}

		private void re_distribute(InetSocketAddress self) {
			return;
		}

		private void receive_join(String node_id, InetSocketAddress saddr) {
			RingTop.put(node_id, saddr);
			String pred_node_id;
			InetSocketAddress pred_saddr, succ_saddr;

			RingTop.put(node_id, saddr);

			Map.Entry<String, InetSocketAddress> map = RingTop
					.lowerEntry(node_id);
			if (null != map) {
				pred_node_id = map.getKey();
				pred_saddr = map.getValue();
			} else {
				map = RingTop.lastEntry();
				pred_node_id = map.getKey();
				pred_saddr = map.getValue();
			}

			map = RingTop.higherEntry(node_id);
			if (null != map) {
				succ_saddr = map.getValue();
			} else {
				map = RingTop.firstEntry();
				succ_saddr = map.getValue();
			}

			// update predecessor and successor of the new node
			update_pred(saddr, pred_saddr, pred_node_id);
			update_succ(saddr, succ_saddr);

			// update successor of the new node's predecessor
			update_succ(pred_saddr, saddr);

			// update predecessor of the new node's successor
			update_pred(succ_saddr, saddr, node_id);

			// redistribute the keys on the new node's successor
			re_distribute(succ_saddr);
		}

		private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

			protected Void doInBackground(ServerSocket... sockets) {
				ServerSocket serverSocket = sockets[0];
				Socket socket;
				String protocol = null;

				try {
					while (true) {
						socket = serverSocket.accept();
						// Log.d("ServerTask::doInBackground", "new client");
						BufferedReader in = new BufferedReader(
								new InputStreamReader(socket.getInputStream()));
						if (null != (protocol = in.readLine())) {
							if (0 == protocol.compareTo(PROT_JOIN)) {
								Log.d("server", PROT_JOIN);
								if (!isLeader) {
									Log.e("ServerTask::doInBackground",
											"none_leader, however receive join");
									throw new IOException();
								}

								String node_id = in.readLine();
								String host_name = in.readLine();
								String port = in.readLine();
								receive_join(node_id, new InetSocketAddress(
										host_name, Integer.parseInt(port)));

							} else if (0 == protocol.compareTo(UPDATE_PRED)) {
								Log.d("server", UPDATE_PRED);
								String host_name = in.readLine();
								String port = in.readLine();
								Log.d("my id:", mynodeid);
								Log.d("old pred id:", mynodeid);
								Log.d("old pred port:",
										Integer.toString(pred_sadr.getPort()));
								pred_nodeid = in.readLine();
								Log.d("new pred id:", pred_nodeid);
								Log.d("new pred port:", port);
								pred_sadr = new InetSocketAddress(host_name,
										Integer.parseInt(port));
							} else if (0 == protocol.compareTo(UPDATE_SUCC)) {
								Log.d("server", UPDATE_SUCC);
								Log.d("my id:", mynodeid);
								Log.d("old succ port:",
										Integer.toString(succ_sadr.getPort()));
								String host_name = in.readLine();
								String port = in.readLine();
								Log.d("new succ port:", port);
								succ_sadr = new InetSocketAddress(host_name,
										Integer.parseInt(port));
							} else if (0 == protocol.compareTo(INSERT)) {
								Log.d("server", INSERT);
								String key = in.readLine();
								String value = in.readLine();
								recv_insert(key, value);
							} else if (0 == protocol
									.compareTo(QUERY_SINGLE_REQUEST)) {
								Log.d("server", QUERY_SINGLE_REQUEST);
								String org_ip = in.readLine();
								String org_port = in.readLine();
								String key = in.readLine();
								recv_query_single(new InetSocketAddress(org_ip,
										Integer.parseInt(org_port)), key);

							} else if (0 == protocol
									.compareTo(QUERY_SINGLE_REPLY)) {
								Log.d("server", QUERY_SINGLE_REPLY);
								String found = in.readLine();
								if (0 == found.compareTo(FOUND)) {
									query_single_value = in.readLine();
									orign_receive_query_single = true;
									
								} else {
									
									orign_receive_query_single = true;
								}
							} else if (0 == protocol
									.compareTo(QUERY_GLOBAL_REQUEST)) {
								Log.d("server", QUERY_GLOBAL_REQUEST);
								String org_ip = in.readLine();
								String org_port = in.readLine();
								recv_global_request(new InetSocketAddress(
										org_ip, Integer.parseInt(org_port)));
							} else if (0 == protocol
									.compareTo(QUERY_GLOBAL_REPLY)) {
								Log.d("server", QUERY_GLOBAL_REPLY);
								String key;
								String value;
								while (null != (key = in.readLine())) {
									value = in.readLine();
									global_query_result.put(key, value);
								}
							} else if (0 == protocol
									.compareTo(QUERY_GLOBAL_REQUEST_END)) {
								Log.d("server", QUERY_GLOBAL_REQUEST_END);
								orign_receive_query_global_all = true;
								
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
