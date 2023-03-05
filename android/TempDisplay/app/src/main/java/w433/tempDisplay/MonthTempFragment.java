package w433.tempDisplay;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonthTempFragment extends Fragment {
    List<TextView[]> txtViewList = new ArrayList<>();
    TableRow[] tableRowArray;
    TableLayout monthTempTable;
    TempActivity tempActivity;
    int monthlyHighTemp = 0;
    int monthlyLowTemp = 9999;
    // array of temps for each day of month table
    int maxDays = 31;
    long [] dayHighTime = new long[maxDays];
    int [] dayHighTemps = new int[maxDays];
    long [] dayLowTime = new long[maxDays];
    int [] dayLowTemps = new int[maxDays];
    String [] rowHdr2 = {"Date","Time","High","Time","Low"};
    int rowItms = rowHdr2.length;
    Calendar cal = Calendar.getInstance();
    TextView monthStrTv;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_month_temp, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tempActivity= (TempActivity) this.getActivity();
        monthTempTable = requireView().findViewById(R.id.monthTempTable);
        initMonthTableLayout();
    }

    public void initMonthTableLayout() {
        monthTempTable.setStretchAllColumns(true);
        monthTempTable.bringToFront();
        createTableHdr();
        createTRs();
        createTVs();
    }

    public void createTableHdr() {
        TextView tv = null;
        TableRow tr1 = null;
        TableRow tr2 = null;
        if (getActivity() != null) {
            monthStrTv = new TextView(getActivity().getApplicationContext());
            monthStrTv.setTextSize(17);
            tr1 = new TableRow(getActivity().getApplicationContext());
            tr2 = new TableRow(getActivity().getApplicationContext());
            tr1.addView(monthStrTv);
        }
        for (int i = 0; i < rowItms; i++) {
            if (getActivity() != null) {
                tv = new TextView(getActivity().getApplicationContext());
                tv.setText(rowHdr2[i]);
                tv.setTextSize(17);
            }
            if (tr2 != null)
                tr2.addView(tv);
        }
        monthTempTable.addView(tr1);
        monthTempTable.addView(tr2);
    }

    public void createTRs() {
        tableRowArray = new TableRow[maxDays];
        for (int i = 0; i < maxDays; i++) {
            if (getActivity() != null)
                tableRowArray[i] = new TableRow(getActivity().getApplicationContext());
        }
    }

    public void createTVs() {
        for (int i = 0; i < maxDays; i++) {
            TextView [] tvRow = new TextView [rowItms];
            for (int j = 0; j < rowItms; j++) {
                if (getActivity() != null) {
                    tvRow[j] = new TextView(getActivity().getApplicationContext());
                    tableRowArray[i].addView(tvRow[j]);
                }
            }
            txtViewList.add(tvRow);
            monthTempTable.addView(tableRowArray[i]);
        }
    }

    public void addMonthTableData(ArrayList<long[]> timeDataList, ArrayList<int[]> tempDataList, Date displayedDate) {
        cal.setTime(displayedDate);
        String monthStr = new SimpleDateFormat("MMMM yyyy", Locale.US).
                format(cal.getTimeInMillis());
        monthStrTv.setText(monthStr);
        int numdays = cal.getMaximum(Calendar.DAY_OF_MONTH);
        initMonthTemps(timeDataList, tempDataList);
        for (int i = 0; i < maxDays; i++) {
            TextView tv = txtViewList.get(i)[0];
            tv.setText(String.valueOf(i+1));
            addMonthDayData(txtViewList.get(i), tableRowArray[i],
                    dayHighTime[i], dayHighTemps[i], dayLowTime[i], dayLowTemps[i]);
        }
        blankUnusedMonthRows(numdays);
    }

    public void blankUnusedMonthRows(int numdays) {
        for (int i=maxDays-1; i >= numdays; i--) {
            TextView [] tvArray = txtViewList.get(i);
            for (int j = 0; j < rowItms; j++)
                tvArray[j].setText("");
        }
    }

    public void addMonthDayData(TextView [] tvArray, TableRow tr, long timeH,
                                long tempH, long timeL, long tempL) {
        // getTempStr and getDateStr check for valid values
        tvArray[2].setTextColor(Color.BLACK);
        tvArray[4].setTextColor(Color.BLACK);
        tvArray[1].setText(getDateStr(timeH));
        if (tempH == monthlyHighTemp)
            tvArray[2].setTextColor(Color.RED);
        tvArray[2].setText(TempActivity.getTempStr(tempH, false));
        tvArray[3].setText(getDateStr(timeL));
        if (tempL == monthlyLowTemp)
            tvArray[4].setTextColor(Color.BLUE);
        tvArray[4].setText(TempActivity.getTempStr(tempL, false));
    }

    public String getDateStr(long time) {
        String dateStr = "-";
        if (time > 0) {
            cal.setTimeInMillis(time * 1000);
            dateStr = new SimpleDateFormat("HH:mm", Locale.US).
                    format(cal.getTimeInMillis());
        }
        return dateStr;
    }

    public void initMonthTemps(ArrayList<long[]> timeDataList, ArrayList<int[]> tempDataList) {
        monthlyHighTemp = 0;
        monthlyLowTemp = 9999;
        for (int i=0; i < maxDays; i++) {
            dayHighTime[i] = 0;
            dayHighTemps[i] = 0;
            dayLowTime[i] = 0;
            dayLowTemps[i] = 9999;
        }

        for (int i = 0; i < tempDataList.size(); i++) {
            long [] timeDataArray = timeDataList.get(i);
            int [] tempDataArray = tempDataList.get(i);
            processMonthTemp(timeDataArray, tempDataArray);
        }
    }

    public void processMonthTemp(long [] timeDataArray, int [] tempDataArray) {
        long timeH = timeDataArray[0];
        int tempH = tempDataArray[0];
        long timeL = timeDataArray[1];
        int tempL = tempDataArray[1];
        cal.setTimeInMillis(timeH * 1000);
        if (cal.get(Calendar.HOUR_OF_DAY) < 6)
            cal.add(Calendar.DAY_OF_MONTH, -1);
        int dayOfMonthIndx = cal.get(Calendar.DAY_OF_MONTH) - 1;

        if (tempH > dayHighTemps[dayOfMonthIndx]) {
            dayHighTemps[dayOfMonthIndx] = tempH;
            dayHighTime[dayOfMonthIndx] = timeH;
        }
        if (tempL < dayLowTemps[dayOfMonthIndx]) {
            dayLowTemps[dayOfMonthIndx] = tempL;
            dayLowTime[dayOfMonthIndx] = timeL;
        }
        if (tempH > monthlyHighTemp)
            monthlyHighTemp = tempH;

        if (tempL < monthlyLowTemp)
            monthlyLowTemp = tempL;
    }
}
