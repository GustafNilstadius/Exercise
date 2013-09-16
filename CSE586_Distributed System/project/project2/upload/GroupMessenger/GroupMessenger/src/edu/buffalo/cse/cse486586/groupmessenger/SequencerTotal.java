package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

class process {
	public String ip;
	public int port;

	public process(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
}

public class SequencerTotal {
	final int SERVER_PORT = 10000;
	final String PROT_MSG = "message";
	final String PROT_ORDER = "order";

	private process own;
	public String myId;
	private ArrayList<process> group;
	private process sequencer;
	private boolean isleader = false;
	private int local_no = 0;
	private int sg_no = 0;
	private int rg_no = 0;

	private HashMap<String, String> hold_back_queue;
	private HashMap<String, Integer> order_queue;
	private TextView mTextView;

	public SequencerTotal(process own, String myId, ArrayList<process> group,
			process sequencer, boolean isleader, TextView textView) {
		this.own = own;
		this.myId = myId;
		this.group = group;
		this.sequencer = sequencer;
		this.isleader = isleader;
		this.hold_back_queue = new HashMap<String, String>();
		this.order_queue = new HashMap<String, Integer>();
		mTextView = textView;

		ServerSocket welcomeSocket;

		try {

			welcomeSocket = new ServerSocket(SERVER_PORT);
			Log.d("onCreate", "build server success");
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					welcomeSocket);
		} catch (IOException e) {
			Log.e("MainActivity", "create server socket error");
			e.printStackTrace();
		}

		return;
	}

	public void to_multicast(String msg) {
		int base = 0;
		if (0 == myId.compareTo("avd0")) {
			base = 10000;
		} else if (0 == myId.compareTo("avd1")) {
			base = 20000;
		} else if (0 == myId.compareTo("avd2")) {
			base = 30000;
		}

		// String identifier ="ident "+ myId + ":" +
		// Integer.toString(local_no)+" ";
		String identifier = "ident" + Integer.toString(base + local_no) + " ";
		local_no++;

		B_mutlicast(PROT_MSG + "\n" + identifier + "\n" + msg, group);

	}

	private void B_mutlicast(String msg, ArrayList<process> group) {
		try {
			for (process p : group) {
				Socket socket = new Socket(p.ip, p.port);
				// Log.d("B_mutlicast", "client socket build success");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream())), true);
				out.print(msg);
				out.flush();
				socket.close();
			}
		} catch (IOException e) {
			Log.e("B_mutlicast", "client socket error");
			e.printStackTrace();
		}
		return;
	}

