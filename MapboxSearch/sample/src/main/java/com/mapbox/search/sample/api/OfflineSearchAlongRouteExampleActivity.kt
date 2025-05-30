@file:OptIn(MapboxExperimental::class)

package com.mapbox.search.sample.api

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.android.gestures.Utils
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.offline.OfflineResponseInfo
import com.mapbox.search.offline.OfflineSearchAlongRouteOptions
import com.mapbox.search.offline.OfflineSearchCallback
import com.mapbox.search.offline.OfflineSearchEngine
import com.mapbox.search.offline.OfflineSearchEngineSettings
import com.mapbox.search.offline.OfflineSearchOptions
import com.mapbox.search.offline.OfflineSearchResult
import com.mapbox.search.sample.R
import com.mapbox.search.sample.databinding.ActivityOfflineSearchAlongRouteBinding
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.DistanceUnitType
import com.mapbox.search.ui.view.SearchResultAdapterItem
import com.mapbox.search.ui.view.SearchResultsView
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import kotlinx.coroutines.launch

class OfflineSearchAlongRouteExampleActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapAnnotationsManager: AnnotationsManager
    private lateinit var binding: ActivityOfflineSearchAlongRouteBinding
    private lateinit var viewModel: SearchAlongRouteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineSearchAlongRouteBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[SearchAlongRouteViewModel::class.java]
        setContentView(binding.root)

        mapView = binding.map
        mapboxMap = mapView.mapboxMap
        mapAnnotationsManager = AnnotationsManager(mapView)

        mapboxMap.loadStyle(Style.MAPBOX_STREETS)

        binding.searchResultsView.initialize(
            SearchResultsView.Configuration(
                commonConfiguration = CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL)
            )
        )

        val routes = resources.getStringArray(R.array.routes)
        val arrayAdapter = ArrayAdapter(this, R.layout.route_dropdown_item, routes)
        binding.routesAutoComplete.setAdapter(arrayAdapter)

        binding.queryText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.updateSearchQuery(binding.queryText.text.toString())
                v.hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.routesAutoComplete.setOnItemClickListener { _, view, position, _ ->
            // these are the route polylines and correspond to the drop down items defined in strings.xml -> "routes"
            val routePolylines = listOf(
                "}~xnFfbrrMrQq@~AvnArjKnpDjzDzfJphVf{U~k\\vhSdeIt_IzI~]ikAjuEfNh_A`kLvvUzlDp`E~jSzgIrlDp|Hbqp@jy\\~uKzmKtiKvaB`vJghB~cWfvBrbo@yzKrij@{j@xwElaDphEkfErvAkw@bMzFqRn^gB{AZm@",
                "{f}lFzopuMmFjKlCnBhJuDuDup@mUmc@d@c`@qjA{@aX`CeFiBwHqs@ug@ge@wDwLbDsvC{Pog@}BgWjEer@kBal@lFih@a[_tB?k[`Geq@Iug@mPy}BCqk@nMml@rJqRhEwBxxAfVte@nDvaAra@pWdBd@sE",
                "a{jlFft_uMyLlLhE}@h@rf@sBlLdArdEmK|SnEvLlFhj@BbLqD`K_[hIeG~Je@tc@dK|e@iIzg@bI`w@fG|IrYtRjVhtAr^~^nObb@b@r}@m]dzA}JxX_FjAyCh]dNlSjHnVrKjQ"
            )
            val polyline = routePolylines[position]
            val selectedRoute = PolylineUtils.decode(polyline, 5)
            viewModel.updateRoute(selectedRoute)
            view.hideKeyboard()
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.searchOptionsData.observe(this) {
            handleOnSearchOptionsUpdated(it)
        }

        viewModel.uiStateData.observe(this) { uiState ->
            when (uiState) {
                is UiState.Ready -> {
                    handleOnReady()
                }

                is UiState.Searching -> {
                    handleOnSearching()
                }

                is UiState.Success -> {
                    handleOnSuccess(uiState)
                }

                is UiState.Error -> {
                    Toast.makeText(this, "Error: ${uiState.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleOnReady() {
        binding.searchResultsView.isVisible = false
        binding.progressBar.isVisible = false
        binding.noResultsText.isVisible = false
        binding.queryText.isEnabled = true
        Toast.makeText(this, "Offline search is ready", Toast.LENGTH_SHORT).show()
    }

    private fun handleOnSearching() {
        binding.searchResultsView.isVisible = false
        binding.noResultsText.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun handleOnSuccess(uiState: UiState.Success) = if (uiState.searchResults.isEmpty()) {
        displayNoResults()
    } else {
        displayResults(uiState.searchResults)
    }

    private fun handleOnSearchOptionsUpdated(options: SearchAlongRouteOptions) {
        mapAnnotationsManager.clearAll()

        if (options.route.isNotEmpty()) {
            mapAnnotationsManager.showRoute(options.route)
        }
    }

    private fun displayResults(searchResults: List<OfflineSearchResult>) {
        val coordinates = searchResults.map { it.coordinate }
        mapAnnotationsManager.showMarkers(coordinates)

        val items = searchResults.map { result ->
            val title = result.name
            val subtitle = if (result.descriptionText?.isNotBlank() == true) {
                result.descriptionText
            } else {
                result.address.toString()
            }

            SearchResultAdapterItem.Result(
                title = title,
                subtitle = subtitle,
                distanceMeters = result.distanceMeters,
                drawable = com.mapbox.search.ui.R.drawable.mapbox_search_sdk_ic_search_result_address,
                payload = result
            )
        }

        binding.searchResultsView.setAdapterItems(items)

        binding.progressBar.isVisible = false
        binding.noResultsText.isVisible = false
        binding.searchResultsView.isVisible = true
    }

    private fun displayNoResults() {
        binding.progressBar.isVisible = false
        binding.searchResultsView.isVisible = false
        binding.noResultsText.isVisible = true
    }

    private fun Context.isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun View.hideKeyboard() =
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)

    private companion object {
        val MARKERS_EDGE_OFFSET = Utils.dpToPx(32f).toDouble()
        val PLACE_CARD_HEIGHT = Utils.dpToPx(300f).toDouble()

        val MARKERS_INSETS = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET
        )

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )
    }

    private class AnnotationsManager(mapView: MapView) {

        private val mapboxMap = mapView.mapboxMap
        private val circleAnnotationManager = mapView.annotations.createCircleAnnotationManager(
            AnnotationConfig(
                layerId = "markers"
            )
        )
        private val polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager(
            AnnotationConfig(
                layerId = "route",
                belowLayerId = "markers"
            )
        )
        private val markers = mutableMapOf<String, CircleAnnotation>()
        private var pointAlongRoute: CircleAnnotation? = null
        var route: List<Point>? = null
            private set

        var onMarkersChangeListener: (() -> Unit)? = null

        val hasRoute: Boolean
            get() = route != null

        val hasMarkers: Boolean
            get() = markers.isNotEmpty()

        fun clearMarkers() {
            circleAnnotationManager.delete(markers.values.toList())
            markers.clear()
        }

        fun clearRoute() {
            route = null
            polylineAnnotationManager.deleteAll()

            if (pointAlongRoute != null) {
                circleAnnotationManager.delete(pointAlongRoute!!)
                pointAlongRoute = null
            }
        }

        fun clearAll() {
            clearMarkers()
            clearRoute()
        }

        fun showMarker(coordinate: Point) {
            showMarkers(listOf(coordinate))
        }

        fun showMarkers(coordinates: List<Point>) {
            clearMarkers()
            if (coordinates.isEmpty()) {
                onMarkersChangeListener?.invoke()
                return
            }

            coordinates.forEach { coordinate ->
                val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                    .withPoint(coordinate)
                    .withCircleRadius(8.0)
                    .withCircleColor("#ee4e8b")
                    .withCircleStrokeWidth(2.0)
                    .withCircleStrokeColor("#ffffff")

                val annotation = circleAnnotationManager.create(circleAnnotationOptions)
                markers[annotation.id] = annotation
            }

            val onOptionsReadyCallback: (CameraOptions) -> Unit = {
                mapboxMap.setCamera(it)
                onMarkersChangeListener?.invoke()
            }

            val emptyCameraOptions = CameraOptions.Builder().build()
            if (route != null) {
                mapboxMap.cameraForCoordinates(
                    route!! + coordinates,
                    emptyCameraOptions,
                    MARKERS_INSETS,
                    null,
                    null,
                    onOptionsReadyCallback,
                )
            } else if (coordinates.size == 1) {
                val options = CameraOptions.Builder()
                    .center(coordinates.first())
                    .padding(MARKERS_INSETS_OPEN_CARD)
                    .zoom(14.0)
                    .build()

                onOptionsReadyCallback(options)
            } else {
                mapboxMap.cameraForCoordinates(
                    coordinates,
                    emptyCameraOptions,
                    MARKERS_INSETS,
                    null,
                    null,
                    onOptionsReadyCallback,
                )
            }
        }

        fun showRoute(route: List<Point>) {
            if (route.isEmpty()) {
                return
            }

            clearAll()

            this.route = route

            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(route)
                .withLineColor("#007bff")
                .withLineBorderColor("#0056b3")
                .withLineWidth(5.0)

            polylineAnnotationManager.create(polylineAnnotationOptions)

            mapboxMap.cameraForCoordinates(
                route, CameraOptions.Builder().build(), MARKERS_INSETS, null, null,
            ) {
                mapboxMap.setCamera(it)
            }
        }
    }

    class SearchAlongRouteViewModel : ViewModel() {
        val uiStateData = MediatorLiveData<UiState>()
        val offlineSearchData = MutableLiveData(OfflineSearchState())
        val searchOptionsData = MutableLiveData<SearchAlongRouteOptions>()
        private val searchEngine: OfflineSearchEngine
        private var searchRequestTask: AsyncOperationTask? = null

        private val searchCallback = object : OfflineSearchCallback {
            override fun onResults(results: List<OfflineSearchResult>, responseInfo: OfflineResponseInfo) {
                uiStateData.value = UiState.Success(results)
            }

            override fun onError(e: Exception) {
                uiStateData.value = UiState.Error("Search failed with message ${e.message}")
            }
        }

        init {
            uiStateData.addSource(offlineSearchData) { offlineSearchState ->
                when (offlineSearchState) {
                    OfflineSearchState(failed = true) -> uiStateData.value = UiState.Error("Failed to initialize Offline Search")
                    OfflineSearchState(tilesLoaded = true, searchEngineReady = true, failed = false) -> uiStateData.value = UiState.Ready
                }
            }

            uiStateData.addSource(searchOptionsData) { searchOptions ->
                updateSearchAlongRouteOptions(searchOptions)?.let {
                    uiStateData.value = it
                }
            }

            val tileStore = TileStore.create()
            val tileRegionId = "OfflineSARActivity - Washington DC"
            val tileDescriptors = listOf(OfflineSearchEngine.createTilesetDescriptor("mbx-gen2", language = "en"))
            val washingtonDc = Point.fromLngLat(-77.0339911055176, 38.899920004207516)
            val tileGeometry = TurfTransformation.circle(washingtonDc, 10.0, 32, TurfConstants.UNIT_KILOMETERS)

            val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
                .descriptors(tileDescriptors)
                .geometry(tileGeometry)
                .acceptExpired(true)
                .build()

            tileStore.loadTileRegion(
                tileRegionId,
                tileRegionLoadOptions,
                { progress -> Log.i(OfflineSearchAlongRouteExampleActivity::javaClass.name, "Loading progress: $progress") },
                { result ->
                    viewModelScope.launch {
                        val currentOfflineSearchState = offlineSearchData.value

                        val newOfflineSearchState = if (result.isValue) {
                            currentOfflineSearchState?.copy(tilesLoaded = true)
                        } else {
                            currentOfflineSearchState?.copy(failed = true)
                        }

                        offlineSearchData.value = newOfflineSearchState
                    }
                }
            )

            searchEngine = OfflineSearchEngine.create(
                OfflineSearchEngineSettings(
                    tileStore = tileStore
                ),
            )

            searchEngine.addEngineReadyCallback(object : OfflineSearchEngine.EngineReadyCallback {
                override fun onEngineReady() {
                    viewModelScope.launch {
                        val offlineSearchState = offlineSearchData.value
                        offlineSearchData.value = offlineSearchState?.copy(searchEngineReady = true)
                    }
                }
            })
        }

        fun updateSearchQuery(query: String) {
            val currentRequest = searchOptionsData.value
            searchOptionsData.value = currentRequest?.copy(query = query) ?: SearchAlongRouteOptions(query = query)
        }

        fun updateRoute(route: List<Point>) {
            val currentRequest = searchOptionsData.value
            searchOptionsData.value = currentRequest?.copy(route = route) ?: SearchAlongRouteOptions(route = route)
        }

        private fun runSearch(options: SearchAlongRouteOptions) {
            searchRequestTask = if (options.route.isNotEmpty()) {
                searchEngine.searchAlongRoute(
                    query = options.query,
                    options = OfflineSearchAlongRouteOptions(route = options.route),
                    callback = searchCallback
                )
            } else {
                searchEngine.search(
                    query = options.query,
                    options = OfflineSearchOptions(),
                    callback = searchCallback
                )
            }
        }

        private fun cancelSearch() {
            if (searchRequestTask != null && searchRequestTask?.isDone != true) {
                searchRequestTask!!.cancel()
            }
        }

        private fun updateSearchAlongRouteOptions(options: SearchAlongRouteOptions): UiState? {
            cancelSearch()

            return if (options.query.isNotBlank()) {
                runSearch(options)
                UiState.Searching(options)
            } else {
                null
            }
        }
    }

    sealed class UiState {

        object Ready : UiState()

        data class Searching(val searchOptions: SearchAlongRouteOptions) : UiState()

        data class Success(val searchResults: List<OfflineSearchResult>) : UiState()

        data class Error(val message: String) : UiState()
    }

    data class OfflineSearchState(
        val tilesLoaded: Boolean = false,
        val searchEngineReady: Boolean = false,
        val failed: Boolean = false
    )

    data class SearchAlongRouteOptions(
        val query: String = "",
        val proximity: Point = Point.fromLngLat(0.0, 0.0),
        val route: List<Point> = arrayListOf()
    )
}
