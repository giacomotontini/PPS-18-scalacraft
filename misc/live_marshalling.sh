#!/bin/bash

FILTER=""
if [ "$1" == "serverbound" ]; then
    FILTER="dst"
else
    FILTER="src"
fi

sudo tcpdump -i wlp2s0 -w - $FILTER port 25566 | tcpflow -r - -B -C -0 | \
 java -cp target/scala-2.12/scalacraft-0.1-SNAPSHOT.jar io.scalacraft.misc.LivePacketsMarshalling $1 $2
