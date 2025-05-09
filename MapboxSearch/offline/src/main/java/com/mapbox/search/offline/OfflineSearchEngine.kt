package com.mapbox.search.offline

import android.app.Application
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.bindgen.Value
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStoreOptions
import com.mapbox.common.TilesetDescriptor
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.search.base.BaseSearchSdkInitializerImpl
import com.mapbox.search.base.SearchRequestContextProvider
import com.mapbox.search.base.core.CoreApiType
import com.mapbox.search.base.core.CoreEngineOptions
import com.mapbox.search.base.core.CoreSearchEngine
import com.mapbox.search.base.core.getUserActivityReporter
import com.mapbox.search.base.location.LocationEngineAdapter
import com.mapbox.search.base.location.WrapperLocationProvider
import com.mapbox.search.base.record.IndexableRecordResolver
import com.mapbox.search.base.result.SearchResultFactory
import com.mapbox.search.base.utils.AndroidKeyboardLocaleProvider
import com.mapbox.search.base.utils.UserAgentProvider
import com.mapbox.search.base.utils.defaultOnlineRequestTimeoutSeconds
import com.mapbox.search.base.utils.orientation.AndroidScreenOrientationProvider
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.common.concurrent.SearchSdkMainThreadWorker
import java.util.concurrent.Executor

/**
 * The [OfflineSearchEngine] interface provides forward and reverse geocoding search that works offline.
 * An instance of the [OfflineSearchEngine] can be obtained with [OfflineSearchEngine.create].
 *
 * The API of this class is temporary and subject to change.
 * Tiles loading functionality is available to selected customers only. Contact our team, to get early preview.
 */
public interface OfflineSearchEngine {

    /**
     * Interface definition for a callback to be invoked when the [OfflineSearchEngine] is ready for use.
     * With the current implementation the callback is invoked when previously downloaded offline data has been prepared
     * and available for search.
     *
     * If the callback returns an error, you can continue to use [OfflineSearchEngine] functions,
     * but previously downloaded offline data won't be available for search.
     */
    public interface EngineReadyCallback {

        /**
         * Called when the engine is ready for use.
         */
        public fun onEngineReady()
    }

    /**
     * Interface for a listener to be invoked when index data is changed in the [OfflineSearchEngine].
     * This interface notifies users when tiles changes made with [com.mapbox.common.TileStore]
     * have been processed and visible in the [OfflineSearchEngine].
     */
    public interface OnIndexChangeListener {

        /**
         * Called when tiles changes made with [com.mapbox.common.TileStore]
         * have been processed and visible in the [OfflineSearchEngine].
         *
         * @param event Information about changes in the offline search index.
         */
        public fun onIndexChange(event: OfflineIndexChangeEvent)

        /**
         * Called when tiles changes made with [com.mapbox.common.TileStore] couldn't be processed in the [OfflineSearchEngine].
         *
         * @param event Information about error.
         */
        public fun onError(event: OfflineIndexErrorEvent)
    }

    /**
     * Settings used for [OfflineSearchEngine] initialization.
     */
    public val settings: OfflineSearchEngineSettings

    /**
     * Selects preferable tileset for offline search. If dataset or version is set, [OfflineSearchEngine] will try to
     * match appropriate tileset and use it. If several tilesets are available, the latest registered will be used.
     *
     * By default, if multiple tilesets are registered at once
     * (e.g. one offline region is loaded for several tilesets in single call),
     * the biggest one (tilesets are compared as pair of strings {dataset, version}) is considered as latest.
     *
     * @param dataset Preferable dataset.
     * @param version Preferable version.
     */
    public fun selectTileset(dataset: String?, version: String?)

    /**
     * Selects preferable tileset for offline search. If dataset or version is set, [OfflineSearchEngine] will try to
     * match appropriate tileset and use it. If several tilesets are available, the latest registered will be used.
     *
     * By default, if multiple tilesets are registered at once
     * (e.g. one offline region is loaded for several tilesets in single call),
     * the biggest one (tilesets are compared as pair of strings {dataset, version}) is considered as latest.
     *
     * @param tilesetParameters [TilesetParameters] of the preferable tileset.
     */
    @MapboxExperimental
    public fun selectTileset(tilesetParameters: TilesetParameters)

