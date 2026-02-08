#!/bin/bash

# =============================================================================
# ShieldDNS - Install Script for Manual Testing
# =============================================================================
# This script builds and installs ShieldDNS on a connected Android device.
# 
# Prerequisites:
#   - Android device connected via USB with USB debugging enabled
#   - ADB installed and in PATH
#
# Usage:
#   ./install.sh         # Build and install debug APK
#   ./install.sh --clean # Clean build before install
# =============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project configuration
PACKAGE_NAME="com.shielddns.app"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Print colored message
print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}!${NC} $1"
}

# Check if ADB is available
check_adb() {
    print_step "Checking ADB..."
    if ! command -v adb &> /dev/null; then
        print_error "ADB not found. Please install Android SDK Platform Tools."
        exit 1
    fi
    print_success "ADB found"
}

# Check for connected device
check_device() {
    print_step "Checking for connected devices..."
    
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep -v "^$" | wc -l | tr -d ' ')
    
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        print_error "No Android device connected."
        echo "    Please connect your device and enable USB debugging."
        exit 1
    fi
    
    DEVICE_NAME=$(adb devices -l | grep -v "List" | grep -v "^$" | head -1)
    print_success "Device found: $DEVICE_NAME"
}

# Build the app
build_app() {
    local clean_build=$1
    
    if [ "$clean_build" = true ]; then
        print_step "Cleaning previous build..."
        ./gradlew clean
        print_success "Clean complete"
    fi
    
    print_step "Building debug APK..."
    ./gradlew assembleDebug
    
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_success "Build complete: $APK_PATH ($APK_SIZE)"
    else
        print_error "Build failed - APK not found"
        exit 1
    fi
}

# Uninstall existing app (if present)
uninstall_app() {
    print_step "Checking for existing installation..."
    
    if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        print_warning "Existing installation found. Uninstalling..."
        adb uninstall "$PACKAGE_NAME" > /dev/null 2>&1 || true
        print_success "Uninstalled previous version"
    else
        print_success "No existing installation"
    fi
}

# Install the app
install_app() {
    print_step "Installing APK..."
    adb install -r "$APK_PATH"
    print_success "Installation complete"
}

# Launch the app
launch_app() {
    print_step "Launching ShieldDNS..."
    adb shell am start -n "$PACKAGE_NAME/.presentation.MainActivity"
    print_success "App launched"
}

# Show app logs
show_logs() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}ShieldDNS installed successfully!${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "To view logs, run:"
    echo -e "  ${YELLOW}adb logcat -s AdBlockVpnService${NC}"
    echo ""
    echo "To view all app logs:"
    echo -e "  ${YELLOW}adb logcat | grep -i shielddns${NC}"
    echo ""
}

# Main execution
main() {
    local clean_build=false
    
    # Parse arguments
    if [ "$1" = "--clean" ] || [ "$1" = "-c" ]; then
        clean_build=true
    fi
    
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     ShieldDNS - Install for Testing       ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════╝${NC}"
    echo ""
    
    check_adb
    check_device
    build_app $clean_build
    uninstall_app
    install_app
    launch_app
    show_logs
}

# Run main function
main "$@"
