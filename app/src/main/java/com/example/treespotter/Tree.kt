package com.example.treespotter

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import java.util.Date

data class Tree(
    val name: String? = null,
    val dateSpotted: Date? = Date(),
    val location: GeoPoint? = null,
    var favorite: Boolean = false,  // names starting with is confuse Firebase
    @get:Exclude @set:Exclude var documentReference: DocumentReference? = null  // don't need to upload back to Firebase
)