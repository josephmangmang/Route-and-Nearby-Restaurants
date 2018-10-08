package eu.blendit.testproject.model.facebookplace

import com.google.gson.annotations.SerializedName

data class Engagement(

	@field:SerializedName("count")
	val count: Int? = null,

	@field:SerializedName("social_sentence")
	val socialSentence: String? = null
)