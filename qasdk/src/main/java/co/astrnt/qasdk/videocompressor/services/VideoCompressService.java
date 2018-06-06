package co.astrnt.qasdk.videocompressor.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import co.astrnt.qasdk.AstrntSDK;
import co.astrnt.qasdk.R;
import co.astrnt.qasdk.dao.QuestionApiDao;
import co.astrnt.qasdk.event.CompressEvent;
import co.astrnt.qasdk.type.UploadStatusType;
import co.astrnt.qasdk.upload.SingleVideoUploadService;
import co.astrnt.qasdk.videocompressor.VideoCompress;
import io.reactivex.annotations.Nullable;
import timber.log.Timber;

public class VideoCompressService extends Service {

    public static final String EXT_QUESTION_ID = "VideoCompressService.QuestionId";
    public static final String EXT_PATH = "VideoCompressService.Path";

    public static final long NOTIFY_INTERVAL = 60 * 1000;

    private File inputFile, outputFile;
    private String inputPath, outputPath;
    private long questionId;

    private Context context;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    private QuestionApiDao currentQuestion;
    private AstrntSDK astrntSDK;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private int mNotificationId;

    public static void start(Context context, String inputPath, long questionId) {
        context.startService(
                new Intent(context, VideoCompressService.class)
                        .putExtra(EXT_PATH, inputPath)
                        .putExtra(EXT_QUESTION_ID, questionId)
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            inputPath = intent.getStringExtra(EXT_PATH);
            questionId = intent.getLongExtra(EXT_QUESTION_ID, 0);

            inputFile = new File(inputPath);
            currentQuestion = astrntSDK.searchQuestionById(questionId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        astrntSDK = new AstrntSDK();

        if (mTimer != null) {
            mTimer.cancel();
        } else {
            mTimer = new Timer();
        }
        mTimer.scheduleAtFixedRate(new VideoCompressService.TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void doCompress() {

        mNotificationId = (int) currentQuestion.getId();
        outputFile = new File(context.getFilesDir(), currentQuestion.getId() + "_video.mp4");
        outputPath = outputFile.getAbsolutePath();

        VideoCompress.compressVideo(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), new VideoCompress.CompressListener() {
            @Override
            public void onStart() {
                mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(context, String.valueOf(currentQuestion.getId()));
                mBuilder.setContentTitle("Video Compress")
                        .setContentText("Compress in progress")
                        .setSmallIcon(R.drawable.ic_autorenew_white_24dp);

                Timber.d("Video Compress compress START %s %s", inputPath, outputPath);
            }

            @Override
            public void onSuccess() {
                Timber.d("Video Compress compress %s %s %s", inputPath, outputPath, "SUCCESS");
                astrntSDK.updateVideoPath(currentQuestion, outputPath);
                if (astrntSDK.isNotLastQuestion()) {
                    SingleVideoUploadService.start(context, currentQuestion.getId());
                } else {
                    EventBus.getDefault().post(new CompressEvent());
                }
                stopService();

                mBuilder.setContentText("Compress completed")
                        // Removes the progress bar
                        .setProgress(0,0,false);
                mNotifyManager.notify(mNotificationId, mBuilder.build());
            }

            @Override
            public void onFail() {
                Timber.e("Video Compress compress %s %s %s", inputPath, outputPath, "FAILED");
                stopService();
            }

            @Override
            public void onProgress(float percent) {
                mBuilder.setProgress(100, (int) percent, false);
                // Displays the progress bar for the first time.
                mNotifyManager.notify(mNotificationId, mBuilder.build());
                Timber.e("Video Compress progress %s", percent);
            }
        });
    }

    public void stopService() {
        mTimer.cancel();
        stopSelf();
    }

    class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (currentQuestion != null) {
                        if (currentQuestion.getUploadStatus().equals(UploadStatusType.PENDING)) {
                            doCompress();
                        } else {
                            stopService();
                        }
                    } else {
                        stopService();
                    }
                }
            });
        }
    }
}