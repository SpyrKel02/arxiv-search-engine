# Scientific Publications Search Engine (arXiv Search Engine)

## Technologies & Prerequisites
* **Programming Language:** Java 21
* **Indexing & Search Engine:** Apache Lucene 10.x
* **PDF Text Extraction:** Apache PDFBox 3.x
* **JSON Serialization:** Google Gson
* **Build System:** Apache Maven

## System Architecture
The system consists of 5 standalone applications:
1. **App 1 – Store Module**: Extracts plain text from 10,000 arXiv PDF papers and stores them sequentially in chunk files of up to 128 MB (`Repository`) in JSON Lines format.
2. **App 2 – Text Preprocessor & Lucene Indexer**: Reads documents from the Repository, performs text preprocessing, and builds the inverted index (`Inverted Index`) using Apache Lucene.
3. **App 3 – Query Server**: Server process listening on a TCP socket that receives user queries and returns search results from the index.
4. **App 4 – Query Client**: Client process that sends JSON queries to the server and prints the retrieved results.
5. **App 5 – Search Interface**: Search user interface for submitting queries and displaying results.

## Project Structure
```text
arxiv-search-engine/
├── pom.xml
├── data/                          # Ignored by Git (.gitignore)
│   ├── input_pdfs/                # The 10,000 original arXiv PDF papers
│   ├── repository/                # Repository files (chunks up to 128 MB)
│   └── lucene_index/              # Lucene Inverted Index
└── src/main/java/
    ├── common/                    # Shared data models (e.g., RepoDocument)
    ├── app1_store/                # App 1: Store Module
    ├── app2_preprocessor_indexer/ # App 2: Indexer
    ├── app3_query_server/         # App 3: Query Server
    └── app4_query_client/         # App 4: Query Client
