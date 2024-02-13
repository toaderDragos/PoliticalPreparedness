package com.example.android.politicalpreparedness.election

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.android.politicalpreparedness.BuildConfig
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.databinding.FragmentVoterInfoBinding
import com.example.android.politicalpreparedness.network.models.Address
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
import kotlinx.io.IOException
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


/** This fragment displays 2 links with voter location and ballot information based on a person's real address.
     * There is also a check to prevent using the links from outside the US  */
class VoterInfoFragment : Fragment() {

    val viewModel: VoterInfoViewModel by viewModel()
    lateinit var _binding: FragmentVoterInfoBinding
    private var addressFromGeolocation: Address? = null

    // Identical, boilerplate code like in RepresentativeFragment
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_voter_info,
            container,
            false)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.viewModel = viewModel

        // Populate voter info -- hide views without provided data.
        val election = VoterInfoFragmentArgs.fromBundle(requireArguments()).argElection
        println("Election name in voter info fragment: $election.name")
        _binding.election = election

        val formatter = SimpleDateFormat("EEE, MMM d, yyyy zzz", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("GMT") // Set the formatter to use GMT
        val formattedDate = formatter.format(election.electionDay)
        _binding.electionDate.text = formattedDate

        // Weird flex but avoids duplicate code
        // val mockAddress = Address("123 Main St", "Apt 123", "Denver", "CO", "80203")
        checkPermissionsAndGetAddress()

        viewModel.vaddress.observe(viewLifecycleOwner) { address ->
            if (address != null) {
                addressFromGeolocation = address
                viewModel.getVoterInfo(election.id, address)
            }
        }

        // _binding.electionDescription.text = election.division.name

        // Button 1: location
        viewModel.votingLocationFinderUrl.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                _binding.votingLocationsLink.apply {
                    isEnabled = true
                    alpha = 1.0f // Optional: Make button fully opaque when enabled
                    setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
                }
            } else {
                _binding.votingLocationsLink.apply {
                    isEnabled = false
                    alpha = 0.5f // Optional: Make button semi-transparent to indicate it's disabled
                }
            }
        }

        // Button 2: observe ballot live data
        viewModel.ballotInfoUrl.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                _binding.ballotInformationLink.apply {
                    isEnabled = true
                    alpha = 1.0f // Optional: Make button fully opaque when enabled
                    setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
                }
            } else {
                _binding.ballotInformationLink.apply {
                    isEnabled = false
                    alpha = 0.5f // Optional: Make button semi-transparent to indicate it's disabled
                }
            }
        }

        /** Button 3: follow election. We observe if and when the election is saved in the database and change the button text accordingly
        * This way the binding of the button is directly linked with the database */
        viewModel.isElectionSaved(election.id).observe(viewLifecycleOwner) { isSaved ->
            if (isSaved) {
                _binding.buttonFollowElection.text = getString(R.string.unfollow_election)
            } else {
                _binding.buttonFollowElection.text = getString(R.string.follow_election)
            }
        }

        viewModel.followButtonText.observe(viewLifecycleOwner) { buttonText ->
            _binding.buttonFollowElection.text = buttonText
        }

        _binding.buttonFollowElection.setOnClickListener {
            viewModel.toggleElectionFollowed(election)
        }
    }

        // Location Permissions and Services and get address
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
                    "Location permission is needed to fetch the closest voting locations. Please enable it.",
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
            this.requestPermissions(permissionsArray,
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }

        /***  STEP 3: Checking  Fine Location, (Background Location IS NOT NECESSARY) and then check location services
         * If the foreground location is approved, then check if device location settings is turned on*/
        private fun checkPermissionsAndGetAddress() {
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
                println("dra: in step 5 the location device settings succes: ${it.isSuccessful}")
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

        /***  STEP 7: Update the location info */
        private fun updateLocationInfo(result: Location?) {
            if (result != null) {
                val address = geoCodeLocation(result)
                // Update ViewModel or UI based on the location
                viewModel.vaddress.value = address
                println("dra: this is the info: ${address.city} ${address.state} ${address.line1} ${address.zip}")
            } else {
                // Handle case where location is null
                Toast.makeText(context, "Location is null", Toast.LENGTH_SHORT).show()
            }
        }

        /***  Helper function to process the extracted location */
        private fun geoCodeLocation(location: Location): Address {
            val geocoder = context?.let { Geocoder(it, Locale.getDefault()) }
            return try {
            if (geocoder != null) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.map { address ->
                        if (address.postalCode == null) {
                            address.postalCode = "11239"
                            address.adminArea = "NY"
                            address.locality = "NY"
                            address.thoroughfare = "App works only in USA"
                            address.subThoroughfare = "Reverting to default address"
                            _binding.ballotInformationLink.isEnabled = false
                            _binding.votingLocationsLink.isEnabled = false
                            _binding.electionDescription.text = getString(R.string.election_description_error)
                            _binding.electionDescription.setTextColor(resources.getColor(R.color.red))
                        }
                        Address(address.thoroughfare, address.subThoroughfare, address.locality, address.adminArea, address.postalCode)
                    }?.get(0) ?: Address("Error in Dragos's app in map", "yes yes yes yes", "Shkbidi Bum", "Bum Bum Bum", "")
            } else {
                Address("Geocoder is null", "", "", "", "")
            }
            } catch (e: IOException) {
                Address("No internet connection", "yes yes yes yes", "Shkbidi Bum", "Bum Bum Bum", "")
            }
        }

        private fun hideKeyboard() {
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        }


    }
