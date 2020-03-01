package com.example.trackmytime;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper dbHelper;
    ArrayList<Button> btns;
    long todaysTime;
    String datetime;


    public static final long SLEEPTIME = 1000;
    boolean running;
    Thread refreshThread;
    TextView timerField;
    TextView overTimeField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        dbHelper.onUpgrade(dbHelper.getWritableDatabase(), 1,1);

        btns = new ArrayList<Button>();
        btns.add((Button)findViewById(R.id.buttonStartWorkday));
        btns.add((Button)findViewById(R.id.buttonStartPause));
        btns.add((Button)findViewById(R.id.buttonEndPause));
        btns.add((Button)findViewById(R.id.buttonEndWorkday));


        timerField = (TextView) findViewById( R.id.todaysElapsedTime );
        overTimeField = (TextView) findViewById(R.id.ueberstundenkonto);


        setInitialParams();
        updateOverTimeField();
    }

    public void initThread() {
     //   final TextView timerField = (TextView) findViewById( R.id.todaysElapsedTime );
        refreshThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    todaysTime = todaysTime + 1;
                    try {
                        Thread.sleep(SLEEPTIME);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String hms = String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(todaysTime),
                                    TimeUnit.SECONDS.toMinutes(todaysTime) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(todaysTime)),
                                    TimeUnit.SECONDS.toSeconds(todaysTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(todaysTime)));
                            timerField.setText(getString(R.string.time_string, hms));
                        }
                    });

                }
            }
        });
        refreshThread.start();
    }

    private void setInitialParams() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        datetime = now.format(formatter);
        TextView textView = findViewById(R.id.date);
        textView.setText(datetime);
    }

    public void startWorkday(View view) {
        btns.get(0).setEnabled(false);
        btns.get(1).setEnabled(true);
        btns.get(2).setEnabled(false);
        btns.get(3).setEnabled(true);

        todaysTime = 0;
        if (!running) {
            running = true;
            initThread();
        } else {
            running = false;
        }
    }

    public void startPause(View view) {
        btns.get(0).setEnabled(false);
        btns.get(1).setEnabled(false);
        btns.get(2).setEnabled(true);
        btns.get(3).setEnabled(true);

        running = false;
    }

    public void endPause(View view) {
        btns.get(0).setEnabled(false);
        btns.get(1).setEnabled(true);
        btns.get(2).setEnabled(false);
        btns.get(3).setEnabled(true);

        running = true;
        initThread();
    }

    public void endWorkday(View view) {
        btns.get(0).setEnabled(true);
        btns.get(1).setEnabled(false);
        btns.get(2).setEnabled(false);
        btns.get(3).setEnabled(false);

        running = false;


        ContentValues values = new ContentValues();
        values.put(DatabaseContract.DatabaseEntry.COLUMN_NAME_DATE, datetime);
        String realOvertime = Long.toString(todaysTime - 30000);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_NAME_TIMEDIFF, realOvertime);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insert(DatabaseContract.DatabaseEntry.TABLE_NAME, null, values);
        db.close();

        updateOverTimeField();
    }

    public long calculateOverTime(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseContract.DatabaseEntry.TABLE_NAME,   // The table to query
                new String[] {DatabaseContract.DatabaseEntry.COLUMN_NAME_TIMEDIFF},           // The array of columns to return (pass null to get all) // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null                    // The sort order
        );

        long overTime = 0;
        String foundValue;
        while(cursor.moveToNext()) {
            foundValue = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.DatabaseEntry.COLUMN_NAME_TIMEDIFF));
            if (foundValue.startsWith("-")){
                overTime -= Integer.parseInt(foundValue.substring(1));
            }else{
                overTime = overTime + Integer.parseInt(foundValue);
            }
            Log.d("dbBug", " " +overTime);
        }
        db.close();
        return (long) overTime;
    }

    public void updateOverTimeField() {
        long overTime = calculateOverTime();
        if (overTime < 0) {
            overTime = Math.abs(overTime);
            String hms = String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(overTime),
                    TimeUnit.SECONDS.toMinutes(overTime) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(overTime)),
                    TimeUnit.SECONDS.toSeconds(overTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(overTime)));
            overTimeField.setText(getString(R.string.ueberstundenkonto, "-" + hms));
        } else {
            String hms = String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(overTime),
                    TimeUnit.SECONDS.toMinutes(overTime) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(overTime)),
                    TimeUnit.SECONDS.toSeconds(overTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(overTime)));
            overTimeField.setText(getString(R.string.ueberstundenkonto, hms));
        }
    }
}
