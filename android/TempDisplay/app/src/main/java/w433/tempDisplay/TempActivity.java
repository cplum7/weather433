package w433.tempDisplay;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/*
This application uses ViewPager2 and fragments to enable the
user to swipe between displays of the current temperature,
a daily table of hourly max and min temperatures and a
table of monthly max and min temperatures.

A database access threads handles requests for either an
entire month of temperature data which is stored in a
hashmap or the current temperature and its time
which is updated to the hashmap if its a max or min
temperature for the hour.  The month data request
is made through a non-empty urlSuffix.

The date picker class allows the user to select a day
so that the daily and monthly tables will display
temperatures for the selected day.  Each day displayed
starts at 6am.

The SharedPreference class allows the user to enter the URL
for database access in the settings screen after the app
is installed.  The settings screen also allows the user
to display centigrade or fahrenheit temperatures.
 */

public class TempActivity extends FragmentActivity implements DatePickerDialog.OnDateSetListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    // number of pages to show when swiping horizontally
    private static final int NUM_PAGES = 3;
    private ViewPager2 viewPager;
    static TempActivity tempActivity;
    DailyTempFragment dailyTempFrag;
    MonthTempFragment monthTempFrag;
    TempFragment tempFrag;
    private static final String ARG_DATE = "date";
    HashMap<String, ArrayList<long[]>> timeData = new HashMap<>();
    HashMap<String, ArrayList<int[]>> tempData = new HashMap<>();
    static Date displayedDate;  // date at 06:00:00 am
    static String urlAddr = "";
    static String phpStr1 = "/getCurrentTimeTemp.php";
    static String phpStr2 = "/getTemps.php?time1=";
    String urlSuffix = "";
    static int TEMP_FRAG = 0;
    static int DAILY_TEMP_FRAG = 1;
    static int MONTH_TEMP_FRAG = 2;
    Calendar cal = Calendar.getInstance();
    static int RECEIVED_CURTEMP = 0;
    boolean doSleep = false;
    static long dpkrMinDate = 0;
    String hashStrCurrent = "";
    String hashStrDateSet = "";
    int currentPosition = 0;
    long curEndMonthTime = 0;
    Date onSetDate = null;
    static boolean displayCentigrade = false;
    static String degFStr = "";
    static String degCStr = "";
    long curTime = 0;
    int curTemp = 0;
    private volatile int sleepInterval = 30;
    private final Object lockObj = new Object();
    private static final String TAG = "TempActivity";
    BufferedWriter buf = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        viewPager = findViewById(R.id.pager);
        viewPager.registerOnPageChangeCallback (new vpager2Callback());
        tempActivity = this;
        setHttpAddrStr();
        // pager adapter, which provides the pages to the view pager widget.
        FragmentStateAdapter pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
        // set the date the tables will display to the current 6am date
        if (cal.get(Calendar.HOUR_OF_DAY) < 6)
            cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        displayedDate = cal.getTime();
        String [] time = getMonthBeginEnd(displayedDate);
        // cal set to end of month by above method
        curEndMonthTime = cal.getTimeInMillis()/1000;
        addLog();
        // request temperature data for the current month
        urlSuffix = time[0] + "&time2=" + time[1];
        getServerData();
        degFStr = getString(R.string.tempSignF);
        degCStr = getString(R.string.tempSignC);
    }

    // set the database URL string from shared preferences if available
    void setHttpAddrStr() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String urlStr = sharedPreferences.getString("urlAddr", "defaultValue");
        assert urlStr != null;
        if (!urlStr.equals("defaultValue"))
            urlAddr = urlStr.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settingsID) {
            startActivity(new Intent(this, SettingsFragment.class));
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the user is currently looking at the first page, allow the system to handle the
        // Back button. This calls finish() on this activity and pops the back stack.
        if (viewPager.getCurrentItem() != 0)
            // Otherwise, select the previous page.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        else super.onBackPressed();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (sharedPreferences != null) {
            if (s.equals("urlAddr"))
                urlAddr = sharedPreferences.getString("urlAddr", "");
            if (s.equals("displayCentigrade"))
                displayCentigrade = sharedPreferences.getBoolean(s, true);
            if (tempFrag != null)
                tempFrag.showTemp(curTime, curTemp);
        }
    }

    private class vpager2Callback extends ViewPager2.OnPageChangeCallback {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            currentPosition = position;
            String hashStr = (hashStrDateSet.isEmpty()) ? hashStrCurrent : hashStrDateSet;
            if (!hashStr.isEmpty()) {
                ArrayList<int[]> tempDataList = tempData.get(hashStr);
                ArrayList<long[]> timeDataList = timeData.get(hashStr);
                updateTables(timeDataList, tempDataList, position);
            }
        }
    }

    // pager adapter displaying 3 Fragment objects, in sequence.
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment selectedFrag = null;
            if (position == TEMP_FRAG) {
                tempFrag = new  TempFragment();
                selectedFrag = tempFrag;
            } else if (position == DAILY_TEMP_FRAG) {
                dailyTempFrag = new  DailyTempFragment();
                selectedFrag = dailyTempFrag;
            } else if (position == MONTH_TEMP_FRAG) {
                monthTempFrag = new  MonthTempFragment();
                selectedFrag = monthTempFrag;
            }
            return selectedFrag;
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    public void addLog() {
        File logFile = new File("sdcard/logTemp.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                buf = new BufferedWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            try {
                buf = new BufferedWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // check if any of the display tables can be updated with curTime/curTemp
    public void processServerData(ArrayList<long[]> timeSrvrDataList, ArrayList<int[]> tempSrvrDataList) {
        if (tempSrvrDataList.size() == 1 && tempSrvrDataList.get(0)[1] == RECEIVED_CURTEMP) {
            curTime = timeSrvrDataList.get(0)[0];
            curTemp = tempSrvrDataList.get(0)[0];
            // set minimum database time for date picker
            if (dpkrMinDate == 0)
                dpkrMinDate = timeSrvrDataList.get(0)[1];
            if (tempFrag != null)
                tempFrag.showTemp(curTime, curTemp);
            // check if curTime/curTemp needs updating in time/temp data hashMap
            updateTempDataHashMap();
            doSleep = true;
        } else { // received monthly temps
            String hashStr = "";
            if (onSetDate != null) {
                displayedDate = onSetDate;
                dailyTempFrag.addDayTableData(timeSrvrDataList, tempSrvrDataList, displayedDate);
                cal.setTime(displayedDate);
                hashStr = String.valueOf(cal.get(Calendar.YEAR)) + String.valueOf(cal.get(Calendar.MONTH));
                hashStrDateSet = (!hashStr.equals(hashStrCurrent)) ? hashStr : "";
                doSleep = true;
            } else {
                cal.setTime(displayedDate);
                hashStrCurrent = String.valueOf(cal.get(Calendar.YEAR)) + String.valueOf(cal.get(Calendar.MONTH));
                hashStr = hashStrCurrent;
            }
            timeData.put(hashStr, timeSrvrDataList);
            tempData.put(hashStr, tempSrvrDataList);
            urlSuffix = "";
        }
    }

    public void updateTempDataHashMap() {
        boolean doUpdateTables = hashStrDateSet.isEmpty() || hashStrDateSet == hashStrCurrent;
        StringBuffer strbuf = new StringBuffer(new SimpleDateFormat("MMMM d, yyyy HH:mm:ss", Locale.US).format(System.currentTimeMillis()) + " curtime=" + curTime + " curTemp=" + curTemp + " curEndMonthTime" + curEndMonthTime + " doUpdateTables=" + doUpdateTables + "\n");
        // is curTime within the current month
        if (curTime < curEndMonthTime) {
            cal.setTimeInMillis(curTime * 1000);
            int curHr = cal.get(Calendar.HOUR_OF_DAY);
            ArrayList<long[]> timeDataList = timeData.get(hashStrCurrent);
            ArrayList<int[]> tempDataList = tempData.get(hashStrCurrent);
            // get latest hour of tempData
            int indx = timeDataList.size() - 1;
            assert (indx == tempDataList.size() - 1);
            strbuf.append(" indx=" + indx + " curHr=" + curHr + "hashStrCurrent=" + hashStrCurrent);
            cal.setTimeInMillis(timeDataList.get(indx)[0] * 1000);
            // update latest hour if curTemp is max or min
            if (curHr == cal.get(Calendar.HOUR_OF_DAY)) {
                long[] timeDataArray = timeDataList.get(indx);
                int[] tempDataArray = tempDataList.get(indx);
                boolean updated = false;
                // is max for hr interval?
                if (curTemp > tempDataArray[0]) {
                    timeDataArray[0] = curTime;
                    tempDataArray[0] = curTemp;
                    updated = true;
                }
                // is min for hr interval?
                if (curTemp < tempDataArray[1]) {
                    timeDataArray[1] = curTime;
                    tempDataArray[1] = curTemp;
                    updated = true;
                }
                if (updated) {
                    String tmpStr = "";
                    timeDataList.set(indx, timeDataArray);
                    tempDataList.set(indx, tempDataArray);
                    timeData.put(hashStrCurrent, timeDataList);
                    tempData.put(hashStrCurrent, tempDataList);
                    strbuf.append("   updated");
                    if (doUpdateTables)
                        updateTables(timeDataList, tempDataList, currentPosition);
                }
            } else { // new hour so add curTime/curTemp values to tempData
                long [] timeDataArray =  {curTime, curTime};
                int [] tempDataArray =  {curTemp, curTemp};
                timeDataList.add(timeDataArray);
                tempDataList.add(tempDataArray);
                timeData.put(hashStrCurrent, timeDataList);
                tempData.put(hashStrCurrent, tempDataList);
                strbuf.append("   new hour");
                if (doUpdateTables)
                    updateTables(timeDataList, tempDataList, currentPosition);
            }
        } else { // curTime/curTemp transitioned to a new month
            cal.setTimeInMillis(curEndMonthTime * 1000);
            cal.add(Calendar.MONTH, 1);
            curEndMonthTime = cal.getTimeInMillis()/1000;
            hashStrCurrent = String.valueOf(cal.get(Calendar.YEAR)) + String.valueOf(cal.get(Calendar.MONTH));
            long [] timeDataArray =  {curTime, curTime};
            int [] tempDataArray =  {curTemp, curTemp};
            // get time/temp array for new month
            ArrayList<long[]> newTimeDataList = new ArrayList<>();
            ArrayList<int[]> newTempDataList = new ArrayList<>();
            newTimeDataList.add(timeDataArray);
            newTempDataList.add(tempDataArray);
            timeData.put(hashStrCurrent, newTimeDataList);
            tempData.put(hashStrCurrent, newTempDataList);
            strbuf.append("   new month");
            if (doUpdateTables)
                updateTables(newTimeDataList, newTempDataList, currentPosition);
        }
        try {
            buf.append(strbuf.toString());
            buf.newLine();
            buf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTables(ArrayList<long[]> timeList, ArrayList<int[]> tempList, int position) {
        if (position == TEMP_FRAG)
            tempFrag.showTemp(curTime, curTemp);
        if (position == DAILY_TEMP_FRAG)
            dailyTempFrag.addDayTableData(timeList, tempList, displayedDate);
        if (position == MONTH_TEMP_FRAG)
            monthTempFrag.addMonthTableData(timeList, tempList, displayedDate);
    }

    public String[] getMonthBeginEnd(Date setDate) {
        String[] rtnVals = new String[2];
        cal.setTime(setDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        rtnVals[0] = Long.toString(cal.getTimeInMillis()/1000);
        cal.add(Calendar.MONTH, 1);
        rtnVals[1] = Long.toString(cal.getTimeInMillis()/1000);
        return rtnVals;
    }

    static public String getTempStr(long temp, boolean deg) {
        String tempStr = "-";
        String suffix = "";
        float tempf = 0;
        // temp range -40C to 70C
        if (temp > 0 && temp < 1100) {
            temp = temp - 400;
            if (displayCentigrade) {
                tempf = temp / 10;
                suffix = (deg) ? degCStr : "";
            } else {
                tempf = (temp * 18 + 3200) / 100.0F;
                suffix = (deg) ? degFStr : "";
            }
            tempStr = String.format(Locale.US, "%.1f", tempf) + suffix;
        }
        return tempStr;
    }

    // set up time/temp tables for user picked date
    public void onDateSet(DatePicker view, int year, int month, int day) {
        cal.set(year, month, day, 6, 0, 0);
        onSetDate = cal.getTime();
        String hashStr =  String.valueOf(cal.get(Calendar.YEAR)) + String.valueOf(cal.get(Calendar.MONTH));
        ArrayList<long[]> timeDataList = timeData.get(hashStr);
        ArrayList<int[]> tempDataList = tempData.get(hashStr);
        // data not in hash table; need to request data from server
        if (tempDataList == null) {
            // request temperature data from getServerDataThrd
            String[] time = getMonthBeginEnd(onSetDate);
            urlSuffix = time[0] + "&time2=" + time[1];
            doSleep = false;
            // notify getServerDataThrd if its in a wait state
            synchronized (lockObj) {
                lockObj.notify();
            }
            String toastStr = "Wait for it!";
            Toast.makeText(getApplicationContext(), toastStr, Toast.LENGTH_SHORT).show();
        } else {
            displayedDate = onSetDate;
            onSetDate = null;
            dailyTempFrag.addDayTableData(timeDataList, tempDataList, displayedDate);
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = DatePickerFragment.newInstance(this);
        newFragment.show(getFragmentManager(), "datePicker");
    }

    public static class DatePickerFragment extends DialogFragment{
        private DatePickerDialog.OnDateSetListener onDateSetListener;

       static DatePickerFragment newInstance(DatePickerDialog.OnDateSetListener onDateSetListener) {
            DatePickerFragment pickerFragment = new DatePickerFragment();
            pickerFragment.setOnDateSetListener(onDateSetListener);
            //Pass the date in a bundle.
            Bundle bundle = new Bundle();
            bundle.putSerializable(ARG_DATE, new Date());
            pickerFragment.setArguments(bundle);
            return pickerFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            // Use the current date as the default date in the picker
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePkrDlog = new DatePickerDialog(getActivity(), onDateSetListener, year, month, day);
            DatePicker dpkr = datePkrDlog.getDatePicker ();
            dpkr.setMinDate(dpkrMinDate);
            if (cal.get(Calendar.HOUR_OF_DAY) < 6)
                cal.add(Calendar.DAY_OF_MONTH, -1);
            dpkr.setMaxDate(cal.getTimeInMillis());
            return datePkrDlog;
        }

        private void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
            this.onDateSetListener = listener;
        }
    }

    // gets either curTime/curTemp or a month of temp data
    // according to time interval indicated by httpSuffix
    // also gets minimum database time for date picker
    public void getServerData() {
        Runnable getServerDataThrd = () -> {
            HttpURLConnection urlConnection = null;
            while (true) {
                if (!urlAddr.isEmpty()) {
                    ArrayList<int[]> tempDataList = new ArrayList<>();
                    ArrayList<long[]> timeDataList = new ArrayList<>();
                    try {
                        URL url = new URL((urlSuffix.isEmpty()) ? urlAddr + phpStr1 : urlAddr + phpStr2 + urlSuffix);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream in = urlConnection.getInputStream();
                        JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                        reader.beginArray();
                        int idx = 0;
                        int[] tempDataArray = new int[2];
                        long[] timeDataArray = new long[2];
                        while (reader.hasNext()) {
                            if (idx == 0 || idx == 2)
                                timeDataArray[idx / 2] = reader.nextLong();
                            else
                                tempDataArray[(idx - 1) / 2] = reader.nextInt();
                            idx = (idx == 3) ? 0 : ++idx;
                            if (idx == 0) {
                                tempDataList.add(tempDataArray);
                                timeDataList.add(timeDataArray);
                                tempDataArray = new int[2];
                                timeDataArray = new long[2];
                            }
                        }
                        if (timeDataList.size() == 0)
                            timeDataList.add(timeDataArray);
                        if (tempDataList.size() == 0)
                            tempDataList.add(tempDataArray);
                        if (urlSuffix.isEmpty() && doSleep) {
                            //Thread.sleep(30 * 1000);
                            synchronized (lockObj) {
                                lockObj.wait(sleepInterval * 1000);
                            }
                        }
                        // if monthly data request fullfilled reset urlSuffix
                        if (tempDataList.size() > 1)
                            urlSuffix = "";
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (tempDataList.size() > 0) {
                            tempActivity.runOnUiThread((new Thread(() ->
                                    processServerData(timeDataList, tempDataList))));
                        }
                    }
                }
            }
        };
        new Thread(getServerDataThrd).start();
    }
}

