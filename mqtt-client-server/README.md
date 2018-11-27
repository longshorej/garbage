# mqtt-client-server

## Overview

This highlights a bug when multiple clients and the server are publishing at the same time.

There's a server program which `PUBACK`s any `PUBLISH`es it receives. When the server receives a `SUBSCRIBE`, it sends a `SUBACK` and starts publishing data to that topic.

There's a client program which connects and subscribes to a random topic. It also sends `PUBLISH`es to the server, waiting for `PUBACK`s before continuing.

## Setup:

Ensure that Alpakka PR has been published locally, and if necessary, the build.sbt version changed accordingly.

## Reproduce:

1) Open three terminals, running `sbt` in each.

2) In terminal A's sbt console, run `server/reStart`

3) In terminal B's sbt console, run `client/reStart`

4) Note that both programs are indicating progress as they print events received and commands sent to stdout

5) In terminal C's sbt console, run `client/reStart`.

## Observe:

Executing step 5 will quickly cause the system to stop making progress, within 10 seconds or so.

## Expectation:

Both clients and the server are able to publish and make progress simultaneously.