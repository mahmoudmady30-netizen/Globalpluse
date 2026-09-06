"""
A deliberately simple file-based cache. The Marketaux free tier is 100
requests/day — this exists to make sure a page refresh (or five) doesn't
burn the daily budget, not to be a general-purpose caching layer.
"""
import hashlib
import json
import time
from pathlib import Path
from typing import Optional

from app.config import settings


def _cache_path(key: str) -> Path:
    cache_dir = Path(settings.cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256(key.encode("utf-8")).hexdigest()
    return cache_dir / f"{digest}.json"


def get(key: str) -> Optional[dict]:
    path = _cache_path(key)
    if not path.exists():
        return None
    payload = json.loads(path.read_text(encoding="utf-8"))
    age_minutes = (time.time() - payload["cached_at"]) / 60
    if age_minutes > settings.cache_ttl_minutes:
        return None
    return payload["data"]


def set(key: str, data: dict) -> None:
    path = _cache_path(key)
    payload = {"cached_at": time.time(), "data": data}
    path.write_text(json.dumps(payload), encoding="utf-8")


def request_count_today() -> int:
    """Counts how many *live* (non-cached) provider requests were logged today."""
    log_path = Path(settings.cache_dir) / "request_log.json"
    if not log_path.exists():
        return 0
    log = json.loads(log_path.read_text(encoding="utf-8"))
    today = time.strftime("%Y-%m-%d")
    return log.get(today, 0)


def log_request() -> None:
    log_path = Path(settings.cache_dir) / "request_log.json"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log = json.loads(log_path.read_text(encoding="utf-8")) if log_path.exists() else {}
    today = time.strftime("%Y-%m-%d")
    log[today] = log.get(today, 0) + 1
    log_path.write_text(json.dumps(log), encoding="utf-8")
