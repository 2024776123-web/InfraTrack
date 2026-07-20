package com.uitm.infratrack.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors the "hazards" collection written by the web admin panel (see README.md).
 * Field names must match exactly, since both the web app and this Android app
 * read/write the same Firestore documents.
 */
data class Hazard(
    @get:PropertyName("hazardType") @set:PropertyName("hazardType")
    var hazardType: String = "",

    @get:PropertyName("locationName") @set:PropertyName("locationName")
    var locationName: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("latitude") @set:PropertyName("latitude")
    var latitude: Double = 0.0,

    @get:PropertyName("longitude") @set:PropertyName("longitude")
    var longitude: Double = 0.0,

    @get:PropertyName("reporterName") @set:PropertyName("reporterName")
    var reporterName: String = "",

    @get:PropertyName("reportedAt") @set:PropertyName("reportedAt")
    @ServerTimestamp
    var reportedAt: Date? = null
) {
    // Firestore's toObject() requires a no-arg constructor; the defaults above provide it.
    var documentId: String = ""
}
