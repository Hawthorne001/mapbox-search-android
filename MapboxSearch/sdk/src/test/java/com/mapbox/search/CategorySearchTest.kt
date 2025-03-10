package com.mapbox.search

import com.mapbox.geojson.Point
import com.mapbox.search.base.SearchRequestContextProvider
import com.mapbox.search.base.core.CoreApiType
import com.mapbox.search.base.core.CoreSearchCallback
import com.mapbox.search.base.core.CoreSearchEngineInterface
import com.mapbox.search.base.core.CoreSearchOptions
import com.mapbox.search.base.result.BaseGeocodingCompatSearchSuggestion
import com.mapbox.search.base.result.BaseSearchResultType
import com.mapbox.search.base.result.BaseSearchSuggestion
import com.mapbox.search.base.result.BaseServerSearchResultImpl
import com.mapbox.search.base.result.SearchRequestContext
import com.mapbox.search.base.result.SearchResultFactory
import com.mapbox.search.base.result.mapToBase
import com.mapbox.search.base.task.AsyncOperationTaskImpl
import com.mapbox.search.common.SearchCancellationException
import com.mapbox.search.common.SearchRequestException
import com.mapbox.search.common.tests.TestExecutor
import com.mapbox.search.common.tests.TestThreadExecutorService
import com.mapbox.search.common.tests.createTestCoreSearchResponseCancelled
import com.mapbox.search.common.tests.createTestCoreSearchResponseHttpError
import com.mapbox.search.common.tests.createTestCoreSearchResponseSuccess
import com.mapbox.search.common.tests.createTestCoreSearchResult
import com.mapbox.search.internal.bindgen.ResultType
import com.mapbox.search.internal.bindgen.SearchAddress
import com.mapbox.search.internal.bindgen.UserActivityReporterInterface
import com.mapbox.search.result.SearchResult
import com.mapbox.search.tests_support.createTestRequestOptions
import com.mapbox.test.dsl.TestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestFactory
import java.util.concurrent.Executor

/**
 * Contains only forward-geocoding related functionality tests.
 * See [SearchEngineTest], [ReverseGeocodingSearchTest] for more tests.
 */
internal class CategorySearchTest {

    private lateinit var coreEngine: CoreSearchEngineInterface
    private lateinit var activityReporter: UserActivityReporterInterface
    private lateinit var searchResultFactory: SearchResultFactory
    private lateinit var executor: Executor
    private lateinit var requestContextProvider: SearchRequestContextProvider

    private lateinit var searchEngine: SearchEngine

    @BeforeEach
    fun setUp() {
        coreEngine = mockk(relaxed = true)
        activityReporter = mockk(relaxed = true)
        searchResultFactory = spyk(SearchResultFactory(mockk()))
        executor = spyk(TestExecutor())
        requestContextProvider = mockk()

        every { requestContextProvider.provide(CoreApiType.GEOCODING) } returns TEST_SEARCH_REQUEST_CONTEXT

        searchEngine = SearchEngineImpl(
            apiType = ApiType.GEOCODING,
            settings = mockk(),
            analyticsService = mockk(relaxed = true),
            coreEngine = coreEngine,
            activityReporter = activityReporter,
            historyService = mockk(),
            requestContextProvider = requestContextProvider,
            searchResultFactory = searchResultFactory,
            engineExecutorService = TestThreadExecutorService(),
            indexableDataProvidersRegistry = mockk(),
        )
    }

