package com.example.languagelistenings.ui.add;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.languagelistenings.ConnectionClass;
import com.example.languagelistenings.LanguageDict;
import com.example.languagelistenings.R;
import com.example.languagelistenings.databinding.FragmentAddBinding;
import com.google.android.material.snackbar.Snackbar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddFragment extends Fragment {
    private FragmentAddBinding binding;
    private View root;
    static ConnectionClass connectionClass = new ConnectionClass();
    static ResultSet resultSet = connectionClass.dbGetTotalAmount();
    LayoutInflater inflater;
    static TextView totalTimeView;
    static String totalTimeOld;

    private static LanguageDict languageDict = new LanguageDict();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        this.inflater = inflater;
        totalTimeView = binding.totalTimeLabel;

        // Add values
        if (connectionClass.isDirtyTimeListened()) {
            updateTimeView();
            connectionClass.setCleanTimeListened();
        } else { totalTimeView.setText(totalTimeOld); }

        String pattern = "yyyy-MM-dd";//"MM/dd/yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        String todayAsString = df.format(today);

        final TextView textViewDate = binding.editTextDate;
        textViewDate.setText(todayAsString);

        // Add button
        binding.buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get values
                final TextView textViewDate = binding.editTextDate;
                final TextView textViewTime = binding.editTextTime;
                final TextView textViewComment = binding.editTextComment;

                String textDate = textViewDate.getText().toString();
                String textMin = textViewTime.getText().toString();
                String textComment = textViewComment.getText().toString();

                String[] args = new String[]{textDate, textMin, textComment};

                // Check date and min format
                if (!inputOK(view, textMin, textDate)) {
                    return;
                }

                // Show action completed
                Snackbar.make(view, String.format("Added %s min with date %s", textMin, textDate), Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.buttonAdd).show();

                // Add to db
                dbAdd(args);

                // Reset fields
                textViewTime.setText("");
                textViewComment.setText("");

                // Update total time listened
                updateTimeView();
                connectionClass.setCleanTimeListened();
            }
        });

        // Switch button
        binding.faba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_addFragment_to_dataFragment);
            }
        });

        return root;
    }

    public void updateTimeView() {
        Integer totalTime = get_total_time();
        Integer hours = totalTime/60;
        Integer minutes = totalTime%60;
        totalTimeOld = "Total Time: "+hours.toString()+"h"+minutes.toString()+" min";
        totalTimeView.setText(totalTimeOld);
    }

    public void dbAdd(String[] args) {
        Connection con;
        ConnectionClass connectionClass = new ConnectionClass();
        con = connectionClass.CONN();

        String dbName = languageDict.getCurrentLanguageInfo().getDbName();
        try {
            PreparedStatement statement = con.prepareStatement("INSERT INTO " + dbName + " VALUES (langs.add_index('" + dbName + "'), ? :: DATE, ? :: INTEGER, ? :: VARCHAR)");

            for (int i = 0; i < 3; i++) {
                statement.setString(i + 1, args[i]);
            }
            statement.execute();
            statement.close();

            connectionClass.dbUpdate();
            connectionClass.setDirty();
            // resultSet = connectionClass.dbGetTotalAmount();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean inputOK(View view, String textMin, String textDate) {
        // Not empty
        if(textDate.trim().isEmpty()) {
            Snackbar.make(view, "ERROR! No date inputted.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.buttonAdd).show();
            return Boolean.FALSE;
        } else if (textMin.trim().isEmpty()) {
            Snackbar.make(view, "ERROR! No time inputted.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.buttonAdd).show();
            return Boolean.FALSE;
        }

        // Correct format
        Pattern patternDate = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$", Pattern.CASE_INSENSITIVE);
        Matcher matcherDate = patternDate.matcher(textDate);
        boolean matchFoundDate = matcherDate.find();
        if(!matchFoundDate) {
            Snackbar.make(view, "ERROR! Date has incorrect format (should be YYYY-MM-DD).", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.buttonAdd).show();
            return Boolean.FALSE;
        }
        Pattern patternMin = Pattern.compile("^[1-9][0-9]*$", Pattern.CASE_INSENSITIVE);
        Matcher matcherMin = patternMin.matcher(textMin);
        boolean matchFoundMin = matcherMin.find();
        if(!matchFoundMin) {
            Snackbar.make(view, "ERROR! Time has incorrect format (should be integer larger than 0). ", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.buttonAdd).show();
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private Integer get_total_time() {
        Integer totalTime=0;
        try {
            resultSet = connectionClass.dbGetTotalAmount();
            resultSet.next();
            if (resultSet.getString("total_time")!=null) { totalTime = Integer.valueOf(resultSet.getString("total_time")); }
            resultSet.beforeFirst();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return totalTime;
    }

    private void changeAddTitle() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) { actionBar.setTitle("Add to " + languageDict.getCurrentLanguageInfo().getName()); }
    }

    @Override
    public void onResume() {
        super.onResume();
        changeAddTitle();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
