package com.example.clashroyaleclanstatviewer

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ClashRoyaleApi {

    // Gets current river race for the provided clan tag. tag must be URL encoded (e.g. %2322YLRLV)
    @GET("clans/{tag}/currentriverrace")
    fun getCurrentRiverRace(
        @Header("Authorization") token: String,
        @Path("tag", encoded = true) tag: String
    ): Call<CurrentRiverRaceResponse>

    // Get clan members (to see current members + roles)
    @GET("clans/{tag}/members")
    fun getClanMembers(
        @Header("Authorization") token: String,
        @Path("tag", encoded = true) tag: String
    ): Call<ClanMembersResponse>

    // Fetch the log of past river races (for calculating lives retroactively)
    @GET("clans/{tag}/riverracelog")
    fun getRiverRaceLog(
        @Header("Authorization") token: String,
        @Path("tag", encoded = true) tag: String,
        @Query("limit") limit: Int = 20 // Get last 20 wars
    ): Call<RiverRaceLogResponse>
}

// --- Data Models (JSON mapping) ---

data class RiverRaceLogResponse(
    val items: List<RiverRaceLogItem>
)

data class RiverRaceLogItem(
    val seasonId: Int,
    val sectionIndex: Int,
    val createdDate: String, // format: "20231122T100000.000Z"
    val standings: List<Standing>?
) {
    // Helper to generate a unique ID for a specific war week
    fun getUniqueId(): String = "$seasonId-$sectionIndex"
}

data class CurrentRiverRaceResponse(
    val state: String,
    val sectionIndex: Int,
    val clan: ClanInfo // In current race, our clan info is nested here
)

data class Standing(
    val clan: ClanInfo
)

data class ClanInfo(
    val tag: String,
    val name: String,
    val participants: List<Participant>
)

data class Participant(
    val tag: String,
    val name: String,
    val decksUsed: Int, // This is the key metric: Attacks used
    val fame: Int
)

data class WarStat(
    val attacks: Int,
    val fame: Int
)

// A custom class to hold the combined data for our App's UI
data class MemberDisplay(
    val tag: String,
    val name: String,
    val role: String,
    val lives: Int,
    val currentAttacks: Int,
    val inCurrentWar: Boolean,
    val totalFameHistory: Int = 0,
    val currentFame: Int = 0,
    val warHistory: List<WarStat> = emptyList()
)

data class ClanMembersResponse(
    val items: List<ClanMember>
)

data class ClanMember(
    val tag: String,
    val name: String,
    val role: String? // Leader, Co-Leader, Member, etc (optional)
)
