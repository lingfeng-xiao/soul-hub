# Sandbox Setup Instructions

## 1. Install Docker Desktop

Download and install from:
https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe

After installation:
- Restart computer if prompted
- Start Docker Desktop
- Wait until the status shows "Docker Desktop is running"

## 2. Build the Sandbox Image

Open PowerShell and run:

```powershell
cd $env:USERPROFILE\.openclaw\sandbox
docker build -t openclaw-sandbox:bookworm-slim -f Dockerfile.sandbox .
```

## 3. Verify the Image

```powershell
docker images | findstr openclaw-sandbox
```

## 4. Restart OpenClaw Gateway

```powershell
openclaw gateway restart
```

## 5. Verify Sandbox is Working

```powershell
openclaw sandbox list
openclaw sandbox explain --agent agent_admin
```

## Optional: Build Enhanced Image with More Tools

If you need Python, Node.js, and other common tools:

```dockerfile
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    git \
    jq \
    procps \
    python3 \
    python3-pip \
    nodejs \
    npm \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /workspace && chmod 777 /workspace
RUN useradd -m -u 1000 -s /bin/bash sandbox

WORKDIR /workspace
USER sandbox

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
```

Build with:
```powershell
docker build -t openclaw-sandbox-common:bookworm-slim -f Dockerfile.common .
```

Then update config:
```json
{
  "agents": {
    "list": [{
      "id": "agent_admin",
      "sandbox": {
        "docker": {
          "image": "openclaw-sandbox-common:bookworm-slim"
        }
      }
    }]
  }
}
```
