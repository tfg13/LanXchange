#!/usr/bin/env bash
# this script extracts the main jar from releases/stable/lxc.zip
# in order to allow development of the cli version from IDEA

if [ ! -f "releases/stable/lxc.zip" ]; then
    echo "build probably failed, aborting"
    exit 1
fi

unzip -o releases/stable/lxc.zip lanxchange.jar