    @TestFactory
    fun `Check initial successful search call`() = TestCase {
        Given("CategorySearchEngine with mocked dependencies") {

            val slotSuggestionCallback = slot<(Result<BaseSearchSuggestion>) -> Unit>()
            every { searchResultFactory.createSearchSuggestionAsync(any(), any(), any(), any(), capture(slotSuggestionCallback)) }.answers {
                slotSuggestionCallback.captured(Result.success(TEST_SEARCH_SUGGESTION))
                AsyncOperationTaskImpl.COMPLETED
            }

            val slotSearchCallback = slot<CoreSearchCallback>()
            every { coreEngine.search(any(), any(), any(), capture(slotSearchCallback)) }.answers {
                slotSearchCallback.captured.run(TEST_SUCCESSFUL_CORE_RESPONSE)
                TEST_REQUEST_ID
            }

            When("Initial search called") {
                val callback = mockk<SearchCallback>(relaxed = true)

                val task = searchEngine.search(
                    categoryName = TEST_CATEGORY_QUERY,
                    options = TEST_SEARCH_OPTIONS,
                    executor = executor,
                    callback = callback
                )

                Then("Task is executed", true, task.isDone)

                Verify("Callbacks called inside executor") {
                    executor.execute(any())
                }

                Verify("CoreSearchEngine.search() called") {
                    coreEngine.search(
                        eq(""),
                        eq(listOf(TEST_CATEGORY_QUERY)),
                        eq(TEST_SEARCH_OPTIONS.mapToCoreCategory()),
                        slotSearchCallback.captured
                    )
                }

                Verify("Results passed to callback") {
                    callback.onResults(
                        listOf(SearchResult(TEST_SEARCH_RESULT)),
                        ResponseInfo(
                            TEST_REQUEST_OPTIONS,
                            TEST_SUCCESSFUL_CORE_RESPONSE.mapToBase(),
                            isReproducible = true
                        )
                    )
                }

                VerifyNo("Request is not cancelled") {
                    coreEngine.cancel(any())
                }

                VerifyOnce("User activity reported") {
                    activityReporter.reportActivity(eq("search-engine-category-search"))
                }
            }
        }
    }

    @TestFactory
    fun `Check successful multiple categories search`() = TestCase {
        Given("CategorySearchEngine with mocked dependencies") {

            val slotSuggestionCallback = slot<(Result<BaseSearchSuggestion>) -> Unit>()
            every { searchResultFactory.createSearchSuggestionAsync(any(), any(), any(), any(), capture(slotSuggestionCallback)) }.answers {
                slotSuggestionCallback.captured(Result.success(TEST_SEARCH_SUGGESTION))
                AsyncOperationTaskImpl.COMPLETED
            }

            val slotSearchCallback = slot<CoreSearchCallback>()
            every { coreEngine.search(any(), any(), any(), capture(slotSearchCallback)) }.answers {
                slotSearchCallback.captured.run(TEST_SUCCESSFUL_CORE_RESPONSE)
                TEST_REQUEST_ID
            }

            When("Multiple categories search called") {
                val callback = mockk<SearchCallback>(relaxed = true)

                val task = searchEngine.search(
                    categoryNames = TEST_CATEGORIES_QUERY,
                    options = TEST_SEARCH_OPTIONS,
                    executor = executor,
                    callback = callback
                )

                Then("Task is executed", true, task.isDone)

                Verify("Callbacks called inside executor") {
                    executor.execute(any())
                }

                Verify("CoreSearchEngine.search() called") {
                    coreEngine.search(
                        eq(""),
                        eq(TEST_CATEGORIES_QUERY),
                        eq(TEST_SEARCH_OPTIONS.mapToCoreCategory()),
                        slotSearchCallback.captured
                    )
                }

                Verify("Results passed to callback") {
                    callback.onResults(
                        listOf(SearchResult(TEST_SEARCH_RESULT)),
                        ResponseInfo(
                            TEST_REQUEST_OPTIONS,
                            TEST_SUCCESSFUL_CORE_RESPONSE.mapToBase(),
                            isReproducible = true
                        )
                    )
                }

                VerifyNo("Request is not cancelled") {
                    coreEngine.cancel(any())
                }

                VerifyOnce("User activity reported") {
                    activityReporter.reportActivity(eq("search-engine-category-search"))
                }
            }
        }
    }

