# Plugin Analyzer
A Kotlin CLI tool to analyze Jetbrains plugin artifacts (ZIP/JAR), extract metadata, dependencies, and class symbols, and generate structured JSON reports.


## TASK:
`Build a small CLI tool that takes a plugin artifact (ZIP or JAR), parses its internal structure (zip entries should be enough) and saves to an output file. It should be possible to compare two output files for similarities somehow.`

## Features

- Parse plugin ZIP or JAR files.
- Extract `plugin.xml` metadata (name, version, dependencies, description).
- Extract class symbols and method names from `.class` files.
- Generate structured JSON output for further processing or comparison.
- Embedding all relevant keywords and using cosine similarity for comparison.

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

<img width="1093" height="868" alt="image" src="https://github.com/user-attachments/assets/fae849f0-63f5-4db2-a795-5a95816e89cb" />


### 2. Compare two plugins
First parameter is `compare` keyword and second and third are file paths to the outputed files parsed by our `CLI tool` into vectors.

<img width="1074" height="876" alt="image" src="https://github.com/user-attachments/assets/d66faf67-b990-41d7-b19f-917485c1246a" />


## How it Works

- The tool reads a plugin ZIP or JAR file.
- It extracts metadata from plugin.xml.
- It scans .class files to extract method names and packages.
- Keywords are extracted and embedded for comparison.
- The plugin data is serialized into JSON.
- Plugins can then be compared using cosine similarity between embeddings.

















