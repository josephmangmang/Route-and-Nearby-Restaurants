package eu.blendit.testproject

import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import eu.blendit.testproject.model.direction.DirectionResponse
import eu.blendit.testproject.model.direction.RoutesItem
import eu.blendit.testproject.model.place.PlaceResponse
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.URL
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerDragListener {


    private lateinit var mMap: GoogleMap
    private lateinit var originLatLng: LatLng
    private lateinit var destinationLatLng: LatLng
    private var dragableMarker: Marker? = null
    private var restaurantMarkers = ArrayList<Marker>()

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            showNearbyRestaurantSwitch.isChecked = false
            removeRestaurantMarkers()
            prepareRoute()
        }
    }


    companion object {
        val TAG = MapsActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        travelMode.onItemSelectedListener = onItemSelectedListener;
        showNearbyRestaurantSwitch.setOnCheckedChangeListener { compoundButton, b -> showNearbyRestaurant(b) }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerDragListener(this)
        mMap.setOnMapLoadedCallback {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds(originLatLng, destinationLatLng), 100))
        }
        prepareRoute()
    }

    override fun onMarkerDragStart(p0: Marker?) {
    }

    override fun onMarkerDrag(p0: Marker?) {
    }

    override fun onMarkerDragEnd(p0: Marker?) {
        if (showNearbyRestaurantSwitch.isChecked) {
            removeRestaurantMarkers()
            requestNearbyRestaurant()
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.e(TAG, "${connectionResult.errorMessage}\n $connectionResult");
    }


    private fun prepareRoute() {
        mMap.clear()

        val originAddress = getString(R.string.riga_latvia)
        val destinationAddress = getString(R.string.tallinn_estonia)

        originLatLng = getLatLngFromAddress(originAddress)
        destinationLatLng = getLatLngFromAddress(destinationAddress)


        val originMarker = MarkerOptions()
                .position(originLatLng)
                .title(originAddress)
        mMap.addMarker(originMarker)

        val destinationMarker = MarkerOptions()
                .position(destinationLatLng)
                .title(destinationAddress)
        mMap.addMarker(destinationMarker)

        prepareDirection(originLatLng, destinationLatLng)
    }

    private fun getLatLngFromAddress(stringAddress: String): LatLng {
        if (Geocoder.isPresent()) {
            val geocoder = Geocoder(this)
            try {
                for (address in geocoder.getFromLocationName(stringAddress, 1)) {
                    Log.d(TAG, address.toString())
                    return LatLng(address.latitude, address.longitude)
                }
            } catch (e: IllegalAccessException) {
                Log.e(TAG, e.message, e)
                return LatLng(0.0, 0.0)
            } catch (io: IOException) {
                Log.e(TAG, io.message, io)
                return LatLng(0.0, 0.0)
            }
        }
        return LatLng(0.0, 0.0);
    }



    private fun prepareDirection(originLatLng: LatLng, destinationLatLng: LatLng) {
        doAsync {
            val directionResponse = RequestDirection(getRequestUrl(originLatLng, destinationLatLng, travelMode.selectedItem.toString())).run()
            val directionPolylines = getDirectionPolylines(directionResponse.routes)

            Log.d(TAG, directionResponse.toString())
            // Get the estimated center of the route
            uiThread {
                addPolyline(directionPolylines, mMap)
                if (directionPolylines.isEmpty()) {
                    showNearbyRestaurantSwitch.isEnabled = false
                    distanceTextView.text = ""
                    durationTextView.text = ""
                    Snackbar.make(rootView, R.string.travel_mode_not_available, Snackbar.LENGTH_LONG)
                            .show()
                } else {
                    showNearbyRestaurantSwitch.isEnabled = true
                    val leg = directionResponse.routes?.get(0)?.legs?.get(0)
                    distanceTextView.text = leg?.distance?.text ?: ""
                    durationTextView.text = leg?.duration?.text ?: ""
                    // add dragable marker for center point when searching nearby reastaurant
                    dragableMarker?.remove()
                    dragableMarker = mMap.addMarker(MarkerOptions()
                            .title("Drag Me!")
                            .position(directionPolylines[(directionPolylines.size / 2) + (directionPolylines.size % 2)])
                            .draggable(true))
                    dragableMarker?.showInfoWindow()
                }
                progressBar.visibility = View.GONE
            }

        }

    }

    private fun getRequestUrl(originLatLng: LatLng, destinationLatLng: LatLng, travelMode: String): String {
        // Origin of route
        val strOrigin = "origin=" + originLatLng.latitude + "," + originLatLng.longitude

        // Destination of route
        val strDest = "destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude

        // Sensor enabled
        val sensor = "sensor=false"
        // travel mode
        val mode = "mode=${travelMode.toLowerCase()}"
        val key = "key=${getString(R.string.google_maps_key)}"
        // Building the parameters to the web service
        val parameters = "$strOrigin&$strDest&$sensor&$mode&$key"

        // Output format
        val output = "json"

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }


    private fun showNearbyRestaurant(show: Boolean) {
        if (show) {
            requestNearbyRestaurant();
        } else {
            // remove restaurant markers
            removeRestaurantMarkers()
        }
    }

    private fun removeRestaurantMarkers() {
        for (marker in restaurantMarkers) {
            marker.remove()
        }
        restaurantMarkers.clear()
        totalRestaurantCountTextView.visibility = View.GONE
    }

    private fun requestNearbyRestaurant() {
        val location = "location=${dragableMarker?.position?.latitude},${dragableMarker?.position?.longitude}"
        val radius = "&radius=20000"
        val types = "&types=restaurant"
        val sensor = "&sensor=true"
        val key = "&key=${getString(R.string.google_maps_key)}"
        val parameters = "$location&$radius&$types&$sensor&$key"
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?$parameters"
        doAsync {
            val placeResponse = RequestPlaces(url).run()
            Log.d(TAG, placeResponse.toString())

            uiThread {
                placeResponse.places?.forEachIndexed { i, placeItem ->
                    Log.d(TAG, "forEachIndexed of places: $i ${placeItem.toString()}")
                    val lat = placeItem?.geometry?.location?.lat
                    val lng = placeItem?.geometry?.location?.lng
                    val position = LatLng(lat ?: 0.0, lng ?: 0.0)

                    val addedMarker = mMap.addMarker(MarkerOptions()
                            .position(position)
                            .title(placeItem?.title)
                            .snippet(placeItem?.snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
                    restaurantMarkers.add(addedMarker)
                }
                totalRestaurantCountTextView.text = if (restaurantMarkers.size == 0) getString(R.string.no_restaurant_found) else resources.getQuantityString(R.plurals.total_restaurant_result,
                        restaurantMarkers.size, restaurantMarkers.size)
                totalRestaurantCountTextView.visibility = View.VISIBLE
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dragableMarker?.position, 9f))

            }
        }
    }


    private fun addPolyline(directionPolylines: ArrayList<LatLng>, mMap: GoogleMap) {
        val polylineOptions = PolylineOptions()
        polylineOptions.addAll(directionPolylines)
        polylineOptions.width(12F)
        polylineOptions.color(Color.BLUE)
        polylineOptions.geodesic(true)
        mMap.addPolyline(polylineOptions)
    }


    private fun getDirectionPolylines(routes: List<RoutesItem?>?): ArrayList<LatLng> {
        val directionList = ArrayList<LatLng>()
        if (routes != null && !routes.isEmpty()) {
            for (route in routes) {
                for (leg in route?.legs.orEmpty()) {
                    for (step in leg?.steps.orEmpty()) {
                        val polyline = step?.polyline
                        val points = polyline?.points
                        val singlePolyline: List<LatLng> = decodePoly(points!!)
                        for (direction in singlePolyline) {
                            directionList.add(direction)
                        }
                    }
                }
            }
        }
        return directionList
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5,
                    lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    class RequestDirection(private val url: String) {
        fun run(): DirectionResponse {
            Log.d(TAG, url)
            val response = URL(url).readText()
            return Gson().fromJson(response, DirectionResponse::class.java)
        }
    }

    class RequestPlaces(private val url: String) {
        fun run(): PlaceResponse {
            Log.d(TAG, url)
            val response = URL(url).readText()
            return Gson().fromJson(response, PlaceResponse::class.java)
        }
    }

}
