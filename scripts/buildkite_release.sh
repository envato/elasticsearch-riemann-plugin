#!/bin/bash

# hackity hack hack hack!  Move this to the AMI
javac -version 2>/dev/null || sudo apt-get install -y openjdk-7-jdk

./mvnw clean package

echo "Assuming IAM role to access discovery-configuration S3 bucket"
eval $(aws sts assume-role \
  --role-arn arn:aws:iam::932328744288:role/pushToS3Role \
  --role-session-name buildkite=pushToS3Role \
  --output text \
  | tr '\n' ' ' \
  | awk '{printf("export AWS_ACCESS_KEY_ID=\"%s\"\nexport AWS_SECRET_ACCESS_KEY=\"%s\"\nexport AWS_SESSION_TOKEN=\"%s\"\n",$5,$7,$8)}')

PLUGIN_LOCATION="s3://discovery-configuration/config/elasticsearch/plugins/"

# Move existing gems into the old/ dir
aws s3 mv "${PLUGIN_LOCATION}" "${PLUGIN_LOCATION}old/" --recursive --exclude "old"

# Upload the newly-built artifact
aws s3 cp target/releases/elasticsearch-riemann-*.zip "${PLUGIN_LOCATION}"
