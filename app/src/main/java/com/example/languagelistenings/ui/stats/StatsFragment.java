package com.example.languagelistenings.ui.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.languagelistenings.ConnectionClass;
import com.example.languagelistenings.LanguageDict;
import com.example.languagelistenings.databinding.FragmentStatsBinding;
import com.example.languagelistenings.ui.data.DataFragment;
import com.google.android.material.chip.ChipGroup;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    ConnectionClass connectionClass = new ConnectionClass();

    private static LanguageDict languageDict = new LanguageDict();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        GraphView graphView = binding.idGraphView;

        //graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);

        try {
            addGraph("All", true, false, false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        RadioGroup radioGroup = binding.radioGroup;
        final String[] last_keyword = {"All"}; // Don't know why this must be an array tbh
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find button text
                RadioButton pressedButton = root.findViewById(checkedId);
                String button_text = (String) pressedButton.getText();
                // Choose which graph to plot
                String keyword;
                if (button_text.equals("All data points")) {keyword="All";}
                else if (button_text.equals("Last 7 days")) {keyword="7 days";}
                else {keyword="30 days";};
                last_keyword[0]=keyword;
                // Find which chips are active
                Boolean chip_day_active = binding.chipDays.isChecked();
                Boolean chip_week_active = binding.chipWeeks.isChecked();
                Boolean chip_month_active = binding.chipMonths.isChecked();
                // Plot graph
                try {
                    addGraph(last_keyword[0], chip_day_active, chip_week_active, chip_month_active);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        ChipGroup chipGroup = binding.chipGroup;
        chipGroup.setSelectionRequired(true);
        chipGroup.setOnCheckedStateChangeListener((chpGrp, id) -> {
            // Find which chips are active
            Boolean chip_day_active = binding.chipDays.isChecked();
            Boolean chip_week_active = binding.chipWeeks.isChecked();
            Boolean chip_month_active = binding.chipMonths.isChecked();
            // Plot graph
            try {
                addGraph(last_keyword[0], chip_day_active, chip_week_active, chip_month_active);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return root;
    }

    public void addGraph(String settings, Boolean chip_day_active, Boolean chip_week_active, Boolean chip_month_active) throws SQLException {
        // Get data
        ResultSet rs = connectionClass.dbGetLong();
        if (rs==null || !rs.next()) { return; }
        // Define graph view
        GraphView graphView = binding.idGraphView;
        graphView.removeAllSeries(); // Reinitialize graph
        // Find first date
        rs.last();
        Date dt = rs.getDate("dt");
        rs.beforeFirst();
        // Calculate date difference between first date and today
        LocalDateTime dt_today_local = LocalDateTime.now();
        LocalDateTime dt_local = LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault());
        long daysBetween = Duration.between(dt_local, dt_today_local).toDays();
        // Add datapoints to graph, amount varies by settings
        DataPoint[] dataPoints;
        Integer amount_i;
        if (settings.equals("All")){
            amount_i = (int) daysBetween;
        } else if (settings.equals("7 days")) {
            amount_i = 6;
        } else { // 30 days
            amount_i = 30;
        };
        dataPoints = new DataPoint[amount_i+1];
        graphView.getViewport().setMaxX(amount_i);
        Integer i=0;
        // Daily
        while (rs.next() && i<=amount_i) {
            // Check if prior day equal to next database date
            Date db_dt = rs.getDate("dt");
            LocalDateTime db_dt_local = LocalDateTime.ofInstant(db_dt.toInstant(), ZoneId.systemDefault());
            while (db_dt_local.isBefore(dt_today_local.minusDays(1)) && i<=amount_i) {
                dataPoints[i] = new DataPoint(i,0);
                dt_today_local=dt_today_local.minusDays(1);
                i++;
            }
            if (i>amount_i) {break;};
            // Add to value
            String value = rs.getString("sumAmount");
            dataPoints[i] = new DataPoint(i,Integer.parseInt(value));
            dt_today_local=dt_today_local.minusDays(1);
            i++;
        };
        rs.beforeFirst(); // reinitialize
        LineGraphSeries<DataPoint> series_day = new LineGraphSeries<DataPoint>(dataPoints);

        // Weekly
        dt_today_local = LocalDateTime.now();
        DataPoint[] dataPoints_week = new DataPoint[amount_i+1];
        Integer days_in_week; Double weekly_average; i=0;
        while (i<=amount_i) {
            weekly_average=dataPoints[i].getY();
            dt_today_local=dt_today_local.minusDays(1); i++; days_in_week=1;
            while (i<=amount_i && !dt_today_local.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                weekly_average+=dataPoints[i].getY();
                dt_today_local=dt_today_local.minusDays(1); i++; days_in_week++;
            }
            weekly_average=weekly_average/days_in_week;
            for (int j=0; j<days_in_week; j++) {
                dataPoints_week[i-days_in_week+j] = new DataPoint(i-days_in_week+j,weekly_average);
            }
        }
        LineGraphSeries<DataPoint> series_week = new LineGraphSeries<DataPoint>(dataPoints_week);

        // Monthly
        dt_today_local = LocalDateTime.now();
        DataPoint[] dataPoints_month = new DataPoint[amount_i+1];
        Integer days_in_month; Double monthly_average; i=0;
        while (i<=amount_i) {
            monthly_average=dataPoints[i].getY();
            dt_today_local=dt_today_local.minusDays(1); i++; days_in_month=1;
            while (i<=amount_i) {
                monthly_average+=dataPoints[i].getY();
                dt_today_local=dt_today_local.minusDays(1); i++; days_in_month++;
                if (dt_today_local.plusDays(1).getDayOfMonth()==1) {break;}
            }
            monthly_average=monthly_average/days_in_month;
            for (int j=0; j<days_in_month; j++) {
                dataPoints_month[i-days_in_month+j] = new DataPoint(i-days_in_month+j,monthly_average);
            }
        }
        LineGraphSeries<DataPoint> series_month = new LineGraphSeries<DataPoint>(dataPoints_month);

        // Cosmetic changes to graph view
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graphView);
        dt_today_local = LocalDateTime.now();
        binding.LabelNewDate.setText(dt_today_local.format(DateTimeFormatter.ofPattern("dd/MM-yy")));
        staticLabelsFormatter.setHorizontalLabels(new String[] {"",""});
        if (settings.equals("All")){
            binding.LabelOldestDate.setText(dt_local.format(DateTimeFormatter.ofPattern("dd/MM-yy")));
        } else if (settings.equals("7 days")) {
            binding.LabelOldestDate.setText(dt_today_local.minusDays(7).format(DateTimeFormatter.ofPattern("dd/MM-yy")));
        } else { // 30 days
            binding.LabelOldestDate.setText(dt_today_local.minusDays(30).format(DateTimeFormatter.ofPattern("dd/MM-yy")));
        };
        graphView.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        // Style series
        series_day.setColor(Color.BLACK);
        series_day.setThickness(3);
        series_week.setColor(Color.RED);
        series_week.setThickness(8);
        series_month.setColor(Color.BLUE);
        series_month.setThickness(8);

        // Add data points
        if (chip_day_active){graphView.addSeries(series_day);}
        if (chip_week_active){graphView.addSeries(series_week);}
        if (chip_month_active){graphView.addSeries(series_month);}
    }

    public void changeStatsTitle() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) { actionBar.setTitle(languageDict.getCurrentLanguageInfo().getName() + " Statistics"); }
    }

    @Override
    public void onResume() {
        super.onResume();
        changeStatsTitle();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}