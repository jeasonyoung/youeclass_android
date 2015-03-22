package com.youeclass.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {
	public static final String DATABASENAME = "eschool.db";
	public static final int VERSION = 1; 
	public static int openedNum = 0;
	public MyDBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, DATABASENAME, null, VERSION);
		// TODO Auto-generated constructor stub
	}
	public MyDBHelper(Context context)
	{
		super(context, DATABASENAME, null, VERSION);
	}
	//建表
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		//this.db = db;
		db.execSQL("CREATE TABLE ClassTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,CLASSID TEXT,CLASSNAME TEXT,USERNAME TEXT,FATHERCLASSID TEXT,CLASSTYPE TEXT)");
		db.execSQL("CREATE TABLE UserTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,UID TEXT,USERNAME TEXT,PASSWORD TEXT,NICKNAME TEXT)");
		db.execSQL("CREATE TABLE CourseTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,COURSEID TEXT,COURSENAME TEXT,CLASSID TEXT,COURSETYPE TEXT,COURSEMODE TEXT,COURSEGROUP TEXT,FILESIZE INTEGER,FINISHSIZE INTEGER,FILEPATH TEXT,FILEURL TEXT,STATE INTEGER,USERNAME TEXT)");
		db.execSQL("CREATE TABLE DownloadTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,THREAD_ID INTEGER,START_POS INTEGER,END_POS INTEGER,COMPLETE_SIZE INTEGER,URL TEXT,USERNAME TEXT)");
		db.execSQL("CREATE TABLE PlayrecordTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,COURSEID TEXT,PLAYTIME DATETIME DEFAULT (datetime('now','localtime')),CURRENTTIME INTEGER,USERNAME TEXT)");
		db.execSQL("CREATE TABLE ExamPaperTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,PAPERID TEXT,PAPERNAME TEXT, PAPERTIME INTEGER ,PAPERSCORE INTEGER ,COURSEID TEXT,EXAMID TEXT)");
		db.execSQL("CREATE TABLE ExamRuleTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,RULEID TEXT,PAPERID TEXT,RULETITLE TEXT,RULETITLEINFO TEXT,RULETYPE TEXT,QUESTIONNUM INTEGER,SCOREFOREACH FLOAT,SCORESET TEXT,ORDERINPAPER INTEGER)");
		db.execSQL("CREATE TABLE ExamQuestionTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,QID TEXT,RULEID TEXT,PAPERID TEXT,EXAMID TEXT,CONTENT TEXT,ANSWER TEXT,ANALYSIS TEXT,QTYPE TEXT,OPTIONNUM INTEGER,ORDERID INTEGER,LINKQID TEXT)");
		db.execSQL("CREATE TABLE ExamRecordTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,PAPERID TEXT,USERNAME TEXT,SCORE FLOAT,LASTTIME DATETIME DEFAULT (datetime('now','localtime')),USETIME INTEGER,TEMPTIME INTEGER,ANSWERS TEXT,TEMPANSWER TEXT,ISDONE TEXT)");
		db.execSQL("CREATE TABLE ExamErrorQuestionTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,QID TEXT,PAPERID TEXT,EXAMID TEXT,ERRORNUM INTEGER,USERNAME TEXT)");
		db.execSQL("CREATE TABLE ExamNoteTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,QID TEXT,PAPERID TEXT,EXAMID TEXT,CONTENT TEXT,ADDTIME DATETIME DEFAULT (datetime('now','localtime')),USERNAME TEXT)");
		db.execSQL("CREATE TABLE ExamFavorTab(_ID INTEGER PRIMARY KEY AUTOINCREMENT,QID TEXT,PAPERID TEXT,EXAMID TEXT,USERNAME TEXT)");
	}
	//升级
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE  IF EXISTS ClassTab");
		db.execSQL("DROP TABLE  IF EXISTS UserTab");
		db.execSQL("DROP TABLE  IF EXISTS CourseTab");
		db.execSQL("DROP TABLE  IF EXISTS DownloadTab");
		db.execSQL("DROP TABLE  IF EXISTS PlayrecordTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamPaperTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamRuleTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamQuestionTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamErrorQuestionTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamRecordTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamNoteTab");
		db.execSQL("DROP TABLE  IF EXISTS ExamFavorTab");
		onCreate(db);
	}
	private SQLiteDatabase db;
//	public long insert(String Table_Name, ContentValues values) {
//		db = getDatabase(WRITE);
//		return db.insert(Table_Name, null, values);
//	}
//	/**
//	 * 
//	 * @param Table_Name
//	 * @param id
//	 * @return 影响行数
//	 */
//	public int delete(String Table_Name, int id) {
//		db = getDatabase(WRITE);
//		return db.delete(Table_Name, BaseColumns._ID + "=?",
//				new String[] { String.valueOf(id) });
//	}
//
//	/**
//	 * @param Table_Name
//	 * @param values
//	 * @param WhereClause
//	 * @param whereArgs
//	 * @return 影响行数
//	 */
//	public int update(String Table_Name, ContentValues values,
//			String WhereClause, String[] whereArgs) {
//		db = getDatabase(WRITE);
//		return db.update(Table_Name, values, WhereClause, whereArgs);
//	}
//
//	public Cursor query(String Table_Name, String[] columns, String whereStr,
//			String[] whereArgs) {
//		db = getDatabase(READ);
//		return db.query(Table_Name, columns, whereStr, whereArgs, null, null,
//				null);
//	}
//
//	public Cursor rawQuery(String sql, String[] args) {
//		db = getDatabase(READ);
//		return db.rawQuery(sql, args);
//	}
//
//	public void ExecSQL(String sql) {
//		db = getDatabase(WRITE);
//		db.execSQL(sql);
//	}
//	public void ExecSQL(String sql ,String[] values)
//	{
//		db = getDatabase(WRITE);
//		db.execSQL(sql,values);
//	}

	public synchronized void closeDb() {
		//一开始就没有开
		if(openedNum==0)
		{
			if(db!=null)
			{
				db.close();
				db = null;
			}
			return;
		}
		openedNum--;
		if(openedNum==0)
		{
			if (db != null) {
				db.close();
				db=null;
			}
		}
	}
	public synchronized SQLiteDatabase getDatabase(int i)
	{
		if(db == null)
		{
			switch(i){
			case 0:db= getReadableDatabase();break;
			case 1:db= getWritableDatabase();break;
			}
		}
		openedNum++;
		return db;
	}
	public static final int READ = 0;
	public static final int WRITE= 1;
	
}
