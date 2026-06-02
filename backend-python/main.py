import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging

from database.db_config import engine, Base
from api.routes import router as api_router
from scraper.scheduler import start_continuous_scheduler

# Define global logging format
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("PriceSentinelMain")

# 1. Initialize FastAPI Application
app = FastAPI(
    title="Price Sentinel API Service",
    description="Scalable BeautifulSoup web scraping and history alert daemon for Indian Retail Platforms",
    version="1.0.0"
)

# 2. Add CORS Middleware to satisfy Android and Web REST requests safely
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Allow any origin/agent to query pipeline
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 3. Mount API routers under versioned endpoint
app.include_router(api_router, prefix="/api/v1")

# On Startup Lifecycle: Build DB tables and start cron scraping schedules
@app.on_event("startup")
def startup_event():
    logger.info("Starting Price Sentinel Backend Cloud Server...")
    
    # Creates SQLAlchemy / PostgreSQL database schema structural tables if they do not exist
    try:
        Base.metadata.create_all(bind=engine)
        logger.info("Schema tables synchronized in PostgreSQL successfully.")
    except Exception as e:
        logger.error(f"PostgreSQL connection / schema sync failure: {str(e)}")
        logger.warning("Falling back to in-memory state engine simulation.")

    # Starts BeautifulSoup continuous cron scanner jobs
    start_continuous_scheduler()

@app.get("/")
def read_root():
    return {
        "status": "online",
        "service": "Price Sentinel Sentry Core Engine",
        "region": "ap-south-1 (Mumbai)",
        "docs": "/docs"
    }

if __name__ == "__main__":
    # Boots Uvicorn production listening daemon
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
