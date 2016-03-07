package sd.biswa.com.sdcardfilesize;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView txtData;
    ProgressBar pbLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        pbLoading = (ProgressBar)findViewById(R.id.pbLoading);
        pbLoading.setVisibility(View.GONE);

        txtData = (TextView)findViewById(R.id.txtData);

        final Button btnStart = (Button)findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pbLoading.setVisibility(View.VISIBLE);

                Intent intent = new Intent(MainActivity.this, ScanService.class);
                intent.setAction(ScanService.SCAN_START);
                startService(intent);
            }
        });

        final Button btnStop = (Button)findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ScanService.class);
                intent.setAction(ScanService.SCAN_STOP);
                startService(intent);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanService.SCAN_COMPLETED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    public BroadcastReceiver mReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction()!=null && intent.getAction().equalsIgnoreCase(ScanService.SCAN_COMPLETED)){

                pbLoading.setVisibility(View.GONE);

                final DataWrapper biggestList = (DataWrapper) intent.getSerializableExtra("biggestFiles");
                final DataWrapper extensionList = (DataWrapper) intent.getSerializableExtra("extensionList");
                final long totalFileSize = intent.getLongExtra("totalFileSize", 0);
                final long totalFileCount = intent.getLongExtra("totalFileCount", 0);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = "";
                        text += "Total File Size : " + totalFileSize;
                        text += "\r\nTotal File Count : " + totalFileCount;
                        if(totalFileCount != 0) {
                            text += "\r\nAverage File Size : " + (totalFileSize / totalFileCount);
                        }

                        text += "\r\n\r\nBiggest Files:";
                        for (PairData curr : biggestList.mDataList) {
                            text += "\r\n" + curr.name + " size = " + curr.value;
                        }

                        text += "\r\n\r\nMost Frequent Extensions:";
                        for (PairData curr : extensionList.mDataList) {
                            text += "\r\n" + curr.name + " total = " + curr.value;
                        }
                        txtData.setText(text);
                    }
                });

            }
        }
    };
}
