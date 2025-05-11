# Sequential Solution

The sequential solution serves as a baseline for performance comparison. It processes
the input le sequentially, counts the occurrences of each word, and identies the most
common words. This implementation helps in understanding the limitations of sequential
processing in handling large data volumes on multicore systems.
Sequential processing is straightforward but doesn't utilize the available cores in a
multicore system, leading to suboptimal performance. Establishing a baseline allows us
to quantify the benets of concurrent approaches.


### How to test this solution:

- In the file WordCount.java define both of the following variables
    - maxPages = 100; (for example)
    - fileName = "C:/Directory/to/enwiki.xml"

- After this just run the main function and you should see the results in the console.