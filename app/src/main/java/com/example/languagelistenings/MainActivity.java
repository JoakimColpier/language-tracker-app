package com.example.languagelistenings;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.languagelistenings.ui.data.DataFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.languagelistenings.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private LanguageDict languageDict = new LanguageDict("nl"); // "nl" is just the default language
    private static ConnectionClass connectionClass = new ConnectionClass();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check if language was passed from intent
        connectionClass.dbUpdate();
        connectionClass.setDirty();

        // Theme based on language
        languageDict.addContext(this);
        setTheme(languageDict.getCurrentLanguageInfo().getTheme());

        // Other
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set flags and texts depending on language
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);
        TextView headerTitle = headerView.findViewById(R.id.nav_header_title);
        ImageView flag = headerView.findViewById(R.id.imageView);
        LinearLayout flagHolder = headerView.findViewById(R.id.linearLayout);
        headerTitle.setText(languageDict.getCurrentLanguageInfo().getName());
        flag.setImageResource(languageDict.getCurrentLanguageInfo().getFlag());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_add, R.id.nav_data, R.id.nav_stats)
                .setOpenableLayout(drawer)
                .build();

        // Set up NavController with drawer and toolbar
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Flag now becomes button
        flagHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCircleColor(com.google.android.material.R.attr.colorSecondary);
                showLanguageSelectionMenu(view);
            }
        });
    }

    public void setCircleColor(Integer color_value) {
        View circleView = findViewById(R.id.frameLayout);
        GradientDrawable backgroundDrawable = (GradientDrawable) circleView.getBackground();

        // Resolve the colorPrimary from the current theme
        TypedValue typedValue = new TypedValue();
        Context context = circleView.getContext();
        context.getTheme().resolveAttribute(color_value, typedValue, true);

        // Set the color
        int color = typedValue.data;
        backgroundDrawable.setColor(color);
    }

    public void showLanguageSelectionMenu(View view) {
        LayoutInflater inflater = getLayoutInflater();
        View languageSelectionMenu = inflater.inflate(R.layout.language_selection_menu, null);
        View root = languageSelectionMenu.getRootView();
        TableRow tableRow = languageSelectionMenu.findViewById(R.id.flag_table_row);

        // Click to switch language
        ImageView original_image_view = languageSelectionMenu.findViewById(R.id.img_view);
        ViewGroup.LayoutParams img_view_params = original_image_view.getLayoutParams();
        tableRow.removeView(original_image_view);
        for (String lang : languageDict.getNonCurrentLanguageNames()) {
            ImageView img_view = new ImageView(root.getContext());
            img_view.setImageResource(languageDict.getLanguageInfo(lang).getFlag());
            img_view.setLayoutParams(img_view_params);

            img_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Reset circle color
                    setCircleColor(com.google.android.material.R.attr.colorPrimaryVariant);

                    // Restart the MainActivity to apply the language change
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    languageDict.setCurrentLanguageId(lang);
                    MainActivity.this.finish(); // Finish current instance
                    MainActivity.this.startActivity(intent); // Start fresh
                }
            });

            tableRow.addView(img_view);
        }

        // Create popup window
        final PopupWindow popupWindow = new PopupWindow(languageSelectionMenu,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true); // lets taps outside the popup also dismiss it
        popupWindow.showAsDropDown(view, -100, -30);

        // CLicking outside window
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setCircleColor(com.google.android.material.R.attr.colorPrimaryVariant);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}