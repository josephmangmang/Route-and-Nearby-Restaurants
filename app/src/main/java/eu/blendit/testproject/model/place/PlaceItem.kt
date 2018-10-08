package eu.blendit.testproject.model.place

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.google.maps.android.clustering.ClusterItem

data class PlaceItem(

        @field:SerializedName("types")
        val types: List<String?>? = null,

        @field:SerializedName("icon")
        val icon: String? = null,

        @field:SerializedName("rating")
        val rating: Double? = null,

        @field:SerializedName("photos")
        val photos: List<PhotosItem?>? = null,

        @field:SerializedName("reference")
        val reference: String? = null,

        @field:SerializedName("scope")
        val scope: String? = null,

        @field:SerializedName("name")
        val name: String? = null,

        @field:SerializedName("opening_hours")
        val openingHours: OpeningHours? = null,

        @field:SerializedName("geometry")
        val geometry: Geometry? = null,

        @field:SerializedName("vicinity")
        val vicinity: String? = null,

        @field:SerializedName("id")
        val id: String? = null,

        @field:SerializedName("plus_code")
        val plusCode: PlusCode? = null,

        @field:SerializedName("place_id")
        val placeId: String? = null
) : ClusterItem {
    val latLng: LatLng
        get() = LatLng(geometry?.location?.lat ?: 0.0, geometry?.location?.lng ?: 0.0)


    override fun getSnippet(): String = "Rating: $rating"

    override fun getTitle(): String  = name ?: ""

    override fun getPosition(): LatLng = latLng
}