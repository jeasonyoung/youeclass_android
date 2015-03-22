package com.youeclass.entity;

public class DowningCourse {
	private String courseName;//课程的名字
	private int filesize;	//文件大小
	private int finishsize;//完成数
	private String filePath;//文件路径
	private String fileurl;	//文件下载路径
	private int status;//文件的下载状态     0:暂停状态, -1:初始状态还没有获得文件大小
	private String username;
	public String getCourseName() {
		return courseName;
	}
	public void setCourseName(String courseName) {
		this.courseName = courseName;
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
	public String getFileurl() {
		return fileurl;
	}
	public void setFileurl(String fileurl) {
		this.fileurl = fileurl;
	}
	public DowningCourse() {
		// TODO Auto-generated constructor stub
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public DowningCourse(String courseName, int filesize, int finishsize,
			String filePath, String fileurl,String username) {
		super();
		this.courseName = courseName;
		this.filesize = filesize;
		this.finishsize = finishsize;
		this.filePath = filePath;
		this.fileurl = fileurl;
		this.username = username;
	}
	
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileurl == null) ? 0 : fileurl.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DowningCourse other = (DowningCourse) obj;
		if (fileurl == null) {
			if (other.fileurl != null)
				return false;
		} else if (!fileurl.equals(other.fileurl))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DowningCourse [courseName=" + courseName + ", fileurl="
				+ fileurl + "]";
	}
	
}
