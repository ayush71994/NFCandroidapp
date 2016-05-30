package com.akulrastogi.nfcreadnosound;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {

    private NfcAdapter nfcAdapter;
    SharedPreferences prefs;
    timer timer = new timer(3000, 1000);
    Random random;

    TextView cardDetails;
    TextView statusText;
    ImageView image;
    TextClock clockTime;
    TextClock clockDayDate;

    boolean [] readOnce;
    int count = 0;
    int size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            Log.i("App", "Database Created Successfully");
            SQLiteDatabase sql = openOrCreateDatabase("Stud_Info", MODE_PRIVATE, null);
            sql.execSQL("CREATE TABLE IF NOT EXISTS student (RegNo VARCHAR, CardId VARCHAR)");
            sql.execSQL("INSERT INTO student VALUES('149105588', '4fd96c30')");
            sql.execSQL("INSERT INTO student VALUES('149105416', '4f9ffce0')");
            //sql.execSQL("INSERT INTO student VALUES('149105416', '4fb84')");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTime", true);
            editor.commit();
        }

        Log.i("App", "Database Opened");
        SQLiteDatabase sql = openOrCreateDatabase("Stud_Info", MODE_PRIVATE, null);
        Cursor c = sql.rawQuery("SELECT * FROM student",null);

        size = c.getCount();
        readOnce = new boolean[size];
        int i = 0;

        while (i < size) {
            readOnce[i] = false;
            i++;
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        cardDetails = (TextView) findViewById(R.id.cardDetails);
        statusText = (TextView) findViewById(R.id.statusText);
        image = (ImageView) findViewById(R.id.image);
        clockTime = (TextClock) findViewById(R.id.time);
        clockDayDate = (TextClock) findViewById(R.id.dayDate);

        random = new Random();

        if (nfcAdapter == null) {
            statusText.setText("Please use a NFC enabled smartphone.");
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            statusText.setText("Please enable NFC from the settings menu.");
        } else {
            statusText.setText("Please tap your card!");
        }
    }

    public void getCurrentEvent() {
        CharSequence currentTime = clockTime.getText();
        CharSequence currentDayDate = clockDayDate.getText();
        int currentHour = Integer.parseInt(currentTime.subSequence(0, 2).toString());
        int currentMinute = Integer.parseInt(currentTime.subSequence(3, 5).toString());

    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i("Here", "tag discovered");
        timer.cancel();

        boolean verification = false;
        final String cardData = getHexStringFromByteArray(tag.getId());

        SQLiteDatabase sql = openOrCreateDatabase("Stud_Info", MODE_PRIVATE, null);
        Cursor c = sql.rawQuery("Select * from student", null);
        int cardIndex = c.getColumnIndex("CardId");
        c.moveToFirst();
        int i = 0;
        try {
            while(c != null) {
                if(cardData.equalsIgnoreCase(c.getString(cardIndex)) && !readOnce[i]) {
                    Log.i("Loop", "On");
                    verification = true;
                    readOnce[i] = true;
                    break;
                }
                i++;
                c.moveToNext();
            }
        } catch(Exception e) {
            verification = false;
        }
        Log.i("App", "Card swiped is : " + cardData);

        final int imageType = verification ? R.drawable.tick : R.drawable.cross;

        final boolean finalVerification = verification;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cardDetails.setText(cardData.toUpperCase());

                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                if (finalVerification)
                    toneGenerator.startTone(random.nextInt(16), 700);

                image.setVisibility(View.VISIBLE);
                image.setImageResource(imageType);
                timer.start();
            }
        });
    }

    public class timer extends CountDownTimer {
        public timer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            image.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFinish() {
            image.setVisibility(View.INVISIBLE);
        }
    }

    private String getHexStringFromByteArray(byte byteArray[]) {
        StringBuilder stringbuilder = new StringBuilder();
        for (int i = byteArray.length - 1; i >= 0; i--) {
            int j = byteArray[i] & 0xff;
            if (j < 16)
                stringbuilder.append('0');
            stringbuilder.append(Integer.toHexString(j));
        }
        return stringbuilder.toString();
    }

}