    @TestFactory
    fun `Check initial error search call`() = TestCase {
        Given("CategorySearchEngine with mocked dependencies") {
            val slotSearchCallback = slot<CoreSearchCallback>()
            val slotSearchOptions = slot<CoreSearchOptions>()

            every { coreEngine.search(any(), any(), capture(slotSearchOptions), capture(slotSearchCallback)) }.answers {
                slotSearchCallback.captured.run(TEST_ERROR_CORE_RESPONSE)
                TEST_REQUEST_ID
            }

            When("Initial search called") {
                val callback = mockk<SearchCallback>(relaxed = true)

                val task = searchEngine.search(
                    categoryName = TEST_CATEGORY_QUERY,
                    options = TEST_SEARCH_OPTIONS,
                    executor = executor,
                    callback = callback
                )

                Then("Task is executed", true, task.isDone)

                Verify("Callbacks called inside executor") {
                    executor.execute(any())
                }

                Verify("CoreSearchEngine.search() called") {
                    coreEngine.search(
                        eq(""),
                        eq(listOf(TEST_CATEGORY_QUERY)),
                        slotSearchOptions.captured,
                        slotSearchCallback.captured
                    )
                }

                Verify("Error passed to callback") {
                    callback.onError(
                        SearchRequestException(
                            message = TEST_ERROR_CORE_RESPONSE_MESSAGE,
                            code = TEST_ERROR_CORE_RESPONSE_HTTP_CODE
                        )
                    )
                }

                VerifyNo("Request is not cancelled") {
                    coreEngine.cancel(any())
                }

                VerifyOnce("User activity reported") {
                    activityReporter.reportActivity(eq("search-engine-category-search"))
                }
            }
        }
    }

    @TestFactory
    fun `Check initial internal error search call`() = TestCase {
        Given("CategorySearchEngine with erroneous core response") {
            val exception = RuntimeException("Test error")
            every { searchResultFactory.createSearchSuggestionAsync(any(), any(), any(), any(), any()) } throws exception

            val slotSearchCallback = slot<CoreSearchCallback>()

            every {
                coreEngine.search(any(), any(), any(), capture(slotSearchCallback))
            } answers {
                val spyResponse = spyk(TEST_SUCCESSFUL_CORE_RESPONSE)
                every { spyResponse.results } throws exception
                every { spyResponse.request } throws exception
                slotSearchCallback.captured.run(spyResponse)
                TEST_REQUEST_ID
            }

            When("Initial search called") {
                val callback = mockk<SearchCallback>(relaxed = true)
                val slotCallbackError = slot<Exception>()
                every { callback.onError(capture(slotCallbackError)) } returns Unit

                searchEngine.search(
                    categoryName = TEST_CATEGORY_QUERY,
                    options = TEST_SEARCH_OPTIONS,
                    executor = executor,
                    callback = callback
                )

                Then(
                    "Exception from core response should be forwarded to callback",
                    slotCallbackError.captured,
                    exception
                )

                VerifyNo("Request is not cancelled") {
                    coreEngine.cancel(any())
                }

                VerifyOnce("User activity reported") {
                    activityReporter.reportActivity(eq("search-engine-category-search"))
                }
            }
        }
    }

    @TestFactory
    fun `Check search call cancellation initiated by SDK`() = TestCase {
        Given("SearchEngine with mocked dependencies") {
            every { searchResultFactory.createSearchResult(any(), any()) } returns TEST_SEARCH_RESULT

            val cancellationReason = "Request cancelled"

            val slotSearchCallback = slot<CoreSearchCallback>()
            every { coreEngine.search(eq(""), eq(listOf(TEST_CATEGORY_QUERY)), any(), capture(slotSearchCallback)) } answers {
                slotSearchCallback.captured.run(createTestCoreSearchResponseCancelled(cancellationReason))
                TEST_REQUEST_ID
            }

            When("Search request cancelled by the Search SDK") {
                val callback = mockk<SearchCallback>(relaxed = true)

                val task = searchEngine.search(TEST_CATEGORY_QUERY, TEST_SEARCH_OPTIONS, callback)

                Then("Task is cancelled", true, task.isCancelled)

                VerifyOnce("Callback called with cancellation error") {
                    callback.onError(eq(SearchCancellationException(cancellationReason)))
                }

                VerifyNo("Core cancel() is not called") {
                    coreEngine.cancel(any())
                }

                VerifyOnce("User activity reported") {
                    activityReporter.reportActivity(eq("search-engine-category-search"))
                }
            }
        }
    }

