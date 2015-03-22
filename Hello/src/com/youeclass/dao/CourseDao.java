package com.youeclass.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.youeclass.db.MyDBHelper;
import com.youeclass.entity.Course;
import com.youeclass.entity.DowningCourse;

public class CourseDao {
	private MyDBHelper dbhelper;
	private static final String TAG = "CourseDao";

	public CourseDao(Context context) {
		dbhelper = new MyDBHelper(context);
	}

	public List<Course> findByClassId(String id,String username) {
		SQLiteDatabase db = dbhelper.getDatabase(MyDBHelper.READ);
		Log.d(TAG, "findByClassId方法打开了数据库连接");
		List<Course> list = new ArrayList<Course>();
		String sql = "select courseid,coursename,classid,coursetype,coursemode,coursegroup,filesize,finishsize,filepath,fileurl,state from CourseTab where classid = ? and username = ?";
		Cursor cursor = db.rawQuery(sql, new String[] { id,username });
		while (cursor.moveToNext()) {
			Course uc = new Course(cursor.getString(0), cursor.getString(1),
					cursor.getString(2), cursor.getString(3),
					cursor.getString(4), cursor.getString(5), cursor.getInt(6),
					cursor.getInt(7), cursor.getString(8), cursor.getString(9),
					cursor.getInt(10),username);
			list.add(uc);
		}
		cursor.close();
		dbhelper.closeDb();
		Log.d(TAG, "findByClassId方法关闭了数据库连接");
		return list;
	}

