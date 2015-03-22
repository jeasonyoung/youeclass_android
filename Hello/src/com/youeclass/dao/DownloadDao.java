package com.youeclass.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.youeclass.db.MyDBHelper;
import com.youeclass.entity.DownloadItem;

public class DownloadDao {
	private MyDBHelper openHelper;

	public DownloadDao(Context context) {
		openHelper = new MyDBHelper(context);
	}
	public List<DownloadItem> findByUrl(String url,String username)
	{
//		System.out.println(url+" "+username );
		List<DownloadItem> list = new ArrayList<DownloadItem>();
		SQLiteDatabase db = openHelper.getDatabase(MyDBHelper.READ);
		Cursor cursor = db.rawQuery("select thread_id,start_pos,end_pos,complete_size,url from DownloadTab where url =? and username = ?", new String[]{url,username});
		while(cursor.moveToNext())
		{
			DownloadItem loader = new DownloadItem(cursor.getInt(0),cursor.getInt(1),cursor.getInt(2),cursor.getInt(3),cursor.getString(4),username);
			list.add(loader);
		}
		cursor.close();
		openHelper.closeDb();
		System.out.println(list);
		return list;
	}
	/**
	 * 保存每条线程已经下载的文件长度
	 * @param path
	 * @param map
	 */
	public void save(List<DownloadItem> loaders)
	{
		SQLiteDatabase db =  openHelper.getDatabase(MyDBHelper.WRITE);
		db.beginTransaction();
		try{
			for(DownloadItem l:loaders)
			{
				db.execSQL("insert into DownloadTab(thread_id,start_pos,end_pos,complete_size,url,username)values(?,?,?,?,?,?)",
						new Object[]{l.getThreadId(),l.getStartPos(),l.getEndPos(),l.getCompleteSize(),l.getUrl(),l.getUsername()});
			}
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		openHelper.closeDb();
	}
	/**
	 * 实时更新每条线程已经下载的文件长度
	 * @param path
	 * @param map
	 */
	public synchronized void update(DownloadItem l)
	{
		SQLiteDatabase db =  openHelper.getDatabase(MyDBHelper.WRITE);
		db.beginTransaction();
		try{
			db.execSQL("update DownloadTab set complete_size=? where url=? and thread_id = ? and username =?",
					new Object[]{l.getCompleteSize(),l.getUrl(),l.getThreadId(),l.getUsername()});
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		openHelper.closeDb();
	}
	/**
	 * 当文件下载完成后，删除对应的下载记录,更新课程表记录
	 * @param path
	 */
	public void deleteAll(String url,int size,String filePath,String username)
	{
		Log.i("Download","删除下载记录");
		SQLiteDatabase db =  openHelper.getDatabase(MyDBHelper.WRITE);
		db.beginTransaction();
		try
		{
			//删除记录
			db.execSQL("delete from DownloadTab where url=? and username = ?", new Object[]{url,username });
			//更新记录
			db.execSQL("update CourseTab set finishsize = ?,filepath = ? ,state = 2 where fileurl = ? and username = ?", new Object[]{size,filePath,url,username});
			db.setTransactionSuccessful();
		}finally
		{
			db.endTransaction();
		}
		openHelper.closeDb();
	}
	public void deleteAll(String url,String username)
	{
		SQLiteDatabase db =  openHelper.getDatabase(MyDBHelper.WRITE);
		db.beginTransaction();
		try
		{
			//删除记录
			db.execSQL("delete from DownloadTab where url=? and username = ?", new Object[]{url,username});
			db.setTransactionSuccessful();
		}finally
		{
			db.endTransaction();
		}
		openHelper.closeDb();
	}
	public int getDownLength(String url,String username) {
		// TODO Auto-generated method stub
		List<DownloadItem> list = findByUrl(url,username);
		int length = 0;
		for(DownloadItem l:list)
		{
			length+=l.getCompleteSize();
		}
		return length;
	}
	public void updateCourse(String downloadUrl, int downloadSize,String filepath,String username) {
		// TODO Auto-generated method stub
			SQLiteDatabase db =  openHelper.getDatabase(MyDBHelper.WRITE);
			db.beginTransaction();
			try
			{
				db.execSQL("update CourseTab set finishsize = ?,filepath = ? where fileurl = ? and username =? ", new Object[]{downloadSize,filepath,downloadUrl,username});
				db.setTransactionSuccessful();
			}finally
			{
				db.endTransaction();
			}
			openHelper.closeDb();
		}
}
