import asyncio
import json
import signal
import sys
import time

import websockets


WS_URL = "ws://localhost:8080/market/ws"
JWT_TOKEN = "PUT_YOUR_JWT_TOKEN_HERE"
TOPICS = ["quote:1", "quote:2"]
REQUEST_ID_PREFIX = "req-"
PING_PONG_ENABLED = True
RECONNECT = False
RECONNECT_DELAY_SECONDS = 3


def build_auth_message():
  return {"type": "auth", "token": JWT_TOKEN}


def build_subscribe_message(request_id, topics):
  return {"type": "subscribe", "request_id": request_id, "topics": topics}


def build_unsubscribe_message(request_id, topics):
  return {"type": "unsubscribe", "request_id": request_id, "topics": topics}


def build_pong_message():
  return {"type": "pong"}


def is_ping_message(payload):
  return payload.get("type") == "ping"


def is_event_message(payload):
  return payload.get("type") == "event"


def is_error_message(payload):
  return payload.get("type") == "error"


def now_millis():
  return int(time.time() * 1000)


async def send_json(websocket, payload):
  await websocket.send(json.dumps(payload))


async def handle_messages(websocket, stop_event):
  async for message in websocket:
    try:
      payload = json.loads(message)
    except json.JSONDecodeError:
      print("[warn] invalid json received")
      continue

    if is_ping_message(payload):
      if PING_PONG_ENABLED:
        await send_json(websocket, build_pong_message())
      continue

    if is_event_message(payload):
      data = payload.get("data", {})
      topic = payload.get("topic")
      ts = payload.get("ts")
      price = data.get("price")
      print(f"[event] topic={topic} ts={ts} price={price} data={data}")
      continue

    if is_error_message(payload):
      code = payload.get("code")
      message_text = payload.get("message")
      request_id = payload.get("request_id")
      print(f"[error] request_id={request_id} code={code} message={message_text}")
      continue

    print(f"[message] {payload}")

  stop_event.set()


async def run_client(stop_event):
  request_id = f"{REQUEST_ID_PREFIX}{now_millis()}"

  async with websockets.connect(WS_URL) as websocket:
    await send_json(websocket, build_auth_message())
    await send_json(websocket, build_subscribe_message(request_id, TOPICS))

    receiver_task = asyncio.create_task(handle_messages(websocket, stop_event))
    await stop_event.wait()

    if TOPICS:
      await send_json(websocket, build_unsubscribe_message(request_id, TOPICS))

    receiver_task.cancel()
    try:
      await receiver_task
    except asyncio.CancelledError:
      pass


async def main():
  stop_event = asyncio.Event()

  for sig in (signal.SIGINT, signal.SIGTERM):
    signal.signal(sig, lambda *_: stop_event.set())

  while True:
    try:
      await run_client(stop_event)
      break
    except Exception as ex:
      print(f"[warn] connection error: {ex}")
      if not RECONNECT:
        break
      await asyncio.sleep(RECONNECT_DELAY_SECONDS)


if __name__ == "__main__":
  if WS_URL.startswith("ws") is False:
    print("[error] WS_URL must start with ws:// or wss://")
    sys.exit(1)

  if JWT_TOKEN == "PUT_YOUR_JWT_TOKEN_HERE":
    print("[error] JWT_TOKEN is not set")
    sys.exit(1)

  asyncio.run(main())
