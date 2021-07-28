#!/bin/sh

echo "Running trivy check"
echo "$(getent hosts host.docker.internal | awk '{ print $1 }') trivy.ci" >> /etc/hosts && \
trivy client --severity CRITICAL --remote http://trivy.ci:8083 gcr.io/${REPOSITORY}:${TAG}
