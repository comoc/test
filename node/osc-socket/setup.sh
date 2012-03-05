#!/bin/bash
npm install
patch -p0 < datatypes.js.diff

#javac StdinToRobot.java
