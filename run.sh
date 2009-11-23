#!/bin/bash

if [ $(uname) = "Linux" ]; then
    export MAPLE=$(ls -dr /usr/local/maple* | head -1)
    export LD_LIBRARY_PATH=$(ls -d $MAPLE/bin.* | head -1)
else
    export MAPLE=/Library/Frameworks/Maple.framework/Versions/Current
    export DYLD_LIBRARY_PATH=$MAPLE/bin.APPLE_UNIVERSAL_OSX/
fi
export CYCLEBASE=$(dirname $0)

java -classpath "$MAPLE/java/externalcall.jar:$MAPLE/java/jopenmaple.jar:$CYCLEBASE/dist/CyclePainter.jar" cyclepainter.Main $@
