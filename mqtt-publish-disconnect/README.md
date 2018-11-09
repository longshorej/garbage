# mqtt-publish-disconnect

This uses the Alpakka mqtt-streaming connector to test its resiliency when publishing from the client given connection failures.

Setup:

Ensure that Alpakka PR has been published locally, and if necessary, the build.sbt version changed accordingly.

Test:

1) Start `eclipse-mosquitto` via Docker:

```bash
docker run -it -p 1883:1883 eclipse-mosquitto
```

2) Start `socat` to forward connections from port 1882 to 1883

```bash
socat TCP-LISTEN:1882,reuseaddr,fork  TCP:127.0.0.1:1883
```

3) Start `mosquitto_sub` to watch messages:

```bash
mosquitto_sub -v -h localhost -p 1883 -t '#'
```

4) In sbt, run `reStart`, which will connect to port 1882 and publish messages. The program will print each command it sends and each event it receives.

5) After observing successful publishes, stop `socat` via CTRL-C. Start it again

Observe:

After restarting `socat`, notice that the client reconnects successfully and receives a ConnAck. Also note that the output from `mosquitto_sub` picks up from where it left off, indicating no messages were lost and publishes continue to work.
