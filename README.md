# SE 314 — Lab 07: Test-First Programming II (Social Network Analysis)

Implementation of **Problem 3** from [MIT 6.005 Problem Set 1](https://ocw.mit.edu/ans7870/6/6.005/s16/psets/ps1/):
inferring a social network from a list of tweets.

## Contents

| Path | Purpose |
| --- | --- |
| `src/twitter/SocialNetwork.java` | `guessFollowsGraph()` and `influencers()` |
| `src/twitter/Extract.java` | `@`-mention and timespan extraction used by the graph |
| `src/twitter/Filter.java` | Tweet filters from the PS1 toolbox |
| `test/twitter/SocialNetworkTest.java` | JUnit 4 tests and partition-based testing strategy |
| `src/twitter/Main.java` | Prints the top 10 most-followed users from a live sample |

## Build and test

```sh
javac -d bin -cp "lib/*" src/twitter/*.java test/twitter/*.java
java -ea -cp "bin:lib/*" org.junit.runner.JUnitCore twitter.SocialNetworkTest
```

## Run the sample

```sh
java -ea -cp "bin:lib/*" twitter.Main
```

`Main` reads the static tweet sample published with the assignment at
`https://courses.csail.mit.edu/6.005/ps1_tweets/tweets.json` (the original
`tweetPoll.py` endpoint now serves its own source instead of JSON).
