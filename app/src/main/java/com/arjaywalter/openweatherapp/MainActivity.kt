package com.arjaywalter.openweatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import androidx.databinding.DataBindingUtil
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.arjaywalter.openweatherapp.databinding.MainActivityBinding
import com.arjaywalter.openweatherapp.model.WeatherItem
import com.arjaywalter.openweatherapp.ui.main.MainViewModel
import com.arjaywalter.openweatherapp.util.toIconUrl
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.main_activity.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.DateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest

    private lateinit var locationCallback: LocationCallback

    /**
     * Represents a geographical location.
     */
    protected var lastLocation: Location? = null

    // Lazy Inject ViewModel
    private val viewModel: MainViewModel by viewModel()

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The layout for this activity is a Data Binding layout so it needs to be inflated using
        // DataBindingUtil.
        binding = DataBindingUtil.setContentView(
            this, R.layout.main_activity
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()

    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                lastLocation = locationResult?.lastLocation
                locationResult?.locations?.let {
                    it.forEach { location ->
                        lastLocation = location
                    }
                }
                loadWeather()
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onStart() {
        super.onStart()
        if (checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
    }

    override fun onStop() {
        super.onStop()
        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { locationSettingsResponse ->
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
            .addOnFailureListener { exception ->

                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        exception.startResolutionForResult(
                            this@MainActivity,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.i(TAG, "User agreed to make required location settings changes.")
                        startLocationUpdates()
                    }
                    Activity.RESULT_CANCELED -> {
                        Log.i(TAG, "User chose not to make required location settings changes.");
//                        updateUI()
                    }
                }
            }
        }
    }

    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     *
     *
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnCompleteListener(this@MainActivity) { task ->
                if (task.isSuccessful && task.result != null) {
                    lastLocation = task.result
                    loadWeather()

                } else {
                    showMessage(getString(R.string.no_location_detected))
                    startLocationUpdates()
                }
            }
    }

    private fun loadWeather() {
        val latitude = lastLocation!!.latitude
        val longitude = lastLocation!!.longitude
        viewModel.loadWeather(latitude, longitude)
            .observe(this, Observer<WeatherItem> { weather ->
                updateUI(weather)
            })
    }

    private fun updateUI(weather: WeatherItem?) {
        weather?.let {
            binding.name.text = it.name
            it.weather?.let { it1 ->
                val w = it1.first()
                binding.weather.text = w.main
                setIconImage(w.icon?.toIconUrl())
            }
        }
    }

    private fun setIconImage(icon: String?) {
        Glide.with(this)
            .load(icon)
            .into(imageView)
    }

    /**
     * Shows a [] using `text`.
     * @param text The Snackbar text.
     */
    private fun showMessage(text: String) {
        val container = findViewById<View>(R.id.main)
        if (container != null) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shows a [].
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * *
     * @param actionStringId   The text of the action item.
     * *
     * @param listener         The listener associated with the Snackbar action.
     */
    private fun showSnackbar(
        mainTextStringId: Int, actionStringId: Int,
        listener: View.OnClickListener
    ) {

        Snackbar.make(main, getString(mainTextStringId), Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(actionStringId), listener).show()
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                View.OnClickListener {
                    // Request permission
                    startLocationPermissionRequest()
                })

        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest()
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                startLocationUpdates();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                    View.OnClickListener {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
            }
        }
    }

    companion object {

        private val TAG = "MainActivity"

        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        private val REQUEST_CHECK_SETTINGS: Int = 0x1

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }
}
