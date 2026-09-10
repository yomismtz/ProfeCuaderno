from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = "sqlite:///./backend.db"
    jwt_secret: str = "development-only-change-me"
    access_token_minutes: int = 10080
    bootstrap_director_email: str = ""
    bootstrap_director_password: str = ""
    cors_origins: str = "*"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @property
    def cors_list(self) -> list[str]:
        value = self.cors_origins.strip()
        return ["*"] if value == "*" else [item.strip() for item in value.split(",") if item.strip()]


settings = Settings()
