package com.mapbox.search.analytics

import com.mapbox.search.ResponseInfo
import com.mapbox.search.common.CompletionCallback
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion

/**
 * Class for tracking analytics events from user side.
 */
public interface AnalyticsService {

    /**
     * Sends feedback event to analytics.
     * @param searchResult Search result for which feedback is given.
     * @param responseInfo Search context associated with the provided [searchResult].
     * @param event Extra information for feedback, provided by user.
     * @param callback Callback to handle completion. Optional.
     */
    public fun sendFeedback(searchResult: SearchResult, responseInfo: ResponseInfo, event: FeedbackEvent, callback: CompletionCallback<Unit>? = null)

    /**
     * Sends feedback event to analytics.
     * @param searchSuggestion Search suggestion for which feedback is given.
     * @param responseInfo Search context associated with the provided [searchSuggestion].
     * @param event Extra information for feedback, provided by user.
     * @param callback Callback to handle completion. Optional.
     */
    public fun sendFeedback(searchSuggestion: SearchSuggestion, responseInfo: ResponseInfo, event: FeedbackEvent, callback: CompletionCallback<Unit>? = null)

    /**
     * Sends feedback event to analytics.
     * @param historyRecord History record for which feedback is given.
     * @param event Extra information for feedback, provided by user.
     * @param callback Callback to handle completion. Optional.
     */
    public fun sendFeedback(historyRecord: HistoryRecord, event: FeedbackEvent, callback: CompletionCallback<Unit>? = null)

    /**
     * Sends feedback event to analytics.
     * @param favoriteRecord Favorite record for which feedback is given.
     * @param event Extra information for feedback, provided by user.
     * @param callback Callback to handle completion. Optional.
     */
    public fun sendFeedback(favoriteRecord: FavoriteRecord, event: FeedbackEvent, callback: CompletionCallback<Unit>? = null)

    /**
     * Sends missing result feedback event to analytics.
     * @param event Extra information for feedback, provided by user.
     * @param callback Callback to handle completion. Optional.
     */
    public fun sendMissingResultFeedback(event: MissingResultFeedbackEvent, callback: CompletionCallback<Unit>? = null)
}
