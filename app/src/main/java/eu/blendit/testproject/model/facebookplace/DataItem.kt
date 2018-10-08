package eu.blendit.testproject.model.facebookplace

import com.google.gson.annotations.SerializedName

data class DataItem(

	@field:SerializedName("engagement")
	val engagement: Engagement? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("location")
	val location: Location? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("checkins")
	val checkins: Int? = null,

	@field:SerializedName("picture")
	val picture: Picture? = null
)