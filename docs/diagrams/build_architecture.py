#!/usr/bin/env python3
"""
Generate the platform architecture diagram as SVG.

Design principles:
  - Implemented components are visually distinct from planned ones. The
    diagram must not overstate what exists.
  - No third-party logos. Provider names appear as text, which states the
    integration target without implying a relationship that does not exist.
  - Consistent with the architecture decision records: no ML in the decision
    path (ADR-0002).
"""

import pathlib

W, H = 1720, 1210

# palette
NAVY   = "#1f3a68"
BLUE   = "#1971c2"
PURPLE = "#6741d9"
ORANGE = "#e8590c"
GREEN  = "#2f9e44"
RED    = "#c92a2a"
GREY   = "#adb5bd"
GREYTX = "#868e96"
INK    = "#212529"
BG     = "#ffffff"

FONT = "Segoe UI, Helvetica Neue, Helvetica, Arial, sans-serif"
MONO = "Consolas, Menlo, monospace"

out = []
A = out.append


def esc(t):
    return (t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def text(x, y, t, size=11, color=INK, weight="normal", anchor="start",
         font=FONT, opacity=1.0, style=""):
    A(f'<text x="{x}" y="{y}" font-family="{font}" font-size="{size}" '
      f'fill="{color}" font-weight="{weight}" text-anchor="{anchor}" '
      f'opacity="{opacity}" {style}>{esc(t)}</text>')


def rect(x, y, w, h, stroke, fill="none", rx=6, sw=1.4, dash=None, opacity=1.0):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    A(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" '
      f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{d} opacity="{opacity}"/>')


def panel(x, y, w, h, num, title, color):
    """Big numbered container."""
    rect(x, y, w, h, color, "none", rx=9, sw=1.8)
    A(f'<rect x="{x}" y="{y}" width="{w}" height="26" rx="9" fill="{color}"/>')
    A(f'<rect x="{x}" y="{y+16}" width="{w}" height="10" fill="{color}"/>')
    text(x + 11, y + 18, f"{num}.  {title}", 12, "#ffffff", "bold")


def card(x, y, w, h, title, lines, color, planned=False, badge=None):
    """Service / component box. Planned components are dashed and muted."""
    if planned:
        rect(x, y, w, h, GREY, "#ffffff", rx=5, sw=1.1, dash="4 3")
        tc, lc = GREYTX, GREYTX
    else:
        rect(x, y, w, h, color, "#ffffff", rx=5, sw=1.5)
        A(f'<rect x="{x}" y="{y}" width="3.5" height="{h}" rx="1.6" fill="{color}"/>')
        tc, lc = color, INK

    text(x + 10, y + 15, title, 10.2, tc, "bold")
    ly = y + 29
    for ln in lines:
        text(x + 10, ly, ln, 8.8, lc)
        ly += 11.5

    if badge:
        bw = 7 * len(badge) + 10
        A(f'<rect x="{x+w-bw-6}" y="{y+5}" width="{bw}" height="13" rx="6.5" '
          f'fill="{color}" opacity="0.13"/>')
        text(x + w - bw / 2 - 6, y + 14.5, badge, 7.6, color, "bold", "middle")


def arrow(x1, y1, x2, y2, color=INK, dash=None, sw=1.5, marker="arrow"):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    A(f'<path d="M {x1} {y1} L {x2} {y2}" stroke="{color}" stroke-width="{sw}" '
      f'fill="none"{d} marker-end="url(#{marker})"/>')


def curve(x1, y1, x2, y2, cx, cy, color=INK, dash=None, sw=1.5, marker="arrow"):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    A(f'<path d="M {x1} {y1} Q {cx} {cy} {x2} {y2}" stroke="{color}" '
      f'stroke-width="{sw}" fill="none"{d} marker-end="url(#{marker})"/>')


# ============================================================ header
A(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">')
A(f'<rect width="{W}" height="{H}" fill="{BG}"/>')
A('<defs>')
A(f'<marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" '
  f'markerHeight="6" orient="auto-start-reverse">'
  f'<path d="M 0 0 L 10 5 L 0 10 z" fill="{INK}"/></marker>')
A(f'<marker id="arrowg" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" '
  f'markerHeight="6" orient="auto-start-reverse">'
  f'<path d="M 0 0 L 10 5 L 0 10 z" fill="{GREY}"/></marker>')
A('</defs>')

# title
text(W / 2, 34, "PAYROLL-INTEGRATED REAL-TIME CREDIT INFRASTRUCTURE", 20, NAVY, "bold", "middle")
text(W / 2, 54, "Target architecture, with the current implementation shown against it", 11.5, GREYTX, "normal", "middle")
A(f'<line x1="{W/2-330}" y1="64" x2="{W/2+330}" y2="64" stroke="{NAVY}" stroke-width="1.2"/>')

# ============================================================ legend (top right)
LX, LY = 1418, 78
rect(LX, LY, 272, 106, NAVY, "#f8f9fa", rx=7, sw=1.3)
text(LX + 12, LY + 19, "LEGEND", 10.5, NAVY, "bold")

rect(LX + 12, LY + 28, 15, 12, BLUE, "#ffffff", rx=3, sw=1.5)
A(f'<rect x="{LX+12}" y="{LY+28}" width="3" height="12" rx="1.4" fill="{BLUE}"/>')
text(LX + 34, LY + 38, "Implemented and running in production", 8.8, INK)

rect(LX + 12, LY + 46, 15, 12, GREY, "#ffffff", rx=3, sw=1.1, dash="4 3")
text(LX + 34, LY + 56, "Planned — see docs/ROADMAP.md", 8.8, GREYTX)

A(f'<line x1="{LX+12}" y1="{LY+70}" x2="{LX+27}" y2="{LY+70}" stroke="{INK}" '
  f'stroke-width="1.5" marker-end="url(#arrow)"/>')
text(LX + 34, LY + 73, "Synchronous / HTTP", 8.8, INK)

A(f'<line x1="{LX+12}" y1="{LY+87}" x2="{LX+27}" y2="{LY+87}" stroke="{INK}" '
  f'stroke-width="1.5" stroke-dasharray="4 3" marker-end="url(#arrow)"/>')
text(LX + 34, LY + 90, "Asynchronous / Kafka event", 8.8, INK)

# ============================================================ 1. data sources
panel(28, 78, 236, 400, 1, "DATA SOURCES", NAVY)
card(40, 116, 212, 56, "PAYROLL PROVIDERS",
     ["ADP · Workday · Paychex", "Gusto · Rippling"], BLUE, planned=False, badge="CONTRACT")
card(40, 180, 212, 50, "EMPLOYERS / HR SYSTEMS",
     ["REST · SOAP · SFTP · Webhooks"], BLUE, planned=True)
card(40, 238, 212, 50, "OPEN BANKING",
     ["Plaid · MX"], BLUE, planned=True)
card(40, 296, 212, 56, "CREDIT BUREAUS",
     ["Experian · Equifax", "TransUnion · FICO"], BLUE, planned=True, badge="STUB")
card(40, 360, 212, 62, "THIRD-PARTY DATA",
     ["Fraud · device intelligence", "identity · AML / KYC"], BLUE, planned=True)
text(40, 442, "Only the payroll event contract is", 8, GREYTX)
text(40, 453, "implemented. The bureau lookup is a", 8, GREYTX)
text(40, 464, "deterministic in-process stub.", 8, GREYTX)

# ============================================================ 2. ingestion
panel(280, 78, 190, 400, 2, "INGESTION", PURPLE)
card(292, 116, 166, 62, "REST ENDPOINT", ["POST /v1/payroll/events", "Java · Spring Boot 3.2"],
     PURPLE, badge="LIVE")
card(292, 188, 166, 46, "CONNECTOR ADAPTERS", ["Per-provider normalisation"], PURPLE, planned=True)
card(292, 244, 166, 46, "FILE / SFTP PROCESSOR", ["Batch feeds"], PURPLE, planned=True)
card(292, 300, 166, 46, "WEBHOOK RECEIVER", ["Push notifications"], PURPLE, planned=True)
card(292, 356, 166, 46, "API GATEWAY", ["Kong / Apigee"], PURPLE, planned=True)

# ============================================================ 3. event streaming
panel(488, 78, 480, 92, 3, "EVENT STREAMING", PURPLE)
card(500, 116, 210, 44, "APACHE KAFKA", ["payroll.events  ·  income.verified"], PURPLE, badge="LIVE")
card(722, 116, 110, 44, "ZOOKEEPER", ["Coordination"], PURPLE, badge="LIVE")
card(844, 116, 112, 44, "SCHEMA REGISTRY", ["Avro / JSON"], PURPLE, planned=True)

# ============================================================ 4. microservices
panel(488, 186, 480, 292, 4, "MICROSERVICES", BLUE)

card(500, 224, 222, 74, "income-verification-service",
     ["Confidence score from coefficient", "of variation, 12-event window",
      "Java · PostgreSQL"], BLUE, badge="LIVE")
card(734, 224, 222, 74, "decision-service",
     ["Deterministic rules engine",
      "Human-readable reasoning",
      "Java · PostgreSQL"], BLUE, badge="LIVE")
card(500, 308, 222, 74, "credit-profile-service",
     ["Thin-file classification",
      "Bureau aggregation (stub)",
      "C# .NET 8 · MongoDB"], BLUE, badge="LIVE")

card(734, 308, 108, 36, "identity", ["OAuth / OIDC"], BLUE, planned=True)
card(848, 308, 108, 36, "employment", ["Tenure"], BLUE, planned=True)
card(734, 350, 108, 32, "affordability", ["DTI"], BLUE, planned=True)
card(848, 350, 108, 32, "aggregation", ["Cash flow"], BLUE, planned=True)
card(500, 392, 222, 32, "notification", ["Email · SMS · push"], BLUE, planned=True)
card(734, 392, 222, 32, "audit export", ["Warehouse feed"], BLUE, planned=True)

# ADR-0002 note
rect(500, 432, 456, 34, RED, "#fff5f5", rx=5, sw=1.2)
text(512, 447, "No machine learning in the decision path.", 9, RED, "bold")
text(512, 459, "Deliberate — see docs/adr/0002-deterministic-rules-engine-over-machine-learning.md", 8.2, RED)

# ============================================================ 5. api gateway
panel(986, 186, 168, 200, 5, "EDGE", PURPLE)
card(996, 224, 148, 62, "Caddy", ["TLS termination", "Subdomain routing", "Automatic HTTPS"],
     PURPLE, badge="LIVE")
card(996, 296, 148, 78, "API GATEWAY",
     ["Rate limiting", "Authentication", "Throttling", "Monitoring"], PURPLE, planned=True)

# ============================================================ 6. consumers
panel(1172, 186, 236, 200, 6, "CONSUMERS", GREEN)
card(1184, 224, 212, 46, "LENDERS / BANKS", ["Credit unions · regional banks"], GREEN, planned=True)
card(1184, 278, 212, 46, "FINTECHS", ["Personal loans · BNPL · EWA"], GREEN, planned=True)
card(1184, 332, 212, 42, "INTERNAL TOOLS", ["Underwriter portal · dashboards"], GREEN, planned=True)

# public endpoints box
rect(1172, 398, 236, 80, GREEN, "#ebfbee", rx=7, sw=1.4)
text(1184, 416, "LIVE PUBLIC ENDPOINTS", 9.5, GREEN, "bold")
for i, sub in enumerate(["payroll-credit.com  (service index)",
                         "ingestion.payroll-credit.com",
                         "income.payroll-credit.com",
                         "decision.payroll-credit.com",
                         "creditprofile.payroll-credit.com"]):
    text(1184, 430 + i * 10.5, sub, 7.6, INK, font=MONO)

# ============================================================ 7. data layer
panel(280, 500, 1128, 116, 7, "DATA LAYER", ORANGE)
card(292, 538, 210, 66, "PostgreSQL 16",
     ["incomeverification", "decisions (append-only audit)"], ORANGE, badge="LIVE")
card(514, 538, 210, 66, "MongoDB 7",
     ["creditprofile", "Aggregate documents"], ORANGE, badge="LIVE")
card(736, 538, 154, 66, "SQL Server", ["Operational store"], ORANGE, planned=True)
card(902, 538, 154, 66, "DynamoDB", ["NoSQL store"], ORANGE, planned=True)
card(1068, 538, 154, 66, "Snowflake / Redshift", ["Warehouse"], ORANGE, planned=True)
card(1234, 538, 162, 66, "Redis · S3", ["Cache · data lake"], ORANGE, planned=True)

# ============================================================ 8. cross-cutting
panel(280, 632, 552, 108, 8, "CROSS-CUTTING", NAVY)
card(292, 670, 160, 58, "CONFIGURATION",
     ["application.yml", "appsettings.json"], NAVY, badge="LIVE")
card(464, 670, 160, 58, "LOGGING & HEALTH",
     ["SLF4J · Actuator", "/health on all services"], NAVY, badge="LIVE")
card(636, 670, 184, 58, "OBSERVABILITY STACK",
     ["Prometheus · Jaeger · ELK", "CI/CD pipeline"], NAVY, planned=True)

# ============================================================ 9. security
panel(852, 632, 556, 108, 9, "SECURITY & COMPLIANCE", RED)
card(864, 670, 128, 58, "TRANSPORT", ["HTTPS, all endpoints", "Let's Encrypt"], RED, badge="LIVE")
card(1004, 670, 128, 58, "AUDIT INTEGRITY", ["Append-only decisions", "Frozen input signals"], RED, badge="LIVE")
card(1144, 670, 128, 58, "ENCRYPTION AT REST", ["Volumes · secrets mgr"], RED, planned=True)
card(1284, 670, 112, 58, "IAM / RBAC", ["WAF · DDoS"], RED, planned=True)

text(864, 748, "Design-time alignment: Basel II model reproducibility · Regulation B adverse action reasoning · CCPA · GLBA · SOC 2 · PCI-DSS", 8.2, GREYTX)

# ============================================================ flow strip
FY = 776
rect(28, FY, 1380, 96, NAVY, "#f8f9fa", rx=8, sw=1.4)
text(44, FY + 21, "END-TO-END FLOW — what actually runs today", 11, NAVY, "bold")

steps = [
    ("1", "Payroll event", "POST over HTTPS"),
    ("2", "Kafka", "payroll.events"),
    ("3", "Income score", "CV over 12 events"),
    ("4", "Kafka", "income.verified"),
    ("5", "Decision", "rules + reasoning"),
    ("6", "Credit profile", "C# consumer"),
    ("7", "Persisted", "Postgres + Mongo"),
]
sx, sw_, gap = 52, 168, 20
for i, (n, t, sub) in enumerate(steps):
    x = sx + i * (sw_ + gap)
    rect(x, FY + 34, sw_, 46, BLUE, "#ffffff", rx=5, sw=1.3)
    A(f'<circle cx="{x+16}" cy="{FY+50}" r="9" fill="{BLUE}"/>')
    text(x + 16, FY + 53.5, n, 9, "#ffffff", "bold", "middle")
    text(x + 31, FY + 51, t, 9.2, INK, "bold")
    text(x + 31, FY + 64, sub, 8, GREYTX)
    if i < len(steps) - 1:
        arrow(x + sw_ + 3, FY + 57, x + sw_ + gap - 4, FY + 57, INK, sw=1.3)

# ============================================================ scope statement
SY = 890
rect(28, SY, 1380, 112, ORANGE, "#fff9db", rx=8, sw=1.4)
text(44, SY + 22, "SCOPE — implemented versus planned", 11, ORANGE, "bold")

text(44, SY + 44, "Implemented", 9.6, INK, "bold")
for i, ln in enumerate([
        "Four microservices in two languages, communicating only via Kafka",
        "PostgreSQL and MongoDB, each owned by the service that writes it",
        "Deployed on AWS EC2, four HTTPS subdomains, publicly reachable",
        "Unit tested; reproducible from source with docker compose up",
]):
    text(44, SY + 60 + i * 12.5, "·  " + ln, 8.6, INK)

text(720, SY + 44, "Not yet built", 9.6, GREYTX, "bold")
for i, ln in enumerate([
        "Identity, employment, affordability and aggregation services",
        "Real HTTP adapter to credit bureaus — currently a deterministic stub",
        "External API gateway, schema registry, warehouse, cache, data lake",
        "Encryption at rest, IAM/RBAC, full observability stack, CI/CD",
]):
    text(720, SY + 60 + i * 12.5, "·  " + ln, 8.6, GREYTX)

text(44, SY + 104, "These are omissions of scope, not of capability: the same patterns implemented in the four running services extend directly to the remainder.", 8.4, GREYTX)

# ============================================================ footer
text(28, H - 26, "Clayton Soares da Mota  ·  github.com/claytonmota/payroll-credit-mvp  ·  Apache 2.0", 9, GREYTX)
text(W - 28, H - 26, "v5  ·  July 2026  ·  see docs/System_Architecture_Document.pdf", 9, GREYTX, anchor="end")

# ============================================================ connectors
arrow(264, 150, 288, 150, INK)                      # sources -> ingestion
arrow(458, 140, 496, 132, INK, dash="4 3")          # ingestion -> kafka
arrow(604, 162, 604, 220, INK, dash="4 3")          # kafka -> income
curve(722, 250, 734, 250, 728, 240, INK, dash="4 3")  # income -> decision
arrow(604, 300, 604, 306, INK, dash="4 3")          # income -> profile
arrow(960, 250, 992, 250, INK)                      # services -> edge
arrow(1148, 250, 1180, 250, INK)                    # edge -> consumers
arrow(604, 428, 604, 534, INK, dash="4 3", sw=1.2)  # services -> data (via left)
arrow(840, 470, 840, 534, INK, dash="4 3", sw=1.2)

A('</svg>')

svg = "\n".join(out)
p = pathlib.Path("/home/claude/arch/architecture-v5.svg")
p.parent.mkdir(parents=True, exist_ok=True)
p.write_text(svg, encoding="utf-8")
print(f"SVG: {p}  ({len(svg)} bytes, {len(out)} elementos)")
