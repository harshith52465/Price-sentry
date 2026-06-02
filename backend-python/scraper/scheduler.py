from apscheduler.schedulers.background import BackgroundScheduler
import logging
import time

from database.db_config import SessionLocal
from database.schema import DBTrackedItem, DBPriceHistoryPoint, DBNotificationItem
from .engine import perform_general_web_scraping

logger = logging.getLogger("SchedulerSentry")

def execute_scheduled_price_scan():
    """
    Core scheduler callback block. Iterates over active items in PostgreSQL,
    performs BeautifulSoup requests, and handles threshold checks.
    """
    logger.info("Scheduler batch routine initiated on all active trackers...")
    db = SessionLocal()
    try:
        items = db.query(DBTrackedItem).filter(DBTrackedItem.isWatched == True).all()
        for item in items:
            logger.info(f"Scanning target node: {item.name} [{item.platform}]")
            
            # Initiate BeautifulSoup scraper module
            data = perform_general_web_scraping(item.productUrl, item.platform)
            
            if data and data.get("current_price"):
                new_price = data["current_price"]
                old_price = item.currentPrice
                
                # Check if price actually changed
                if new_price != old_price:
                    logger.info(f"Price change detected for {item.name}! ₹{old_price} -> ₹{new_price}")
                    
                    # 1. Update main table item
                    item.currentPrice = new_price
                    now_ms = int(time.time() * 1000)
                    
                    alert_type = None
                    if new_price < item.historicalLow:
                        item.historicalLow = new_price
                        alert_type = "HISTORICAL_LOW"
                    elif new_price <= item.targetPrice and old_price > item.targetPrice:
                        alert_type = "TARGET_MET"
                    elif (item.originalPrice - new_price) / item.originalPrice >= 0.30:
                        alert_type = "EXCEPTIONAL_DEAL"
                        
                    # 2. Add price history node
                    history_point = DBPriceHistoryPoint(
                        itemId=item.id,
                        price=new_price,
                        timestamp=now_ms
                    )
                    db.add(history_point)
                    
                    # 3. Create active in-app alert notification
                    if alert_type:
                        notification = DBNotificationItem(
                            itemId=item.id,
                            itemName=item.name,
                            platform=item.platform,
                            oldPrice=old_price,
                            newPrice=new_price,
                            alertType=alert_type,
                            timestamp=now_ms,
                            isRead=False
                        )
                        db.add(notification)
                        logger.info(f"Triggered notification alert payload ({alert_type}) for {item.name}")
                        
                    db.commit()
                else:
                    logger.info(f"No price variance for {item.name}. Held steady.")
    except Exception as e:
        logger.error(f"Error during scheduler scanning execution: {str(e)}")
        db.rollback()
    finally:
        db.close()

def start_continuous_scheduler():
    """
    Initializes the Cron-Scheduler in a background thread
    """
    scheduler = BackgroundScheduler()
    # Runs the scraper routine once every 15 minutes in standard production context
    scheduler.add_job(execute_scheduled_price_scan, "interval", minutes=2)
    scheduler.start()
    logger.info("Continuous Sentry Cron Scraper Scheduler started (Interval: 15 minutes)")
