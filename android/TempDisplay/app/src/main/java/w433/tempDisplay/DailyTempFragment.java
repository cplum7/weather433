package w433.tempDisplay;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class DailyTempFragment extends Fragment {
    List<TextView[]> txtViewList = new ArrayList<>();
    int tableRows = 12;
    TableRow[] tableRowArray = new TableRow[tableRows];
    TableLayout tempTable;
    Button button;
    TextView dateTV;
    TempActivity tempActivity;
    int maxDayTemp = 0;
    int minDayTemp = 9999;
    int maxNightTemp = 0;
    int minNightTemp = 9999;
    // array of temps for each hr of day table
    int [] hrHighTemps = new int[24];
    int [] hrLowTemps = new int[24];
    static String [] rowHdr1 = {"", "Day", "", "Night", ""};
    static String [] rowHdr2 = {"Time", "High", "Low", "High", "Low"};
    static int rowItms = rowHdr2.length;
    Calendar cal = Calendar.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_daily_temp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tempTable = requireView().findViewById(R.id.tempTable);
        dateTV = requireView().findViewById(R.id.date);
        button = requireView().findViewById(R.id.button);
        tempActivity= (TempActivity) this.getActivity();
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                tempActivity.showDatePickerDialog(v);
            }
        });
        initDayTableLayout();
    }

    public void initDayTableLayout() {
        tempTable.setStretchAllColumns(true);
        tempTable.bringToFront();
        createTableHdr();
        createTRs();
        createTVs();
        addTimeColStrs();
    }

    public void createTableHdr() {
        TextView tv = null;
        TableRow tr1 = null;
        TableRow tr2 = null;
        if (getActivity() != null) {
            tr1 = new TableRow(getActivity().getApplicationContext());
            tr2 = new TableRow(getActivity().getApplicationContext());
        }
        for (int i = 0; i < 5; i++) {
            if (getActivity() != null) {
                tv = new TextView(getActivity().getApplicationContext());
                tv.setText(rowHdr1[i]);
                tv.setTextSize(17);
            }
            if (tr1 != null)
                tr1.addView(tv);
            if (getActivity() != null) {
                tv = new TextView(getActivity().getApplicationContext());
                tv.setText(rowHdr2[i]);
                tv.setTextSize(17);
            }
            if (tr2 != null)
                tr2.addView(tv);
        }
        tempTable.addView(tr1);
        tempTable.addView(tr2);
    }

    public void createTRs() {
        for (int i = 0; i < tableRows; i++) {
            if (getActivity() != null)
                tableRowArray[i] = new TableRow(getActivity().getApplicationContext());
        }
    }

    public void createTVs() {
        for (int i = 0; i < tableRows; i++) {
            TextView [] tvRow = new TextView [rowItms];
            for (int j = 0; j < rowItms; j++) {
                if (getActivity() != null) {
                    tvRow[j] = new TextView(getActivity().getApplicationContext());
                    tableRowArray[i].addView(tvRow[j]);
                }
            }
            txtViewList.add(tvRow);
            tempTable.addView(tableRowArray[i]);
        }
    }

    public void addTimeColStrs() {
        int hr1 = 5;
        int hr2 = 6;
        TextView tv;
        for (int i = 0; i < tableRows; i++) {
            if (hr1 == 12)
                hr1 = 0;
            if (hr2 == 12)
                hr2 = 0;
            hr1++;
            hr2++;
            String hrstr1 = Integer.toString(hr1);
            String hrstr2 = Integer.toString(hr2);
            String time = hrstr1 + ":00 -" + hrstr2 + ":00";
            tv = txtViewList.get(i)[0];
            tv.setTextSize(15);
            tv.setText(time);
        }
    }

    public void addDayTableRow(int row) {
        long [] dailyMaxMin = {maxDayTemp,minDayTemp,maxNightTemp,minNightTemp};
        TextView tv;
        int [] temp = new int[4];
        temp[0] = hrHighTemps[row];
        temp[1] = hrLowTemps[row];
        temp[2] = hrHighTemps[row+12];
        temp[3] = hrLowTemps[row+12];
        for (int j = 0; j < 4; j++) {
            tv = txtViewList.get(row)[j+1];
            tv.setTextSize(15);
            tv.setTextColor(Color.BLACK);
            if (temp[j] > 0 && temp[j] < 9999) {
                boolean high = (j == 0 || j == 2);
                if ((high) && temp[j] == dailyMaxMin[j])
                    tv.setTextColor(Color.RED);
                else if (!high && temp[j] == dailyMaxMin[j])
                    tv.setTextColor(Color.BLUE);
                tv.setText(TempActivity.getTempStr(temp[j], false));
            } else
                tv.setText("-");
        }
    }

    public void addDayTableData(ArrayList<long[]> timeDataList, ArrayList<int[]> tempDataList, Date displayedDate) {
        for (int i=0; i < 24; i++) {
            hrHighTemps[i] = 0;
            hrLowTemps[i] = 9999;
        }
        cal.setTime(displayedDate);
        String dateStr = new SimpleDateFormat("MMMM d, yyyy", Locale.US).
                format(cal.getTimeInMillis());
        dateTV.setText(dateStr);
        long beginTime = cal.getTimeInMillis()/1000;
        cal.add(Calendar.DAY_OF_MONTH, 1);
        long endTime = cal.getTimeInMillis()/1000;
        for (int i = 0; i < tempDataList.size(); i++) {
            long [] timeDataArray = timeDataList.get(i);
            int [] tempDataArray = tempDataList.get(i);
            if (timeDataArray[0] >= beginTime  && timeDataArray[0] < endTime)
                processDayTemp(timeDataArray, tempDataArray);
        }

        for (int i = 0; i < tableRows; i++)
            addDayTableRow(i);
    }

    public void processDayTemp(long [] timeDataArray, int [] tempDataArray) {
        long timeH = timeDataArray[0];
        int tempH = tempDataArray[0];
        long timeL = timeDataArray[1];
        int tempL = tempDataArray[1];
        cal.setTimeInMillis(timeH * 1000);
        int hrOfDay = cal.get(Calendar.HOUR_OF_DAY);
        // 6am or after and before midnight
        if (hrOfDay >= 6) {
            hrHighTemps[hrOfDay - 6] = Math.max(tempH, hrHighTemps[hrOfDay - 6]);
            hrLowTemps[hrOfDay - 6] = Math.min(tempL, hrLowTemps[hrOfDay - 6]);
            if (hrOfDay < 18) {
                maxDayTemp = Math.max(tempH, maxDayTemp);
                minDayTemp = Math.min(tempL, minDayTemp);
            } else {
                maxNightTemp = Math.max(tempH, maxNightTemp);
                minNightTemp = Math.min(tempL, minNightTemp);
            }
        }
        // after midnight and before six of next day
        else {
            hrHighTemps[hrOfDay + 18] = Math.max(tempH, hrHighTemps[hrOfDay + 18]);
            hrLowTemps[hrOfDay + 18] = Math.min(tempL, hrLowTemps[hrOfDay + 18]);
            maxNightTemp = Math.max(tempH, maxNightTemp);
            minNightTemp = Math.min(tempL, minNightTemp);
        }
    }
}
