package com.example.languagelistenings.ui.data;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.languagelistenings.ConnectionClass;
import com.example.languagelistenings.LanguageDict;
import com.example.languagelistenings.R;
import com.example.languagelistenings.databinding.FragmentDataBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class DataFragment extends Fragment {

    private FragmentDataBinding binding;

    static ConnectionClass connectionClass = new ConnectionClass();
    static Connection con = connectionClass.CONN();
    static ResultSet resultSet = connectionClass.dbGetLong();
    static View root;
    static TableLayout tableLayout;
    LayoutInflater inflater;
    static TextView totalTimeView;

    private static LanguageDict languageDict = new LanguageDict();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDataBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        // Add table data
        this.inflater = inflater;
        tableLayout = binding.mainTable;
        totalTimeView = binding.totalTimeLabel;
        Integer totalTime;
        totalTime = createTable(root, tableLayout, inflater);
        Integer hours = totalTime/60;
        Integer minutes = totalTime%60;
        totalTimeView.setText("Total Time: "+hours.toString()+"h"+minutes.toString()+" min");

        // Initialize button to switch to add fragment
        binding.faba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_dataFragment_to_addFragment);
            }
        });

        return root;
    }

    // Create table
    public Integer createTable(View root, TableLayout tableLayout, LayoutInflater inflater) {
        Integer i = 0;
        Integer totalTime = 0;
        Integer bgColor, fontColor;
        resultSet = connectionClass.dbGetLong();
        tableLayout.removeAllViews();
        TableRow tr_margin = new TableRow(DataFragment.root.getContext());
        tr_margin.setMinimumHeight(245);
        DataFragment.tableLayout.addView(tr_margin, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.FILL_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));

        try {
            Integer[][] colors = null;
            while (resultSet.next()) {
                // Create and initialize table row
                TableRow tr_head = new TableRow(root.getContext());
                colors = languageDict.getTableColors(tr_head, colors);
                if (i%2==1) { bgColor=colors[0][0]; fontColor=colors[0][1]; }
                else { bgColor=colors[1][0]; fontColor=colors[1][1]; }

                tr_head.setBackgroundColor(bgColor);
                tr_head.setMinimumHeight(120);
                tr_head.setGravity(Gravity.CENTER_VERTICAL);

                // Create textviews
                TextView label_date = new TextView(root.getContext());
                String date = resultSet.getString("dt");
                label_date.setText(date);
                label_date.setTextColor(fontColor);
                label_date.setTextSize(18);
                tr_head.addView(label_date);// add the column to the table row here

                TextView label_time = new TextView(root.getContext());
                String time = resultSet.getString("sumAmount");
                totalTime += Integer.valueOf(time);
                label_time.setText(time + " min");
                label_time.setTextColor(fontColor); // set the color
                label_time.setTextSize(18);
                tr_head.addView(label_time); // add the column to the table row here

                TextView label_comment = new TextView(root.getContext());
                String comment = resultSet.getString("com");
                label_comment.setText(comment);
                label_comment.setTextColor(fontColor); // set the color
                label_comment.setTextSize(18);
                tr_head.addView(label_comment); // add the column to the table row here

                // Create clickable table row (if comment)
                if (Objects.equals(comment, "YES")) {
                    tr_head.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {showCommentFragment(date);
                        }
                    });
                }
                tableLayout.addView(tr_head, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                i++;
            }
            resultSet.beforeFirst(); // reset result set
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return totalTime;
    }

    public void showCommentFragment(String date) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle bundle = new Bundle();
        bundle.putString("date", date);
        navController.navigate(R.id.action_dataFragment_to_commentFragment, bundle);
    }

    public void changeDataTitle() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) { actionBar.setTitle(languageDict.getCurrentLanguageInfo().getName() + " Data"); }
    }

    @Override
    public void onResume() {
        super.onResume();
        changeDataTitle();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}