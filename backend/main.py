from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import yfinance as yf
import pandas as pd
import numpy as np
from typing import Optional
import uvicorn

app = FastAPI(title="Darvas Box API", version="1.0.0")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def calculate_darvas_box(df: pd.DataFrame, n_up: int = 3, n_down: int = 3, volume_multiplier: float = 1.2) -> dict:
    """
    Calculate Darvas Box using the proper algorithm:
    1. Identify candidate highs/lows
    2. Confirm them only after N consecutive bars fail to break them
    3. Generate signals based on breakouts with volume confirmation
    """
    if len(df) < max(n_up, n_down) + 20:  # Need enough data for confirmation + volume average
        raise ValueError("Not enough data for analysis")

    # Calculate 20-day average volume for comparison
    df['Volume_20MA'] = df['Volume'].rolling(window=20).mean()

    # Find confirmed box high
    box_high = find_confirmed_high(df, n_up)

    # Find confirmed box low
    box_low = find_confirmed_low(df, n_down)

    # Current price and volume
    current_price = df['Close'].iloc[-1]
    current_volume = df['Volume'].iloc[-1]
    current_volume_avg = df['Volume_20MA'].iloc[-1]

    # Generate trading signal
    signal = generate_signal(current_price, current_volume, current_volume_avg,
                           box_high, box_low, volume_multiplier)

    # Calculate price change
    previous_close = df['Close'].iloc[-2] if len(df) > 1 else current_price
    change = current_price - previous_close
    change_percent = (change / previous_close) * 100 if previous_close > 0 else 0

    return {
        "price": round(current_price, 2),
        "box_high": round(box_high, 2) if box_high else None,
        "box_low": round(box_low, 2) if box_low else None,
        "signal": signal,
        "volume": int(current_volume),
        "change": round(change, 2),
        "change_percent": round(change_percent, 2),
        "volume_avg_20": int(current_volume_avg) if current_volume_avg else None
    }

def find_confirmed_high(df: pd.DataFrame, n_up: int) -> Optional[float]:
    """
    Find the most recent confirmed box high.
    A high is confirmed when the next n_up bars all have highs lower than the candidate high.
    """
    highs = df['High'].values
    confirmed_high = None

    # Start from the end and work backwards, but leave room for confirmation
    for i in range(len(highs) - n_up - 1, n_up, -1):
        candidate_high = highs[i]

        # Check if this is a local peak
        if i > 0 and highs[i] <= highs[i-1]:
            continue

        # Check if the next n_up bars all have lower highs
        confirmed = True
        for j in range(i + 1, min(i + 1 + n_up, len(highs))):
            if highs[j] >= candidate_high:
                confirmed = False
                break

        if confirmed:
            confirmed_high = candidate_high
            break

    return confirmed_high

def find_confirmed_low(df: pd.DataFrame, n_down: int) -> Optional[float]:
    """
    Find the most recent confirmed box low.
    A low is confirmed when the next n_down bars all have lows higher than the candidate low.
    """
    lows = df['Low'].values
    confirmed_low = None

    # Start from the end and work backwards, but leave room for confirmation
    for i in range(len(lows) - n_down - 1, n_down, -1):
        candidate_low = lows[i]

        # Check if this is a local trough
        if i > 0 and lows[i] >= lows[i-1]:
            continue

        # Check if the next n_down bars all have higher lows
        confirmed = True
        for j in range(i + 1, min(i + 1 + n_down, len(lows))):
            if lows[j] <= candidate_low:
                confirmed = False
                break

        if confirmed:
            confirmed_low = candidate_low
            break

    return confirmed_low

def generate_signal(current_price: float, current_volume: float, volume_avg: float,
                   box_high: Optional[float], box_low: Optional[float],
                   volume_multiplier: float) -> str:
    """
    Generate trading signal based on Darvas Box rules:
    - BUY: Price closes above box high AND volume > average * multiplier
    - SELL: Price closes below box low
    - IGNORE: Price within box boundaries or insufficient volume for breakout
    """
    if box_high is None or box_low is None:
        return "IGNORE"

    # BUY Signal: Breakout above box high with volume confirmation
    if current_price > box_high:
        if volume_avg and current_volume > (volume_avg * volume_multiplier):
            return "BUY"
        else:
            return "IGNORE"  # Breakout without volume confirmation

    # SELL Signal: Breakdown below box low
    elif current_price < box_low:
        return "SELL"

    # IGNORE: Price within box boundaries
    else:
        return "IGNORE"

@app.get("/")
async def root():
    return {"message": "Darvas Box Trading API", "status": "active"}

@app.get("/api/v1/analyze")
async def analyze_stock(symbol: str):
    """
    Analyze a stock using Darvas Box strategy

    Args:
        symbol: Stock symbol (e.g., RELIANCE.NS, TCS.NS for Indian stocks)

    Returns:
        JSON with price, box_high, box_low, signal, volume, change, change_percent
    """
    try:
        # Download stock data
        ticker = yf.Ticker(symbol)

        # Get 60 days of data to have enough for analysis
        hist = ticker.history(period="60d")

        if hist.empty:
            raise HTTPException(status_code=404, detail=f"No data found for symbol: {symbol}")

        # Calculate Darvas Box analysis
        analysis = calculate_darvas_box(hist)
        analysis["symbol"] = symbol.upper()

        return analysis

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error analyzing stock: {str(e)}")

@app.get("/api/v1/health")
async def health_check():
    return {"status": "healthy", "service": "Darvas Box API"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
