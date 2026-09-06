"""
Shared data shapes. Every news item carries its provenance (source, timestamps,
data_status) so the dashboard never shows a number or headline without
knowing where it came from — this is a hard rule, not a nice-to-have.
"""
from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel


SentimentLabel = Literal["bullish", "neutral", "bearish"]
DataStatus = Literal["LIVE", "DELAYED", "HISTORICAL", "CACHED"]
Market = Literal["us", "egx"]
VerificationStatus = Literal["SINGLE_SOURCE", "MULTI_SOURCE_VERIFIED", "CONFLICTING_REPORTS"]


class TickerMention(BaseModel):
    symbol: str
    company_name: str
    market: Market
    sentiment_score: float  # -1.0 to 1.0, as returned by the provider
    sentiment_label: SentimentLabel


class NewsItem(BaseModel):
    id: str
    headline: str
    summary: Optional[str] = None
    source: str
    source_url: str
    published_at: datetime
    fetched_at: datetime
    data_status: DataStatus
    tickers: list[TickerMention]

    # Filled in by the verification/impact pipeline in news_service.py —
    # not set by providers themselves.
    verification_status: VerificationStatus = "SINGLE_SOURCE"
    corroborating_sources: list[str] = []
    potential_impact: list[str] = []  # rule-based, transparent — see app/impact_rules.py

    def to_dashboard_dict(self) -> dict:
        return {
            "id": self.id,
            "headline": self.headline,
            "summary": self.summary,
            "source": self.source,
            "source_url": self.source_url,
            "published_at": self.published_at.isoformat(),
            "fetched_at": self.fetched_at.isoformat(),
            "data_status": self.data_status,
            "tickers": [t.model_dump() for t in self.tickers],
            "verification_status": self.verification_status,
            "corroborating_sources": self.corroborating_sources,
            "potential_impact": self.potential_impact,
        }


def sentiment_label_from_score(score: float) -> SentimentLabel:
    """Simple, transparent thresholding — not a black box.
    +/-0.15 deadband avoids labeling noise as a signal."""
    if score >= 0.15:
        return "bullish"
    if score <= -0.15:
        return "bearish"
    return "neutral"
