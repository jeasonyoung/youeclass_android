package com.youeclass;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.TextView;

public class Player implements OnBufferingUpdateListener,
		OnCompletionListener, MediaPlayer.OnPreparedListener,OnErrorListener,
		SurfaceHolder.Callback {
	private int videoWidth;
	private int videoHeight;
	public MediaPlayer mediaPlayer;
	private SurfaceHolder surfaceHolder;
	private SeekBar skbProgress;
	private TextView currentTime;
	private TextView totalTime;
	private int duration;
	private Timer mTimer=new Timer();
	public Player(SurfaceView surfaceView,SeekBar skbProgress,TextView currentTime,TextView totalTime)
	{
		this.skbProgress=skbProgress;
		this.currentTime = currentTime;
		this.totalTime = totalTime;
		surfaceHolder=surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mTimer.schedule(mTimerTask, 0, 1000);
	}
	
	/*******************************************************
	 * 通过定时器和Handler来更新进度条
	 ******************************************************/
	TimerTask mTimerTask = new TimerTask() {
		@Override
		public void run() {
			if(mediaPlayer==null)
				return;
			if (mediaPlayer.isPlaying() && skbProgress.isPressed() == false) {
				handleProgress.sendEmptyMessage(0);
			}
		}
	};
	
	Handler handleProgress = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
				case 0:
					int position = mediaPlayer.getCurrentPosition();
					int duration = mediaPlayer.getDuration();
					if (duration > 0) {
						long pos = skbProgress.getMax() * position / duration;
						skbProgress.setProgress((int) pos);
						currentTime.setText(getTime(position/1000));
					}
					if(duration==position)
					{
						currentTime.setText(totalTime.getText());
					}
			}
			
		};
	};
	private String getTime(int count)
	{
		int m = count/60;
		int s = count%60;
		String ms = m<10?"0"+m:m+"";
		return ms+":"+(s<10?"0"+s:s+"");
	}
	//*****************************************************
	
	public void play()
	{
		mediaPlayer.start();
	}
	
	public void playUrl(String videoUrl)
	{
		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(videoUrl);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.prepareAsync();//prepare之后自动播放
			mediaPlayer.setOnBufferingUpdateListener(this);
			//mediaPlayer.start();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(Exception e1)
		{
			e1.printStackTrace();
		}
	}

	
	public void pause()
	{
		mediaPlayer.pause();
	}
	
	public void stop()
	{
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
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDisplay(surfaceHolder);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnPreparedListener(this);
		} catch (Exception e) {
			Log.e("mediaPlayer", "error", e);
		}
		Log.e("mediaPlayer", "surface created");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Log.e("mediaPlayer", "surface destroyed");
	}

	
	@Override
	/**
	 * 通过onPrepared播放
	 */
	public void onPrepared(MediaPlayer arg0) {
		videoWidth = mediaPlayer.getVideoWidth();
		videoHeight = mediaPlayer.getVideoHeight();
		if (videoHeight != 0 && videoWidth != 0) {
			arg0.start();
		}
		duration = mediaPlayer.getDuration();
		totalTime.setText(getTime(duration/1000));
		Log.e("mediaPlayer", "onPrepared");
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int bufferingProgress) {
		skbProgress.setSecondaryProgress(bufferingProgress);
		int currentProgress=skbProgress.getMax()*mediaPlayer.getCurrentPosition()/mediaPlayer.getDuration();
		Log.e(currentProgress+"% play", bufferingProgress + "% buffer");
	}
	public boolean isPlaying()
	{
		return mediaPlayer.isPlaying();
	}
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		System.out.println(what+"..."+extra);
		return false;
	}
	//获取视频的毫秒值
	public int getDuration()
	{
		return duration;
	}
	
}
