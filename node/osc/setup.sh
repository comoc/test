#!/bin/bash
npm install
npm install websocket
npm install osc4node
npm install jspack
patch -p0 < datatypes.js.diff
