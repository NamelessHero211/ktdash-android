"""Compare upstream v2 HEAD with our latest Android release marker.
Upstream content is metadata only and is never executed.
"""
import json
import os
import urllib.error
import urllib.request


def get(path):
    request = urllib.request.Request(
        "https://api.github.com/" + path,
        headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"],
                 "Accept": "application/vnd.github+json", "User-Agent": "KTDash-Android-CI"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


sha = get("repos/vjosset/ktdash/commits/v2")["sha"]
if len(sha) != 40 or any(c not in "0123456789abcdef" for c in sha):
    raise ValueError("Invalid upstream commit SHA")
try:
    latest = get("repos/" + os.environ["GITHUB_REPOSITORY"] + "/releases/latest")
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise
    latest = {}
changed = "<!-- upstream:" + sha + " -->" not in latest.get("body", "")
build = changed or os.environ["GITHUB_EVENT_NAME"] != "schedule"
with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
    output.write("sha=" + sha + "\n")
    output.write("build=" + str(build).lower() + "\n")
print("Upstream commit:", sha, "Build needed:", build)
