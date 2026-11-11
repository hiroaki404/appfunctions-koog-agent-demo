package dev.hiroaki404.appfunctions.koogdemo.tripapp

import androidx.appfunctions.AppFunctionSerializable

/** A tourist spot or activity that can be visited during a trip. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Spot(
    /** The spot name in Japanese, for example "小樽運河". */
    val name: String,
    /** A short Japanese description of the spot and what to do there. */
    val description: String,
)

/** The plan for one day of a trip. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ItineraryDay(
    /** The calendar date of this day in `yyyy-MM-dd` format, for example "2026-12-20". */
    val date: String,
    /** The spots to visit on this day, in visiting order. */
    val spots: List<Spot>,
)

/** A trip plan made of one or more days. The dates of its days show when the trip takes place. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Itinerary(
    /** The trip title in Japanese, for example "2025年夏の北海道". */
    val title: String,
    /** The days of the trip, in date order. */
    val days: List<ItineraryDay>,
)

/** A confirmed flight booking. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class FlightBooking(
    /** The booking number assigned by the app, for example "TRP-0001". */
    val bookingId: String,
    /** The flight number assigned by the app, for example "TR501". */
    val flightNumber: String,
    /** The departure city in Japanese, for example "東京". */
    val fromCity: String,
    /** The arrival city in Japanese, for example "札幌". */
    val toCity: String,
    /** The departure date in `yyyy-MM-dd` format. */
    val date: String,
)

/**
 * The user's answer to a confirmation request for an action that needs their approval. The calling
 * agent fills this in after asking the user; the LLM never sets it.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class UserApproval(
    /** Whether the user approved this action. */
    val approved: Boolean,
)
