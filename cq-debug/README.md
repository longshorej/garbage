# cq-debug

Test program to debug issues with DurableQueue/Chronicle Queue.

To test, use three terminals. Start the producer, transformer, and consumer. Additionally, consider using Docker for one or more of the programs to test bind mounts.

The producer will emit elements to topic "A" periodically (100ms delay between each). The transformer will tail topic "A" and emit the items to topic "B." The consumer will tail topic "B" and print each item as it receives them.

**Producer**

```>sbt
set mainClass in reStart := Some("Producer")
reStart
```

**Transformer**

```>sbt
set mainClass in reStart := Some("Transformer")
reStart
```

or in Docker:

```sbt docker:publishLocal```

```bash
chmod -R 777 /tmp/cq-debug
sbt docker:publishLocal
docker run --entrypoint bin/transformer -v /tmp/cq-debug:/tmp/cq-debug --rm -t -i cq-debug:0.1.0-SNAPSHOT
```

**Consumer**

```>sbt
set mainClass in reStart := Some("Consumer")
reStart
```


**Transformer (Docker)**

```>sbt
set mainClass in 
```