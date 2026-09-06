"""
Starter ticker/company configuration.

IMPORTANT — read before trusting this list:
This is a small, hand-picked starter set for an MVP, not a synced copy of any
official index. It exists so the app has something to query on day one.

  - US_TICKERS: a handful of large, extremely well-established NYSE/NASDAQ
    companies. Low risk of being wrong (these have been listed for decades),
    but not a complete market.

  - EGX_TICKERS: long-standing, well-known EGX-listed companies. EGX30 index
    membership itself is reviewed twice a year by the exchange — this list
    is NOT presented as "the current EGX30" and should be checked against
    https://www.egx.com.eg before you rely on it for anything real.

Treat this file as configuration you own and update, not verified fact.
When the Security Master Sync Engine described in the Phase 1 architecture
doc gets built, this file goes away and the list comes from EGX/exchange
data directly.
"""

US_TICKERS: dict[str, str] = {
    "AAPL": "Apple Inc.",
    "MSFT": "Microsoft Corporation",
    "AMZN": "Amazon.com, Inc.",
    "GOOGL": "Alphabet Inc.",
    "NVDA": "NVIDIA Corporation",
    "TSLA": "Tesla, Inc.",
    "META": "Meta Platforms, Inc.",
}

EGX_TICKERS: dict[str, str] = {
    "COMI": "Commercial International Bank (Egypt)",
    "ETEL": "Telecom Egypt",
    "TMGH": "Talaat Moustafa Group Holding",
    "HRHO": "EFG Hermes Holding",
    "SWDY": "Elsewedy Electric",
}


def all_tickers() -> dict[str, tuple[str, str]]:
    """Returns {symbol: (company_name, market)}"""
    merged: dict[str, tuple[str, str]] = {}
    for symbol, name in US_TICKERS.items():
        merged[symbol] = (name, "us")
    for symbol, name in EGX_TICKERS.items():
        merged[symbol] = (name, "egx")
    return merged
