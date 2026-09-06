"""
NewsProvider interface. Adding a new source (free or licensed, later) means
writing one class that implements this — nothing in main.py or news_service.py
needs to change.
"""
from abc import ABC, abstractmethod

from app.models import NewsItem


class NewsProvider(ABC):
    """Every provider fetches news for a given set of ticker symbols and
    returns fully-formed NewsItem objects, with provenance already attached."""

    @abstractmethod
    async def fetch_news_for_tickers(self, symbols: list[str]) -> list[NewsItem]:
        ...

    @property
    @abstractmethod
    def name(self) -> str:
        ...


class ProviderError(Exception):
    """Raised on any provider failure (network, auth, rate limit, bad response).
    Callers decide how to degrade — e.g. fall back to cache — not the provider."""
