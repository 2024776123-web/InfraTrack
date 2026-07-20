package com.uitm.infratrack.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.uitm.infratrack.R
import com.uitm.infratrack.util.NetworkUtils

/**
 * Lets a road user crowdsource a new hazard report. Captures GPS coordinates
 * on-device, then writes a new document to the "hazards" collection —
 * exactly the same schema the web admin panel uses (see README.md).
 */
class ReportHazardFragment : Fragment(R.layout.fragment_report_hazard) {

    private val hazardTypes = listOf(
        "Pothole", "Flooding", "Fallen Tree", "Damaged Traffic Light",
        "Broken Streetlight", "Roadworks", "Road Closure", "Landslide", "Other"
    )

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var capturedLat: Double? = null
    private var capturedLng: Double? = null

    private lateinit var inputHazardType: MaterialAutoCompleteTextView
    private lateinit var layoutLocationName: TextInputLayout
    private lateinit var inputLocationName: TextInputEditText
    private lateinit var inputDescription: TextInputEditText
    private lateinit var inputReporterName: TextInputEditText
    private lateinit var btnUseGps: MaterialButton
    private lateinit var textGpsResult: TextView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var submitProgress: ProgressBar

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) captureLocation() else {
            Toast.makeText(requireContext(), "Location permission is required to capture GPS.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        inputHazardType = view.findViewById(R.id.input_hazard_type)
        layoutLocationName = view.findViewById(R.id.layout_location_name)
        inputLocationName = view.findViewById(R.id.input_location_name)
        inputDescription = view.findViewById(R.id.input_description)
        inputReporterName = view.findViewById(R.id.input_reporter_name)
        btnUseGps = view.findViewById(R.id.btn_use_gps)
        textGpsResult = view.findViewById(R.id.text_gps_result)
        btnSubmit = view.findViewById(R.id.btn_submit)
        submitProgress = view.findViewById(R.id.submit_progress)

        inputHazardType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, hazardTypes)
        )
        inputHazardType.setOnClickListener { inputHazardType.showDropDown() }
        inputHazardType.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) inputHazardType.showDropDown() }

        btnUseGps.setOnClickListener { requestLocation() }
        btnSubmit.setOnClickListener { submitReport() }
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            captureLocation()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun captureLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        btnUseGps.isEnabled = false
        textGpsResult.text = "Getting your location..."

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location ->
                btnUseGps.isEnabled = true
                if (location != null) {
                    onLocationCaptured(location.latitude, location.longitude)
                } else {
                    // getCurrentLocation can return null on emulators / when GPS has no
                    // recent fix yet. Fall back to the last cached location if we have one.
                    fusedLocationClient.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            onLocationCaptured(last.latitude, last.longitude)
                        } else {
                            textGpsResult.text = ""
                            Toast.makeText(
                                requireContext(),
                                "No location fix yet. On an emulator: open the ⋮ Extended Controls panel → Location → set a point → Send.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                btnUseGps.isEnabled = true
                textGpsResult.text = ""
                Toast.makeText(requireContext(), "Couldn't get GPS location. Make sure location is enabled.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onLocationCaptured(lat: Double, lng: Double) {
        capturedLat = lat
        capturedLng = lng
        textGpsResult.text = getString(R.string.gps_captured, lat, lng)
    }

    private fun submitReport() {
        val hazardType = inputHazardType.text.toString().trim()
        val locationName = inputLocationName.text.toString().trim()
        val description = inputDescription.text.toString().trim()
        val reporterName = inputReporterName.text.toString().trim()

        if (hazardType.isEmpty() || locationName.isEmpty() || description.isEmpty() || reporterName.isEmpty()) {
            Toast.makeText(requireContext(), R.string.field_required, Toast.LENGTH_SHORT).show()
            return
        }
        val lat = capturedLat
        val lng = capturedLng
        if (lat == null || lng == null) {
            Toast.makeText(requireContext(), R.string.gps_missing, Toast.LENGTH_SHORT).show()
            return
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(), R.string.submit_error, Toast.LENGTH_SHORT).show()
            return
        }

        submitProgress.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        val hazard = hashMapOf(
            "hazardType" to hazardType,
            "locationName" to locationName,
            "description" to description,
            "latitude" to lat,
            "longitude" to lng,
            "reporterName" to reporterName,
            "reportedAt" to FieldValue.serverTimestamp() // set by server on save, per README
        )

        FirebaseFirestore.getInstance().collection("hazards")
            .add(hazard)
            .addOnSuccessListener {
                submitProgress.visibility = View.GONE
                btnSubmit.isEnabled = true
                Toast.makeText(requireContext(), R.string.submit_success, Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener {
                submitProgress.visibility = View.GONE
                btnSubmit.isEnabled = true
                Toast.makeText(requireContext(), R.string.submit_error, Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        inputHazardType.text = null
        inputLocationName.text = null
        inputDescription.text = null
        inputReporterName.text = null
        textGpsResult.text = ""
        capturedLat = null
        capturedLng = null
    }
}
