package com.uitm.infratrack.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.uitm.infratrack.R
import com.uitm.infratrack.model.Hazard
import com.uitm.infratrack.util.NetworkUtils

/**
 * Interactive map screen. Hazard markers are NOT hard-coded — they are downloaded
 * dynamically from the "hazards" collection in Firestore, per the project requirement.
 */
class HazardMapFragment : Fragment(R.layout.fragment_hazard_map), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private lateinit var firestore: FirebaseFirestore
    private var registration: ListenerRegistration? = null
    private val markerHazards = mutableMapOf<String, Hazard>()

    private lateinit var progressBar: ProgressBar
    private lateinit var noInternetBanner: TextView

    private val locationPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableMyLocation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        progressBar = view.findViewById(R.id.map_progress)
        noInternetBanner = view.findViewById(R.id.text_no_internet)

        noInternetBanner.visibility =
            if (NetworkUtils.isOnline(requireContext())) View.GONE else View.VISIBLE

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Use a custom info window to prevent text truncation (...) and show all details clearly
        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? = null

            override fun getInfoContents(marker: Marker): View {
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.custom_info_window, null)
                val hazard = markerHazards[marker.tag as? String]
                
                val title: TextView = view.findViewById(R.id.info_title)
                val reporter: TextView = view.findViewById(R.id.info_reporter)
                val dateTime: TextView = view.findViewById(R.id.info_date_time)
                val location: TextView = view.findViewById(R.id.info_location)

                if (hazard != null) {
                    val dateText = hazard.reportedAt?.let {
                        DateFormat.format("dd MMM yyyy, hh:mm a", it)
                    } ?: "Just now"
                    
                    title.text = hazard.hazardType
                    reporter.text = "Reported by: ${hazard.reporterName}"
                    dateTime.text = "Date/Time: $dateText"
                    location.text = "Location: ${hazard.locationName}"
                } else {
                    title.text = marker.title
                }
                
                return view
            }
        })

        // Default camera near campus (KUKM Machang area) until markers/GPS arrive.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(5.7667, 102.2167), 11f))

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        loadHazardMarkers()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        }
    }

    private fun loadHazardMarkers() {
        progressBar.visibility = View.VISIBLE
        registration = firestore.collection("hazards")
            .orderBy("reportedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE
                if (error != null || googleMap == null) return@addSnapshotListener

                googleMap?.clear()
                markerHazards.clear()

                snapshot?.documents?.forEach { doc ->
                    val hazard = doc.toObject(Hazard::class.java)?.apply { documentId = doc.id }
                        ?: return@forEach
                    val position = LatLng(hazard.latitude, hazard.longitude)
                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(position)
                    )
                    marker?.tag = doc.id
                    markerHazards[doc.id] = hazard
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registration?.remove()
    }
}
