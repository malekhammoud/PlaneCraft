#!/bin/bash

echo "Starting MinMod launcher with offline mode..."

# Create essential directories
GAME_DIR="$HOME/.minecraft"
mkdir -p "$GAME_DIR/mods"

# Stop any running gradle daemons
./gradlew --stop

# Build the mod
./gradlew build

# Copy our mod to the mods folder
cp build/libs/minmod-1.0.0.jar "$GAME_DIR/mods/"

# Use the fabric-installer to setup offline mode
if [ ! -f fabric-installer.jar ]; then
    echo "Downloading Fabric installer..."
    wget -O fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/0.11.2/fabric-installer-0.11.2.jar
fi

# Run the fabric installer in client mode (only if not already installed)
if [ ! -d "$GAME_DIR/libraries/net/fabricmc" ]; then
    echo "Installing Fabric client..."
    java -jar fabric-installer.jar client -dir "$GAME_DIR" -mcversion 1.20.2 -loader 0.14.23
fi

# Run Minecraft directly with the Fabric launcher
echo "Launching Minecraft in offline mode..."
java -Xmx2G \
     -Dfabric.skipAuth=true \
     -Dfabric.skipAuthLib=true \
     -Dfabric.development=true \
     -Dminecraft.client.jar="$HOME/.gradle/caches/fabric-loom/minecraft-client-1.20.2-merged-named.jar" \
     -DignoreProfileCredentials=true \
     -DuserProperties.file=/dev/null \
     -jar "$GAME_DIR/libraries/net/fabricmc/fabric-loader/0.14.23/fabric-loader-0.14.23-launch.jar" \
     --username DevUser \
     --accessToken 0 \
     --gameDir "$GAME_DIR" \
     --version "fabric-loader-0.14.23-1.20.2" \
     --assetIndex 5 \
     --assetsDir "$HOME/.gradle/caches/fabric-loom/assets"
