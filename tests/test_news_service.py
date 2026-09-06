from datetime import datetime, timezone

import pytest

from app.models import NewsItem, TickerMention
from app.providers.base import NewsProvider
from app.services.news_service import NewsService


class FakeProvider(NewsProvider):
    """A provider that returns canned data — lets us test the service layer
    without hitting the real Marketaux API (no network needed to run this)."""

    def __init__(self, items: list[NewsItem]):
        self._items = items

    @property
    def name(self) -> str:
        return "fake"

    async def fetch_news_for_tickers(self, symbols: list[str]) -> list[NewsItem]:
        return self._items


def make_item(symbol: str, market: str, published_at: datetime) -> NewsItem:
    return NewsItem(
        id=f"id-{symbol}-{published_at.isoformat()}",
        headline=f"Headline about {symbol}",
        summary=None,
        source="Test Source",
        source_url="https://example.com",
        published_at=published_at,
        fetched_at=datetime.now(timezone.utc),
        data_status="DELAYED",
        tickers=[
            TickerMention(
                symbol=symbol,
                company_name=f"{symbol} Inc.",
                market=market,
                sentiment_score=0.3,
                sentiment_label="bullish",
            )
        ],
    )


@pytest.mark.asyncio
async def test_filters_by_market():
    items = [
        make_item("AAPL", "us", datetime(2026, 1, 1, tzinfo=timezone.utc)),
        make_item("COMI", "egx", datetime(2026, 1, 2, tzinfo=timezone.utc)),
    ]
    service = NewsService(ticker_provider=FakeProvider(items))

    us_only = await service.get_trending_news(market="us", include_general=False)
    assert len(us_only) == 1
    assert us_only[0].tickers[0].symbol == "AAPL"

    egx_only = await service.get_trending_news(market="egx", include_general=False)
    assert len(egx_only) == 1
    assert egx_only[0].tickers[0].symbol == "COMI"


@pytest.mark.asyncio
async def test_sorts_newest_first():
    items = [
        make_item("AAPL", "us", datetime(2026, 1, 1, tzinfo=timezone.utc)),
        make_item("MSFT", "us", datetime(2026, 1, 5, tzinfo=timezone.utc)),
    ]
    service = NewsService(ticker_provider=FakeProvider(items))

    result = await service.get_trending_news(include_general=False)
    assert result[0].tickers[0].symbol == "MSFT"
    assert result[1].tickers[0].symbol == "AAPL"


@pytest.mark.asyncio
async def test_marks_single_source_when_no_corroboration():
    items = [make_item("AAPL", "us", datetime(2026, 1, 1, tzinfo=timezone.utc))]
    service = NewsService(ticker_provider=FakeProvider(items))

    result = await service.get_trending_news(include_general=False)
    assert result[0].verification_status == "SINGLE_SOURCE"


@pytest.mark.asyncio
async def test_tags_potential_impact_from_keywords():
    item = make_item("AAPL", "us", datetime(2026, 1, 1, tzinfo=timezone.utc))
    item.headline = "Oil prices surge amid supply concerns"
    service = NewsService(ticker_provider=FakeProvider([item]))

    result = await service.get_trending_news(include_general=False)
    assert len(result[0].potential_impact) > 0
