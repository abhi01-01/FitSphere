from __future__ import annotations

import math
import textwrap
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[3]
OUT_DIR = ROOT / "docs" / "diagrams"

FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"
FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"


def regular(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_REGULAR, size=size)


def bold(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_BOLD, size=size)


COLORS = {
    "bg": "#F4F7F5",
    "bg_alt": "#EEF3F1",
    "ink": "#14231C",
    "muted": "#5D6D65",
    "line": "#475569",
    "line_soft": "#8FA1B8",
    "panel_border": "#D6E0DA",
    "panel_fill": "#FFFFFF",
    "green_fill": "#ECF6F0",
    "green_border": "#B9D7C4",
    "blue_fill": "#EEF6FF",
    "blue_border": "#B7D5F4",
    "purple_fill": "#F5F1FF",
    "purple_border": "#D8C9F6",
    "orange_fill": "#FFF5EB",
    "orange_border": "#F2CFB0",
}


@dataclass
class Box:
    x: int
    y: int
    w: int
    h: int
    title: str
    body: list[str]
    tag: str | None = None
    fill: str = COLORS["panel_fill"]
    border: str = COLORS["panel_border"]

    @property
    def left(self) -> tuple[int, int]:
        return self.x, self.y + self.h // 2

    @property
    def right(self) -> tuple[int, int]:
        return self.x + self.w, self.y + self.h // 2

    @property
    def top(self) -> tuple[int, int]:
        return self.x + self.w // 2, self.y

    @property
    def bottom(self) -> tuple[int, int]:
        return self.x + self.w // 2, self.y + self.h


def make_canvas(width: int, height: int) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (width, height), COLORS["bg"])
    draw = ImageDraw.Draw(image)
    return image, draw


def draw_title(draw: ImageDraw.ImageDraw, title: str, subtitle: str) -> None:
    draw.text((90, 70), title, font=bold(42), fill=COLORS["ink"])
    draw.text((90, 130), subtitle, font=regular(20), fill=COLORS["muted"])


def wrapped_lines(text: str, font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        trial = word if not current else f"{current} {word}"
        width = font.getbbox(trial)[2] - font.getbbox(trial)[0]
        if width <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_panel(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, title: str) -> None:
    draw.rounded_rectangle((x, y, x + w, y + h), radius=30, fill=COLORS["panel_fill"], outline=COLORS["panel_border"], width=3)
    draw.text((x + 30, y + 28), title, font=bold(24), fill="#173C30")


def draw_box(draw: ImageDraw.ImageDraw, box: Box) -> None:
    draw.rounded_rectangle((box.x, box.y, box.x + box.w, box.y + box.h), radius=22, fill=box.fill, outline=box.border, width=3)
    tx = box.x + 28
    ty = box.y + 24
    if box.tag:
        draw.text((tx, ty), box.tag, font=bold(16), fill="#206A4B")
        ty += 34
    draw.text((tx, ty), box.title, font=bold(22), fill=COLORS["ink"])
    ty += 44
    body_font = regular(15)
    for paragraph in box.body:
        lines = wrapped_lines(paragraph, body_font, box.w - 56)
        for line in lines:
            draw.text((tx, ty), line, font=body_font, fill=COLORS["muted"])
            ty += 24
        ty += 6


def draw_arrow(
    draw: ImageDraw.ImageDraw,
    start: tuple[int, int],
    end: tuple[int, int],
    label: str | None = None,
    elbow: tuple[int, int] | None = None,
    dashed: bool = False,
) -> None:
    points = [start]
    if elbow:
        points.append((elbow[0], start[1]))
        points.append(elbow)
        points.append((end[0], elbow[1]))
    points.append(end)

    if dashed:
        for a, b in zip(points, points[1:]):
            draw_dashed_line(draw, a, b, fill=COLORS["line_soft"], width=4)
    else:
        draw.line(points, fill=COLORS["line"], width=4)
    draw_arrow_head(draw, points[-2], points[-1], fill=COLORS["line_soft"] if dashed else COLORS["line"])

    if label:
        lx = (start[0] + end[0]) // 2
        ly = min(start[1], end[1]) - 28 if not elbow else elbow[1] - 28
        draw.text((lx - 40, ly), label, font=bold(15), fill=COLORS["muted"])


def draw_dashed_line(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], fill: str, width: int) -> None:
    x1, y1 = start
    x2, y2 = end
    length = math.hypot(x2 - x1, y2 - y1)
    if length == 0:
        return
    dash = 16
    gap = 10
    dx = (x2 - x1) / length
    dy = (y2 - y1) / length
    dist = 0
    while dist < length:
      x_start = x1 + dx * dist
      y_start = y1 + dy * dist
      x_end = x1 + dx * min(dist + dash, length)
      y_end = y1 + dy * min(dist + dash, length)
      draw.line((x_start, y_start, x_end, y_end), fill=fill, width=width)
      dist += dash + gap


