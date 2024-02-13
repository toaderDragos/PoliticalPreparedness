package com.example.android.politicalpreparedness.representative

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.android.politicalpreparedness.BuildConfig
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.utils.hideLoadingAnimation
import com.example.android.politicalpreparedness.database.utils.showLoadingAnimation
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.representative.adapter.RepresentativeListAdapter
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.Locale

class RepresentativeFragment : Fragment() {

    companion object {
        //Constant for Location request
        const val LOCATION_PERMISSION_INDEX = 0
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    }

    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    private val scope = CoroutineScope(Dispatchers.Main)
    val _viewModel: RepresentativeViewModel by viewModel()
    private lateinit var binding: FragmentRepresentativeBinding

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        // Establish bindings
        binding = FragmentRepresentativeBinding.inflate(inflater)
        binding.lifecycleOwner = this

        binding.viewModel = _viewModel

        // Define and assign Representative adapter
        val adapter = RepresentativeListAdapter()
        binding.recyclerRepresentatives.adapter = adapter

        _viewModel.representatives.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                Toast.makeText(context, "No representatives found", Toast.LENGTH_SHORT).show()
            }
            // Populate Representative adapter
                adapter.submitList(_viewModel.representatives.value)
                adapter.notifyDataSetChanged()
        }

        // Button listeners for field and location search
        binding.buttonSearch.setOnClickListener {

            val userInputtedAddress = Address(
                binding.addressLine1.text.toString(),
                binding.addressLine2.text.toString(),
                binding.city.text.toString(),
                binding.stateSpinner.selectedItem.toString(),
                binding.zip.text.toString()
            )
            _viewModel.address.value = userInputtedAddress
            _viewModel.setAddress(userInputtedAddress)

            hideKeyboard()
            if (_viewModel.addressHasZipAndState()) {
                showLoadingAnimation()    // found in Extensions.kt
                _viewModel.getRepresentativesFromAPI(userInputtedAddress)

                // Populate Representative adapter
                adapter.submitList(_viewModel.representatives.value)
                adapter.notifyDataSetChanged()
                hideLoadingAnimation()
            } else {
                Toast.makeText(context, "Please fill in the address fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonLocation.setOnClickListener {
            // Check location permissions and then the flow continues in the callback onRequestPermissionsResult
            // After that we fetch the location and then the representatives - see steps below
            checkPermissionsGetAddressAndGetRepresentativesFromAPIFlow()
        }

        // Populate the state spinner
        _viewModel.state.observe(viewLifecycleOwner) { newState ->
            val stateIndex = getIndexForState(requireContext(), newState)
            binding.stateSpinner.setSelection(stateIndex)
        }
        return binding.root
    }

    // Helper function to get the index for the selected state in the spinner
    private fun getIndexForState(context: Context, stateName: String): Int {
        val statesArray = context.resources.getStringArray(R.array.states)
        return statesArray.indexOf(stateName).also { index ->
            if (index == -1) {
                Timber.tag("getIndexForState").d("State not found: %s", stateName)
            } else {
                Timber.tag("getIndexForState").d( "Index of %s",  index)
            }
        }
    }

    /***  STEP 1: Determines whether the app has the appropriate permissions */
    private fun foregroundLocationPermissionApproved(): Boolean {
        // is permission granted - the val below has the answer
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == context?.let {
                    ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                })
        if (!foregroundLocationApproved) {
            // Show a toast message with a rationale to why the app needs this permission
            Toast.makeText(
                context,
                "Location permission is needed for core functionality",
                Toast.LENGTH_SHORT
            ).show()
        }
        return foregroundLocationApproved
    }

    /***  STEP 2: If the Location is approved, then continue, else Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q)).*/
    private fun requestForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved())
            return
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        Timber.tag("dra").d("Request user's location")
        // Fragment calls requestPermissions to ask the user for the location permission
        this.requestPermissions(permissionsArray, REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)
    }

    /***  STEP 3: Checking  Fine Location, (Background Location IS NOT NECESSARY) and then check location services
     * If the foreground location is approved, then check if device location settings is turned on*/
    private fun checkPermissionsGetAddressAndGetRepresentativesFromAPIFlow() {
        if (!foregroundLocationPermissionApproved()) {
            requestForegroundLocationPermissions()
        } else {
            checkDeviceLocationSettings()
        }
    }

    /***  STEP 4: Handle the result of the permission request */
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
        ) {
            Snackbar.make(
                this.requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            // Gets called immediately after the user grants the location permission
            // Handle location permission result to get location on permission granted
            checkDeviceLocationSettings()
        }
    }

    /**
     *  STEP 5: LOCATION SERVICES. Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn ON location services within our app. It is activated on click on the my location button
     */
    @SuppressLint("VisibleForTests")
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = activity?.let { LocationServices.getSettingsClient(it) }
        // Checks if the location is enabled on the phone
        val locationSettingsResponseTask = settingsClient?.checkLocationSettings(builder.build())
        locationSettingsResponseTask?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.tag("dra")
                        .d("Error getting location settings resolution: %s", sendEx.message)
                }
            }
            // If the user doesn't turn on the location services, then we display a message
            this.view?.let {
                Snackbar.make(it, R.string.location_required_error, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok) {
                        checkDeviceLocationSettings()
                    }.show()
            }
        }
        locationSettingsResponseTask?.addOnCompleteListener {
            println("dra: in step 5 the location device settings success: ${it.isSuccessful}")
            if (it.isSuccessful) {
                Toast.makeText(context, "Device location turned on", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
        }
    }

    /***  STEP 6: Fetch the location in high accuracy
     * I made sure I had all the permissions until this point */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        scope.launch(Dispatchers.IO) {
            val result = locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            scope.launch(Dispatchers.Main) {
                updateLocationInfo(result)
            }
        }
    }

    private fun updateLocationInfo(result: Location?) {
        if (result != null) {
            val address = geoCodeLocation(result)
            // Update ViewModel or UI based on the location
            _viewModel.address.value = address
            _viewModel.setAddress(address)
            _viewModel.getRepresentativesFromAPI(address)
            println("dra: this is the info: ${address.city} ${address.state} ${address.line1} ${address.zip}")
        } else {
            // Handle case where location is null
            Toast.makeText(context, "Location is null", Toast.LENGTH_SHORT).show()
        }
    }

    /***  Helper function to process the extracted location */
    private fun geoCodeLocation(location: Location): Address {
        val geocoder = context?.let { Geocoder(it, Locale.getDefault()) }
        return if (geocoder != null) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.map { address ->
                    if (address.postalCode == null) {
                        address.postalCode = "11239"
                        address.adminArea = "NY"
                        address.locality = "NY"
                        address.thoroughfare = "App works only in USA"
                        address.subThoroughfare = "Reverting to default address"
                        binding.stateSpinner.setSelection(getIndexForState(requireContext(), "NY"))
                    }
                    Address(address.thoroughfare, address.subThoroughfare, address.locality, address.adminArea, address.postalCode)
                }?.get(0) ?: Address("Error in Dragos's app in map", "Reverting to default address", "", "NY", "11239")
        } else {
            Address("Error: Outside USA", "App works exclusively in USA", "NY", "NY", "11239")
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

}