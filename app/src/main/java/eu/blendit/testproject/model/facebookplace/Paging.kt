package eu.blendit.testproject.model.facebookplace

import com.google.gson.annotations.SerializedName

data class Paging(

	@field:SerializedName("next")
	val next: String? = null,

	@field:SerializedName("cursors")
	val cursors: Cursors? = null
)