	public void save(List<Course> courses,String username) {
		SQLiteDatabase db = dbhelper.getDatabase(1);
		Log.d(TAG, "save方法打开了数据库连接");
		db.beginTransaction();
		try {
			if (courses.size() == 0) {
				return;
			} else {
				// 如果一开始数据库数据为空,直接加
				String sql = "select classid from CourseTab where username = ?";
				Cursor cursor = db.rawQuery(sql, new String[] {username});
				if (cursor.getCount() == 0) {
					cursor.close();
					//循环加
					for (Course c1 : courses) {
						String sql1 = "insert into CourseTab(courseid,coursename,classid,coursetype,coursemode,coursegroup,filesize,finishsize,filepath,fileurl,state,username) values (?,?,?,?,?,?,?,?,?,?,?,?)";
						Object[] values = new Object[] { c1.getCourseId(),
								c1.getCourseName(), c1.getClassid(),
								c1.getCourseType(), c1.getCourseMode(),
								c1.getCourseGroup(), c1.getFilesize(),
								c1.getFinishsize() , c1.getFilePath(),
								c1.getFileUrl(), c1.getState(),c1.getUsername() };
						db.execSQL(sql1, values);
					}
				} else {
					cursor.close();
					for (Course c : courses) {
						String sqleach = "select classid from CourseTab where fileurl = ? and username=?";
						Cursor cursoreach = db.rawQuery(sqleach,
								new String[] { c.getFileUrl(),c.getUsername() });
						if (cursoreach.getCount() > 0) {	//有记录则跳过不再增加
							cursoreach.close();
							continue;
						}
						cursoreach.close();
						String sql1 = "insert into CourseTab(courseid,coursename,classid,coursetype,coursemode,coursegroup,filesize,finishsize,filepath,fileurl,state,username) values (?,?,?,?,?,?,?,?,?,?,?,?)";
						Object[] values = new Object[] { c.getCourseId(),
								c.getCourseName(), c.getClassid(),
								c.getCourseType(), c.getCourseMode(),
								c.getCourseGroup(), c.getFilesize(),
								c.getFinishsize() , c.getFilePath(),
								c.getFileUrl(), c.getState() ,c.getUsername()};
						db.execSQL(sql1, values);
					}
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		dbhelper.closeDb();
		Log.d(TAG, "save方法关闭了数据库连接");
	}

	public void deleteAll(String classid,String username) {
		SQLiteDatabase db = dbhelper.getDatabase(1);
		Log.d(TAG, "deleteAll方法打开了数据库连接");
		db.beginTransaction();
		try {
			String sql = "delete from CourseTab where classid = ? and username = ?";
			db.execSQL(sql, new String[] { classid,username });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		dbhelper.closeDb();
		Log.d(TAG, "deleteAll方法关闭了数据库连接");
	}

	public List<DowningCourse> findAll(String username) {
		List<DowningCourse> list = new ArrayList<DowningCourse>();
		SQLiteDatabase db = dbhelper.getDatabase(0);
		Log.d(TAG, "findAll方法打开了数据库连接");
		String sql = "select coursename,filesize,finishsize,filepath,fileurl from CourseTab where username = ?";
		Cursor cursor = db.rawQuery(sql, new String[]{username});
		while (cursor.moveToNext()) {
			DowningCourse dc = new DowningCourse(cursor.getString(0),
					cursor.getInt(1), cursor.getInt(2), cursor.getString(3),
					cursor.getString(4),username);
			list.add(dc);
		}
		cursor.close();
		dbhelper.closeDb();
		Log.d(TAG, "findAll方法关闭了数据库连接");
		return list;
	}

	public List<DowningCourse> findAllDowning(String username) {
		// TODO Auto-generated method stub
		List<DowningCourse> list = new ArrayList<DowningCourse>();
		SQLiteDatabase db = dbhelper.getDatabase(0);
		// 查找所有正在下载的课程
		String sql = "select coursename,filesize,finishsize,filepath,fileurl from CourseTab where state = 1 and username = ?";
		Cursor cursor = db.rawQuery(sql, new String[] {username});
		while (cursor.moveToNext()) {
			DowningCourse dc = new DowningCourse(cursor.getString(0),
					cursor.getInt(1), cursor.getInt(2), cursor.getString(3),
					cursor.getString(4),username);
			list.add(dc);
		}
		cursor.close();
		dbhelper.closeDb();
		return list;
	}
	public List<Course> findAllDowned(String username)
	{
		List<Course> list = new ArrayList<Course>();
		SQLiteDatabase db = dbhelper.getDatabase(0);
		// 查找所有正在下载的课程
		String sql = "select courseid,coursename,filepath,fileurl from CourseTab where state = 2 and username = ?";
		Cursor cursor = db.rawQuery(sql, new String[] {username});
		while (cursor.moveToNext()) {
			Course c = new Course();
			c.setCourseId(cursor.getString(0));
			c.setCourseName(cursor.getString(1));
			c.setFilePath(cursor.getString(2));
			c.setFileUrl(cursor.getString(3));
			c.setUsername(username);
			list.add(c);
		}
		cursor.close();
		dbhelper.closeDb();
		return list;
	}
	public void updateState(String url, int state , String username) {
		System.out.println("更新文件下载状态:"+state);
		SQLiteDatabase db = dbhelper.getDatabase(0);
		db.beginTransaction();
		try {
			String sql = "update CourseTab set state = ? where fileurl = ? and username = ?";
			db.execSQL(sql, new Object[] { state, url,username });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		dbhelper.closeDb();
	}

	public void updateFileSize(String url, int size ,int status,String filePath, String username) {
		SQLiteDatabase db = dbhelper.getDatabase(MyDBHelper.WRITE);
		db.beginTransaction();
		try {
			String sql = "update CourseTab set filesize = ? ,state = ?,filepath = ? where fileurl = ? and username = ?";
			db.execSQL(sql, new Object[] { size,status,filePath, url ,username});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		dbhelper.closeDb();
	}
	public void deleteDowingCourse(String url,String username)
	{
		SQLiteDatabase db = dbhelper.getDatabase(MyDBHelper.WRITE);
		db.beginTransaction();
		try
		{
			String sql = "update CourseTab set filesize = 0 ,state = 0,filepath = null,finishsize = 0 where fileurl = ? and username = ?";
			String sql2 = "delete from DownloadTab where url = ? and username = ?";
			Object[] params = new Object[]{url,username};
			db.execSQL(sql, params);
			db.execSQL(sql2, params);
			db.setTransactionSuccessful();
		}finally {
			db.endTransaction();
		}
		dbhelper.closeDb();
	}
//	public void updateDowningCourse(DowningCourse course, int state , String username) {
//		SQLiteDatabase db = dbhelper.getDatabase(0);
//		db.beginTransaction();
//		try {
//			String sql = "update CourseTab set coursename = ?,filepath = ? ,filesize = ?,finishsize = ?,state = ? where fileurl = ? and username = ?";
//			db.execSQL(
//					sql,
//					new Object[] { course.getCourseName(),
//							course.getFilePath(), course.getFilesize(),
//							course.getFinishsize(), state, course.getFileurl(),course.getUsername() });
//			db.setTransactionSuccessful();
//		} finally {
//			db.endTransaction();
//		}
//		dbhelper.closeDb();
//	}
}
