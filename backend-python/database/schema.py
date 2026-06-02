from sqlalchemy import Column, Integer, String, Float, Boolean, BigInteger, ForeignKey
from sqlalchemy.orm import relationship
from .db_config import Base

# SQLAlchemy Database Models matching the SQLite/Room structure

class DBTrackedItem(Base):
    __tablename__ = "tracked_items"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    platform = Column(String, nullable=False)
    currentPrice = Column(Float, nullable=False)
    originalPrice = Column(Float, nullable=False)
    historicalLow = Column(Float, nullable=False)
    historicalHigh = Column(Float, nullable=False)
    imageUrl = Column(String, nullable=True)
    productUrl = Column(String, nullable=False)
    targetPrice = Column(Float, nullable=False)
    isWatched = Column(Boolean, default=True)
    category = Column(String, nullable=False)
    rating = Column(Float, default=4.0)
    addedAt = Column(BigInteger, nullable=False)

    history = relationship("DBPriceHistoryPoint", back_populates="item", cascade="all, delete-orphan")


class DBPriceHistoryPoint(Base):
    __tablename__ = "price_history"

    id = Column(Integer, primary_key=True, index=True)
    itemId = Column(Integer, ForeignKey("tracked_items.id", ondelete="CASCADE"), nullable=False)
    price = Column(Float, nullable=False)
    timestamp = Column(BigInteger, nullable=False)

    item = relationship("DBTrackedItem", back_populates="history")


class DBNotificationItem(Base):
    __tablename__ = "notifications"

    id = Column(Integer, primary_key=True, index=True)
    itemId = Column(Integer, nullable=False)
    itemName = Column(String, nullable=False)
    platform = Column(String, nullable=False)
    oldPrice = Column(Float, nullable=False)
    newPrice = Column(Float, nullable=False)
    alertType = Column(String, nullable=False) # "HISTORICAL_LOW", "TARGET_MET", "EXCEPTIONAL_DEAL"
    timestamp = Column(BigInteger, nullable=False)
    isRead = Column(Boolean, default=False)
