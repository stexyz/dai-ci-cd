docker pull minio/minio
docker run -p 9000:9000 \
  -v "/tmp/minio/data":"/data" \
  -v "/tmp/minio/config":"/root/.minio" \
  -e "MINIO_ACCESS_KEY=accesskey" \
  -e "MINIO_SECRET_KEY=secretkey" \
  --name minio \
  minio/minio server /data