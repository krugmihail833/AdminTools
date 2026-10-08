package com.grandmobile.admintools

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val st = Store(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun tv(t: String) = TextView(this).apply { text = t; textSize = 15f; setPadding(0, pad / 2, 0, pad / 4) }
        fun bt(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }

        root.addView(tv("1. Включите «Админ-клавиатура» в списке клавиатур, затем выберите её. Приложение работает без интернета."))
        root.addView(bt("Открыть настройки клавиатур") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        root.addView(bt("Выбрать клавиатуру") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        root.addView(tv("2. Быстрые ответы, по одному на строку: группа|текст"))
        val ans = EditText(this).apply { setText(st.answersText()); minLines = 8; gravity = Gravity.TOP }
        root.addView(ans)
        root.addView(tv("3. Шаблоны команд ({id} ID, {t} срок, {r} причина или текст ответа)"))
        val tpl = st.templates()
        val fields = DEF_T.keys.associateWith { k ->
            EditText(this).apply { setText(tpl.getValue(k)); setSingleLine() }
        }
        fields.forEach { (k, e) -> root.addView(tv(k)); root.addView(e) }
        root.addView(bt("Сохранить") {
            val t = fields.mapValues { en -> en.value.text.toString().ifBlank { DEF_T.getValue(en.key) } }
            st.save(t, ans.text.toString())
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        })
        root.addView(bt("Сбросить всё") { st.reset(); recreate() })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
