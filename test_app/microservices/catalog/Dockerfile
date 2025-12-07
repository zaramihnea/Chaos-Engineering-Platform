FROM python:3.11-slim
WORKDIR /app
RUN pip install --no-cache-dir flask prometheus_client python-dotenv pg8000
COPY ./common /app/common
COPY ./observability /app/observability
COPY ./aop /app/aop
COPY ./microservices/base_service.py /app/microservices/base_service.py
COPY ./microservices/catalog /app/microservices/catalog
ENV PYTHONPATH=/app
EXPOSE 5001
CMD ["python", "-m", "microservices.catalog.app"]