def draw_arrow_head(draw: ImageDraw.ImageDraw, prev: tuple[int, int], end: tuple[int, int], fill: str) -> None:
    angle = math.atan2(end[1] - prev[1], end[0] - prev[0])
    size = 15
    left = (
        end[0] - size * math.cos(angle - math.pi / 6),
        end[1] - size * math.sin(angle - math.pi / 6),
    )
    right = (
        end[0] - size * math.cos(angle + math.pi / 6),
        end[1] - size * math.sin(angle + math.pi / 6),
    )
    draw.polygon([end, left, right], fill=fill)


def draw_lane(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, title: str) -> None:
    draw.rounded_rectangle((x, y, x + w, y + h), radius=28, fill=COLORS["panel_fill"], outline=COLORS["panel_border"], width=3)
    draw.text((x + 34, y + 24), title, font=bold(24), fill="#173C30")


def architecture_overview() -> None:
    image, draw = make_canvas(2800, 1700)
    draw_title(
        draw,
        "FitSphere Architecture Overview",
        "A cleaner runtime map of the user-facing path, core platform services, and persisted state.",
    )

    draw_panel(draw, 70, 220, 520, 1410, "Experience and Identity")
    draw_panel(draw, 650, 220, 1260, 1410, "Core Runtime")
    draw_panel(draw, 1970, 220, 760, 1410, "Data and AI Dependencies")

    browser = Box(170, 330, 280, 140, "Browser User", ["Signs in, logs workouts, and reads coaching output."], fill=COLORS["orange_fill"], border=COLORS["orange_border"])
    frontend = Box(170, 560, 320, 180, "Frontend", ["OAuth PKCE client.", "Dashboard and activity flows.", "Calls only the gateway."], tag="REACT + VITE", fill=COLORS["green_fill"], border=COLORS["green_border"])
    keycloak = Box(150, 900, 320, 190, "Identity Provider", ["Hosts login, signup, OIDC logout, and token issuance."], tag="KEYCLOAK", fill=COLORS["purple_fill"], border=COLORS["purple_border"])

    configserver = Box(760, 330, 280, 160, "Config Server", ["Serves Spring configuration for each deployable service."], tag="SPRING CLOUD")
    gateway = Box(1130, 580, 320, 180, "API Gateway", ["Validates JWTs.", "Applies CORS and security filters.", "Routes external API traffic."], tag="SPRING CLOUD GATEWAY")
    eureka = Box(1540, 330, 250, 160, "Eureka", ["Gateway, user, activity, and AI services register here for discovery."], tag="NETFLIX OSS")
    userservice = Box(760, 860, 300, 200, "User Service", ["Registers app users.", "Maps Keycloak subjects.", "Persists user records."], tag="SPRING BOOT", fill=COLORS["blue_fill"], border=COLORS["blue_border"])
    activityservice = Box(1130, 1110, 320, 220, "Activity Service", ["Validates the user id.", "Stores workouts in MongoDB.", "Publishes workout events."], tag="SPRING BOOT", fill=COLORS["blue_fill"], border=COLORS["blue_border"])
    aiservice = Box(1500, 1110, 300, 220, "AI Service", ["Consumes workout events.", "Prompts Gemini.", "Stores recommendation documents."], tag="SPRING BOOT + GEMINI", fill=COLORS["orange_fill"], border=COLORS["orange_border"])
    broker = Box(1130, 1370, 300, 150, "Event Broker", ["RabbitMQ exchange and queue used for activity recommendation jobs."], tag="RABBITMQ")

    usersdb = Box(2040, 885, 240, 150, "Users DB", ["PostgreSQL system of record for application users."], tag="POSTGRESQL")
    activitydb = Box(2040, 1145, 240, 150, "Activity DB", ["MongoDB store for workout history and activity documents."], tag="MONGODB")
    aidb = Box(2330, 1145, 270, 150, "Recommendation DB", ["MongoDB store for generated recommendation payloads."], tag="MONGODB")
    gemini = Box(2330, 1380, 270, 150, "Gemini", ["External model API used to generate recommendation content."], tag="EXTERNAL API")

    for box in [browser, frontend, keycloak, configserver, gateway, eureka, userservice, activityservice, aiservice, broker, usersdb, activitydb, aidb, gemini]:
        draw_box(draw, box)

    draw_arrow(draw, browser.bottom, frontend.top)
    draw_arrow(draw, frontend.right, gateway.left)
    draw_arrow(draw, frontend.bottom, keycloak.top, "OIDC")
    draw_arrow(draw, gateway.top, configserver.bottom, "config", elbow=(1290, 500), dashed=True)
    draw_arrow(draw, gateway.top, eureka.bottom, "discovery", elbow=(1665, 500), dashed=True)
    draw_arrow(draw, gateway.bottom, userservice.top, elbow=(910, 790))
    draw_arrow(draw, gateway.bottom, activityservice.top)
    draw_arrow(draw, gateway.bottom, aiservice.top, elbow=(1650, 1040))
    draw_arrow(draw, userservice.right, usersdb.left)
    draw_arrow(draw, activityservice.right, activitydb.left, elbow=(1450, 1360))
    draw_arrow(draw, activityservice.bottom, broker.top, "publish event")
    draw_arrow(draw, broker.right, aiservice.bottom, "consume", elbow=(1650, 1445))
    draw_arrow(draw, aiservice.right, aidb.left)
    draw_arrow(draw, aiservice.right, gemini.left, "prompt model", elbow=(2200, 1455))

    draw.text((820, 1600), "Auth handshake, user sync, and activity recommendation internals are expanded in the flow diagrams below.", font=bold(18), fill=COLORS["muted"])

    image.save(OUT_DIR / "architecture-overview.png")


