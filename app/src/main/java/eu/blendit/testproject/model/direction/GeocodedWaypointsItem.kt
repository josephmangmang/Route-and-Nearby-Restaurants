package eu.blendit.testproject.model.direction

data class GeocodedWaypointsItem(
	val types: List<String?>? = null,
	val geocoderStatus: String? = null,
	val placeId: String? = null
)
