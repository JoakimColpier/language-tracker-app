package com.example.languagelistenings.ui.data;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.languagelistenings.ConnectionClass;
import com.example.languagelistenings.LanguageDict;
import com.example.languagelistenings.R;
import com.example.languagelistenings.databinding.FragmentAddBinding;
import com.example.languagelistenings.databinding.FragmentChangeCommentBinding;
import com.google.android.material.snackbar.Snackbar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangeCommentFragment extends Fragment {
    private FragmentChangeCommentBinding binding;
    private View root;
    static ConnectionClass connectionClass = new ConnectionClass();
    static ResultSet resultSet = connectionClass.dbGetTotalAmount();
    LayoutInflater inflater;
    static TextView totalTimeView;
    static String totalTimeOld;

    private static LanguageDict languageDict = new LanguageDict();

    String date;
    String comment;
    Integer id;
    Integer amount;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChangeCommentBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        this.inflater = inflater;

        // Add previous values to text views
        final TextView textViewDate = binding.editTextDate;
        final TextView textViewTime = binding.editTextTime;
        final TextView textViewComment = binding.editTextComment;
        date = getArguments().getString("date");
        comment = getArguments().getString("comment");
        id = getArguments().getInt("id");
        amount = getArguments().getInt("amount");

        textViewDate.setText(date);
        textViewTime.setText(String.valueOf(amount));
        textViewComment.setText(comment);

        // Delete button
        binding.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Ask if sure
                askConsent(view, "delete", consent -> {
                    if (consent) {
                        // Delete value
                        try {
                            dbDelete();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        // Return to data
                        returnToData();
                    } else {
                        // Return to data
                        returnToData();
                    }
                });
            }
        });

        // Change button
        binding.buttonChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get values
                final TextView textViewDate = binding.editTextDate;
                final TextView textViewTime = binding.editTextTime;
                final TextView textViewComment = binding.editTextComment;

                String textDate = textViewDate.getText().toString();
                String textMin = textViewTime.getText().toString();
                String textComment = textViewComment.getText().toString();

                String[] args = new String[]{textDate, textMin, textComment, String.valueOf(id)};

                // Check date and min format
                if (!inputOK(view, textMin, textDate)) {
                    return;
                }

                // Check changes
                if (textDate.equals(date) &&
                        textMin.equals(String.valueOf(amount)) &&
                        textComment.equals(comment)) {
                    Snackbar.make(view, String.format("No changes were made."), Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .setAnchorView(R.id.buttonChange).show();
                    return;
                }

                // Ask if sure
                askConsent(view, "change", consent -> {
                    if (consent) {
                        // Show action completed
                        Snackbar.make(view, String.format("Changed input %d.", id), Snackbar.LENGTH_LONG)
                                .setAction("Action", null)
                                .setAnchorView(R.id.buttonChange).show();

                        // Add to db
                        dbChange(args);

                        // Return to data
                        returnToData();
                    } else {
                        // Return to data
                        returnToData();
                    }
                });
            }
        });

        return root;
    }

    public void dbDelete() throws SQLException {
        Connection con;
        ConnectionClass connectionClass = new ConnectionClass();
        con = connectionClass.CONN();

        String dbName = languageDict.getCurrentLanguageInfo().getDbName();
        try {
            PreparedStatement statement = con.prepareStatement("DELETE FROM " + dbName + " WHERE id=? :: INTEGER");
            statement.setString(1, String.valueOf(id));
            statement.execute();
            statement.close();

            connectionClass.setDirty(); // changes made to db
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void dbChange(String[] args) {
        Connection con;
        ConnectionClass connectionClass = new ConnectionClass();
        con = connectionClass.CONN();

        String dbName = languageDict.getCurrentLanguageInfo().getDbName();
        try {
            PreparedStatement statement = con.prepareStatement("UPDATE " + dbName + " SET dt=? :: DATE, amount=? :: INTEGER, comment=? :: VARCHAR WHERE id=? :: INTEGER");

            for (int i = 0; i < 4; i++) {
                statement.setString(i + 1, args[i]);
            }
            statement.execute();
            statement.close();

            connectionClass.setDirty(); // changes made to db
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface ConsentCallback {
        void onResult(boolean consent);
    }

    public void askConsent(View view, String keyword, ConsentCallback callback) {
        Context context = view.getContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to " + keyword + " comment?");

        builder.setPositiveButton("YES", (dialog, which) -> {
            callback.onResult(true);
            dialog.dismiss();
        });

        builder.setNegativeButton("NO", (dialog, which) -> {
            callback.onResult(false);
            dialog.dismiss();
        });

        builder.create().show();
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

    public void returnToData() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_changeCommentFragment_to_dataFragment);
    }

    private void changeTitle() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) { actionBar.setTitle("Change Comment " + id); }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        changeTitle();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_changeCommentFragment_to_dataFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
