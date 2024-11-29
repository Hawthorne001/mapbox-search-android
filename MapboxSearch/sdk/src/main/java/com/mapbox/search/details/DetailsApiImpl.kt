package com.mapbox.search.details

import com.mapbox.search.SearchResultCallback
import com.mapbox.search.adapter.SearchResultCallbackAdapter
import com.mapbox.search.base.ExperimentalMapboxSearchAPI
import com.mapbox.search.base.SearchRequestContextProvider
import com.mapbox.search.base.core.CoreApiType
import com.mapbox.search.base.core.CoreSearchEngineInterface
import com.mapbox.search.base.engine.BaseSearchEngine
import com.mapbox.search.base.engine.OneStepRequestCallbackWrapper
import com.mapbox.search.base.result.SearchResultFactory
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.internal.bindgen.UserActivityReporterInterface
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMapboxSearchAPI::class)
internal class DetailsApiImpl(
    private val coreEngine: CoreSearchEngineInterface,
    private val activityReporter: UserActivityReporterInterface,
    private val requestContextProvider: SearchRequestContextProvider,
    private val searchResultFactory: SearchResultFactory,
    private val engineExecutorService: ExecutorService = DEFAULT_EXECUTOR,
) : BaseSearchEngine(), DetailsApi {

    override fun retrieveDetails(
        mapboxId: String,
        options: RetrieveDetailsOptions,
        executor: Executor,
        callback: SearchResultCallback
    ): AsyncOperationTask {
        activityReporter.reportActivity("details-api-retrieve")

        val baseCallback = SearchResultCallbackAdapter(callback)

        return makeRequest(baseCallback) { task ->
            val requestId = coreEngine.retrieveDetails(
                mapboxId,
                options.mapToCore(),
                OneStepRequestCallbackWrapper(
                    searchResultFactory = searchResultFactory,
                    callbackExecutor = executor,
                    workerExecutor = engineExecutorService,
                    searchRequestTask = task,
                    searchRequestContext = requestContextProvider.provide(CoreApiType.SEARCH_BOX),
                    isOffline = false,
                )
            )

            task.addOnCancelledCallback {
                coreEngine.cancel(requestId)
            }
        }
    }

    private companion object {
        val DEFAULT_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "DetailsApi executor")
        }
    }
}