    @TestFactory
    fun `Check search call cancellation initiated by user`() = TestCase {
        Given("SearchEngine with mocked dependencies") {
            every { coreEngine.search(eq(""), eq(listOf(TEST_CATEGORY_QUERY)), any(), any()) } answers {
                TEST_REQUEST_ID
            }

            When("Search request cancelled by user") {
                val callback = mockk<SearchCallback>(relaxed = true)

                val task = searchEngine.search(TEST_CATEGORY_QUERY, TEST_SEARCH_OPTIONS, callback)
                task.cancel()

                Then("Task is marked as cancelled", true, task.isCancelled)

                VerifyNo("Callback is not called") {
                    callback.onResults(any(), any())
                    callback.onError(any())
                }

                VerifyOnce("Core cancel() is called with correct request id") {
                    coreEngine.cancel(TEST_REQUEST_ID)
                }

                VerifyOnce("User activity reported") {
                    activityReporter.reportActivity(eq("search-engine-category-search"))
                }
            }
        }
    }

    private companion object {

        const val TEST_REQUEST_ID = 1L

        const val TEST_CATEGORY_QUERY = "cafe"
        val TEST_CATEGORIES_QUERY = listOf("cafe", "hotel")

        val TEST_SEARCH_OPTIONS = CategorySearchOptions()

        const val TEST_RESPONSE_UUID = "UUID test"

        val TEST_USER_LOCATION: Point = Point.fromLngLat(10.0, 11.0)
        val TEST_SEARCH_ADDRESS = SearchAddress(null, null, null, null, null, null, null, null, null)
        val TEST_SEARCH_REQUEST_CONTEXT = SearchRequestContext(apiType = CoreApiType.GEOCODING, responseUuid = TEST_RESPONSE_UUID)

        val TEST_CORE_SEARCH_RESULT = createTestCoreSearchResult(
            id = "test result id",
            types = listOf(ResultType.ADDRESS),
            names = listOf("Result name"),
            languages = listOf("Default"),
            addresses = listOf(TEST_SEARCH_ADDRESS),
            distanceMeters = 123.0,
            center = Point.fromLngLat(20.0, 30.0),
            routablePoints = emptyList(),
            categories = emptyList(),
        )

        val TEST_REQUEST_OPTIONS = createTestRequestOptions(
            "",
            options = SearchOptions(proximity = TEST_USER_LOCATION),
            proximityRewritten = true,
            requestContext = TEST_SEARCH_REQUEST_CONTEXT
        )

        val TEST_CORE_SEARCH_SUGGESTION = createTestCoreSearchResult(
            id = "test result id",
            types = listOf(ResultType.ADDRESS),
            names = listOf("Result name"),
            languages = listOf("Default"),
            addresses = listOf(TEST_SEARCH_ADDRESS),
            distanceMeters = 123.0,
            center = Point.fromLngLat(20.0, 30.0),
            routablePoints = emptyList(),
            categories = emptyList(),
        )

        val TEST_SEARCH_RESULT = BaseServerSearchResultImpl(
            types = listOf(BaseSearchResultType.ADDRESS),
            rawSearchResult = TEST_CORE_SEARCH_RESULT.mapToBase(),
            requestOptions = TEST_REQUEST_OPTIONS.mapToBase()
        )

        val TEST_SEARCH_SUGGESTION = BaseGeocodingCompatSearchSuggestion(
            rawSearchResult = TEST_CORE_SEARCH_SUGGESTION.mapToBase(),
            requestOptions = TEST_REQUEST_OPTIONS.mapToBase()
        )

        val TEST_SUCCESSFUL_CORE_RESPONSE = createTestCoreSearchResponseSuccess(
            TEST_REQUEST_OPTIONS.mapToBase().core,
            listOf(TEST_CORE_SEARCH_RESULT),
            TEST_RESPONSE_UUID
        )

        const val TEST_ERROR_CORE_RESPONSE_HTTP_CODE = 401

        const val TEST_ERROR_CORE_RESPONSE_MESSAGE = "Auth failed"

        val TEST_ERROR_CORE_RESPONSE = createTestCoreSearchResponseHttpError(
            TEST_ERROR_CORE_RESPONSE_HTTP_CODE,
            TEST_ERROR_CORE_RESPONSE_MESSAGE,
            TEST_REQUEST_OPTIONS.mapToBase().core,
            TEST_RESPONSE_UUID
        )
    }
}