def activity_flow() -> None:
    image, draw = make_canvas(2800, 1500)
    draw_title(
        draw,
        "Activity to Recommendation Flow",
        "A low-level swimlane for the synchronous activity write path and the asynchronous AI recommendation path.",
    )

    lane_x = [90, 510, 930, 1350, 1770, 2190]
    lane_w = 340
    lane_h = 1180
    lane_y = 220
    titles = ["Frontend", "Gateway", "User Service", "Activity Service", "RabbitMQ", "AI Service + Gemini"]
    for x, title in zip(lane_x, titles):
        draw_lane(draw, x, lane_y, lane_w, lane_h, title)

    steps = [
        Box(130, 340, 260, 140, "1. POST /api/activities", ["Bearer token + workout payload submitted from the UI."], fill=COLORS["green_fill"], border=COLORS["green_border"]),
        Box(550, 340, 260, 170, "2. Sync identity", ["Gateway validates the JWT and derives the canonical user id from the token subject."], fill=COLORS["panel_fill"], border=COLORS["panel_border"]),
        Box(970, 340, 260, 170, "3. Validate user", ["User service confirms or creates the application user mapped to that Keycloak subject."], fill=COLORS["blue_fill"], border=COLORS["blue_border"]),
        Box(1390, 340, 260, 200, "4. Persist activity", ["Activity service validates the request, stores it in MongoDB, and returns the activity response immediately."], fill=COLORS["blue_fill"], border=COLORS["blue_border"]),
        Box(1810, 340, 260, 170, "5. Publish event", ["Activity service emits an event for async recommendation generation."], fill=COLORS["panel_fill"], border=COLORS["panel_border"]),
        Box(2230, 340, 260, 200, "6. Generate recommendation", ["AI service consumes the event, builds a Gemini prompt, and normalizes the output."], fill=COLORS["orange_fill"], border=COLORS["orange_border"]),
        Box(2230, 650, 260, 170, "7. Save result", ["The recommendation is persisted in the AI MongoDB store."], fill=COLORS["orange_fill"], border=COLORS["orange_border"]),
        Box(130, 980, 260, 160, "8. GET recommendation", ["The UI later requests recommendation details for a specific activity id."], fill=COLORS["green_fill"], border=COLORS["green_border"]),
        Box(550, 980, 260, 160, "9. Route request", ["Gateway resolves the AI service through Eureka and forwards the request."], fill=COLORS["panel_fill"], border=COLORS["panel_border"]),
        Box(2230, 980, 260, 170, "10. Return 200 or 404", ["The AI service returns 200 when ready or 404 if the recommendation is not stored yet."], fill=COLORS["orange_fill"], border=COLORS["orange_border"]),
    ]
    for step in steps:
        draw_box(draw, step)

    draw_arrow(draw, steps[0].right, steps[1].left)
    draw_arrow(draw, steps[1].right, steps[2].left)
    draw_arrow(draw, steps[2].right, steps[3].left)
    draw_arrow(draw, steps[3].right, steps[4].left)
    draw_arrow(draw, steps[4].right, steps[5].left)
    draw_arrow(draw, steps[5].bottom, steps[6].top, "persist")
    draw_arrow(draw, steps[7].right, steps[8].left)
    draw_arrow(draw, steps[8].right, steps[9].left)
    draw_arrow(draw, steps[9].left, steps[8].right, "response", dashed=True)
    draw_arrow(draw, steps[8].left, steps[7].right, "response", dashed=True)

    draw.text((1080, 1220), "Client response returns after step 4. Steps 5–7 continue asynchronously.", font=bold(18), fill=COLORS["muted"])

    image.save(OUT_DIR / "activity-recommendation-flow.png")


