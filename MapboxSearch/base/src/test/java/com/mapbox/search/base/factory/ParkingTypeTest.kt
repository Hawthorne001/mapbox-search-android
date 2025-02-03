package com.mapbox.search.base.factory

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.search.base.core.CoreParkingRestriction
import com.mapbox.search.base.core.CoreParkingType
import com.mapbox.search.common.ParkingRestriction
import com.mapbox.search.common.ParkingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(MapboxExperimental::class)
class ParkingTypeTest {

    @Test
    fun `CoreParkingType mapToPlatform() test`() {
        assertEquals(ParkingType.ALONG_MOTORWAY, CoreParkingType.ALONG_MOTORWAY.mapToPlatform())
        assertEquals(ParkingType.PARKING_GARAGE, CoreParkingType.PARKING_GARAGE.mapToPlatform())
        assertEquals(ParkingType.PARKING_LOT, CoreParkingType.PARKING_LOT.mapToPlatform())
        assertEquals(ParkingType.ON_DRIVEWAY, CoreParkingType.ON_DRIVEWAY.mapToPlatform())
        assertEquals(ParkingType.ON_STREET, CoreParkingType.ON_STREET.mapToPlatform())
        assertEquals(ParkingType.UNDERGROUND_GARAGE, CoreParkingType.UNDERGROUND_GARAGE.mapToPlatform())
        assertEquals(ParkingType.UNKNOWN, CoreParkingType.UNKNOWN.mapToPlatform())
    }

    @Test
    fun `createCoreParkingType() test`() {
        assertEquals(CoreParkingType.ALONG_MOTORWAY, createCoreParkingType(ParkingType.ALONG_MOTORWAY))
        assertEquals(CoreParkingType.PARKING_GARAGE, createCoreParkingType(ParkingType.PARKING_GARAGE))
        assertEquals(CoreParkingType.PARKING_LOT, createCoreParkingType(ParkingType.PARKING_LOT))
        assertEquals(CoreParkingType.ON_DRIVEWAY, createCoreParkingType(ParkingType.ON_DRIVEWAY))
        assertEquals(CoreParkingType.ON_STREET, createCoreParkingType(ParkingType.ON_STREET))
        assertEquals(CoreParkingType.UNDERGROUND_GARAGE, createCoreParkingType(ParkingType.UNDERGROUND_GARAGE))
        assertEquals(CoreParkingType.UNKNOWN, createCoreParkingType(ParkingType.UNKNOWN))
        assertNull(createCoreParkingType("UNKNOWN_TYPE"))
    }

    @Test
    fun `CoreParkingRestriction mapToPlatform() test`() {
        assertEquals(ParkingRestriction.EV_ONLY, CoreParkingRestriction.EV_ONLY.mapToPlatform())
        assertEquals(ParkingRestriction.PLUGGED, CoreParkingRestriction.PLUGGED.mapToPlatform())
        assertEquals(ParkingRestriction.DISABLED, CoreParkingRestriction.DISABLED.mapToPlatform())
        assertEquals(ParkingRestriction.CUSTOMERS, CoreParkingRestriction.CUSTOMERS.mapToPlatform())
        assertEquals(ParkingRestriction.MOTOR_CYCLES, CoreParkingRestriction.MOTOR_CYCLES.mapToPlatform())
        assertEquals(ParkingRestriction.UNKNOWN, CoreParkingRestriction.UNKNOWN.mapToPlatform())
    }
}
