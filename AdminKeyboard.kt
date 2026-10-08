package com.grandmobile.admintools

import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private val BTN = 0xFF2A3050.toInt()
private val ACC = 0xFF3D5AFE.toInt()
private val COL = mapOf(
    "jail" to 0xFF7A5200.toInt(), "ban" to 0xFF8A2A27.toInt(),
    "mute" to 0xFF1F4A8F.toInt(), "rmute" to 0xFF1F4A8F.toInt(), "warn" to 0xFF533589.toInt()
)

class AdminKeyboard : InputMethodService() {
    private val st by lazy { Store(this) }
    private lateinit var list: LinearLayout
    private lateinit var idTv: TextView
    private lateinit var tb0: Button
    private lateinit var tb1: Button
    private var tab = 0
    private var open = -1
    private var grp = ""
    private val id = StringBuilder()
    private var tpl: Map<String, String> = DEF_T
    private var ans: List<Pair<String, String>> = emptyList()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun btn(t: String, col: Int = BTN, w: Float = 1f, size: Float = 14f, h: Int = 40, click: () -> Unit): Button =
        Button(this).apply {
            text = t; isAllCaps = false; textSize = size
            setTextColor(Color.WHITE); setBackgroundColor(col)
            minHeight = 0; minimumHeight = 0
            setPadding(dp(6), 0, dp(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(h), w).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
            setOnClickListener { click() }
        }

    private fun hrow(vararg v: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        v.forEach { addView(it) }
    }

    private fun fmt(cmd: String, t: Int, r: String): String =
        (tpl[cmd] ?: DEF_T.getValue(cmd))
            .replace("{id}", if (id.isEmpty()) "ID" else id.toString())
            .replace("{t}", if (t == 0) "" else t.toString())
            .replace("{r}", r)
            .replace(Regex("\\s+"), " ").trim()

    private fun commit(s: String) { currentInputConnection?.commitText(s, 1) }

    private fun reload() { tpl = st.templates(); ans = st.answers() }

    override fun onCreateInputView(): View {
        reload()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF14182B.toInt())
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        idTv = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1.2f)
        }
        tb0 = btn("Наказания") { setTab(0) }
        tb1 = btn("Ответы") { setTab(1) }
        val abc = btn("ABC") {
            val ok = if (Build.VERSION.SDK_INT >= 28) switchToPreviousInputMethod() else false
            if (!ok) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        root.addView(hrow(tb0, tb1, abc, idTv))

        fun key(d: Char) = btn(d.toString()) { if (id.length < 7) id.append(d); idChanged() }
        root.addView(hrow(*"12345".map { key(it) }.toTypedArray(),
            btn("⌫") { if (id.isNotEmpty()) id.deleteCharAt(id.length - 1); idChanged() }))
        root.addView(hrow(*"67890".map { key(it) }.toTypedArray(),
            btn("C") { id.setLength(0); idChanged() }))

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sv = ScrollView(this).apply { addView(list) }
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)))

        idChanged()
        setTab(tab)
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::list.isInitialized) { reload(); renderList() }
    }

    private fun idChanged() {
        idTv.text = "ID: " + (if (id.isEmpty()) "—" else id.toString())
        if (::list.isInitialized) renderList()
    }

    private fun setTab(t: Int) {
        tab = t; open = -1
        tb0.setBackgroundColor(if (t == 0) ACC else BTN)
        tb1.setBackgroundColor(if (t == 1) ACC else BTN)
        renderList()
    }

    private fun renderList() {
        list.removeAllViews()
        if (tab == 0) {
            Data.V.forEachIndexed { i, v ->
                val label = v.code + (if (v.note.isBlank()) "" else " (" + v.note + ")")
                list.addView(hrow(btn((if (open == i) "▾ " else "▸ ") + label, if (open == i) ACC else BTN) {
                    open = if (open == i) -1 else i
                    renderList()
                }))
                if (open == i) {
                    v.variants.forEach { acts ->
                        acts.forEach { a ->
                            a.times().forEach { t ->
                                val c = fmt(a.cmd, t, v.code)
                                list.addView(hrow(btn(c, COL[a.cmd] ?: BTN, size = 15f) { commit(c) }))
                            }
                        }
                    }
                }
            }
        } else {
            val groups = listOf("") + ans.map { it.first }.distinct()
            list.addView(hrow(*groups.map { g ->
                btn(if (g.isEmpty()) "Все" else g, if (grp == g) ACC else BTN) { grp = g; renderList() }
            }.toTypedArray()))
            ans.filter { grp.isEmpty() || it.first == grp }.forEach { (_, text) ->
                list.addView(hrow(btn(text, BTN, size = 13f, h = 56) { commit(fmt("ans", 0, text)) }))
            }
        }
    }
}
