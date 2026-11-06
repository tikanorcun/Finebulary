package com.language.finebulary.data.model

data class StarterDeck(
    val name: String,
    val targetLang: String,
    val nativeLang: String,
    val words: List<StarterWord>
)

data class StarterWord(
    val front: String,
    val back: String,
    val definition: String,
    val example: String
)

object StarterDecksRegistry {
    val starterDecks = listOf(
        StarterDeck(
            name = "Essential English 101",
            targetLang = "en",
            nativeLang = "tr",
            words = listOf(
                StarterWord("Hello", "Merhaba", "A common greeting", "Hello, how are you?"),
                StarterWord("Thank you", "Teşekkür ederim", "Expression of gratitude", "Thank you for your help."),
                StarterWord("Friend", "Arkadaş", "A person whom one knows and with whom one has a bond of mutual affection", "She is my best friend."),
                StarterWord("School", "Okul", "An institution for educating children", "The kids are at school."),
                StarterWord("Book", "Kitap", "A written or printed work", "I am reading a good book.")
            )
        ),
        StarterDeck(
            name = "Business Spanish",
            targetLang = "es",
            nativeLang = "en",
            words = listOf(
                StarterWord("Empresa", "Company", "A business organization", "Nuestra empresa es líder en el mercado."),
                StarterWord("Reunión", "Meeting", "An assembly of people for a purpose", "Tenemos una reunión a las diez."),
                StarterWord("Presupuesto", "Budget", "An estimate of income and expenditure", "El presupuesto para el proyecto es limitado."),
                StarterWord("Contrato", "Contract", "A written or spoken agreement", "Firma el contrato aquí."),
                StarterWord("Mercado", "Market", "An area or arena in which commercial dealings are conducted", "El mercado está creciendo.")
            )
        )
    )
}
