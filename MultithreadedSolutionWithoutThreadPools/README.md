# Multithreaded Solution (Without Thread Pools)

In this approach, multiple threads are explicitly created and managed to process different
parts of the data concurrently. A common strategy like the producer-consumer pattern
can be employed for work distribution.
Manually managing threads provides fine-grained control over concurrency but introduces complexity in thread coordination and resource management. This approach helps
in understanding the challenges of thread synchronization and data sharing.