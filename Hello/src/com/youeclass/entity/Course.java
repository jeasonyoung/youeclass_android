package com.youeclass.entity;

import com.youeclass.annotation.Column;
import com.youeclass.util.Constant;
import com.youeclass.util.StringUtils;

public class Course {
	private long _id;
	@Column(name="courseid")
	private String courseId;
	@Column(name="coursename")
	private String courseName;
	@Column(name="classid")
	private String classid;
	@Column(name="coursetype")
	private String courseType;
	@Column(name="coursemode")
	private String courseMode;
	@Column(name="coursegroup")
	private String courseGroup;
	@Column(name="filesize")
	private int filesize;
	@Column(name="finishsize")
	private int finishsize;
	@Column(name="filepath")
	private String filePath;
	@Column(name="fileurl")
	private String fileUrl;
	@Column(name="state")
	private int state;
	@Column(name="username")
	private String username;
	
	public long get_id() {
		return _id;
	}
	public void set_id(long _id) {
		this._id = _id;
	}
	public String getCourseId() {
		return courseId;
	}
	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}
	public String getCourseName() {
		return courseName;
	}
	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	public String getClassid() {
		return classid;
	}
	public void setClassid(String classid) {
		this.classid = classid;
	}
	public String getCourseType() {
		return courseType;
	}
	public void setCourseType(String courseType) {
		this.courseType = courseType;
	}
	public String getCourseMode() {
		return courseMode;
	}
	public void setCourseMode(String courseMode) {
		this.courseMode = courseMode;
	}
	public String getCourseGroup() {
		return courseGroup;
	}
	public void setCourseGroup(String courseGroup) {
		this.courseGroup = courseGroup;
	}
	public int getFilesize() {
		return filesize;
	}
	public void setFilesize(int filesize) {
		this.filesize = filesize;
	}
	public int getFinishsize() {
		return finishsize;
	}
	public void setFinishsize(int finishsize) {
		this.finishsize = finishsize;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFileUrl() {
		if(!StringUtils.isEmpty(fileUrl))
		{
			if(fileUrl.indexOf(Constant.MEDIA_DOMAIN_URL)==-1)
			{
				fileUrl = Constant.NGINX_URL +fileUrl;
			}
		}
		return fileUrl;
	}
	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Course(String courseId, String courseName, String classid,
			String courseType, String courseMode, String courseGroup,
			int filesize, int finishsize, String filePath, String fileUrl,
			int state,String username) {
		super();
		this.courseId = courseId;
		this.courseName = courseName;
		this.classid = classid;
		this.courseType = courseType;
		this.courseMode = courseMode;
		this.courseGroup = courseGroup;
		this.filesize = filesize;
		this.finishsize = finishsize;
		this.filePath = filePath;
		this.fileUrl = fileUrl;
		this.state = state;
		this.username = username;
	}
	public Course(String courseId, String courseName, String classid,
			int filesize, int finishsize, String filePath, String fileUrl,
			int state) {
		super();
		this.courseId = courseId;
		this.courseName = courseName;
		this.classid = classid;
		this.filesize = filesize;
		this.finishsize = finishsize;
		this.filePath = filePath;
		this.fileUrl = fileUrl;
		this.state = state;
	}
	
	public Course(String courseId, String courseName, String classid,
			String fileUrl, int state,String username) {
		super();
		this.courseId = courseId;
		this.courseName = courseName;
		this.classid = classid;
		this.fileUrl = fileUrl;
		this.state = state;
		this.username = username;
	}
	public Course() {
		// TODO Auto-generated constructor stub
	}
	
}
