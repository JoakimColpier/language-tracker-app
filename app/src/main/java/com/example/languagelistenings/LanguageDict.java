package com.example.languagelistenings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.widget.TableRow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

public class LanguageDict {
    private static HashMap<String, Language> languageDict = new HashMap<String, Language>();
    private static String currentLanguageId;
    private Context context;

    public LanguageDict(String id) {
        setCurrentLanguageId(id);
        generateDict();
    }

    public LanguageDict() {
        setCurrentLanguageId(currentLanguageId);
        generateDict();
    }

    public void addContext(Context context) {
        this.context=context.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        setCurrentLanguageId(prefs.getString("currentLanguageId", "nl"));
    }

    public static String getCurrentLanguageId() { return currentLanguageId; }

    public void addLanguage(String id,
                            String languageName,
                            String connectionUrl,
                            String dbName,
                            Integer flagMipMap,
                            Integer languageTheme,
                            Locale locale) {
        // Add to dict w/ id
        Language language = new Language(languageName, connectionUrl, dbName, flagMipMap, languageTheme, locale);
        languageDict.put(id, language);
    }

    public ArrayList<String> getNonCurrentLanguageNames() {
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(languageDict.keySet());
        keys.remove(currentLanguageId);
        return keys;
    }

    public void setCurrentLanguageId(String id) {
        currentLanguageId = id;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            prefs.edit().putString("currentLanguageId", currentLanguageId).apply();
        }
    }

    public Language getLanguageInfo(String id) { return languageDict.get(id); }

    public Language getCurrentLanguageInfo() { return getLanguageInfo(currentLanguageId); }

    public void generateDict() {
        String connectionUrl = BuildConfig.DB_URL;

        /////////////////////////////////////////////////
        // !! Change this when adding new languages !! //

        // Dutch
        this.addLanguage("nl",
                "Dutch",
                connectionUrl,
                "langs.Dutch",
                R.mipmap.nl_flag_round,
                R.style.Theme_LanguageListenings_NL,
                Locale.ENGLISH
        );

        // Spanish
        this.addLanguage("es",
                "Spanish",
                connectionUrl,
                "langs.Spanish",
                R.mipmap.es_flag_round,
                R.style.Theme_LanguageListenings_ES,
                Locale.ENGLISH
                );

        // Georgian
        this.addLanguage("ge",
                "Georgian",
                connectionUrl,
                "langs.Spanish", // TODO: change this
                R.mipmap.ge_flag_round,
                R.style.Theme_LanguageListenings_GE,
                Locale.ENGLISH
        );

        // !! End of changes !! //
        //////////////////////////
    }

    public static Integer[][] getTableColors(TableRow tr_head, Integer[][] color) {
        if (color != null) { return color; } // only run if haven't run before

        Context context = tr_head.getContext();
        // Primary
        TypedValue typedValuePrimaryFont = new TypedValue();
        TypedValue typedValuePrimaryContainer = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValuePrimaryFont, true);
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValuePrimaryContainer, true);
        Integer[] primaryVals = new Integer[]{typedValuePrimaryContainer.data, typedValuePrimaryFont.data};
        //Secondary
        TypedValue typedValueSecondaryFont = new TypedValue();
        TypedValue typedValueSecondaryContainer = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSecondaryContainer, typedValueSecondaryFont, true);
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondaryContainer, typedValueSecondaryContainer, true);
        Integer[] secondaryVals = new Integer[]{typedValueSecondaryContainer.data, typedValueSecondaryFont.data};

        return new Integer[][]{primaryVals, secondaryVals};
    }

    public class Language {
        private String languageName;
        private String connectionUrl;
        private String dbName;
        private Integer flagMipMap;
        private Integer languageTheme;
        private Locale locale;

        public Language(String ln,
                        String cu,
                        String db,
                        Integer fmm,
                        Integer lt,
                        Locale lc) {
            languageName = ln;
            connectionUrl = cu;
            dbName = db;
            flagMipMap = fmm;
            languageTheme = lt;
            locale = lc;
        }

        public String getName() { return languageName; }
        public String getUrl() { return connectionUrl; }
        public String getDbName() { return dbName; }
        public Integer getFlag() { return flagMipMap; }
        public Integer getTheme() { return languageTheme; }
        public Locale getLocale() { return locale; }
    }

}