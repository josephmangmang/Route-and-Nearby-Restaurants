package eu.blendit.testproject.model.facebookplace

import com.google.gson.annotations.SerializedName

data class FacebookPlaceResponse(

	@field:SerializedName("data")
	val data: List<DataItem?>? = null,

	@field:SerializedName("paging")
	val paging: Paging? = null
)