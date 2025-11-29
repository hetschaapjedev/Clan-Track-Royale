package com.example.clashroyaleclanstatviewer

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GlassyAdapter(private val mode: Int) : RecyclerView.Adapter<GlassyAdapter.GlassViewHolder>() {
    private var list: List<MemberDisplay> = emptyList()

    fun submitList(newList: List<MemberDisplay>) {
        list = newList
        notifyDataSetChanged()
    }

    class GlassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view // To attach click listener
        val name: TextView = view.findViewById(R.id.tvName)
        val role: TextView = view.findViewById(R.id.tvRole)
        val lives: TextView = view.findViewById(R.id.tvLives)
        val detail: TextView = view.findViewById(R.id.tvDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member_glassy, parent, false)
        return GlassViewHolder(view)
    }

    override fun onBindViewHolder(holder: GlassViewHolder, position: Int) {
        val item = list[position]

        holder.name.text = item.name
        holder.role.text = item.role

        // --- DISPLAY LOGIC (Same as before) ---
        if (mode == 0) { // Dashboard
            holder.lives.visibility = View.VISIBLE
            holder.lives.text = "❤️ ${item.lives}"
            if (item.lives == 0) holder.lives.setTextColor(Color.parseColor("#FF073A"))
            else holder.lives.setTextColor(Color.WHITE)

            holder.detail.text = "Current: ${item.currentAttacks}/16 (Fame ${item.currentFame})"
            if (item.currentAttacks >= 4) holder.detail.setTextColor(Color.parseColor("#39FF14"))
            else holder.detail.setTextColor(Color.parseColor("#B3FFFFFF"))
        }
        else if (mode == 1) { // Stats
            holder.lives.visibility = View.GONE
            holder.detail.text = "Total Fame: ${item.totalFameHistory}"
            holder.detail.setTextColor(Color.parseColor("#00E5FF"))
        }
        else { // History
            holder.lives.visibility = View.GONE
            val hist = item.warHistory
            if (hist.isEmpty()) {
                holder.detail.text = "No recorded history"
                holder.detail.setTextColor(Color.LTGRAY)
            } else {
                // Show tiny summary
                val summary = hist.take(5).joinToString(" ") { "[${it.attacks}]" }
                holder.detail.text = "Last 5 Wars: $summary"
                holder.detail.setTextColor(Color.WHITE)
            }
        }

        // --- CLICK LISTENER FOR POPUP ---
        holder.root.setOnClickListener {
            showDetailPopup(holder.itemView, item, mode)
        }
    }

    override fun getItemCount() = list.size

    // --- POPUP LOGIC ---
    private fun showDetailPopup(view: View, item: MemberDisplay, mode: Int) {
        val context = view.context
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_detail_popup)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)

        // Dim background
        val lp = dialog.window?.attributes
        lp?.dimAmount = 0.7f
        dialog.window?.attributes = lp

        // Fill Data
        dialog.findViewById<TextView>(R.id.tvPopupName).text = item.name
        dialog.findViewById<TextView>(R.id.tvPopupRole).text = item.role
        val tvLives = dialog.findViewById<TextView>(R.id.tvPopupLives)
        tvLives.text = "❤️ ${item.lives}"
        if(item.lives == 0) tvLives.setTextColor(Color.parseColor("#FF073A"))

        val tvCategory = dialog.findViewById<TextView>(R.id.tvPopupCategory)
        val container = dialog.findViewById<LinearLayout>(R.id.llStatsContainer)

        // Dynamic Content based on Mode
        container.removeAllViews()

        if (mode == 0) {
            tvCategory.text = "CURRENT WAR STATUS"
            addStatRow(context, container, "Decks Used", "${item.currentAttacks} / 16")
            addStatRow(context, container, "Current Fame", "${item.currentFame}")
            addStatRow(context, container, "Status", if(item.inCurrentWar) "Participating" else "Not in Boat")
        }
        else if (mode == 1) {
            tvCategory.text = "FAME HISTORY"
            addStatRow(context, container, "Total Recorded Fame", "${item.totalFameHistory}")
            addStatRow(context, container, "Wars Tracked", "${item.warHistory.size}")
        }
        else {
            tvCategory.text = "WAR ATTACK LOG"
            if (item.warHistory.isEmpty()) {
                addStatRow(context, container, "No Data", "---")
            } else {
                // Header
                addStatRow(context, container, "WAR ID", "ATTACKS | FAME", true)
                item.warHistory.forEachIndexed { index, stat ->
                    val label = "War -${index + 1}" // "War -1" means 1 war ago
                    val value = "${stat.attacks} used | ${stat.fame}"
                    addStatRow(context, container, label, value)
                }
            }
        }

        dialog.findViewById<Button>(R.id.btnClosePopup).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addStatRow(context: android.content.Context, parent: LinearLayout, label: String, value: String, isHeader: Boolean = false) {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, 8, 0, 8)

        val tvLabel = TextView(context)
        tvLabel.text = label
        tvLabel.setTextColor(if(isHeader) Color.CYAN else Color.LTGRAY)
        tvLabel.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val tvValue = TextView(context)
        tvValue.text = value
        tvValue.setTextColor(if(isHeader) Color.CYAN else Color.WHITE)
        tvValue.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        row.addView(tvLabel)
        row.addView(tvValue)
        parent.addView(row)
    }
}