    /**
     * Performs forward geocoding search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param query Search query.
     * @param options Search options.
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param callback The callback to handle search result.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    public fun search(
        query: String,
        options: OfflineSearchOptions,
        executor: Executor,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask

    /**
     * Performs forward geocoding search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param query Search query.
     * @param options Search options.
     * @param callback The callback to handle search result on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    public fun search(
        query: String,
        options: OfflineSearchOptions,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = search(
        query = query,
        options = options,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback,
    )

    /**
     * Performs category search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param categoryNames List of categories to search.
     * @param options Category search options.
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param callback The callback to handle search result on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @MapboxExperimental
    public fun categorySearch(
        categoryNames: List<String>,
        options: OfflineCategorySearchOptions,
        executor: Executor,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask

    /**
     * Performs category search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param categoryNames List of categories to search.
     * @param options Category search options.
     * @param callback The callback to handle search result. Events are dispatched on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @MapboxExperimental
    public fun categorySearch(
        categoryNames: List<String>,
        options: OfflineCategorySearchOptions,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = categorySearch(
        categoryNames = categoryNames,
        options = options,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback,
    )

    /**
     * Performs category search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param categoryName Name of category to search.
     * @param options Category search options.
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param callback The callback to handle search result on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @MapboxExperimental
    public fun categorySearch(
        categoryName: String,
        options: OfflineCategorySearchOptions,
        executor: Executor,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = categorySearch(
        categoryNames = listOf(categoryName),
        options = options,
        executor = executor,
        callback = callback,
    )

    /**
     * Performs category search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param categoryName Name of category to search.
     * @param options Category search options.
     * @param callback The callback to handle search result. Events are dispatched on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @MapboxExperimental
    public fun categorySearch(
        categoryName: String,
        options: OfflineCategorySearchOptions,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = categorySearch(
        categoryName = categoryName,
        options = options,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback,
    )

    /**
     * Performs reverse geocoding search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param options Reverse geocoding options.
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param callback Search result callback.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    public fun reverseGeocoding(
        options: OfflineReverseGeoOptions,
        executor: Executor,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask

    /**
     * Performs reverse geocoding search request.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param options Reverse geocoding options.
     * @param callback Search result callback, delivers results on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    public fun reverseGeocoding(
        options: OfflineReverseGeoOptions,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = reverseGeocoding(
        options = options,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback,
    )

    /**
     * Searches for addresses nearby (around [proximity] point), matched with specified [street] name.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param street Street name to match.
     * @param proximity Coordinate to search in its vicinity.
     * @param radiusMeters Radius (in meters) around [proximity].
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param callback Search result callback.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    public fun searchAddressesNearby(
        street: String,
        proximity: Point,
        radiusMeters: Double,
        executor: Executor,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask

    /**
     * Searches for addresses nearby (around [proximity] point), matched with specified [street] name.
     * Each new search request cancels the previous one if it is still in progress.
     * In this case [OfflineSearchCallback.onError] will be called with [com.mapbox.search.common.SearchCancellationException].
     *
     * @param street Street name to match.
     * @param proximity Coordinate to search in its vicinity.
     * @param radiusMeters Radius (in meters) around [proximity].
     * @param callback Search result callback, delivers results on the main thread.
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    public fun searchAddressesNearby(
        street: String,
        proximity: Point,
        radiusMeters: Double,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = searchAddressesNearby(
        street = street,
        proximity = proximity,
        radiusMeters = radiusMeters,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback,
    )

    /**
     * Deprecated: Use an overloaded function that accepts [OfflineSearchAlongRouteOptions]
     * as a parameter instead.
     * If you choose to use this function, it is recommended to set the first point of the route
     * as the [proximity] for future compatibility. In upcoming SDK updates,
     * the semantics of the [proximity] parameter will change.
     *
     * Performs a search along the supplied [route].
     *
     * @param query the search query
     * @param proximity coordinate along the route
     * @param route list of points that make up route line
     * @param executor executor for dispatching event, by default events are dispatched to the main thread
     * @param callback search result callback, delivers results on the main thread
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @Deprecated(
        "Deprecated, use an overloading that accepts OfflineSearchAlongRouteOptions as a parameter",
        ReplaceWith("searchAlongRoute(query, OfflineSearchAlongRouteOptions(route), executor, callback)"),
    )
    public fun searchAlongRoute(
        query: String,
        proximity: Point,
        route: List<Point>,
        executor: Executor,
        callback: OfflineSearchCallback
    ): AsyncOperationTask

    /**
     * Deprecated: Use an overloaded function that accepts [OfflineSearchAlongRouteOptions]
     * as a parameter instead.
     * If you choose to use this function, it is recommended to set the first point of the route
     * as the [proximity] for future compatibility. In upcoming SDK updates,
     * the semantics of the [proximity] parameter will change.
     *
     * Performs a search along the supplied [route].
     *
     * @param query the search query
     * @param proximity coordinate along the route
     * @param route list of points that make up route line
     * @param callback search result callback, delivers results on the main thread
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @Deprecated(
        "Deprecated, use an overloading that accepts OfflineSearchAlongRouteOptions as a parameter",
        ReplaceWith("searchAlongRoute(query, OfflineSearchAlongRouteOptions(route), callback)"),
    )
    @Suppress("DEPRECATION")
    public fun searchAlongRoute(
        query: String,
        proximity: Point,
        route: List<Point>,
        callback: OfflineSearchCallback
    ): AsyncOperationTask = searchAlongRoute(
        query = query,
        proximity = proximity,
        route = route,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback
    )

    /**
     * Performs a search along the supplied in the [options] route.
     *
     * @param query the search query
     * @param options the search along route options
     * @param executor executor for dispatching event, by default events are dispatched to the main thread
     * @param callback search result callback, delivers results on the main thread
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @MapboxExperimental
    public fun searchAlongRoute(
        query: String,
        options: OfflineSearchAlongRouteOptions,
        executor: Executor,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask

    /**
     * Performs a search along the supplied in the [options] route.
     *
     * @param query the search query
     * @param options the search along route options
     * @param callback search result callback, delivers results on the main thread
     * @return [AsyncOperationTask] object which allows to cancel the request.
     */
    @MapboxExperimental
    public fun searchAlongRoute(
        query: String,
        options: OfflineSearchAlongRouteOptions,
        callback: OfflineSearchCallback,
    ): AsyncOperationTask = searchAlongRoute(
        query = query,
        options = options,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback
    )

