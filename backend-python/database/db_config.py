import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# Database URL: Reads postgres configuration from environment variables
DB_USER = os.getenv("POSTGRES_USER", "scandeals_user")
DB_PASSWORD = os.getenv("POSTGRES_PASSWORD", "secure_password_123")
DB_HOST = os.getenv("POSTGRES_HOST", "16.112.223.66")
DB_PORT = os.getenv("POSTGRES_PORT", "5432")
DB_NAME = os.getenv("POSTGRES_DB", "scandeals")

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# SQLAlchemy engine configured for connection pooling and robustness
engine = create_engine(
    DATABASE_URL,
    pool_size=10,
    max_overflow=20,
    pool_recycle=1800,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# Dependency override to yield database connection inside API routes
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
