package com.klinker.android.twitter_l.ui.search;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.SearchPagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.settings.SettingsActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.utils.Utils;

import java.lang.reflect.Field;

import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Created by luke on 11/29/13.
 */
public class SearchPager extends Activity {

    private SearchPagerAdapter mSectionsPagerAdapter;
    public AppSettings settings;
    public Activity context;
    public SharedPreferences sharedPrefs;
    public ActionBar actionBar;
    public boolean translucent;
    public ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        context = this;
        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        settings = AppSettings.getInstance(this);

        try {
            searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
        } catch (Exception e) {
            searchQuery = "";
        }

        if (searchQuery == null) {
            searchQuery = "";
        }

        handleIntent(getIntent());

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE || getResources().getBoolean(R.bool.isTablet))) {
            translucent = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            try {
                int immersive = android.provider.Settings.System.getInt(getContentResolver(), "immersive_mode");

                if (immersive == 1) {
                    translucent = false;
                }
            } catch (Exception e) {
            }
        } else {
            translucent = false;
        }

        Utils.setUpTweetTheme(context, settings);

        setContentView(R.layout.search_pager);

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.search));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        View statusBar = findViewById(R.id.activity_status_bar);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        statusBar.setVisibility(View.VISIBLE);
        statusBar.setBackgroundColor(settings.themeColors.primaryColorDark);

        int statusBarHeight = Utils.getStatusBarHeight(context);

        LinearLayout.LayoutParams statusParams = (LinearLayout.LayoutParams) statusBar.getLayoutParams();
        statusParams.height = statusBarHeight;
        statusBar.setLayoutParams(statusParams);

        mSectionsPagerAdapter = new SearchPagerAdapter(getFragmentManager(), context, onlyStatus, onlyProfile, searchQuery, translucent);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOffscreenPageLimit(3);

        PagerSlidingTabStrip strip = (PagerSlidingTabStrip) findViewById(R.id.pager_tab_strip);
        //PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
        strip.setShouldExpand(true);
        strip.setBackgroundColor(settings.themeColors.primaryColor);
        strip.setTextColorResource(R.color.white);
        strip.setIndicatorColor(settings.themeColors.accentColor);
        strip.setTextSize((int)getResources().getDimension(R.dimen.pager_tab_strip_text));
        strip.setViewPager(mViewPager);

        int height = Utils.getActionBarHeight(this);
        strip.setTranslationY(height);
        mViewPager.setTranslationY(height);

        mViewPager.setCurrentItem(1);

        Utils.setActionBar(context, true);

        if (onlyProfile) {
            mViewPager.setCurrentItem(2);
        }
    }

    public String searchQuery = "";
    private boolean onlyStatus = false;
    private boolean onlyProfile = false;

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
            suggestions.saveRecentQuery(searchQuery, null);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String uriString = uri.toString();
            if (uriString.contains("status/")) {
                long id;
                String replace = uriString.substring(uriString.indexOf("status")).replace("status/", "").replaceAll("photo/*", "");
                if (replace.contains("/")) {
                    replace = replace.substring(0, replace.indexOf("/"));
                } else if (replace.contains("?")) {
                    replace = replace.substring(0, replace.indexOf("?"));
                }
                try {
                    id = Long.parseLong(replace);
                } catch (Exception e) {
                    id = 0l;
                }
                searchQuery = id + "";
                onlyStatus = true;
            } else if (!uriString.contains("q=") && !uriString.contains("screen_name%3D")) { // going to try searching for users i guess
                String name = uriString.substring(uriString.indexOf(".com/"));
                name = name.replaceAll("/", "").replaceAll(".com", "");
                searchQuery = name;
                onlyProfile = true;
                Log.v("talon_searching", "only profile");
            } else if (uriString.contains("q=")){
                try {
                    String search = uri.getQueryParameter("q");

                    if (search != null) {
                        searchQuery = search;
                        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                        suggestions.saveRecentQuery(searchQuery, null);
                    } else {
                        searchQuery = "";
                    }

                } catch (Exception e) {

                }
            } else {
                try {
                    String search = uriString;

                    search = search.substring(search.indexOf("screen_name%3D") + 14);
                    search = search.substring(0, search.indexOf("%"));

                    if (search != null) {
                        searchQuery = search;

                        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                        suggestions.saveRecentQuery(searchQuery, null);
                    } else {
                        searchQuery = "";
                    }

                    onlyProfile = true;
                } catch (Exception e) {

                }
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        removeKeyboard();
        actionBar.setDisplayShowHomeEnabled(false);

        Log.v("talon_searching", "on new intent, query: " + searchQuery);

        // since this is the single top activity, it won't launch a new one to research,
        // so we have to do the re search stuff when it receives the new intent
        /*Intent broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
        broadcast.putExtra("query", searchQuery);
        context.sendBroadcast(broadcast);*/
        //recreate();
    }

    @Override
    public void onResume() {
        super.onResume();

        removeKeyboard();
    }

    public void removeKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        } catch (Exception e) {

        }
    }

    private SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);

        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView view = (ImageView) searchView.findViewById(searchImgId);
        view.setImageResource(R.drawable.ic_action_search_dark);

        return true;
    }

    public static final int SETTINGS_RESULT = 101;



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                onBackPressed();
                return true;

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

            case R.id.menu_save_search:
                Toast.makeText(context, getString(R.string.saving_search), Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
                            twitter.createSavedSearch(searchQuery);

                            ((Activity)context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, getString(R.string.success), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (TwitterException e) {
                            // something went wrong
                        }
                    }
                }).start();
                return super.onOptionsItemSelected(item);

            case R.id.menu_compose_with_search:
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", searchQuery);
                startActivity(compose);
                return  super.onOptionsItemSelected(item);

            case R.id.menu_search:
                //overridePendingTransition(0,0);
                //finish();
                //overridePendingTransition(0,0);
                //return super.onOptionsItemSelected(item);

            case R.id.menu_pic_filter:
                if (!item.isChecked()) {
                    searchQuery += " filter:links twitter.com";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace("filter:links", "").replace("twitter.com", "");
                    item.setChecked(false);
                }

                Intent broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
                broadcast.putExtra("query", searchQuery);
                context.sendBroadcast(broadcast);

                return super.onOptionsItemSelected(item);

            case R.id.menu_remove_rt:
                if (!item.isChecked()) {
                    searchQuery += " -RT";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace(" -RT", "");
                    item.setChecked(false);
                }

                broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
                broadcast.putExtra("query", searchQuery);
                context.sendBroadcast(broadcast);

                return super.onOptionsItemSelected(item);

            case R.id.menu_show_top_tweets:
                if (!item.isChecked()) {
                    searchQuery += " TOP";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace(" TOP", "");
                    item.setChecked(false);
                }

                broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
                broadcast.putExtra("query", searchQuery);
                context.sendBroadcast(broadcast);

                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, SearchPager.class);
        restart.putExtra(SearchManager.QUERY, searchQuery);
        restart.setAction(Intent.ACTION_SEARCH);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}