def auth_flow() -> None:
    image, draw = make_canvas(2400, 1500)
    draw_title(
        draw,
        "Authentication and User Sync Flow",
        "A low-level authentication view showing PKCE login, gateway user sync, and OIDC logout behavior.",
    )

    lane_x = [90, 520, 950, 1380, 1810]
    lane_w = 340
    lane_h = 1180
    lane_y = 220
    titles = ["Browser User", "Frontend", "Keycloak", "Gateway", "User Service"]
    for x, title in zip(lane_x, titles):
        draw_lane(draw, x, lane_y, lane_w, lane_h, title)

    steps = [
        Box(130, 340, 260, 150, "1. Open app", ["Unauthenticated user lands on the SPA."], fill=COLORS["orange_fill"], border=COLORS["orange_border"]),
        Box(560, 340, 260, 180, "2. Start PKCE login", ["Frontend redirects to Keycloak with a code challenge and prompt=login."], fill=COLORS["green_fill"], border=COLORS["green_border"]),
        Box(990, 340, 260, 200, "3. Authenticate", ["Login or signup happens in Keycloak, which returns the user to the redirect uri with an auth code."], fill=COLORS["purple_fill"], border=COLORS["purple_border"]),
        Box(560, 620, 260, 190, "4. Exchange code", ["The SPA exchanges the auth code for access, refresh, and id tokens."], fill=COLORS["green_fill"], border=COLORS["green_border"]),
        Box(1420, 340, 260, 180, "5. Receive business request", ["Gateway validates the JWT and extracts the effective user id from the token subject."], fill=COLORS["panel_fill"], border=COLORS["panel_border"]),
        Box(1850, 340, 260, 180, "6. Validate or register", ["User service confirms the subject exists or creates the application user record."], fill=COLORS["blue_fill"], border=COLORS["blue_border"]),
        Box(1420, 680, 260, 170, "7. Forward request", ["Gateway injects X-User-ID from JWT subject before calling downstream services."], fill=COLORS["panel_fill"], border=COLORS["panel_border"]),
        Box(560, 1040, 260, 150, "8. Logout", ["Frontend clears local auth state and starts OIDC logout."], fill=COLORS["green_fill"], border=COLORS["green_border"]),
        Box(990, 1040, 260, 150, "9. OIDC logout", ["Keycloak ends the SSO session and returns to the app."], fill=COLORS["purple_fill"], border=COLORS["purple_border"]),
    ]
    for step in steps:
        draw_box(draw, step)

    draw_arrow(draw, steps[0].right, steps[1].left)
    draw_arrow(draw, steps[1].right, steps[2].left)
    draw_arrow(draw, steps[2].bottom, steps[3].top, "redirect back")
    draw_arrow(draw, steps[3].right, steps[4].left, "Bearer JWT")
    draw_arrow(draw, steps[4].right, steps[5].left, "validate/sync")
    draw_arrow(draw, steps[5].left, steps[4].right, dashed=True)
    draw_arrow(draw, steps[4].bottom, steps[6].top, "route onward")
    draw_arrow(draw, steps[7].right, steps[8].left, "logout endpoint")

    draw.text((1320, 1280), "Only the gateway decides the effective downstream user id.", font=bold(18), fill=COLORS["muted"])

    image.save(OUT_DIR / "auth-and-user-sync-flow.png")


if __name__ == "__main__":
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    architecture_overview()
    activity_flow()
    auth_flow()