    /**
     * Function to retrieve the details for a given map feature. The callback will be invoked with
     * a [OfflineSearchResult] on successful execution. An example usage would be resolving a
     * location when a user clicks on a map in "offline" mode.
     *
     * @param feature [Feature] to retrieve details for
     * @param executor [Executor] used for events dispatching, default is the main thread
     * @param callback used to receive the [OfflineSearchResult] on successful execution
     * @return [AsyncOperationTask] object representing pending completion of the request
     */
    public fun retrieve(
        feature: Feature,
        executor: Executor,
        callback: OfflineSearchResultCallback,
    ): AsyncOperationTask

    /**
     * Function to retrieve the details for a given mapboxId that dispatches events using the
     * main executor. The callback will be invoked with a [OfflineSearchResult] on successful execution.
     *
     * @param feature the [Feature] item to retrieve details for
     * @param callback used to receive the [OfflineSearchResult] on successful execution
     * @return [AsyncOperationTask] object representing pending completion of the request
     */
    public fun retrieve(
        feature: Feature,
        callback: OfflineSearchResultCallback
    ): AsyncOperationTask = retrieve(
        feature = feature,
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback
    )

    /**
     * Adds a callback to be notified when engine is ready.
     * If the engine is already ready, callback will be notified immediately.
     *
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param callback The callback to notify when engine is ready.
     */
    public fun addEngineReadyCallback(executor: Executor, callback: EngineReadyCallback)

    /**
     * Adds a callback to be notified when engine is ready.
     * If the engine is already ready, callback will be notified immediately.
     *
     * @param callback The callback to notify when engine is ready. Events are dispatched on the main thread.
     */
    public fun addEngineReadyCallback(callback: EngineReadyCallback): Unit = addEngineReadyCallback(
        executor = SearchSdkMainThreadWorker.mainExecutor,
        callback = callback,
    )

    /**
     * Removes a previously added callback.
     *
     * @param callback The callback to remove.
     */
    public fun removeEngineReadyCallback(callback: EngineReadyCallback)

    /**
     * Adds a listener to be notified of index change events.
     *
     * @param executor Executor used for events dispatching. By default events are dispatched on the main thread.
     * @param listener The listener to notify when an event happens.
     */
    public fun addOnIndexChangeListener(executor: Executor, listener: OnIndexChangeListener)

    /**
     * Adds a listener to be notified of index change events.
     *
     * @param listener The listener to notify when an event happens. Events are dispatched on the main thread.
     */
    public fun addOnIndexChangeListener(listener: OnIndexChangeListener): Unit = addOnIndexChangeListener(
        executor = SearchSdkMainThreadWorker.mainExecutor,
        listener = listener,
    )

