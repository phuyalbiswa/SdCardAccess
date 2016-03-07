package sd.biswa.com.sdcardfilesize;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScanService extends Service {

    private final String TAG = "ScanService";

    public static final String SCAN_START = "start";
    public static final String SCAN_STOP = "stop";
    public static final String SCAN_COMPLETED = "completed";

    private final int NOTIFICATION_ID = 132322;
    ScanThread mScanThread;
    NotificationCompat.Builder mNotificationBuilder;

    public ArrayList<File> mBiggestFile = new ArrayList<File>(); //descending. biggest at index 0
    public ArrayList<Map.Entry> mFileExtensionBiggest = new ArrayList<Map.Entry>();
    public HashMap<String, Long> mFreqExtensions = new HashMap<String, Long>();
    public long mTotalSize;
    public long mTotalFileCount;

    private final int MAX_BIGGEST_SIZE = 10;
    private final int MAX_EXTENSION_SIZE = 5;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();

            Log.i(TAG, "onStartCommand " + action);

            if (action.equalsIgnoreCase(SCAN_START)) {
                if (mScanThread == null) {
                    setNoficiation("Scanning...");

                    //reset data
                    mTotalFileCount = 0;
                    mTotalSize = 0;
                    mBiggestFile.clear();
                    mFileExtensionBiggest.clear();
                    mFreqExtensions.clear();

                    mScanThread = new ScanThread();
                    mScanThread.start();
                } else {
                    Log.e(TAG, "mScanThread already exist");
                }
            } else if (action.equalsIgnoreCase(SCAN_STOP)) {
                if (mScanThread != null) {
                    mScanThread.setStop();
                    mScanThread = null;
                } else {
                    Log.e(TAG, "mScanThread not exist");
                }
            }
        }
        return START_STICKY;
    }

    public void setNoficiation(String text) {

        if (mNotificationBuilder == null) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

            mNotificationBuilder = new NotificationCompat.Builder(this);
            mNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mNotificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
            mNotificationBuilder.setOnlyAlertOnce(true);
            mNotificationBuilder.setAutoCancel(false);
            mNotificationBuilder.setContentIntent(pIntent);
        }

        mNotificationBuilder.setContentText(text);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    public class ScanThread extends Thread {
        private boolean mIsStoped = false;
        ArrayList<File> tempList = new ArrayList<File>();

        public void setStop() {
            mIsStoped = true;
        }

        @Override
        public void run() {
            super.run();

            File file = new File(Environment.getExternalStorageDirectory().getPath());
            //File file = new File(Environment.getRootDirectory().getPath());//testing on nexus 6
            mTotalSize = scanFolder(file);

            setNoficiation(mIsStoped?"Stopped":"Completed");
            mScanThread = null;

            Log.e(TAG, "SCAN COMPLETED. total files=" + mTotalFileCount + ", size=" + mTotalSize + ", average=" + (mTotalFileCount != 0 ?(mTotalSize / mTotalFileCount) : 0));
            Log.e(TAG, "Biggest Files:");
            for (File curr : mBiggestFile) {
                Log.e(TAG, "-" + curr.getAbsolutePath() + ", size=" + curr.length());
            }


            ///get most frequent extensions
            {
                for (Map.Entry<String, Long> pair : mFreqExtensions.entrySet()) {

                    ArrayList<Map.Entry> tempList = new ArrayList<Map.Entry>();

                    int i = 0;
                    boolean isAdded = false;
                    for (i = 0; i < mFileExtensionBiggest.size(); i++) {
                        if (!isAdded && (Long) pair.getValue() >= (Long) mFileExtensionBiggest.get(i).getValue()) {
                            isAdded = true;
                            tempList.add(pair);
                        }

                        tempList.add(mFileExtensionBiggest.get(i));

                        //max 5 biggest
                        if (tempList.size() >= MAX_EXTENSION_SIZE) {
                            break;
                        }
                    }
                    if (!isAdded && tempList.size() < MAX_EXTENSION_SIZE) {
                        tempList.add(pair);
                    }

                    mFileExtensionBiggest = tempList;
                }
            }

            Log.e(TAG, "Most frequent extensions:");
            for (Map.Entry curr : mFileExtensionBiggest) {
                Log.e(TAG, "-" + curr.getKey() + ", count=" + curr.getValue());
            }


            //////sending broadcast
            ArrayList<PairData> biggestList = new ArrayList<PairData>();
            for (File curr : mBiggestFile) {
                biggestList.add(new PairData(curr.getAbsolutePath(), Long.valueOf(curr.length())));
            }

            ArrayList<PairData> extensionList = new ArrayList<PairData>();
            for (Map.Entry curr : mFileExtensionBiggest) {
                extensionList.add(new PairData((String)curr.getKey(), curr.getValue()));
            }

            Intent intent = new Intent();
            intent.putExtra("biggestFiles", new DataWrapper(biggestList));
            intent.putExtra("extensionList", new DataWrapper(extensionList));
            intent.putExtra("totalFileSize", mTotalSize);
            intent.putExtra("totalFileCount", mTotalFileCount);
            intent.setAction(SCAN_COMPLETED);
            sendBroadcast(intent);
        }

        private long scanFolder(final File file) {
            if (mIsStoped) {
                return 0;
            }

            long totalSize = 0;
            if (file == null ) {
                return 0;
            }
            if (file.isDirectory()) {
                File[] innerFiles = file.listFiles();
                if (innerFiles == null ) {
                    return 0;
                }
                for (File curr : innerFiles) {
                    totalSize += scanFolder(curr);
                }
                //at this point we get folder total size
                Log.i(TAG, "Folder " + file.getAbsolutePath() + ", total=" + totalSize);
            } else {
                mTotalFileCount++;

                /////update extension frequency
                int lastIndex = file.getName().lastIndexOf(".");
                if (lastIndex > 0) {
                    String extension = file.getName().substring(lastIndex + 1);
                    Long freq = mFreqExtensions.get(extension);
                    if (freq == null) {
                        freq = new Long(0);
                    }
                    freq++;
                    mFreqExtensions.put(extension, freq);
                }
                //////////////

                ///Sort if biggest file
                tempList.clear();
                int i = 0;
                boolean isAdded = false;
                for (i = 0; i < mBiggestFile.size(); i++) {
                    if (!isAdded && file.length() >= mBiggestFile.get(i).length()) {
                        isAdded = true;
                        tempList.add(file);
                    }

                    tempList.add(mBiggestFile.get(i));

                    //max 5 biggest
                    if (tempList.size() >= MAX_BIGGEST_SIZE) {
                        break;
                    }
                }
                if (!isAdded && tempList.size() < MAX_BIGGEST_SIZE) {
                    tempList.add(file);
                }
                mBiggestFile.clear();
                mBiggestFile.addAll(tempList);
                //////////

                totalSize += file.length();
            }

            return totalSize;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
