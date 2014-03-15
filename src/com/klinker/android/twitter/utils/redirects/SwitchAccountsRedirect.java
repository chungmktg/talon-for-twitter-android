package com.klinker.android.twitter.utils.redirects;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;

public class SwitchAccountsRedirect extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        if (currentAccount == 1) {
            sharedPrefs.edit().putInt("current_account", 2).commit();
        } else {
            sharedPrefs.edit().putInt("current_account", 1).commit();
        }

        int page1Type = sharedPrefs.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
        int page2Type = sharedPrefs.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);

        int extraPages = 0;
        if (page1Type != AppSettings.PAGE_TYPE_NONE) {
            extraPages++;
        }

        if (page2Type != AppSettings.PAGE_TYPE_NONE) {
            extraPages++;
        }

        sharedPrefs.edit().putBoolean("open_a_page", true).commit();
        sharedPrefs.edit().putInt("open_what_page", extraPages + 1).commit();

        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        main.putExtra("switch_account", true);
        overridePendingTransition(0, 0);
        finish();
        startActivity(main);
    }
}