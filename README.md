# NewsArticleSearchEngine
IR Group Project

## Usage
```
java -jar target/LuceneIntro-1.0.jar custom bm25
```

## Project Structure

For the indexing to work, topics should be inside a directory called resources. The dataset should be inside a directory called newsarticles inside resources. The topics file should be inside a folder called topics in resources.
I.e:

```
+-- src/main/java
|   +-- *source code files*
+-- resources
|   +-- topics
|       +-- topics
|   +-- newsarticles
|   |   +-- Assignment Two
|   |   |   +-- dtds
|   |   |   +-- fbis
|   |   |   +-- fr94
|   |   |   +-- ft
|   |   |   +-- latimes
|   |   +-- _MACOSX
|   |   |   +-- Assignment Two
|   |   |   |   +-- dtds
|   |   |   |   +-- fbis
|   |   |   |   +-- fr94
|   |   |   |   +-- ft
|   |   |   |   +-- latimes
```