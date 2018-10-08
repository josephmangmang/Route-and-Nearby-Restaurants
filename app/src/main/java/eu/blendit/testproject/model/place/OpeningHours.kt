package eu.blendit.testproject.model.place

import com.google.gson.annotations.SerializedName

data class OpeningHours(

	@field:SerializedName("open_now")
	val openNow: Boolean? = null
)