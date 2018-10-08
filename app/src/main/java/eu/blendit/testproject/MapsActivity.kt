package eu.blendit.testproject

import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import eu.blendit.testproject.model.direction.DirectionResponse
import eu.blendit.testproject.model.direction.RoutesItem
import eu.blendit.testproject.model.facebookplace.FacebookPlaceResponse
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
    // add dragable marker for center point when searching nearby reastaurant
    private var dragableMarker: Marker? = null
    private var restaurantMarkers = ArrayList<Marker>()

    companion object {
        // declare a static tag for this activity for logging
        val TAG = MapsActivity::class.simpleName
    }

    private val onTravelItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            // clean map and recreate route and markers after user select new travel mode
            showNearbyRestaurantSwitch.isChecked = false
            removeRestaurantMarkers()
            prepareRoute()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // set listener to handle travel mode selection
        travelMode.onItemSelectedListener = onTravelItemSelectedListener;

        // set listener to for handling check change
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
            // when the map is loaded move the camera to the selected route
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
            requestFacebookNearbyPlace()
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.e(TAG, "${connectionResult.errorMessage}\n $connectionResult");
    }

    /**
     * Prepare the route base on selected origin and destination
     */
    private fun prepareRoute() {
        mMap.clear()

        // get the static origin and destination for our route
        val originAddress = getString(R.string.riga_latvia)
        val destinationAddress = getString(R.string.tallinn_estonia)

        originLatLng = getLatLngFromAddress(originAddress)
        destinationLatLng = getLatLngFromAddress(destinationAddress)


        // add origin marker in our map
        val originMarker = MarkerOptions()
                .position(originLatLng)
                .title(originAddress)
        mMap.addMarker(originMarker)

        // add destination marker in our map
        val destinationMarker = MarkerOptions()
                .position(destinationLatLng)
                .title(destinationAddress)
        mMap.addMarker(destinationMarker)

        prepareDirection(originLatLng, destinationLatLng)
    }

    /**
     * Convert string address to LatLng object
     */
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

    /**
     * Prepare direction
     * @param originLatLng the origin address
     * @param destinationLatLng the destination address
     */
    private fun prepareDirection(originLatLng: LatLng, destinationLatLng: LatLng) {
        // this is long task.. do it in separate thread
        doAsync {
            val directionResponse = RequestDirection(getDirectionRequestUrl(originLatLng, destinationLatLng, travelMode.selectedItem.toString())).run()
            val directionPolylines = getDirectionPolylines(directionResponse.routes)

            Log.d(TAG, directionResponse.toString())

            // any
            uiThread {
                addPolyline(directionPolylines, mMap)

                // when no route available inform user
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

    /**
     * Build the direction request url
     * @param originLatLng starting point address
     * @param destinationLatLng destination address
     * @param travelMode available travel modes Driving, Walking, Bicycling, Transit
     */
    private fun getDirectionRequestUrl(originLatLng: LatLng, destinationLatLng: LatLng, travelMode: String): String {
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
            requestFacebookNearbyPlace()
        } else {
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

    private fun requestFacebookNearbyPlace() {
        // prepare required facebook graph parameters
        val parameters = Bundle()
        parameters.putString("type", "place")
        parameters.putString("fields", "engagement,name,checkins,picture,location")
        parameters.putString("q", "restaurant")
        parameters.putString("center", "${dragableMarker?.position?.latitude},${dragableMarker?.position?.longitude}")
        parameters.putString("distance", "20000")

        // set app client token
        FacebookSdk.setClientToken("e949b4399d071b6a22dd65df109b5601")

        // build graph request for searching nearby restaurant
        val request = GraphRequest.newGraphPathRequest(
                AccessToken.getCurrentAccessToken(),
                "/search"
        ) {
            // parse json response to FacebookPlaceResponse object
            val facebookPlaceResponse = Gson().fromJson(it.rawResponse, FacebookPlaceResponse::class.java)

            Log.d(TAG, " ${facebookPlaceResponse?.toString()}")
            // sometimes response is empty or null
            try {
                // add result to our map
                facebookPlaceResponse?.data?.forEachIndexed { i, place ->
                    doAsync {
                        val lat = place?.location?.latitude
                        val lng = place?.location?.longitude
                        val position = LatLng(lat ?: 0.0, lng ?: 0.0)
                        val bitmap = BitmapFactory.decodeStream(URL(place?.picture?.data?.url).openConnection().getInputStream())
                        uiThread {
                            val addedMarker = mMap.addMarker(MarkerOptions()
                                    .position(position)
                                    .title(place?.name)
                                    .snippet(place?.engagement?.socialSentence)
                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)))
                            restaurantMarkers.add(addedMarker)
                        }
                    }
                }

                val totalResult = facebookPlaceResponse?.data?.size ?: 0
                totalRestaurantCountTextView.text = if (totalResult == 0) getString(R.string.no_restaurant_found) else resources.getQuantityString(R.plurals.total_restaurant_result,
                        totalResult, totalResult)
                totalRestaurantCountTextView.visibility = View.VISIBLE
            } catch (e: NullPointerException) {
                Log.e(TAG, e.message)
            }
            // zoom in around the search results
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dragableMarker?.position, 9f))
        }
        // set the parameters
        request.parameters = parameters
        // execute task in async
        request.executeAsync()
    }

    /**
     * Add polyline to our map
     */
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

    /**
     * Class to handle direction request
     */
    class RequestDirection(private val url: String) {
        fun run(): DirectionResponse {
            Log.d(TAG, url)
            val response = URL(url).readText()
            return Gson().fromJson(response, DirectionResponse::class.java)
        }
    }

}
