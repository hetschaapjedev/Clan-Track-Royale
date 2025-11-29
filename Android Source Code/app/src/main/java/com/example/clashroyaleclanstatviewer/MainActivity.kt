package com.example.clashroyaleclanstatviewer

import android.animation.ArgbEvaluator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    // --- CONFIG ---
    private val API_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiIsImtpZCI6IjI4YTMxOGY3LTAwMDAtYTFlYi03ZmExLTJjNzQzM2M2Y2NhNSJ9.eyJpc3MiOiJzdXBlcmNlbGwiLCJhdWQiOiJzdXBlcmNlbGw6Z2FtZWFwaSIsImp0aSI6ImU0ZGVkMmJjLTBhYWEtNGM5MS05NGJkLTc5MjRjNTg2NGFjYSIsImlhdCI6MTc2NDI4MTc1MSwic3ViIjoiZGV2ZWxvcGVyLzZjYjAyMDYwLTJiM2QtZmFiZS02ZWViLTYzZTgyYTA0NWJiOCIsInNjb3BlcyI6WyJyb3lhbGUiXSwibGltaXRzIjpbeyJ0aWVyIjoiZGV2ZWxvcGVyL3NpbHZlciIsInR5cGUiOiJ0aHJvdHRsaW5nIn0seyJjaWRycyI6WyI0NS43OS4yMTguNzkiXSwidHlwZSI6ImNsaWVudCJ9XX0.JLDdLSgajlJeqiYqFJjfHSQoPnb_iyXuEt_4qm4TmeVmuOe3XgN7dFCQQjcj0amExaWcYcLgrj6It0lo1Ky2DA"

    private lateinit var viewPager: ViewPager2
    private lateinit var api: ClashRoyaleApi
    private lateinit var rootLayout: View
    private lateinit var tvPageTitle: TextView
    private lateinit var tvClanName: TextView

    companion object {
        var sharedMemberList: List<MemberDisplay> = emptyList()
        var currentScrollIndex = 0
        var currentScrollOffset = 0
    }

    private var thresholdGain = 12
    private var thresholdLose = 3
    private var maxLives = 3
    private var startLives = 1
    private var currentClanTag = "#000000" //Our clan code #22YLRLV

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        tvPageTitle = findViewById(R.id.tvPageTitle)
        tvClanName = findViewById(R.id.tvClanName)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://proxy.royaleapi.dev/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ClashRoyaleApi::class.java)

        loadSettings()

        viewPager = findViewById(R.id.viewPager)
        viewPager.offscreenPageLimit = 2
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tabDots)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // --- SPACING LOGIC ---
        val spacingDp = 12
        val spacingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, spacingDp.toFloat(), resources.displayMetrics).toInt()
        val tabStrip = tabLayout.getChildAt(0) as? ViewGroup
        tabStrip?.let { strip ->
            for (i in 0 until strip.childCount) {
                val params = strip.getChildAt(i).layoutParams as ViewGroup.MarginLayoutParams
                params.marginEnd = if (i != strip.childCount - 1) spacingPx else 0
                if (i == 0) params.marginStart = spacingPx / 2
                strip.getChildAt(i).layoutParams = params
            }
        }

        setupColorMorphing()

        findViewById<View>(R.id.btnRefresh).setOnClickListener { fetchData() }
        findViewById<View>(R.id.btnSettings).setOnClickListener { showGlassySettings() }

        fetchData()
    }

    private fun setupColorMorphing() {
        val colorBlue = ContextCompat.getColor(this, R.color.theme_blue)
        val colorPurple = ContextCompat.getColor(this, R.color.theme_purple)
        val colorRed = ContextCompat.getColor(this, R.color.theme_red)
        val evaluator = ArgbEvaluator()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                var colorUpdate: Int = colorBlue
                if (position == 0) colorUpdate = evaluator.evaluate(positionOffset, colorBlue, colorPurple) as Int
                else if (position == 1) colorUpdate = evaluator.evaluate(positionOffset, colorPurple, colorRed) as Int
                else colorUpdate = colorRed
                rootLayout.setBackgroundColor(colorUpdate)
            }

            override fun onPageSelected(position: Int) {
                when(position) {
                    0 -> tvPageTitle.text = "LIVES DASHBOARD"
                    1 -> tvPageTitle.text = "FAME LEADERBOARD"
                    2 -> tvPageTitle.text = "WAR HISTORY LOG"
                }

                // --- SCROLL SYNC TRIGGER ---
            }
        })
    }

    interface ScrollSyncable {
        fun scrollToStoredPosition()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DashboardFragment()
                1 -> StatsFragment()
                else -> WarHistoryFragment()
            }
        }
    }

    private fun fetchData() {
        Toast.makeText(this, "Calculating...", Toast.LENGTH_SHORT).show()
        val encodedTag = URLEncoder.encode(currentClanTag, "UTF-8")
        api.getRiverRaceLog(API_TOKEN, encodedTag).enqueue(object : Callback<RiverRaceLogResponse> {
            override fun onResponse(call: Call<RiverRaceLogResponse>, response: Response<RiverRaceLogResponse>) {
                if (response.isSuccessful) {
                    val logItems = response.body()?.items ?: emptyList()
                    val (livesMap, historyMap, totalFameMap) = calculateLivesAndHistory(logItems)
                    fetchClanMembersAndCurrentWar(encodedTag, logItems, livesMap, historyMap, totalFameMap)
                }
            }
            override fun onFailure(call: Call<RiverRaceLogResponse>, t: Throwable) {}
        })
    }

    private fun fetchClanMembersAndCurrentWar(encodedTag: String, logs: List<RiverRaceLogItem>, livesMap: Map<String, Int>, historyMap: Map<String, List<WarStat>>, totalFameMap: Map<String, Int>) {
        api.getClanMembers(API_TOKEN, encodedTag).enqueue(object : Callback<ClanMembersResponse> {
            override fun onResponse(call: Call<ClanMembersResponse>, response: Response<ClanMembersResponse>) {
                if (response.isSuccessful) {
                    val clanMembers = response.body()?.items ?: emptyList()
                    val clanMap: Map<String, String> = clanMembers.associate { normalizeTag(it.tag) to (it.role ?: "Member") }
                    fetchCurrentWar(encodedTag, clanMap, logs, livesMap, historyMap, totalFameMap)
                }
            }
            override fun onFailure(call: Call<ClanMembersResponse>, t: Throwable) {}
        })
    }

    private fun fetchCurrentWar(encodedTag: String, clanMap: Map<String, String>, logs: List<RiverRaceLogItem>, livesMap: Map<String, Int>, historyMap: Map<String, List<WarStat>>, totalFameMap: Map<String, Int>) {
        api.getCurrentRiverRace(API_TOKEN, encodedTag).enqueue(object : Callback<CurrentRiverRaceResponse> {
            override fun onResponse(call: Call<CurrentRiverRaceResponse>, response: Response<CurrentRiverRaceResponse>) {
                if (response.isSuccessful) {
                    val currentWar = response.body()
                    val clanData = currentWar?.clan
                    if (clanData != null) tvClanName.text = clanData.name.uppercase()
                    else tvClanName.text = "UNKNOWN CLAN"
                    val participants = clanData?.participants ?: emptyList()
                    processAndDisplayData(participants, clanMap, logs, livesMap, historyMap, totalFameMap)
                }
            }
            override fun onFailure(call: Call<CurrentRiverRaceResponse>, t: Throwable) {}
        })
    }

    private fun normalizeTag(tag: String?): String {
        return tag?.trim()?.uppercase()?.removePrefix("#") ?: ""
    }

    private fun calculateLivesAndHistory(logs: List<RiverRaceLogItem>): Triple<Map<String, Int>, Map<String, List<WarStat>>, Map<String, Int>> {
        val playerLives = mutableMapOf<String, Int>()
        val history = mutableMapOf<String, MutableList<WarStat>>()
        val totalFame = mutableMapOf<String, Int>()
        for (log in logs.reversed()) {
            val ourClan = log.standings?.find { normalizeTag(it.clan.tag) == normalizeTag(currentClanTag) }?.clan
            ourClan?.participants?.forEach { p ->
                val key = normalizeTag(p.tag)
                val list = history.getOrPut(key) { mutableListOf() }
                list.add(WarStat(attacks = p.decksUsed, fame = p.fame))
                totalFame[key] = (totalFame[key] ?: 0) + p.fame
                var life = playerLives.getOrDefault(key, startLives)
                if (p.decksUsed >= thresholdGain) life += 1
                else if (p.decksUsed <= thresholdLose) life -= 1
                playerLives[key] = life.coerceIn(0, maxLives)
            }
        }
        return Triple(playerLives.toMap(), history.mapValues { it.value.toList() }, totalFame.toMap())
    }

    private fun processAndDisplayData(currentParticipants: List<Participant>, clanMap: Map<String, String>, historyLogs: List<RiverRaceLogItem>, livesMap: Map<String, Int>, historyMap: Map<String, List<WarStat>>, totalFameMap: Map<String, Int>) {
        val displayList = ArrayList<MemberDisplay>()
        for ((normTag, role) in clanMap) {
            val currentP = currentParticipants.find { normalizeTag(it.tag) == normTag }
            val life = livesMap[normTag] ?: startLives
            val decks = currentP?.decksUsed ?: 0
            val totalFame = totalFameMap[normTag] ?: 0
            val fullHist = historyMap[normTag] ?: emptyList()
            val recent = if (fullHist.isEmpty()) emptyList() else fullHist.reversed().take(10)
            val currentFame = currentP?.fame ?: 0

            displayList.add(MemberDisplay(tag = "#$normTag", name = currentP?.name ?: "Member", role = role, lives = life, currentAttacks = decks, inCurrentWar = (currentP != null), totalFameHistory = totalFame, currentFame = currentFame, warHistory = recent))
        }
        displayList.sortByDescending { it.lives }
        sharedMemberList = displayList

        supportFragmentManager.fragments.forEach {
            if (it is DashboardFragment) it.refresh()
            if (it is StatsFragment) it.refresh()
            if (it is WarHistoryFragment) it.refresh()
        }
    }

    // --- NEW GLASSY SETTINGS DIALOG ---
    private fun showGlassySettings() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_settings_glass)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)

        val etTag = dialog.findViewById<EditText>(R.id.etClanTag)
        val etGain = dialog.findViewById<EditText>(R.id.etGain)
        val etLose = dialog.findViewById<EditText>(R.id.etLose)
        val etMax = dialog.findViewById<EditText>(R.id.etMaxLives)

        etTag.setText(currentClanTag)
        etGain.setText(thresholdGain.toString())
        etLose.setText(thresholdLose.toString())
        etMax.setText(maxLives.toString())

        dialog.findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val t = etTag.text.toString().trim()
            val g = etGain.text.toString().toIntOrNull()
            val l = etLose.text.toString().toIntOrNull()
            val m = etMax.text.toString().toIntOrNull()

            if (g != null && l != null && m != null && t.isNotEmpty()) {
                thresholdGain = g
                thresholdLose = l
                maxLives = m
                currentClanTag = t
                saveSettings()
                fetchData()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Invalid Inputs", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("ClashApp", Context.MODE_PRIVATE)
        prefs.edit().putInt("gain", thresholdGain).putInt("lose", thresholdLose).putInt("maxLives", maxLives).putString("clanTag", currentClanTag).apply()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("ClashApp", Context.MODE_PRIVATE)
        thresholdGain = prefs.getInt("gain", 12)
        thresholdLose = prefs.getInt("lose", 3)
        maxLives = prefs.getInt("maxLives", 3)
        currentClanTag = prefs.getString("clanTag", "#000000") ?: "#000000"
    }
}