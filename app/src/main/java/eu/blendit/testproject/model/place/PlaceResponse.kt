package eu.blendit.testproject.model.place

import com.google.gson.annotations.SerializedName

data class PlaceResponse(

		@field:SerializedName("next_page_token")
	val nextPageToken: String? = null,

		@field:SerializedName("html_attributions")
	val htmlAttributions: List<Any?>? = null,

		@field:SerializedName("results")
	val places: List<PlaceItem?>? = null,

		@field:SerializedName("status")
	val status: String? = null
)