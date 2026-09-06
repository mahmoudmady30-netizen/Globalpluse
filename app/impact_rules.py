"""
Rule-based "potential impact" tagging.

This is deliberately NOT a machine-learning prediction. It is a documented,
inspectable lookup table: if a headline mentions certain keywords, we surface
which sectors/assets are *commonly* linked to that kind of news, labeled as
"potential impact" — never as a forecast or a fact.

Building an actual predictive model (the AI_FORECAST tier described in the
original project brief) needs historical price data, a trained and
back-tested model, and ongoing calibration monitoring — that's Phase 8/9
of that plan, not something that can honestly exist as a side-effect of a
keyword match. Do not upgrade this file into something that claims more
confidence than "these topics are commonly related."
"""

IMPACT_RULES: list[tuple[list[str], str]] = [
    (["oil", "opec", "crude", "petroleum"], "Energy sector, airlines (fuel costs), inflation-sensitive assets"),
    (["interest rate", "federal reserve", "fed ", "rate hike", "rate cut", "central bank"],
     "Bond yields, bank stocks, growth stocks, USD"),
    (["inflation", "cpi", "consumer price"], "Central bank policy expectations, bond yields, gold"),
    (["egp", "egyptian pound", "central bank of egypt"], "EGX-listed importers/exporters, inflation-sensitive EGX stocks"),
    (["usd", "dollar index", "dxy"], "Emerging-market currencies, gold, USD-denominated debt"),
    (["earnings", "quarterly results", "profit warning"], "The specific company's stock price and sector peers"),
    (["war", "conflict", "sanctions", "geopolitical"], "Oil, gold, defense stocks, regional currencies"),
    (["ipo", "listing", "initial public offering"], "Sector sentiment, brokerage/exchange volumes"),
    (["chip", "semiconductor", "AI ", "artificial intelligence"], "Tech sector, chipmakers, cloud infrastructure stocks"),
    (["gold", "bullion"], "Safe-haven demand, mining stocks, USD relationship"),
]


def infer_potential_impact(headline: str, summary: str | None) -> list[str]:
    text = f"{headline} {summary or ''}".lower()
    matches = []
    for keywords, impact in IMPACT_RULES:
        if any(keyword.lower() in text for keyword in keywords):
            matches.append(impact)
    return matches
