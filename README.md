# Plugin Analyzer
A Kotlin CLI tool to analyze Jetbrains plugin artifacts (ZIP/JAR), extract metadata, dependencies, and class symbols, and generate structured JSON reports.


## TASK:
`Build a small CLI tool that takes a plugin artifact (ZIP or JAR), parses its internal structure (zip entries should be enough) and saves to an output file. It should be possible to compare two output files for similarities somehow.`

## RESULTS:


### Example 1 - comparing two different AWS plugins for Intelij

We evaluate plugin similarity using two complementary metrics: cosine similarity over embedding vectors and SuperMinHash with Jaccard distance.
In our experiments, two AWS-related plugins — one for deploying Lambdas and another for visualizing/deploying multiple AWS services — showed a large discrepancy: the embedding-based metric predicted ~69% similarity, while SuperMinHash gave only ~13%.
This is because SuperMinHash relies on exact token matches, and the plugins have very different package names, method names, and file structures. In contrast, embeddings capture semantic similarity between conceptually related terms, producing a more meaningful estimate in this case.
Therefore, a hybrid approach that combines both metrics could provide the most accurate assessment of plugin similarity.

<img width="1070" height="148" alt="image" src="https://github.com/user-attachments/assets/590aaa39-c11b-4587-a863-01fd31225f31" />

### Example 2 - comparing two same AWS plugins but different versions (this is the case when we want to find near duplicate plugins)

We expect high scores as this is the same plugin but only different versions (simulating duplicate plugins)

<img width="1001" height="145" alt="image" src="https://github.com/user-attachments/assets/b084bee2-91ff-45d0-a1aa-6570df81ada6" />



### Example 3 - comparing two completely different plugins first `AWS-lambda-deployer` and second `Intelij-rainbow-brackets` 

Logical expectation is for both metric to be low and that is the exact value we got!

<img width="1158" height="157" alt="image" src="https://github.com/user-attachments/assets/a07ab3fd-72c3-404a-bcf6-cbf5f737f2d0" />

### Example 4 - Comparing two different plugins once again

Low scores as expected.

<img width="1135" height="147" alt="image" src="https://github.com/user-attachments/assets/aa49467f-6a95-4411-841c-f30f5bc58cb6" />


## METRICS

### 1. Cosine similarity with word embeddings

We calculate cosine similarity for each of the three vectors and then calculate result with this formula: 
```bash
KEYWORD_WEIGHT * keywordSimilarity + DESCRIPTION_WEIGHT * description + METHOD_WEIGHT * methodSimilarity
```

<img width="396" height="194" alt="image" src="https://github.com/user-attachments/assets/07228390-1976-456e-9a03-fa2be0975c52" />


### 2. SuperMinHash with Jaccard distance

First we tokenize both json files and place into SuperMinHash and calculate signature for each file. Next fraction is calculated and placed into Jaccard distance formula:

```bash
(fraction - 2.0.pow(-bitsPerComponent)) / (1.0 - 2.0.pow(-bitsPerComponent))
```




## Features

- Parse plugin ZIP or JAR files.
- Extract `plugin.xml` metadata (name, version, dependencies, description).
- Extract class symbols and method names from `.class` files.
- Generate structured JSON output for further processing or comparison.
- Embedding all relevant keywords and using cosine similarity for comparison.
- Tokenizing both json files and feeding SuperMinHash and calculating distance

## Installation

Clone the repository:

```bash
git clone https://github.com/milanlazarevic/JetBrains-Plugin-DNA.git
cd JetBrains-DNA-Project
```

Build the project with Gradle:
```bash
./gradlew build
```

## Setup & Folder Structure
```
project-root/
├── in/          # Place plugin ZIP/JAR files here
├── out/         # JSON output for each plugin
      └── embedding/   # Generated embeddings for comparison
```

Folder structure looks like this:
<img width="418" height="447" alt="image" src="https://github.com/user-attachments/assets/3df2adef-d65a-49da-9f37-3400d5b3617e" />

You can add new zipped plugins into `in` package and the result of processing will appear in out package (one file will be in root `out` package that has all relevant information about plugin and other will be in `embedding` - that will be used for comparing alter on.)


Next you will have to add new run configurations. One for parsing the plugin and the other for comparing two plugins.


### 1. Parse one plugin
You have to add `extract` as a first param and the other two parameters are filepaths. First is path to the input zip and the other is the file path where the output file will be placed (inside `out` package)

```bash
extract
"src/main/resources/in/aws-toolkit-3.97.252.zip"
"src/main/resources/out/aws-toolkit-3.97.252.json"
```

<img width="1093" height="868" alt="image" src="https://github.com/user-attachments/assets/fae849f0-63f5-4db2-a795-5a95816e89cb" />


### 2. Compare two plugins
First parameter is `compare` keyword and second and third are file paths to the outputed files parsed by our `CLI tool` into vectors.

```bash
compare
"src/main/resources/out/embedding/aws-lambda-deployer-2025.2.json"
"src/main/resources/out/embedding/intellij-rainbow-brackets-2025.3.5.json"
```

<img width="1074" height="876" alt="image" src="https://github.com/user-attachments/assets/d66faf67-b990-41d7-b19f-917485c1246a" />


## How it Works

- The tool reads a plugin ZIP or JAR file.
- It extracts metadata from plugin.xml.
- It scans .class files to extract method names and packages.
- Keywords are extracted and embedded for comparison.
- The plugin data is serialized into JSON.
- Plugins can then be compared using cosine similarity between embeddings.

















