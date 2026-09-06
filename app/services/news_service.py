"""
Orchestration layer between the API routes and the providers. Keeping this
separate from main.py means the routing layer never talks to a provider
directly -- swap or add providers here, once.
"""
from app.impact_rules import infer_potential_impact
from app.models import Market, NewsItem
from app.providers.base import NewsProvider, ProviderError
from app.providers.currents import CurrentsProvider
from app.tickers import all_tickers
from app.verification import apply_verification


class NewsService:
    def __init__(self, ticker_provider: NewsProvider, general_provider: CurrentsProvider | None = None):
        self._ticker_provider = ticker_provider
        self._general_provider = general_provider

    async def get_trending_news(
        self, market: Market | None = None, include_general: bool = True
    ) -> list[NewsItem]:
        tickers = all_tickers()
        symbols = [
            symbol
            for symbol, (_, ticker_market) in tickers.items()
            if market is None or ticker_market == market
        ]
        items = await self._ticker_provider.fetch_news_for_tickers(symbols)

        # Only keep ticker items that actually mention a symbol we track for
        # the requested market -- the provider may return broader matches.
        if market is not None:
            items = [i for i in items if any(t.market == market for t in i.tickers)]

        if include_general and self._general_provider is not None:
            try:
                general_items = await self._general_provider.fetch_general_news()
                items = items + general_items
            except ProviderError:
                # General news is a supplement, not the core feature -- if it's
                # unavailable (missing key, quota hit), degrade quietly rather
                # than breaking the whole response.
                pass

        for item in items:
            item.potential_impact = infer_potential_impact(item.headline, item.summary)

        items = apply_verification(items)

        return sorted(items, key=lambda i: i.published_at, reverse=True)
