package com.uitm.infratrack.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.uitm.infratrack.R
import com.uitm.infratrack.adapter.HazardAdapter
import com.uitm.infratrack.model.Hazard
import com.uitm.infratrack.util.NetworkUtils

/**
 * News-feed screen: shows the latest hazard reports, newest first,
 * pulled live from the "hazards" collection in Firestore.
 */
class HazardListFragment : Fragment(R.layout.fragment_hazard_list) {

    private lateinit var firestore: FirebaseFirestore
    private var registration: ListenerRegistration? = null
    private lateinit var adapter: HazardAdapter

    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var statusDot: View
    private lateinit var statusText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        recycler = view.findViewById(R.id.recycler_hazards)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyView = view.findViewById(R.id.empty_view)
        statusDot = view.findViewById(R.id.status_dot)
        statusText = view.findViewById(R.id.status_text)

        adapter = HazardAdapter { hazard ->
            Toast.makeText(
                requireContext(),
                "${hazard.hazardType} — ${hazard.locationName}",
                Toast.LENGTH_SHORT
            ).show()
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            // The Firestore listener is realtime, so refresh just re-checks connectivity
            // and clears the "pull to refresh" spinner.
            checkConnectivityBanner()
            swipeRefresh.isRefreshing = false
        }

        checkConnectivityBanner()
    }

    override fun onStart() {
        super.onStart()
        progressBar.visibility = View.VISIBLE

        if (!NetworkUtils.isOnline(requireContext())) {
            progressBar.visibility = View.GONE
            emptyView.text = getString(R.string.feed_error)
            emptyView.visibility = View.VISIBLE
            return
        }

        registration = firestore.collection("hazards")
            .orderBy("reportedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE
                if (error != null) {
                    setConnected(false)
                    emptyView.text = getString(R.string.feed_error)
                    emptyView.visibility = View.VISIBLE
                    return@addSnapshotListener
                }
                setConnected(true)
                val hazards = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Hazard::class.java)?.apply { documentId = doc.id }
                } ?: emptyList()

                adapter.submitList(hazards)
                emptyView.visibility = if (hazards.isEmpty()) View.VISIBLE else View.GONE
                emptyView.text = getString(R.string.feed_empty)
            }
    }

    override fun onStop() {
        super.onStop()
        registration?.remove()
    }

    private fun checkConnectivityBanner() {
        setConnected(NetworkUtils.isOnline(requireContext()))
    }

    // In HazardListFragment.kt
    private fun setConnected(connected: Boolean) {
        if (!isAdded) return
        statusDot.setBackgroundResource(R.drawable.status_dot)

        val colorRes = if (connected) R.color.status_connected else R.color.status_disconnected
        statusDot.background.setTint(androidx.core.content.ContextCompat.getColor(requireContext(), colorRes))

        statusText.text = if (connected) "Connected" else "Offline"
    }
}
