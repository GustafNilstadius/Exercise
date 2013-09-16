package edu.buffalo.cse.cse486586.simpledht;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 *
 * http://code.google.com/p/openmobster/wiki/ContentProvider
 */
public class DBHelper extends SQLiteOpenHelper 
{
	SQLiteDatabase db;
	
	public DBHelper(Context context, String name, CursorFactory factory, int version) 
	{
		super(context, name, factory, version);
		 db = getWritableDatabase();
		 onCreate(db);
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		
			//Name of the table to be created
			String tableName = "content_provider_tutorial";
			Log.e("debugging","enter DBHelper.onCreate");
			
			String tableSql = "DROP TABLE IF EXISTS " + tableName;
			db.execSQL(tableSql);
			// Create a table
			tableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
					+  "key TEXT PRIMARY KEY, " + "value TEXT"
					+ ");";
			db.execSQL(tableSql);
			
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{	
	}
	
	
}

