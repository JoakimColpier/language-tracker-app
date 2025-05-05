package com.example.languagelistenings.ui.data;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.languagelistenings.ConnectionClass;
import com.example.languagelistenings.LanguageDict;
import com.example.languagelistenings.R;
import com.example.languagelistenings.databinding.FragmentCommentBinding;
import com.example.languagelistenings.databinding.FragmentDataBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CommentFragment  extends Fragment {

    private FragmentCommentBinding binding;
    private static LanguageDict languageDict = new LanguageDict();
    private String date;
    static ConnectionClass connectionClass = new ConnectionClass();
    static Connection con = connectionClass.CONN();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCommentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Retrieve arguments (e.g., the date)
        date = getArguments().getString("date");

        // Get result set
        ResultSet rs = retrieveCommentInfo(date);

        // Add comment info
        TextView commentInfo = binding.commentInfo;
        DateFormatter dateFormatter = new DateFormatter(date, "yyyy-MM-dd", languageDict.getCurrentLanguageInfo().getLocale());
        commentInfo.setText(dateFormatter.getDateString());

        // Add comments to popup
        TableLayout tableLayout = root.findViewById(R.id.table);

        try {
            Integer i=0;
            Integer bgColor, fontColor;
            Integer[][] colors = null;
            while (rs.next()) {
                // Get comment
                String comment = rs.getString("comment");
                String time = rs.getString("amount") + " min";
                // Create and initialize table row
                TableRow tr_head = new TableRow(root.getContext());
                tr_head.setMinimumHeight(120);
                tr_head.setGravity(Gravity.CENTER_VERTICAL);
                TextView label_comment = new TextView(root.getContext());
                TextView label_time = new TextView(root.getContext());
                // Fix colors
                colors = languageDict.getTableColors(tr_head, colors);
                if (i%2==1) { bgColor=colors[0][0]; fontColor=colors[0][1]; }
                else { bgColor=colors[1][0]; fontColor=colors[1][1]; }
                tr_head.setBackgroundColor(bgColor);
                label_comment.setTextColor(fontColor);
                label_time.setTextColor(fontColor);
                // Time modifiers
                label_time.setText(time);
                label_time.setTextSize(20);
                label_time.setTypeface(null, Typeface.BOLD);
                label_time.setLayoutParams(new TableRow.LayoutParams(
                        220, // fixed width in pixels
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                // Comment modifiers
                if (comment.equals("")) {
                    comment = "~ No comment ~";
                    label_comment.setTypeface(null, Typeface.ITALIC);
                }
                label_comment.setText(comment);
                label_comment.setTextSize(20);
                label_comment.setSingleLine(false); // Important: allow multi-line
                label_comment.setMaxLines(5);       // Optional: limit number of lines
                label_comment.setEllipsize(null);   // Optional: no ellipsis
                label_comment.setLayoutParams(new TableRow.LayoutParams(
                        0, // Weight-based width
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f // Use remaining horizontal space
                ));
                // Add to table row
                tr_head.addView(label_time);
                tr_head.addView(label_comment);
                // Add to table
                tableLayout.addView(tr_head, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));
                i++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return root;
    }

    public ResultSet retrieveCommentInfo(String date) {
        try {
            PreparedStatement statement =
                    con.prepareStatement(
                            "SELECT amount, comment FROM " + languageDict.getCurrentLanguageInfo().getDbName() + " WHERE dt = ? :: DATE"
                    );
            statement.setString(1, date);
            return statement.executeQuery();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enable up (back) button in the action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Comment"); // Or dynamic title
        }

        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_commentFragment_to_dataFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class DateFormatter {
        private String dateStr;

        LocalDate date;
        private String dayOfWeek;
        private int dayOfMonth;
        private String month;
        private int year;
        private int weekNumber;

        public DateFormatter(String dateStr, String pattern, Locale locale) {
            this.dateStr = dateStr;

            // Parse input string to LocalDate
            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            // Get parts of the date
            dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
            dayOfMonth = date.getDayOfMonth();
            month = date.getMonth().getDisplayName(TextStyle.FULL, locale);
            year = date.getYear();

            // Calculate ISO week number
            weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        }

        public String getDateString() {
            return String.format("%s %d %s %d, week no. %d", dayOfWeek, dayOfMonth, month, year, weekNumber);
        }
    }
}
