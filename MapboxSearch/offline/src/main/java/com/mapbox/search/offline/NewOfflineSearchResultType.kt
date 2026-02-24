package com.mapbox.search.offline

import androidx.annotation.StringDef
import com.mapbox.search.base.core.CoreResultType

/**
 * Defines type of the [OfflineSearchResult].
 * Replaces enum [OfflineSearchResult].
 */
public object NewOfflineSearchResultType {

    /**
     * Generally recognized countries.
     */
    public const val COUNTRY: String = "COUNTRY"

    /**
     * Top-level sub-national administrative features, like states or provinces.
     */
    public const val REGION: String = "REGION"

    /**
     * Typically these are cities, villages, municipalities, etc.
     * They’re usually features used in postal addressing, and are suitable for display in ambient
     * end-user applications where current-location context is needed (for example, in weather displays).
     */
    public const val PLACE: String = "PLACE"

    /**
     * Official sub-city features like city districts.
     */
    public const val LOCALITY: String = "LOCALITY"

    /**
     * Colloquial sub-city features, like neighborhoods.
     */
    public const val NEIGHBORHOOD: String = "NEIGHBORHOOD"

    /**
     * Features that are smaller than places and that correspond to streets in cities, villages, etc.
     */
    public const val STREET: String = "STREET"

    /**
     * Individual residential or business addresses.
     */
    public const val ADDRESS: String = "ADDRESS"

    /**
     * Points of interest.
     * These include EV charging stations, restaurants, stores, concert venues, parks, museums, etc.
     */
    public const val POI: String = "POI"

    /**
     * Retention policy for the [NewOfflineSearchResultType].
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        COUNTRY,
        REGION,
        PLACE,
        LOCALITY,
        NEIGHBORHOOD,
        STREET,
        ADDRESS,
        POI,
    )
    public annotation class Type

    @Type
    @JvmSynthetic
    internal val FALLBACK_TYPE = PLACE

    @Type
    internal fun createFromRawResultType(type: CoreResultType): String? {
        return when (type) {
            CoreResultType.COUNTRY -> COUNTRY
            CoreResultType.REGION -> REGION
            CoreResultType.PLACE -> PLACE
            CoreResultType.LOCALITY -> LOCALITY
            CoreResultType.NEIGHBORHOOD -> NEIGHBORHOOD
            CoreResultType.STREET -> STREET
            CoreResultType.ADDRESS -> ADDRESS
            CoreResultType.POI -> POI
            CoreResultType.DISTRICT,
            CoreResultType.POSTCODE,
            CoreResultType.BLOCK,
            CoreResultType.CATEGORY,
            CoreResultType.BRAND,
            CoreResultType.QUERY,
            CoreResultType.USER_RECORD,
            CoreResultType.UNKNOWN -> null
        }
    }

    @Suppress("DEPRECATION")
    @JvmSynthetic
    internal fun toOldResultType(@Type type: String): OfflineSearchResultType {
        return when (type) {
            PLACE -> OfflineSearchResultType.PLACE
            STREET -> OfflineSearchResultType.STREET
            ADDRESS -> OfflineSearchResultType.ADDRESS
            NEIGHBORHOOD, LOCALITY, REGION, COUNTRY -> OfflineSearchResultType.DEFAULT
            else -> OfflineSearchResultType.DEFAULT
        }
    }
}
