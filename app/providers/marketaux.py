"""
Marketaux implementation of NewsProvider.

Docs: https://www.marketaux.com/documentation
Free tier: 100 requests/day, no card required. Each request can return
multiple articles, so we ask for several symbols per call rather than
one call per ticker.

This module has NOT been run against the live API in this environment
(sandboxed, no network access) — it is built directly from Marketaux's
documented request/response shape. Before relying on it, run
`pytest tests/` (which uses a fake provider, no network needed) and then
do one manual smoke-test call with your real token.
"""
from datetime import datetime, timezone

import httpx

from app.cache import get as cache_get, set as cache_set, log_request, request_count_today
from app.config import settings
from app.models import NewsItem, TickerMention, sentiment_label_from_score
from app.providers.base import NewsProvider, ProviderError
from app.tickers import all_tickers

BASE_URL = "https://api.marketaux.com/v1"


class MarketauxProvider(NewsProvider):
    @property
    def name(self) -> str:
        return "marketaux"

    async def fetch_news_for_tickers(self, symbols: list[str]) -> list[NewsItem]:
        if not settings.marketaux_api_token:
            raise ProviderError(
                "MARKETAUX_API_TOKEN is not set. Get a free token at "
                "https://www.marketaux.com/ and add it to your .env file."
            )

        cache_key = f"marketaux:{','.join(sorted(symbols))}"
        cached = cache_get(cache_key)
        if cached is not None:
            return [_parse_article(a, from_cache=True) for a in cached["data"]]

        if request_count_today() >= settings.marketaux_daily_request_budget:
            raise ProviderError(
                "Daily Marketaux request budget reached. Showing cached data "
                "only until tomorrow — this protects your free-tier quota."
            )

        params = {
            "api_token": settings.marketaux_api_token,
            "symbols": ",".join(symbols),
            "filter_entities": "true",
            "language": "en",
            "limit": 25,
        }

        async with httpx.AsyncClient(timeout=15.0) as client:
            try:
                response = await client.get(f"{BASE_URL}/news/all", params=params)
                response.raise_for_status()
            except httpx.HTTPError as exc:
                raise ProviderError(f"Marketaux request failed: {exc}") from exc

        log_request()
        payload = response.json()
        cache_set(cache_key, payload)

        return [_parse_article(a, from_cache=False) for a in payload.get("data", [])]


def _parse_article(article: dict, from_cache: bool) -> NewsItem:
    tickers = []
    for entity in article.get("entities", []):
        symbol = entity.get("symbol")
        if not symbol or symbol not in all_tickers():
            continue
        company_name, market = all_tickers()[symbol]
        score = float(entity.get("sentiment_score", 0.0))
        tickers.append(
            TickerMention(
                symbol=symbol,
                company_name=company_name,
                market=market,
                sentiment_score=score,
                sentiment_label=sentiment_label_from_score(score),
            )
        )

    return NewsItem(
        id=article["uuid"],
        headline=article["title"],
        summary=article.get("description"),
        source=article.get("source", "unknown"),
        source_url=article["url"],
        published_at=datetime.fromisoformat(
            article["published_at"].replace("Z", "+00:00")
        ),
        fetched_at=datetime.now(timezone.utc),
        data_status="CACHED" if from_cache else "DELAYED",
        tickers=tickers,
    )