	public void to_deliver(String msg) {
		// to application

		if (msg.startsWith("send")) {
			msg = msg.substring(new String("send").length());
			mTextView.append(msg + "\n");
		} else if (msg.startsWith("test1")) {
			msg = msg.substring(new String("test1").length());
			mTextView.append(msg + "\n");
		} else if (msg.startsWith("test2")) {
			msg = msg.substring(new String("test2").length());

			String displaymsg;
			displaymsg = msg.substring(new String("0").length());
			mTextView.append(displaymsg + "\n");

			int i = 1;
			String message;
			if (msg.startsWith("0")) {

				for (i = 1; i <= 2; i++) {

					message = "test2" + Integer.toString(i) + myId + ":"
							+ Integer.toString(i);
					Log.d("sendMessage", message);

					OnTest2ClickListener Test2Listen = GroupMessengerActivity.Test2Listener;
					Test2Listen.new ClientTask().execute(message);
					// to_multicast(message);
				}
			}

		}

	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		protected Void doInBackground(ServerSocket... sockets) {
			String protocol = null;
			ServerSocket serverSocket = sockets[0];
			Socket socket;
			String identifier, msg;

			try {
				while (true) {
					socket = serverSocket.accept();
					// Log.d("ServerTask::doInBackground", "new client");
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					if (null != (protocol = in.readLine())) {
						if (0 == protocol.compareTo(PROT_MSG)) {
							identifier = in.readLine();
							msg = in.readLine();

							Integer sg_int;
							sg_int = order_queue.get(identifier);
							if (null != sg_int) {

								Log.d(myId + "recv PROT_MSG, sg_int",
										identifier + msg + "sg:"
												+ Integer.toString(sg_int)
												+ "rg: "
												+ Integer.toString(rg_no));

							} else {
								Log.d(myId + "recv PROT_MSG",
										"not found in order queue:"
												+ identifier + msg);
							}

							if (null != (sg_int = (order_queue.get(identifier)))
									&& (rg_no == sg_int)) {
								Log.d(myId + "send to UI", identifier + msg);
								publishProgress(msg);
								order_queue.remove(identifier);
								Log.d(myId + "recv PROT_MSG,", identifier + msg
										+ "is removed from order_queue");
								rg_no++;
								// after rg_no increase,it is possible that we can find from order queue by updated rg_no
								while (true) {
									boolean rg_change = false;
									Iterator entries = order_queue.entrySet()
											.iterator();
									while (entries.hasNext()) {
										Map.Entry entry = (Map.Entry) entries
												.next();
										String identifier_l = (String) entry
												.getKey();
										Integer sg_l = (Integer) entry
												.getValue();
										if (sg_l == rg_no) {
											String msg_l = hold_back_queue
													.get(identifier_l);
											if (null != msg_l) {
												publishProgress(msg);
												entries.remove();
												hold_back_queue
														.remove(identifier_l);
												rg_no++;
												rg_change = true;
											}
										}
									}
									if (false == rg_change)
										break;
								}
							} else {
								hold_back_queue.put(identifier, msg);
								Log.d(myId + "recv PROT_MSG,", identifier + msg
										+ "is added to hold_back_queue");
							}

							if (isleader) {
								// ???should upto UI thread, then new a
								// clientTask from UI???
								Log.d(myId
										+ "recv PROT_MSG isleader send oder sg_no",
										Integer.toString(sg_no));
								B_mutlicast(PROT_ORDER + "\n" + identifier
										+ "\n" + Integer.toString(sg_no), group);

								sg_no++;
								
							}
						} else if (0 == protocol.compareTo(PROT_ORDER)) {
							String sg;
							identifier = in.readLine();
							sg = in.readLine();

							msg = hold_back_queue.get(identifier);
							if (null != msg) {
								Log.d(myId + "recv PROT_ORDER, sg_int",
										identifier + msg + "sg:" + sg + "rg: "
												+ Integer.toString(rg_no));
							} else {
								Log.d(myId + "recv PROT_ORDER,",
										"not found in hold_back_queue:"
												+ identifier);
							}

							if ((null != msg)
									&& (rg_no == Integer.parseInt(sg))) {

								Log.d(myId + "recv PROT_ORDER,", identifier
										+ msg
										+ "is removed from hold_back_queue");
								hold_back_queue.remove(identifier);
								Log.d(myId + "send to UI", identifier + msg);
								publishProgress(msg);
								rg_no++;
								// after rg_no increase,it is possible that we can find from order queue by updated rg_no
								while (true) {
									boolean rg_change = false;
									Iterator entries = order_queue.entrySet()
											.iterator();
									while (entries.hasNext()) {
										Map.Entry entry = (Map.Entry) entries
												.next();
										String identifier_l = (String) entry
												.getKey();
										Integer sg_l = (Integer) entry
												.getValue();
										if (sg_l == rg_no) {
											String msg_l = hold_back_queue
													.get(identifier_l);
											if (null != msg_l) {
												publishProgress(msg);
												entries.remove();
												hold_back_queue
														.remove(identifier_l);
												rg_no++;
												rg_change = true;
											}
										}
									}
									if (false == rg_change)
										break;
								}
							} else {

								Log.d(myId + "recv PROT_ORDER,", identifier
										+ "sg " + sg
										+ " is added to order_queue");
								order_queue
										.put(identifier, Integer.valueOf(sg));
							}

						} else {
							// throw exception
							Log.e(myId + "ServerTask.doInBackground",
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
			// callback.callbackCall(strings[0]);
			to_deliver(strings[0]);
			return;
		}
	}
}
