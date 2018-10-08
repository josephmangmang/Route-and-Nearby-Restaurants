package eu.blendit.testproject.model.facebookplace

import com.google.gson.annotations.SerializedName

data class Picture(

	@field:SerializedName("data")
	val data: Data? = null
)