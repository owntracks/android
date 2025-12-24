#!/bin/bash

if [[ -z $POEDITOR_API_KEY ]]; then
    echo "Set POEDITOR_API_KEY first"
    exit 1
fi

APP_ID=419041

for lang in $(curl -s -X POST https://api.poeditor.com/v2/languages/list \
    -d api_token="${POEDITOR_API_KEY}" \
    -d id=${APP_ID} | jq -r .result.languages[].code); do
    if [[ $lang == "zh-CN" ]]; then
        locale="zh"
    elif [[ $lang == "en-gb" ]]; then
        locale="en-rGB"
    elif [[ $lang == "id" ]]; then # Indonesian
        locale="in"
    elif [[ $lang == "he" ]]; then # Hebrew
        locale="iw"
    else
        locale=$lang
    fi


    if [[ $lang == "en" || ! -f project/app/src/main/res/values-"${locale}"/strings.xml ]]; then
        echo "Skipping $lang because $locale doesn't exist"
        continue
    fi
    echo "Fetching $lang into $locale"

    URL=$(curl -s -X POST https://api.poeditor.com/v2/projects/export \
        -d api_token="${POEDITOR_API_KEY}" \
        -d id="${APP_ID}" \
        -d language="${lang}" \
        -d order="terms" \
        -d filters=translated \
        -d type="android_strings" \
        -d options="[{\"unquoted\":0}]" | jq -r .result.url)

    curl -s -o project/app/src/main/res/values-"${locale}"/strings.xml "${URL}"
    sed -i '$a\' project/app/src/main/res/values-"${locale}"/strings.xml
done
