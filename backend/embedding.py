from  langchain_community.embeddings import HuggingFaceEmbeddings

class EmbeddingsGemmaEmbeddings(HuggingFaceEmbeddings):
  def __init__(self, **kwargs):
    super().__init__(
      model_name="google/embeddinggemma-300m",
      encode_kwargs={"normalize_embeddings": True},
      **kwargs
    )

  def embed_documents(self, texts: list[str]) -> list[list[float]]:
    proccessed_texts = [f"title: none | text: {t}" for t in texts]
    return super().embed_documents(proccessed_texts)
  
  def embed_query(self, text: str) -> list[float]:
    quer_text = f"title: none | text: {text}"
    return super().embed_query(quer_text)