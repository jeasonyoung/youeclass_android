package com.youeclass.entity;

import com.youeclass.annotation.Column;

public class DownloadItem {
	private long _id;
	@Column(name="thread_id")
	private int threadId;
	@Column(name="start_pos")
	private int startPos;
	@Column(name="end_pos")
	private int endPos;
	@Column(name="complete_size")
	private int completeSize;
	@Column(name="url")
	private String url;
	@Column(name="username")
	private String username;
	public long get_id() {
		return _id;
	}
	public void set_id(long _id) {
		this._id = _id;
	}
	public int getThreadId() {
		return threadId;
	}
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
	public int getStartPos() {
		return startPos;
	}
	public void setStartPos(int startPos) {
		this.startPos = startPos;
	}
	public int getEndPos() {
		return endPos;
	}
	public void setEndPos(int endPos) {
		this.endPos = endPos;
	}
	public int getCompleteSize() {
		return completeSize;
	}
	public void setCompleteSize(int completeSize) {
		this.completeSize = completeSize;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public DownloadItem(int threadId, int startPos, int endPos,
			int completeSize, String url,String username) {
		super();
		this.threadId = threadId;
		this.startPos = startPos;
		this.endPos = endPos;
		this.completeSize = completeSize;
		this.url = url;
		this.username = username;
	}
	public DownloadItem() {
		// TODO Auto-generated constructor stub
	}
	
}
