from pydantic import BaseModel, HttpUrl
from typing import Optional, List
from datetime import datetime

# --- Pydantic Schemas for API Serialization & Validation ---

class PriceHistoryPointSchema(BaseModel):
    id: int
    itemId: int
    price: float
    timestamp: datetime

    class Config:
        from_attributes = True


class TrackedItemBase(BaseModel):
    name: str
    platform: str
    currentPrice: float
    originalPrice: float
    targetPrice: float
    productUrl: str
    imageUrl: Optional[str] = None
    category: str


class TrackedItemCreate(TrackedItemBase):
    pass


class TrackedItemUpdateTarget(BaseModel):
    targetPrice: float


class TrackedItemResponse(TrackedItemBase):
    id: int
    historicalLow: float
    historicalHigh: float
    isWatched: bool
    rating: float
    addedAt: int

    class Config:
        from_attributes = True


class NotificationResponse(BaseModel):
    id: int
    itemId: int
    itemName: str
    platform: str
    oldPrice: float
    newPrice: float
    alertType: str # e.g. "HISTORICAL_LOW", "EXCEPTIONAL_DEAL", "TARGET_MET"
    timestamp: int
    isRead: bool

    class Config:
        from_attributes = True