    /**
     * Removes a previously added listener.
     *
     * @param listener The listener to remove.
     */
    public fun removeOnIndexChangeListener(listener: OnIndexChangeListener)

    /**
     * Companion object.
     */
    public companion object {

        /**
         * Creates a new instance of [OfflineSearchEngine].
         * @param settings [OfflineSearchEngine] settings.
         * @return a new instance of [OfflineSearchEngine].
         */
        @JvmStatic
        public fun create(settings: OfflineSearchEngineSettings): OfflineSearchEngine {
            val app = BaseSearchSdkInitializerImpl.appContext as Application

            with(settings) {
                tileStore.setOption(
                    TileStoreOptions.MAPBOX_APIURL,
                    TileDataDomain.SEARCH,
                    Value.valueOf(tilesBaseUri.toString())
                )
            }

            val coreEngine = CoreSearchEngine(
                CoreEngineOptions(
                    baseUrl = null,
                    apiType = CoreApiType.SBS,
                    sdkInformation = UserAgentProvider.sdkInformation(),
                    eventsUrl = null,
                    onlineRequestTimeout = defaultOnlineRequestTimeoutSeconds(),
                ),
                WrapperLocationProvider(
                    LocationEngineAdapter(app, settings.locationProvider),
                    null
                ),
            )

            val requestContextProvider = SearchRequestContextProvider(
                AndroidKeyboardLocaleProvider(app),
                AndroidScreenOrientationProvider(app)
            )

            val searchResultFactory = SearchResultFactory(IndexableRecordResolver.EMPTY)

            return OfflineSearchEngineImpl(
                settings = settings,
                coreEngine = coreEngine,
                activityReporter = getUserActivityReporter(),
                requestContextProvider = requestContextProvider,
                searchResultFactory = searchResultFactory,
            )
        }

        /**
         * Creates TilesetDescriptor for offline search index data using the specified dataset and version.
         * Downloaded data will include addresses and places.
         *
         * @param dataset Tiles dataset.
         * @param version Tiles version, chosen automatically if empty.
         * @param language an ISO 639-1 language code
         */
        @OptIn(MapboxExperimental::class)
        @JvmStatic
        @JvmOverloads
        public fun createTilesetDescriptor(
            dataset: String = TilesetParameters.DEFAULT_DATASET,
            version: String = TilesetParameters.DEFAULT_VERSION,
            language: String? = null
        ): TilesetDescriptor {
            return CoreSearchEngine.createTilesetDescriptor(
                DatasetNameBuilder.buildDatasetName(dataset, language),
                version,
            )
        }

        /**
         * Creates [TilesetDescriptor] for offline search using the tileset parameters.
         * Downloaded data will include addresses and places.
         *
         * @param tilesetParameters Tiles parameters.
         */
        @JvmStatic
        @MapboxExperimental
        public fun createTilesetDescriptor(
            tilesetParameters: TilesetParameters,
        ): TilesetDescriptor {
            return CoreSearchEngine.createTilesetDescriptor(
                tilesetParameters.generatedDatasetName,
                tilesetParameters.version,
            )
        }

        /**
         * Creates TilesetDescriptor for offline search using the specified dataset and version.
         * Downloaded data will include only places.
         *
         * @param dataset Tiles dataset.
         * @param version Tiles version, chosen automatically if empty.
         * @param language an ISO 639-1 language code
         */
        @OptIn(MapboxExperimental::class)
        @JvmStatic
        @JvmOverloads
        public fun createPlacesTilesetDescriptor(
            dataset: String = TilesetParameters.DEFAULT_DATASET,
            version: String = TilesetParameters.DEFAULT_VERSION,
            language: String? = null
        ): TilesetDescriptor {
            return CoreSearchEngine.createPlacesTilesetDescriptor(
                DatasetNameBuilder.buildDatasetName(dataset = dataset, language = language),
                version,
            )
        }

        /**
         * Creates [TilesetDescriptor] for offline search using the tileset parameters.
         * Downloaded data will include only places.
         *
         * @param tilesetParameters Tiles parameters.
         */
        @JvmStatic
        @MapboxExperimental
        public fun createPlacesTilesetDescriptor(
            tilesetParameters: TilesetParameters,
        ): TilesetDescriptor {
            return CoreSearchEngine.createPlacesTilesetDescriptor(
                tilesetParameters.generatedDatasetName,
                tilesetParameters.version,
            )
        }
    }
}
