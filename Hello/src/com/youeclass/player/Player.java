package com.youeclass.player;

import android.media.AudioManager;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class Player implements SurfaceHolder.Callback {
	private int videoWidth;
	private int videoHeight;
	public MyMediaPlayer mediaPlayer;	//使用这个状态包裹类,里面有mediaPlayer的引用
	private SurfaceHolder surfaceHolder;
	private SeekBar skbProgress;
	private TextView currentTime;
	private TextView totalTime;
	private RelativeLayout loadLayout;
	private int duration;
	private int recordTime;
	private OnErrorListener errorListener;
	private OnCompletionListener completionListener;
	private boolean flag; //surface创建与否的标识
	public Player(SurfaceView surfaceView, SeekBar skbProgress,
			TextView currentTime, TextView totalTime,int recordTime,RelativeLayout loadLayout,OnErrorListener listener, OnCompletionListener completionListener) {
		this.skbProgress = skbProgress;
		this.currentTime = currentTime;
		this.totalTime = totalTime;
		this.recordTime = recordTime;
		this.loadLayout = loadLayout;
		this.errorListener = listener;
		this.completionListener = completionListener;
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void play() {
		mediaPlayer.start();
	}

	public void playUrl(String videoUrl) {
		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(videoUrl);
			mediaPlayer.prepareAsync();// prepare之后自动播放
			// mediaPlayer.start();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void pause() {
		mediaPlayer.pause();
	}

	public void stop() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.e("mediaPlayer", "surface changed");
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		try {
			mediaPlayer = new MyMediaPlayer(skbProgress, currentTime, totalTime,recordTime,loadLayout);
			mediaPlayer.setDisplay(surfaceHolder);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setLooping(true);
			mediaPlayer.setOnErrorListener(errorListener);
			mediaPlayer.setOnCompletionListener(completionListener);
			flag = true;
		} catch (Exception e) {
			Log.e("mediaPlayer", "error", e);
		}
		Log.e("mediaPlayer", "surface created");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Log.e("mediaPlayer", "surface destroyed");
	}


	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	// 获取视频的毫秒值
	public int getDuration() {
		return duration;
	}
	// 获得当前播放的毫秒数
	public int getCurrentTime()
	{
		if(mediaPlayer!=null)
		{
			return mediaPlayer.getCurrentPosition();
		}
		return 0;
	}
	public boolean isNull()
	{
		return mediaPlayer==null;
	}
	/////////////////
	public boolean isCreated()
	{
		return flag;
	}
	public void setForward()
	{
		if(mediaPlayer!=null)
		{
			mediaPlayer.setForward();
		}
	}
	public void setBack()
	{
		if(mediaPlayer!=null)
		{
			mediaPlayer.setBack();
		}
	}
}
