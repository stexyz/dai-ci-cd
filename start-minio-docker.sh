docker pull minio/minio
docker run -p 9000:9000 \
  -v "`pwd`/minio/data":"/data" \
  -v "`pwd`/minio/config":"/root/.minio" \
  -e "MINIO_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE" \
  -e "MINIO_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY" \
  --name minio \
  minio/minio server /data