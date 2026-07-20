package com.uitm.infratrack.ui.about

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.uitm.infratrack.R

class AboutFragment : Fragment(R.layout.fragment_about) {

    // Update this list with your actual group members / student IDs (see report cover page)
    private val developers = listOf(
        "Muhamad Afiq Danial Bin Abdul Waliyy — 2024776123",
        "Adam Bin Arush — 2024568687",
        "Muhammad Fahmi Bin Md Saini — 2024764375",
        "Muhammad Syahmi Bin Rosman — 2024394233",
        "Siti Nursolehah Binti Saufi — 2024907823",
        "Noor Azizah Ain Azila Binti Abdul Ghani — 2024545237"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textDevelopers = view.findViewById<TextView>(R.id.text_developers)
        textDevelopers.text = developers.joinToString("\n")
    }
}
