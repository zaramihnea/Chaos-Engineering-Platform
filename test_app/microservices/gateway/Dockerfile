FROM python:3.11-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir flask prometheus_client python-dotenv pg8000
COPY ./common /app/common
COPY ./observability /app/observability
COPY ./aop /app/aop
COPY ./microservices/base_service.py /app/microservices/base_service.py
COPY ./microservices/gateway /app/microservices/gateway
COPY ./microservices/catalog /app/microservices/catalog
COPY ./microservices/cart /app/microservices/cart
COPY ./microservices/payment /app/microservices/payment
ENV PYTHONPATH=/app
EXPOSE 5000
CMD ["python", "-m", "microservices.gateway.app"]