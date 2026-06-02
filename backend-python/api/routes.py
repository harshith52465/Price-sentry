from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from typing import List, Optional
import time

from database.db_config import get_db
from database.schema import DBTrackedItem, DBPriceHistoryPoint, DBNotificationItem
from .models import (
    TrackedItemCreate, 
    TrackedItemResponse, 
    TrackedItemUpdateTarget, 
    NotificationResponse,
    PriceHistoryPointSchema
)

router = APIRouter()

# 1. Fetch All Active Tracked Deals (With Filtering & Query parameters)
@router.get("/deals", response_model=List[TrackedItemResponse])
def get_all_deals(
    category: Optional[str] = Query(None, description="Filter by category"),
    search: Optional[str] = Query(None, description="Search term matching item or platform"),
    db: Session = Depends(get_db)
):
    query = db.query(DBTrackedItem)
    if category and category != "All":
        query = query.filter(DBTrackedItem.category == category)
    if search:
        query = query.filter(
            (DBTrackedItem.name.ilike(f"%{search}%")) | 
            (DBTrackedItem.platform.ilike(f"%{search}%"))
        )
    return query.all()


# 2. Add Item to Sentry Scanning Pipeline
@router.post("/deals", response_model=TrackedItemResponse, status_code=status.HTTP_201_CREATED)
def create_deal(payload: TrackedItemCreate, db: Session = Depends(get_db)):
    now_ms = int(time.time() * 1000)
    
    # Establish dynamic initial values
    db_item = DBTrackedItem(
        name=payload.name,
        platform=payload.platform,
        currentPrice=payload.currentPrice,
        originalPrice=payload.originalPrice,
        historicalLow=payload.currentPrice,
        historicalHigh=payload.originalPrice if payload.originalPrice > payload.currentPrice else payload.currentPrice,
        imageUrl=payload.imageUrl or "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&q=80",
        productUrl=payload.productUrl,
        targetPrice=payload.targetPrice,
        isWatched=True,
        category=payload.category,
        rating=4.5,
        addedAt=now_ms
    )
    db.add(db_item)
    db.commit()
    db.refresh(db_item)
    
    # Add initial price history checkpoint
    history_point = DBPriceHistoryPoint(
        itemId=db_item.id,
        price=payload.currentPrice,
        timestamp=now_ms
    )
    db.add(history_point)
    db.commit()
    
    return db_item


# 3. Retrieve Historical Price Details & Line Curve Points
@router.get("/deals/{item_id}", response_model=TrackedItemResponse)
def get_deal_by_id(item_id: int, db: Session = Depends(get_db)):
    item = db.query(DBTrackedItem).filter(DBTrackedItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Tracked product item not found in Pipeline")
    return item


@router.get("/deals/{item_id}/history", response_model=List[PriceHistoryPointSchema])
def get_deal_price_history(item_id: int, db: Session = Depends(get_db)):
    history = db.query(DBPriceHistoryPoint).filter(DBPriceHistoryPoint.itemId == item_id).order_by(DBPriceHistoryPoint.timestamp.asc()).all()
    return history


# 4. Modify Sentry Target Alert Threshold
@router.put("/deals/{item_id}/target-price", response_model=TrackedItemResponse)
def update_target_threshold(item_id: int, payload: TrackedItemUpdateTarget, db: Session = Depends(get_db)):
    item = db.query(DBTrackedItem).filter(DBTrackedItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Tracked item not configured")
    
    item.targetPrice = payload.targetPrice
    db.commit()
    db.refresh(item)
    return item


# 5. Untrack / Delete Sentry Pipeline Item
@router.delete("/deals/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_tracked_item(item_id: int, db: Session = Depends(get_db)):
    item = db.query(DBTrackedItem).filter(DBTrackedItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Tracked item not configured")
    
    db.delete(item)
    db.commit()
    return


# 6. Fetch Global Alert Notifications
@router.get("/notifications", response_model=List[NotificationResponse])
def get_notifications(db: Session = Depends(get_db)):
    return db.query(DBNotificationItem).order_by(DBNotificationItem.timestamp.desc()).all()


# 7. Batch Clear/Read Notifications
@router.post("/notifications/clear-unread")
def clear_all_unread(db: Session = Depends(get_db)):
    db.query(DBNotificationItem).filter(DBNotificationItem.isRead == False).update({DBNotificationItem.isRead: True})
    db.commit()
    return {"status": "success", "message": "All unread alert payloads cleared successfully"}


# 8. Simulate Price Drop Job API Injection (Scraper callback trigger simulator)
@router.post("/deals/{item_id}/simulate-drop")
def inject_price_drop_simulation(item_id: int, price: float, db: Session = Depends(get_db)):
    item = db.query(DBTrackedItem).filter(DBTrackedItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Tracked item not found in repository")
    
    old_price = item.currentPrice
    item.currentPrice = price
    
    # Analyze and adjust benchmarks
    if price < item.historicalLow:
        item.historicalLow = price
        alert_type = "HISTORICAL_LOW"
    elif price <= item.targetPrice:
        alert_type = "TARGET_MET"
    else:
        alert_type = "EXCEPTIONAL_DEAL"
        
    db.commit()
    
    # Store historic point index
    now_ms = int(time.time() * 1000)
    history_point = DBPriceHistoryPoint(
        itemId=item.id,
        price=price,
        timestamp=now_ms
    )
    db.add(history_point)
    
    # Insert instant notify push payload
    notification = DBNotificationItem(
        itemId=item.id,
        itemName=item.name,
        platform=item.platform,
        oldPrice=old_price,
        newPrice=price,
        alertType=alert_type,
        timestamp=now_ms,
        isRead=False
    )
    db.add(notification)
    db.commit()
    
    return {"status": "success", "message": f"Injected backend-scraper deal job. Old: ₹{old_price} -> New: ₹{price}"}
