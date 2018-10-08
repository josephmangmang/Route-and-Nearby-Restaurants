package eu.blendit.testproject.model.facebookplace

import com.google.gson.annotations.SerializedName

data class Data(

	@field:SerializedName("is_silhouette")
	val isSilhouette: Boolean? = null,

	@field:SerializedName("width")
	val width: Int? = null,

	@field:SerializedName("url")
	val url: String? = null,

	@field:SerializedName("height")
	val height: Int? = null
)