"""
Central configuration. Nothing sensitive is hardcoded here — all secrets
come from environment variables (.env locally, real env vars in production).
"""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    marketaux_api_token: str = ""
    marketaux_daily_request_budget: int = 90

    currents_api_key: str = ""

    cache_dir: str = ".cache"
    cache_ttl_minutes: int = 30

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()
