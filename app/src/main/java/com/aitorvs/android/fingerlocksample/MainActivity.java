/*
 * Copyright (c) 2016 Aitor Viana Sanchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.aitorvs.android.fingerlocksample;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aitorvs.android.fingerlock.FingerLock;
import com.aitorvs.android.fingerlock.FingerLockResultCallback;
import com.aitorvs.android.fingerlock.FingerLockManager;
import com.aitorvs.android.fingerlock.FingerprintDialog;


public class MainActivity extends AppCompatActivity
        implements FingerLockResultCallback,
        FingerprintDialog.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_NAME = "FingerLockAppKey";
    private TextView mStatus;
    private Button mButton;
    private FingerLockManager mFingerLock;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatus = (TextView) findViewById(R.id.status);
        mButton = (Button) findViewById(R.id.beginAuthentication);
        Button useDialog = (Button) findViewById(R.id.useDialog);

        // initialize the library and keep a reference to it
        mFingerLock = FingerLock.initialize(this, KEY_NAME);

        if (useDialog != null) {
            useDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new FingerprintDialog.Builder()
                            .with(MainActivity.this)
                            .setKeyName(KEY_NAME)
                            .setRequestCode(69)
                            .show();
                }
            });
        }
    }

    @Override
    public void onFingerLockError(@FingerLock.FingerLockErrorState int errorType, Exception e) {
        switch (errorType) {
            case FingerLock.FINGERPRINT_PERMISSION_DENIED:
                // USE_PERMISSION is denied by the user, fallback to password authentication
            case FingerLock.FINGERPRINT_ERROR_HELP:
                // there's some kind of recoverable error that can be solved. Call e.getMessage()
                // to get help about the error
            case FingerLock.FINGERPRINT_NOT_RECOGNIZED:
                // The fingerprint was not recognized, try another one
            case FingerLock.FINGERPRINT_NOT_SUPPORTED:
                // Fingerprint authentication is not supported by the device. Fallback to password
                // authentication
            case FingerLock.FINGERPRINT_REGISTRATION_NEEDED:
                // There are no fingerprints registered in this device.
                // Go to Settings -> Security -> Fingerprint and register at least one
            case FingerLock.FINGERPRINT_UNRECOVERABLE_ERROR:
                // Unrecoverable internal error occurred. Unregister and register back
                mStatus.setText(getString(R.string.status_error, e.getMessage()));
                break;
        }
    }

    @Override
    public void onFingerLockAuthenticationSucceeded() {
        mStatus.setText(R.string.status_authenticated);

        // Setup button to start listening again
        mButton.setText(R.string.start_scanning);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerLock.start();
            }
        });
        // Close App
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onFingerLockReady() {
        mStatus.setText(R.string.status_ready);
        mButton.setText(R.string.start_scanning);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerLock.start();
            }
        });
        mButton.setEnabled(true);
    }

    @Override
    public void onFingerLockScanning(boolean invalidKey) {
        // clicking the button will stop the scanning
        mButton.setText(R.string.stop_scanning);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop listening
                mFingerLock.stop();
                mStatus.setText(R.string.status_ready);
                // Clicking the button again will start listening again
                mButton.setText(R.string.start_scanning);
                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFingerLock.start();
                    }
                });
            }
        });

        mStatus.setText(invalidKey ? R.string.status_scanning_new : R.string.status_scanning);
    }

    // Dialog callbacks

    @Override
    public void onFingerprintDialogAuthenticated() {
        Toast.makeText(this, R.string.dialog_authenticated, Toast.LENGTH_LONG).show();
        // Close App
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    @Override
    public void onFingerprintDialogVerifyPassword(final FingerprintDialog dialog, final String password) {
        // Simulate exchange with backend
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.notifyPasswordValidation(password.equals("12345678"));
            }
        }, 1000);
    }

    @Override
    public void onFingerprintDialogStageUpdated(FingerprintDialog dialog, FingerprintDialog.Stage stage) {
        Log.d(TAG, "Dialog stage: " + stage.name());
    }

    @Override
    public void onFingerprintDialogCancelled() {
        Toast.makeText(this, R.string.dialog_cancelled, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do nothing or catch the keys you want to block
        return false;
    }
}
