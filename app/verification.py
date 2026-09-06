"""
Multi-source verification via simple, transparent clustering.

What this IS: if the same story (by headline similarity) shows up from
multiple distinct sources, we mark it MULTI_SOURCE_VERIFIED and list who
corroborated it. One source only -> SINGLE_SOURCE. This is literally just
counting distinct sources reporting a similar story — no judgment is made
about which source is "correct."

What this is NOT: a truth/fact-check score. We never claim a story is true
or false. "Multiple outlets reported this" and "this is accurate" are
different claims, and this module only ever makes the first one.

CONFLICTING_REPORTS is reserved for a narrower, explicit case: multiple
sources covering the same entities/topic with numbers that contradict each
other (e.g. different reported figures) — not just "wrote about the same
thing." Detecting that reliably needs entity/number extraction beyond this
MVP's scope, so today the pipeline only ever emits SINGLE_SOURCE or
MULTI_SOURCE_VERIFIED. The type is kept for when that gets built, rather than
faking a distinction we can't actually detect yet.
"""
import re

from app.models import NewsItem

_STOPWORDS = {
    "the", "a", "an", "of", "in", "on", "for", "to", "and", "is", "are",
    "at", "as", "by", "with", "its", "it's", "after", "amid", "over",
}


def _title_tokens(headline: str) -> set[str]:
    words = re.findall(r"[a-zA-Z0-9]+", headline.lower())
    return {w for w in words if w not in _STOPWORDS and len(w) > 2}


def _similarity(a: set[str], b: set[str]) -> float:
    if not a or not b:
        return 0.0
    intersection = len(a & b)
    union = len(a | b)
    return intersection / union if union else 0.0


def apply_verification(items: list[NewsItem], similarity_threshold: float = 0.45) -> list[NewsItem]:
    """Mutates and returns items with verification_status + corroborating_sources set."""
    token_cache = [_title_tokens(item.headline) for item in items]

    for i, item in enumerate(items):
        corroborating = {item.source}
        for j, other in enumerate(items):
            if i == j or other.source == item.source:
                continue
            if _similarity(token_cache[i], token_cache[j]) >= similarity_threshold:
                corroborating.add(other.source)

        item.corroborating_sources = sorted(corroborating)
        item.verification_status = (
            "MULTI_SOURCE_VERIFIED" if len(corroborating) > 1 else "SINGLE_SOURCE"
        )

    return items
