package eu.blendit.testproject.model.direction

data class DirectionResponse(
        val routes: List<RoutesItem?>? = null,
        val geocodedWaypoints: List<GeocodedWaypointsItem?>? = null,
        val status: String? = null
)
