package com.example.taek.blescanner_bylyt.UI;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.taek.blescanner_bylyt.R;
import com.example.taek.blescanner_bylyt.Services.BLEScanService;
import com.example.taek.blescanner_bylyt.Utils.BackPressCloseHandler;
import com.example.taek.blescanner_bylyt.Utils.Constants;
import com.example.taek.blescanner_bylyt.Utils.IncomingHandler;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Close the app when back button twice pressed
    private BackPressCloseHandler backPressCloseHandler;

    private Context context_mainActivity = this;
    private String TAG = "MainActiviy";
    private Messenger incomingMessenger;
    private ServiceConnection mServiceConnection;
    public Messenger mMessenger;

    private boolean isConnectedService;

    // Navigation header information
    public static TextView tvNavHeadId;
    public static TextView tvNavHeadAddr;

    public void connectMessenger() {
        Log.d(TAG, "connectMessenger(): call connectMessenger");
        ComponentName cn = new ComponentName(context_mainActivity, BLEScanService.class);
        Intent intent = new Intent();
        intent.setComponent(cn);

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "connectMessenger(): connected to service");
                isConnectedService = true;
                mMessenger = new Messenger(service);

                // start BLE scanning
                try {
                    Message msg = Message.obtain(null, Constants.HANDLE_MESSAGE_TYPE_BLE_SCAN);
                    msg.replyTo = incomingMessenger;
                    mMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        context_mainActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isConnectedService = false;

        initUIElements();

        // switch_scan = (Switch) findViewById(R.id.BLEScanSwitch);
        // editView_saveData = (TextView) findViewById(R.id.editView_dataName);
        // checkbox_dataName = (CheckBox) findViewById(R.id.checkBox_saveData);
        // editView_saveData.setEnabled(false);

        // BLE 관련 Permission 주기
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Android M Permission check
            if(this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton("Ok", null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }

            incomingMessenger = new Messenger(new IncomingHandler(Constants.HANDLER_TYPE_ACTIVITY, context_mainActivity));

            backPressCloseHandler = new BackPressCloseHandler(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int reqeustCode, String permission[], int[] grantResults){
        switch (reqeustCode){
            case Constants.PERMISSION_REQUEST_COARSE_LOCATION:{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("permission", "coarse location permission granted");
                }else{
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, " +
                            "this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton("Ok", null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    /* DrawerLayout object */
    private DrawerLayout drawerLayout;

    /* Fragments objects */
    public MainFragment fragMain;
    public SetupFragment fragSetup;

    /* Navigation View object */
    private NavigationView navigationView;

    private void initUIElements() {
        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Fragments
        fragMain = new MainFragment();
        fragMain.sendContext(context_mainActivity);
        // fragMain = MainFragment.newInstance(context_mainActivity);
        fragSetup = new SetupFragment();
        fragSetup.sendContext(context_mainActivity);
        //fragSetup = SetupFragment.newInstance();

        // DrawerLayout
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout,toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Navigation View
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set the header of the Navigation View
        View navHeaderView = navigationView.inflateHeaderView(R.layout.nav_header);
        tvNavHeadId = (TextView) navHeaderView.findViewById(R.id.nav_head_id);
        tvNavHeadAddr = (TextView) navHeaderView.findViewById(R.id.nav_head_bluetooth_addr);

        // Set the menu of the Navigation View
        navigationView.inflateMenu(R.menu.nav_menu);

        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragMain)
                .detach(fragMain).attach(fragMain)
                .commit();
    }

    /* Essential overriding methods */
    @Override
    public void onBackPressed() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // super.onBackPressed();
            backPressCloseHandler.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            moveTaskToBack(true);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        FragmentManager fragmentManager = getFragmentManager();
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_main:
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragMain)
                        .detach(fragMain).attach(fragMain)
                        .commit();
                break;
            case R.id.nav_setup:
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragSetup)
                        .detach(fragSetup).attach(fragSetup)
                        .commit();
                break;
            default:
                break;
        }

        // Change title on appbar
        if (id == R.id.nav_main)
            setTitle(R.string.app_name);
        else
            setTitle(item.getTitle());

        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isConnectedService)
            context_mainActivity.unbindService(mServiceConnection);
    }
}
