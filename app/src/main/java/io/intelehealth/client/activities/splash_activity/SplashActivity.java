package io.intelehealth.client.activities.splash_activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.List;
import java.util.Locale;

import io.intelehealth.client.BuildConfig;
import io.intelehealth.client.R;
import io.intelehealth.client.activities.loginActivity.LoginActivity;
import io.intelehealth.client.activities.setupActivity.SetupActivity;
import io.intelehealth.client.dataMigration.SmoothUpgrade;
import io.intelehealth.client.utilities.Logger;
import io.intelehealth.client.utilities.SessionManager;

import static io.intelehealth.client.app.AppConstants.APP_VERSION_CODE;


public class SplashActivity extends AppCompatActivity {
    SessionManager sessionManager = null;
    ProgressDialog TempDialog;
    int i = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_activity);
//        Getting App language through the session manager
        sessionManager = new SessionManager(SplashActivity.this);

        String appLanguage = sessionManager.getAppLanguage();


        if (!appLanguage.equalsIgnoreCase("")) {
            Locale locale = new Locale(appLanguage);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }

        checkPerm();
        Crashlytics.log(Log.DEBUG, "tag", "message");
    }

    private void checkPerm() {
        PermissionListener permissionlistener = new PermissionListener() {

            @Override
            public void onPermissionGranted() {
//                Toast.makeText(SplashActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
//                Timer t = new Timer();
//                t.schedule(new splash(), 2000);

                TempDialog = new ProgressDialog(SplashActivity.this);
                TempDialog.setMessage("Data migrating...");
                TempDialog.setCancelable(false);
                TempDialog.setProgress(i);
                TempDialog.show();

                if (BuildConfig.VERSION_CODE <= APP_VERSION_CODE && sessionManager.isFirstTimeLaunched()) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 100ms
                            SmoothUpgrade smoothUpgrade = new SmoothUpgrade(SplashActivity.this);
                            boolean smoothupgrade = smoothUpgrade.checkingDatabase();
                            if (smoothupgrade) {
                                TempDialog.dismiss();
                                nextActivity();
                            } else {
                                TempDialog.dismiss();
                                nextActivity();
                            }

                        }
                    }, 2000);
                } else {
                    nextActivity();
                }

            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(SplashActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }

        };
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.SEND_SMS)
                .check();
    }

    private void nextActivity() {
        boolean setup = sessionManager.isSetupComplete();

        String LOG_TAG = "SplashActivity";
        Logger.logD(LOG_TAG, String.valueOf(setup));
        if (setup) {
            Logger.logD(LOG_TAG, "Starting login");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            Logger.logD(LOG_TAG, "Starting setup");
            Intent intent = new Intent(this, SetupActivity.class);
            startActivity(intent);
            finish();
        }
    }

}