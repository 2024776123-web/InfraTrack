package com.uitm.infratrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uitm.infratrack.R
import com.uitm.infratrack.model.Hazard
import java.text.SimpleDateFormat
import java.util.Locale

class HazardAdapter(
    private val onItemClick: (Hazard) -> Unit
) : ListAdapter<Hazard, HazardAdapter.HazardViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HazardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hazard, parent, false)
        return HazardViewHolder(view)
    }

    override fun onBindViewHolder(holder: HazardViewHolder, position: Int) {
        val hazard = getItem(position)
        holder.bind(hazard)
        holder.itemView.setOnClickListener { onItemClick(hazard) }
    }

    inner class HazardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val type: TextView = itemView.findViewById(R.id.text_hazard_type)
        private val location: TextView = itemView.findViewById(R.id.text_location)
        private val description: TextView = itemView.findViewById(R.id.text_description)
        private val meta: TextView = itemView.findViewById(R.id.text_meta)

        fun bind(hazard: Hazard) {
            type.text = hazard.hazardType
            location.text = hazard.locationName
            description.text = hazard.description
            val dateText = hazard.reportedAt?.let { dateFormat.format(it) } ?: "Just now"
            meta.text = "Reported by ${hazard.reporterName} • $dateText"
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Hazard>() {
            override fun areItemsTheSame(oldItem: Hazard, newItem: Hazard) =
                oldItem.documentId == newItem.documentId

            override fun areContentsTheSame(oldItem: Hazard, newItem: Hazard) =
                oldItem == newItem
        }
    }
}
