"""Fetches the latest version code for the given track"""


from apiclient.discovery import build
import json
import os
from google.oauth2 import service_account
import jmespath
import sys

package_name = "org.owntracks.android"


def main():
    track = sys.argv[1]
    service_account_json = json.loads(os.environ["ANDROID_PUBLISHER_CREDENTIALS"])
    credentials = service_account.Credentials.from_service_account_info(
        service_account_json
    )
    scoped_credentials = credentials.with_scopes(
        ["https://www.googleapis.com/auth/androidpublisher"]
    )

    service = build("androidpublisher", "v3", credentials=scoped_credentials)
    edit_request = service.edits().insert(body={}, packageName=package_name).execute()
    edit_id = edit_request["id"]

    response = (
        service.edits()
        .tracks()
        .list(editId=edit_id, packageName=package_name)
        .execute()
    )

    version_code = jmespath.search(
        f"tracks[?track=='{track}'].releases | [] | [?status=='completed'] | [0].versionCodes | [0]",
        response,
    )
    print(version_code)

    service.edits().delete(editId=edit_id, packageName=package_name).execute()


if __name__ == "__main__":
    main()
