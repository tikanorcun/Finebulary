package com.language.finebulary.util

data class IdiomEntry(
    val english: String,
    val turkishMeaning: String,
    val explanation: String? = null
)

object LocalIdiomDictionary {

    private val idioms = listOf(
        IdiomEntry("a load of waffle", "boş konuşma, zırva, saçmalık", "Gerçek kelime anlamı yerine boş/anlamsız konuşmayı ifade eden deyim."),
        IdiomEntry("waffle on", "boş konuşmak, lafı uzatmak", "Gereksiz yere uzun ve boş konuşmak."),
        IdiomEntry("spill the beans", "ağzındaki baklayı çıkarmak, sırrı ifşa etmek", "Gizli bir bilgiyi veya sırrı açıklamak."),
        IdiomEntry("piece of cake", "çocuk oyuncağı, çok kolay", "Yapılması çok kolay olan iş."),
        IdiomEntry("hit the sack", "yatmaya gitmek, kafayı vurup yatmak", "Uyumak üzere yatağa gitmek."),
        IdiomEntry("hit the hay", "yatmaya gitmek, uyumak", "Uyumaya gitmek."),
        IdiomEntry("call it a day", "paydos etmek, bugünlük bu kadar demek", "Günü bitirmek, çalışmayı sonlandırmak."),
        IdiomEntry("bite the bullet", "dişini sıkmak, acı reçeteyi kabul etmek", "Zor bir duruma cesaretle katlanmak."),
        IdiomEntry("break a leg", "şeytanın bacağını kırmak, iyi şanslar", "Özellikle sahneye çıkacak birine şans dilemek."),
        IdiomEntry("cut to the chase", "sadede gelmek, uzatmadan konuya girmek", "Lafı dolandırmadan ana konuya geçmek."),
        IdiomEntry("cost an arm and a leg", "ateş pahası olmak, dünyanın parası olmak", "Aşırı derecede pahalı olmak."),
        IdiomEntry("under the weather", "keyifsiz, halsiz, hasta gibi", "Kendini biraz rahatsız veya hasta hissetmek."),
        IdiomEntry("once in a blue moon", "kırk yılda bir, ayda yılda bir", "Aşırı nadir gerçekleşen olay."),
        IdiomEntry("burn the midnight oil", "gece gündüz çalışmak, dirsek çürütmek", "Gece geç saatlere kadar ders çalışmak/çalışmak."),
        IdiomEntry("add fuel to the fire", "yangına körükle gitmek", "Kötü bir durumu daha da kışkırtmak."),
        IdiomEntry("back to square one", "baştan başlamak, en başa dönmek", "Elde var sıfır, en başa geri dönmek."),
        IdiomEntry("barking up the wrong tree", "yanlış kapıyı çalmak, yanlış hedef seçmek", "Hatalı bir yerde veya kişide çözüm aramak."),
        IdiomEntry("beat around the bush", "lafı dolandırmak, dereden tepeden konuşmak", "Ana konuya girmekten kaçınarak dolaylı konuşmak."),
        IdiomEntry("blessing in disguise", "her işte bir hayır vardır, başta kötü görünen kısmet", "İlk başta kötü görünen ama sonradan hayırlı çıkan durum."),
        IdiomEntry("break the ice", "buzları eritmek, ortamı yumuşatmak", "Yabancılar arasındaki gerginliği/soğukluğu gidermek."),
        IdiomEntry("burn bridges", "köprüleri yakmak", "Geri dönüşü olmayan kararlar almak."),
        IdiomEntry("by the skin of my teeth", "ucuz atlatmak, ucu ucuna", "Aşırı dar bir farkla kıl payı başarmak/kurtulmak."),
        IdiomEntry("compare apples to oranges", "elma ile armudu karşılaştırmak", "Birbiriyle alakası olmayan iki şeyi kıyaslamak."),
        IdiomEntry("cry over spilt milk", "son pişmanlık fayda etmez", "Olan biten geçmiş bir şey için üzülmek."),
        IdiomEntry("drive someone up the wall", "birini çileden çıkarmak", "Birini aşırı derecede rahatsız etmek/çıldırtmak."),
        IdiomEntry("face the music", "sonuçlarına katlanmak, cezasını çekmek", "Yaptıklarının olumsuz sonuçlarıyla yüzleşmek."),
        IdiomEntry("get out of hand", "kontrolden çıkmak, çığırından çıkmak", "Durumun hakimiyet dışına çıkması."),
        IdiomEntry("give someone the cold shoulder", "soğuk davranmak, görmezden gelmek", "Birine karşı ilgisiz ve soğuk tutum sergilemek."),
        IdiomEntry("go down in flames", "fiyasko ile sonuçlanmak, tamamen batmak", "Büyük bir başarısızlığa uğramak."),
        IdiomEntry("hit the nail on the head", "tam üstüne basmak, taşı gediğine koymak", "Bir durumu veya sorunu tam isabetle teşhis etmek."),
        IdiomEntry("jump on the bandwagon", "akıma kapılmak, popüler olana uymak", "Herkes yapıyor diye bir akıma dahil olmak."),
        IdiomEntry("keep an eye on", "göz kulak olmak", "Bir şeyi veya kimseyi gözlemek/korumak."),
        IdiomEntry("let the cat out of the bag", "sırrı açığa çıkarmak, ağzından kaçırmak", "İstemeden veya bilerek sırrı faş etmek."),
        IdiomEntry("miss the boat", "fırsatı kaçırmak", "Ağır davranıp uygun zamanı kaçırmak."),
        IdiomEntry("no pain no gain", "emek olmadan yemek olmaz", "Zahmetsiz rahmet olmaz."),
        IdiomEntry("on the fence", "kararsız kalmak, ikilemde olmak", "İki seçenek arasında taraf seçememek."),
        IdiomEntry("pull someone's leg", "biriyle kafa bulmak, takılmak", "Birine şaka yapmak, kandırmaya çalışmak."),
        IdiomEntry("speak of the devil", "iti an çomağı hazırla", "Hakkında konuşulan kişinin çıkıp gelmesi."),
        IdiomEntry("take with a grain of salt", "şüpheyle yaklaşmak, ihtiyatla karşılamak", "Söylenen her şeye körü körüne inanmamak."),
        IdiomEntry("the best of both worlds", "her iki durumun da avantajını yaşamak", "İki farklı seçeneğin de en iyi yanına sahip olmak."),
        IdiomEntry("through thick and thin", "iyi günde kötü günde", "Her türlü zorlukta ve koşulda yanında olmak."),
        IdiomEntry("up in the air", "belirsiz, henüz netleşmemiş", "Plan veya kararın henüz havada kalması."),
        IdiomEntry("wrap one's head around", "aklı ermek, bir durumu kavrayabilmek", "Karmaşık bir konuyu anlayabilmek."),

        // Phrasal Verbs & Common Expressions
        IdiomEntry("figure out", "anlamak, çözmek, mantığını kavramak"),
        IdiomEntry("give up", "vazgeçmek, pes etmek, bırakmak"),
        IdiomEntry("look forward to", "dört gözle beklemek, sabırsızlanmak"),
        IdiomEntry("run out of", "tükenmek, bitmek, kalmamak"),
        IdiomEntry("turn down", "reddetmek, geri çevirmek, sesini kısmak"),
        IdiomEntry("take place", "gerçekleşmek, meydana gelmek, olmak"),
        IdiomEntry("catch up", "yakalamak, aynı seviyeye gelmek, yetişmek"),
        IdiomEntry("bring up", "gündeme getirmek, çocuk büyütmek"),
        IdiomEntry("come across", "rastlamak, tesadüfen bulmak, denk gelmek"),
        IdiomEntry("pass away", "vefat etmek, hayatını kaybetmek"),
        IdiomEntry("put off", "ertelemek, soğutmak, tiksindirmek"),
        IdiomEntry("set up", "kurmak, düzenlemek, tuzak kurmak"),
        IdiomEntry("break down", "bozulmak, ruhen yıkılmak, parçalara ayırmak"),
        IdiomEntry("call off", "iptal etmek, son vermek"),
        IdiomEntry("carry out", "yürütmek, gerçekleştirmek, uygulamak"),
        IdiomEntry("get along", "iyi anlaşmak, geçinmek"),
        IdiomEntry("give in", "teslim olmak, pes etmek, boyun eğmek"),
        IdiomEntry("look up to", "hayranlık duymak, saygı duymak, örnek almak"),
        IdiomEntry("make up", "uydurmak, telafi etmek, barışmak, makyaj yapmak"),
        IdiomEntry("run into", "rastlamak, karşılaşmak, çarpmak"),
        IdiomEntry("take off", "havalanmak (uçak), çıkarmak (giysi), birden popüler olmak"),
        IdiomEntry("turn out", "ortaya çıkmak, sonuçlanmak, çıkmak"),
        IdiomEntry("work out", "egzersiz yapmak, sorunu çözmek, işe yarama"),
        IdiomEntry("blow away", "büyülemek, hayran bırakmak, uçurmak"),
        IdiomEntry("bring about", "neden olmak, yol açmak, sebep olmak"),
        IdiomEntry("check out", "incelemek, göz atmak, otelden ayrılmak"),
        IdiomEntry("come up with", "fikir/çözüm üretmek, ortaya atmak"),
        IdiomEntry("cut down on", "azaltmak, kısmak, tasarruf etmek"),
        IdiomEntry("drop by", "uğramak, kısa bir ziyaret yapmak"),
        IdiomEntry("fall apart", "dağılmak, parçalanmak, çökmek"),
        IdiomEntry("get away with", "yanına kâr kalmak, cezasız atlatmak"),
        IdiomEntry("get over", "atlatmak, üstesinden gelmek, iyileşmek"),
        IdiomEntry("keep up with", "ayak uydurmak, hızına yetişmek, takip etmek"),
        IdiomEntry("pay off", "meyvesini vermek, borcu kapatmak, karşılığını almak"),
        IdiomEntry("point out", "dikkat çekmek, işaret etmek, belirtmek"),
        IdiomEntry("stand out", "göze çarpmak, öne çıkmak, farklı olmak"),
        IdiomEntry("stick to", "sadık kalmak, bağlı kalmak, ayrılmamak"),
        IdiomEntry("take over", "devralmak, üstlenmek, ele geçirmek"),
        IdiomEntry("use up", "tüketmek, bitirmek, sonuna kadar kullanmak"),
        IdiomEntry("count on", "güvenmek, bel bağlamak"),
        IdiomEntry("cross out", "üstünü çizmek, silmek"),
        IdiomEntry("look down on", "hor görmek, küçük görmek"),
        IdiomEntry("rule out", "gözden çıkarmak, elemek, imkansız saymak"),
        IdiomEntry("show off", "hava atmak, gösteriş yapmak"),
        IdiomEntry("stand for", "temsil etmek, anlamına gelmek, katlanmak"),
        IdiomEntry("take after", "benzemek (aile büyüğüne)"),
        IdiomEntry("wear out", "yıpratmak, eskitmek, çok yormak")
    )

    private fun normalize(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
    }

    fun findIdiom(text: String, nativeLangCode: String = "tr"): IdiomEntry? {
        val clean = normalize(text)
        if (clean.isBlank()) return null

        // 1. Direct normalized match
        val exactMatch = idioms.firstOrNull { normalize(it.english) == clean }
        if (exactMatch != null) return exactMatch

        // 2. Partial containment match for multi-word phrases (e.g., "it's a load of waffle" -> matches "a load of waffle")
        val containmentMatch = idioms
            .filter { it.english.split(" ").size >= 2 }
            .firstOrNull { clean.contains(normalize(it.english)) }

        return containmentMatch
    }
}
