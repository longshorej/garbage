# akka-ping-pong

A small program using Akka where two actors message themselves.

This is used to naively measure memory usage, and to play around with Graal.

## native-image

This application doesn't work with Graal's `native-image` tool. See rough notes below:

```bash
sbt assembly

cd target

native-image '-H:IncludeResources=^([^.]+\.conf)$' -jar scala-2.12/akka-ping-pong-assembly-0.1.0-SNAPSHOT.jar
```
