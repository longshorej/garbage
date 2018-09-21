# cq-debug

Test program to debug issues with DurableQueue/Chronicle Queue.

To test, use two terminals. Start the consumer in one and the producer in another.

The producer will emit elements periodically (100ms delay between each). The consumer will print each item as it receives them.

## Producer

```>sbt
set mainClass in reStart := Some("Producer")
reStart
```

## Consumer

```>sbt
set mainClass in reStart := Some("Consumer")
reStart
```
