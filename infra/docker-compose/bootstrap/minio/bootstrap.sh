#!/bin/sh
# Create default buckets and a service-account for document-service.
set -eu

mc alias set local http://minio:9000 minioadmin minioadmin

mc mb --ignore-existing local/tmpmgmt-documents
mc mb --ignore-existing local/tmpmgmt-templates-attachments

# Optional: versioning on the documents bucket (enables immutable-once-written semantics).
mc version enable local/tmpmgmt-documents

echo "MinIO bootstrap complete."
