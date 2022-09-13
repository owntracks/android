#!/bin/bash
echo "Commit Sha: $GITHUB_SHA"
PIPELINE_OUTPUT=$(curl -s -u ${CIRCLE_CI_TOKEN}: https://circleci.com/api/v2/project/gh/owntracks/android/pipeline)
echo "Found $(echo $PIPELINE_OUTPUT | jq '.items | length') pipelines"
MATCHING_PIPELINE_ID=$(echo $PIPELINE_OUTPUT | jq -r '.items[] | select(.vcs.revision == env.GITHUB_SHA and .vcs.branch == "master") |.id')
echo "Pipeline ID that matches git rev $GITHUB_SHA is $MATCHING_PIPELINE"
if [ -z "$MATCHING_PIPELINE_ID" ]; then exit 1; fi

WORKFLOW_OUTPUT=$(curl -s -u ${CIRCLE_CI_TOKEN}: https://circleci.com/api/v2/pipeline/$MATCHING_PIPELINE_ID/workflow)
WORKFLOW_ID=$(echo $WORKFLOW_OUTPUT | jq -r '.items[] | select(.name=="build-and-test" and .status=="success") |.id')
echo "This pipeline has $(echo $WORKFLOW_OUTPUT | jq '.items | length') workflows"
echo "Workflow ID that matches 'build-and-test' is $WORKFLOW_ID"
if [ -z "$WORKFLOW_ID" ]; then exit 1; fi

JOB_OUTPUT=$(curl -s -u ${CIRCLE_CI_TOKEN}: https://circleci.com/api/v2/workflow/$WORKFLOW_ID/job)
echo "This workflow has $(echo $JOB_OUTPUT | jq '.items | length') jobs"
JOB_ID=$(echo $JOB_OUTPUT | jq -r '.items[] | select (.name == "publish-to-play-store" and .status=="success") | .id')
echo "Job ID that matches 'publish-to-play-store' is $JOB_ID"
if [ -z "$JOB_ID" ]; then exit 1; fi

MINIMUM_SIZE_KB_CHECK=1000

echo "Fetching OSS APK"
curl -s -L -o oss.apk https://output.circle-artifacts.com/output/job/$JOB_ID/artifacts/0/oss-apk
du -h oss.apk
OSS_SIZE_KB=$(du -k oss.apk | cut -f1)
if [ "$OSS_SIZE_KB" -lt "$MINIMUM_SIZE_KB_CHECK" ]; then exit 1; fi
echo "Fetching GMS APK"
curl -s -L -o gms.apk https://output.circle-artifacts.com/output/job/$JOB_ID/artifacts/0/gms-apk
du -h gms.apk
GMS_SIZE_KB=$(du -k oss.apk | cut -f1)
if [ "$GMS_SIZE_KB" -lt "$MINIMUM_SIZE_KB_CHECK" ]; then exit 1; fi
