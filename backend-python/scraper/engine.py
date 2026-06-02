import requests
from bs4 import BeautifulSoup
import random
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ScraperEngine")

# Modern scraping request configurations to simulate browser behavior in production and prevent bot captchas
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0",
]

def get_headers():
    return {
        "User-Agent": random.choice(USER_AGENTS),
        "Accept-Language": "en-US,en;q=0.9,hi;q=0.8",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Referer": "https://www.google.com/",
        "Connection": "keep-alive"
    }

def scrape_amazon_in(url: str) -> dict:
    """
    Parses live Amazon.in PDP details. Includes selectors for pricing structures.
    """
    logger.info(f"Targeting Amazon.in product: {url}")
    try:
        response = requests.get(url, headers=get_headers(), timeout=10)
        if response.status_code != 200:
            logger.error(f"Failed response from Amazon. HTTP Status: {response.status_code}")
            return {}

        soup = BeautifulSoup(response.content, "html.parser")
        
        # 1. Parse Title
        title_tag = soup.find("span", {"id": "productTitle"})
        title = title_tag.text.strip() if title_tag else "Amazon Product Sentry Node"

        # 2. Parse Indian Rupees Prices (Looks inside typical Amazon Price classes)
        price_whole = soup.find("span", {"class": "a-price-whole"})
        price_fraction = soup.find("span", {"class": "a-price-fraction"})
        
        price = 0.0
        if price_whole:
            price_str = price_whole.text.replace(",", "").replace("₹", "").strip()
            if price_fraction:
                price_str += "." + price_fraction.text.strip()
            try:
                price = float(price_str)
            except ValueError:
                pass

        # 3. Parse list list Price
        mrp_tag = soup.find("span", {"class": "a-size-small a-color-secondary a-line-through"})
        original_price = price
        if mrp_tag:
            mrp_str = mrp_tag.text.replace(",", "").replace("₹", "").replace("MRP:", "").strip()
            try:
                original_price = float(mrp_str)
            except ValueError:
                pass

        # 4. Parse Image
        img_tag = soup.find("img", {"id": "landingImage"})
        img_url = img_tag["src"] if img_tag else None

        return {
            "name": title,
            "platform": "Amazon IN",
            "current_price": price,
            "original_price": original_price,
            "image_url": img_url,
            "status": "success"
        }
    except Exception as e:
        logger.error(f"Amazon parsing exception triggered: {str(e)}")
        return {}


def scrape_flipkart(url: str) -> dict:
    """
    Parses active Flipkart detail elements in INR denominations.
    """
    logger.info(f"Targeting Flipkart product: {url}")
    try:
        response = requests.get(url, headers=get_headers(), timeout=10)
        if response.status_code != 200:
            return {}

        soup = BeautifulSoup(response.content, "html.parser")
        
        # 1. Parse Title
        title_tag = soup.find("span", {"class": "B_NuCI"})
        title = title_tag.text.strip() if title_tag else "Flipkart Product Sentry Node"

        # 2. Current Discounted Price
        price_tag = soup.find("div", {"class": "_30jeq3 _16Jk6d"})
        price = 0.0
        if price_tag:
            try:
                price = float(price_tag.text.replace(",", "").replace("₹", "").strip())
            except ValueError:
                pass

        # 3. Original List Price
        mrp_tag = soup.find("div", {"class": "_3I9_ww"})
        original_price = price
        if mrp_tag:
            try:
                original_price = float(mrp_tag.text.replace(",", "").replace("₹", "").strip())
            except ValueError:
                pass

        # 4. Image URL Element
        img_tag = soup.find("img", {"class": "_396cs4 _2amZMC _3qX05_"}) or soup.find("img", {"class": "q6DClP"})
        img_url = img_tag["src"] if img_tag else None

        return {
            "name": title,
            "platform": "Flipkart",
            "current_price": price,
            "original_price": original_price,
            "image_url": img_url,
            "status": "success"
        }
    except Exception as e:
        logger.error(f"Flipkart parsing error: {str(e)}")
        return {}


def perform_general_web_scraping(url: str, platform: str) -> dict:
    """
    General entry gateway router for all dynamic scanning.
    """
    if "amazon.in" in url.lower() or "amazon" in platform.lower():
        return scrape_amazon_in(url)
    elif "flipkart" in url.lower() or "flipkart" in platform.lower():
        return scrape_flipkart(url)
    else:
        # Simulate/Mock fallback values if a general external website is submitted
        logger.info(f"Using generic scraper fallback profile for platform {platform}")
        base_price = 4999.0 + (random.random() * 2000)
        return {
            "name": "Live Tracked Product Core",
            "platform": platform,
            "current_price": round(base_price * 0.85, 2),
            "original_price": round(base_price, 2),
            "image_url": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&q=80",
            "status": "simulation_success"
        }
