package eu.blendit.testproject.model.direction

data class RoutesItem(
        val summary: String? = null,
        val copyrights: String? = null,
        val legs: List<LegsItem?>? = null,
        val warnings: List<Any?>? = null,
        val bounds: Bounds? = null,
        val overviewPolyline: OverviewPolyline? = null,
        val waypointOrder: List<Any?>? = null
)
