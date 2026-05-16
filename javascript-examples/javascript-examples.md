# SEDA-like implementation in JavaScript

> Author note: On a daily basis I use Java and Kotlin

In JavaScript, it's important not to `block` the event loop with one huge task.
Instead, a huge task can be `chunked` into smaller steps, letting the event loop
breathe in between. This is a SEDA-like approach to concurrency.

From a logical perspective, many huge tasks can execute at the same time -
so it's good to limit execution to `1 task at a time`. Basically, a Mutex. Without Mutex, event loop lag can be low, but from logical point of view, it's like doing many huge tasks at the same time :<
As a side effect, it's very easy to monitor `queue wait time` this way!

Check the directories:
- [processingDefault](processingDefault)
- [processingSEDA](processingSEDA)

```shell
node index.js
```