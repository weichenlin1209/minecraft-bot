import os
import logging
from dotenv import load_dotenv
from huggingface_hub import login

from langchain_community.vectorstores import FAISS
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import (
  TextLoader,
  PyPDFLoader,
  UnstructuredWordDocumentLoader,
  BSHTMLLoader
)

from embedding import EmbeddingsGemmaEmbeddings


load_dotenv()
HUGGING_FACE_TOKEN = os.getenv('HUGGING_FACE_TOKEN')
login(token=HUGGING_FACE_TOKEN)

logging.basicConfig(level=logging.INFO,)
logging.info("It has logged in to HuggingFace.")

DATA_FOLDER = "./loaded_docs"
OUTPUT_PATH = "faiss_db"

documents = []
for file in os.listdir(DATA_FOLDER):
  path = os.path.join(DATA_FOLDER, file)
  print(f"Loading {file}...")
  if file.endswith(".txt"):
    loader = TextLoader(path, encoding="utf-8")
  elif file.endswith(".pdf"):
    loader = PyPDFLoader(path)
  elif file.endswith(".docx"):
    loader = UnstructuredWordDocumentLoader(path)
  elif file.endswith(".html") or file.endswith(".htm"):
        loader = BSHTMLLoader(path)
  else:
    continue
  documents.extend(loader.load())

chunk_size = 300
chunk_overlap = 50

splitter = RecursiveCharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
split_docs = splitter.split_documents(documents)
logging.info(f"Split {len(split_docs)} documents into {chunk_size} chunks.")

embedding_model = EmbeddingsGemmaEmbeddings()
vectorstore = FAISS.from_documents(split_docs, embedding_model)
vectorstore.save_local(OUTPUT_PATH)