package com.youeclass.player;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.youeclass.VideoActivity2;

public class VitamioPlayer implements OnBufferingUpdateListener,
		OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener,OnInfoListener,
		SurfaceHolder.Callback {
	private static final String TAG = "VitamioPlayer";
	private int videoWidth;
	private int videoHeight;
	public MediaPlayer mediaPlayer; //
	private SurfaceHolder surfaceHolder;
	private SeekBar skbProgress;
	private TextView currentTime;
	private TextView totalTime;
	private RelativeLayout loadLayout;
	private long duration;
	private int recordTime;
	private Timer mTimer ;
	private boolean flag; // surface创建与否的标识
	private boolean mIsVideoSizeKnown = false;
	private boolean mIsVideoReadyToBePlayed = false;
	TimerTask mTimerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				if (mediaPlayer.isPlaying() && skbProgress.isPressed() == false) {
					handleProgress.sendEmptyMessage(0);
				}
			} catch (NullPointerException e) {
				Log.e(TAG, "播放器还没有创建");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	Handler handleProgress = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try {
					int position = (int) mediaPlayer.getCurrentPosition();
					// int duration = mediaPlayer.getDuration();
					if (duration > 0) {
						long pos = skbProgress.getMax() * position / duration;
						skbProgress.setProgress((int) pos);
						currentTime.setText(getTime(position / 1000));
					}
					if (duration == position) {

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		};
	};

	private String getTime(long count) {
		long m = count / 60;
		long s = count % 60;
		String ms = m < 10 ? "0" + m : m + "";
		return ms + ":" + (s < 10 ? "0" + s : s + "");
	}

	private Context context;

	// 初始化vitamioPlayer
	public VitamioPlayer(Context context, SurfaceView surfaceView,
			SeekBar skbProgress, TextView currentTime, TextView totalTime,
			int recordTime, RelativeLayout loadLayout) {
		this.context = context;
		this.skbProgress = skbProgress;
		this.currentTime = currentTime;
		this.totalTime = totalTime;
		this.recordTime = recordTime;
		this.loadLayout = loadLayout;
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setFormat(PixelFormat.RGBA_8888);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		skbProgress.setSecondaryProgress(percent);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		// 退出来
		Log.e("mediaPlayer", "onCompletion 播放完毕");
		skbProgress.setProgress(0);
		currentTime.setText("00:00");
		mediaPlayer.stop();
		mTimer.cancel();
		mTimer = null;
		((VideoActivity2)context).onCompletion(mp);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.e("mediaPlayer", "onPrepared");
		loadLayout.setVisibility(View.GONE);
		mIsVideoReadyToBePlayed = true;
		// videoWidth = mediaPlayer.getVideoWidth();
		// videoHeight = mediaPlayer.getVideoHeight();
		/*
		 * if (videoHeight != 0 && videoWidth != 0) { start(); }
		 */
		skbProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
		duration = mediaPlayer.getDuration();
		totalTime.setText(getTime(duration / 1000));
		if (recordTime > 0 && recordTime< duration-10) {
			skbProgress
					.setProgress((int) (recordTime * skbProgress.getMax() / duration));
			mediaPlayer.seekTo(recordTime);
		}
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			play();
		}
	}

	// seekbar监听事件
	class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
		int progress;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
			this.progress = (int) (progress * mediaPlayer.getDuration() / seekBar
					.getMax());
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
			mediaPlayer.seekTo(progress);
			// 更改时间
			currentTime.setText(getTime(progress / 1000));
		}
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onVideoSizeChanged called"+" invalid video width(" + width + ") or height(" + height
				+ ")");
		if (width == 0 || height == 0) {
			Log.e(TAG, "invalid video width(" + width + ") or height(" + height
					+ ")");
			return;
		}
		videoWidth = width;
		videoHeight = height;
		mIsVideoSizeKnown = true;
//		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
//			play();
//		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		Log.d(TAG, "surfaceChanged called");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d(TAG, "surfaceCreated called");
		try {
			mediaPlayer = new MediaPlayer(context);
			mediaPlayer.setDisplay(surfaceHolder);
			mediaPlayer.setOnBufferingUpdateListener(this);
			mediaPlayer.setOnCompletionListener(this);
//			mediaPlayer.setOnInfoListener(this);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnVideoSizeChangedListener(this);
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "error", e);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d(TAG, "surfaceDestroyed called");

	}
	private boolean needResume;
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		switch (what) {
        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            //开始缓存，暂停播放
            if (isPlaying()) {
                mediaPlayer.pause();
                needResume = true;
            }
            loadLayout.setVisibility(View.VISIBLE);
            break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            //缓存完成，继续播放
            if (needResume)
                mediaPlayer.start();
            loadLayout.setVisibility(View.GONE);
            break;
        case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
            //显示 下载速度
            Log.e(TAG,"download rate:" + extra);
            break;
        }
        return true;
	}
	// 快进
	public void setForward() {
		long ct = mediaPlayer.getCurrentPosition();
		if (ct + 5000 < mediaPlayer.getDuration()) {
			ct = ct + 5000;
			mediaPlayer.seekTo(ct);
			skbProgress
					.setProgress((int) (ct * skbProgress.getMax() / duration));
			currentTime.setText(getTime(ct / 1000));
		}
	}

	// 快退
	public void setBack() {
		long ct = mediaPlayer.getCurrentPosition();
		if (ct - 5000 > 0) {
			ct = ct - 5000;
			mediaPlayer.seekTo(ct);
			skbProgress
					.setProgress((int) (ct * skbProgress.getMax() / duration));
			currentTime.setText(getTime(ct / 1000));
		}
	}

	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	// 获取视频的毫秒值
	public long getDuration() {
		return duration;
	}

	// 获得当前播放的毫秒数
	public long getCurrentTime() {
		if (mediaPlayer != null) {
			return mediaPlayer.getCurrentPosition();
		}
		return 0;
	}

	public boolean isNull() {
		return mediaPlayer == null;
	}

	// ///////////////
	public boolean isCreated() {
		return flag;
	}

	public void playUrl(String url) {
		doCleanUp();
		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.setDisplay(surfaceHolder);
			mediaPlayer.prepareAsync();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "播放错误");
		}
	}

	public void play() {
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			surfaceHolder.setFixedSize(videoWidth, videoHeight);
			Log.e(TAG,"开始播放。。。。。");
			mediaPlayer.start();
			if(mTimer==null)
			{
				mTimer = new Timer();
				mTimer.schedule(mTimerTask, 0, 1000);
			}
		}
	}

	public void pause() {
		if(mediaPlayer!=null)
		{
			mediaPlayer.pause();
		}
	}

	public void stop() {
		if (mediaPlayer != null) {
			mTimer.cancel();
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
			mTimer = null;
		}
	}
	private void doCleanUp() {
		videoWidth = 0;
		videoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
	}
}
