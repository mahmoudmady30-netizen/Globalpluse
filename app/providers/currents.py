"""
Currents API implementation — for general world/local news (business,
politics, economy), as opposed to Marketaux which is ticker-tagged financial
news specifically. Docs: https://currentsapi.services/en/docs/

Free tier (as documented at the time this was written): no card required,
several hundred requests/day. Terms have shifted between "personal use" and
"commercial use permitted" across different provider comparisons — verify
the current ToS at currentsapi.services before relying on this for anything
beyond personal use.

Like marketaux.py, this has NOT been exercised against the live API in this
sandbox (no network access here) — built from documented request/response
shape. Smoke-test it yourself before trusting it.
"""
from datetime import datetime, timezone

import httpx

from app.cache import get as cache_get, set as cache_set
from app.config import settings
from app.models import NewsItem
from app.providers.base import NewsProvider, ProviderError

BASE_URL = "https://api.currentsapi.services/v1"

# Keywords chosen to cover world/local news relevant to a beginner investor —
# not an exhaustive news feed, a filtered one.
DEFAULT_KEYWORDS = [
    "economy", "inflation", "central bank", "interest rate",
    "Egypt economy", "EGX", "oil price", "geopolitical",
]


class CurrentsProvider(NewsProvider):
    @property
    def name(self) -> str:
        return "currents"

    async def fetch_general_news(self, keywords: list[str] | None = None) -> list[NewsItem]:
        if not settings.currents_api_key:
            raise ProviderError(
                "CURRENTS_API_KEY is not set. Get a free key at "
                "https://currentsapi.services/ and add it to your .env file."
            )

        keywords = keywords or DEFAULT_KEYWORDS
        query = " OR ".join(keywords)
        cache_key = f"currents:{query}"
        cached = cache_get(cache_key)
        if cached is not None:
            return [_parse_article(a, from_cache=True) for a in cached.get("news", [])]

        params = {"apiKey": settings.currents_api_key, "keywords": query, "language": "en"}

        async with httpx.AsyncClient(timeout=15.0) as client:
            try:
                response = await client.get(f"{BASE_URL}/search", params=params)
                response.raise_for_status()
            except httpx.HTTPError as exc:
                raise ProviderError(f"Currents API request failed: {exc}") from exc

        payload = response.json()
        cache_set(cache_key, payload)
        return [_parse_article(a, from_cache=False) for a in payload.get("news", [])]

    # NewsProvider requires this method, but Currents is ticker-agnostic —
    # general news doesn't key off symbols, so this just calls the real method.
    async def fetch_news_for_tickers(self, symbols: list[str]) -> list[NewsItem]:
        return await self.fetch_general_news()


def _parse_article(article: dict, from_cache: bool) -> NewsItem:
    return NewsItem(
        id=article["id"],
        headline=article["title"],
        summary=article.get("description"),
        source=article.get("author") or _domain_from_url(article.get("url", "")),
        source_url=article["url"],
        published_at=datetime.fromisoformat(
            article["published"].replace("Z", "+00:00")
        ),
        fetched_at=datetime.now(timezone.utc),
        data_status="CACHED" if from_cache else "DELAYED",
        tickers=[],
    )


def _domain_from_url(url: str) -> str:
    try:
        return url.split("/")[2].replace("www.", "")
    except IndexError:
        return "unknown"
