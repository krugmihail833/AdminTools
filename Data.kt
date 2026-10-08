package com.grandmobile.admintools

import android.content.Context

class Act(val cmd: String, val a: Int = 0, val b: Int = 0) {
    fun times(): List<Int> = when {
        a == 0 -> listOf(0)
        a == b -> listOf(a)
        else -> listOf(a, b)
    }
}

class Vio(val code: String, val note: String, val variants: List<List<Act>>)

// строка: код|пояснение|вариант / вариант ; в варианте действия через «+» ; срок: N или A-B
private val TABLE = """
2.1 ОП|оск. игрока|mute 15-30
2.1 ОП|упом. род|mute 90-180
2.1 ОП|оск. род|ban 3-10
3.1 ОП|оффтоп в реп|rmute 30
3.2 ОП|оск. адм|mute 30-60 / ban 3-31
2.5 ОП|шум в микро|mute 20
2.5 ОП|музыка в микро|mute 30-60
5.2 ОП|багаюз|warn
5.2 ОП|софт|ban 31
5.3 ОП|уход от рп|jail 120
1.1 GZ|урон в зз|jail 60+warn
1.2 GZ|веревка в зз|warn
DM||jail 30-60
DB||jail 30
MG||mute 15
RK||jail 30
SK||jail 60-120
PG||jail 30
Mass DM||warn
Mass DM в GZ||jail 90+warn
""".trimIndent()

object Data {
    val V: List<Vio> = TABLE.lines().map { l ->
        val p = l.split("|")
        Vio(p[0], p[1], p[2].split("/").map { v ->
            v.split("+").map { a ->
                val t = a.trim().split(" ")
                val r = t.getOrNull(1)?.split("-")
                val x = r?.get(0)?.toInt() ?: 0
                Act(t[0], x, r?.getOrNull(1)?.toInt() ?: x)
            }
        })
    }
}

val DEF_T = linkedMapOf(
    "ans" to "/pm {id} {r}",
    "jail" to "/jail {id} {t} {r}",
    "ban" to "/ban {id} {t} {r}",
    "mute" to "/mute {id} {t} {r}",
    "rmute" to "/rmute {id} {t} {r}",
    "warn" to "/warn {id} {r}"
)

val DEF_ANS = """
re|Приветствую, приятной игры на Grand Mobile 10 <3
re|Приветствую, слежу за Вами.
re|Приветствую, не владеем данной информацией.
re|Приветствую, при следующем оффтопе последует мут репорта.
re|Приветствую, при наличии доказательств подайте жалобу.
re|Приветствую, не чиним, воспользуйтесь рем.комплектом/эвакуатором.
re|Приветствую, уточните ваш вопрос или жалобу.
re|Приветствую, слежу за игроком.
re|Приветствую, не вмешиваемся в РП процесс.
re|Приветствую, не чиним / не заправляем / не выдаем / не телепортируем.
re|Приветствую, если не согласны с наказанием, напишите жалобу.
re|Приветствую, рп термин.
re|Приветствую, нашли баг? обратитесь в тех.поддержку.
re|Приветствую, ожидайте.
re|Приветствую, не предоставляем данную услугу.
re|Приветствую, можете узнать самостоятельно.
re|Приветствую, помогли Вам. Приятной игры!<3
re|Приветствую, слежу за игроком!
ev|Приветствую, можете помочь себе сами — кнопка эвакуатора на спидометре.
ev|Приветствую, можете воспользоваться эвакуатором — кнопка на спидометре.
ev|Приветствую, нажмите кнопку эвакуатора на спидометре для помощи.
ev|Приветствую, чтобы перевернуть автомобиль, нажмите кнопку эвакуатора на спидометре.
ev|Приветствую, чтобы починить автомобиль, нажмите кнопку эвакуатора на спидометре.
ev|Приветствую, чтобы выбраться из воды, нажмите кнопку эвакуатора на спидометре.
ev|Здравствуйте, Вы можете помочь себе самостоятельно, нажав кнопку эвакуатора на спидометре.
rev|Приветствую, игрок был наказан, приятной игры!
rev|Приветствую, за спам и флуд в чате наказания не выдаем!
rev|Приветствую, администрация не является гарантом обмена.
rev|Приветствую, телепорт на мероприятие закрыт.
rev|Приветствую, игрок не нарушает.
rev|Приветствую, за время наблюдения нарушений не зафиксировано.
""".trimIndent()

class Store(c: Context) {
    private val p = c.getSharedPreferences("s", Context.MODE_PRIVATE)
    fun templates(): Map<String, String> = DEF_T.mapValues { p.getString("t_" + it.key, it.value) ?: it.value }
    fun answersText(): String = p.getString("ans", DEF_ANS) ?: DEF_ANS
    fun answers(): List<Pair<String, String>> = answersText().lines().mapNotNull { l ->
        val i = l.indexOf('|')
        if (i < 1 || i == l.length - 1) null else l.substring(0, i).trim() to l.substring(i + 1).trim()
    }
    fun save(t: Map<String, String>, a: String) {
        val e = p.edit()
        t.forEach { (k, v) -> e.putString("t_$k", v) }
        e.putString("ans", a).apply()
    }
    fun reset() { p.edit().clear().apply() }
}
