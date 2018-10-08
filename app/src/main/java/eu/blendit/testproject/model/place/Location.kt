package eu.blendit.testproject.model.place

import com.google.gson.annotations.SerializedName

data class Location(

        @field:SerializedName("lng")
        val lng: Double = 0.0,

        @field:SerializedName("lat")
        val lat: Double = 0.0
)