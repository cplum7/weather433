package w433.tempDisplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static w433.tempDisplay.TempActivity.urlAddr;

public class TempFragment extends Fragment {
    private TextView txtview1;
    private TextView txtview2;
    private TextView txtview3;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.fragment_current_temp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        txtview1 = requireView().findViewById(R.id.temp);
        txtview2 = requireView().findViewById(R.id.date);
        txtview3 = requireView().findViewById(R.id.urlMsg);
        if (urlAddr.isEmpty())
            txtview3.setText("Click on 3 dot icon in upper right, then settings to enter database URL");
    }

    public void showTemp(long curTime, long curTemp) {
        long timeMS = curTime * 1000;
        Date date = new Date(timeMS);
        String dateStr = new SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.US).format(date);
        String tempStr = TempActivity.getTempStr(curTemp, true);
        txtview1.setText(tempStr);
        txtview2.setText(dateStr);
        txtview3.setText("");
    }
}