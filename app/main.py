from pathlib import Path

from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from app.models import Market
from app.providers.base import ProviderError
from app.providers.currents import CurrentsProvider
from app.providers.marketaux import MarketauxProvider
from app.services.news_service import NewsService
from app.tickers import all_tickers

app = FastAPI(
    title="GlobalPulse News",
    description=(
        "News-awareness dashboard for EGX + US markets. "
        "Not a trading signal — see /disclaimer."
    ),
)

news_service = NewsService(
    ticker_provider=MarketauxProvider(),
    general_provider=CurrentsProvider(),
)

STATIC_DIR = Path(__file__).resolve().parent.parent / "static"
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


@app.get("/")
async def dashboard():
    return FileResponse(STATIC_DIR / "index.html")


@app.get("/api/tickers")
async def get_tickers():
    return {
        symbol: {"company_name": name, "market": market}
        for symbol, (name, market) in all_tickers().items()
    }


@app.get("/api/news")
async def get_news(
    market: Market | None = Query(default=None),
    include_general: bool = Query(default=True),
):
    try:
        items = await news_service.get_trending_news(market=market, include_general=include_general)
    except ProviderError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {"count": len(items), "items": [item.to_dashboard_dict() for item in items]}


@app.get("/disclaimer")
async def disclaimer():
    return {
        "notice": (
            "This app shows news headlines and automated sentiment scores for "
            "informational purposes only. It is not investment advice and does "
            "not recommend buying or selling anything. See DISCLAIMER.md."
        )
    }
