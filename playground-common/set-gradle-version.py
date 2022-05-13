#! /usr/bin/env python3
# Script to update gradle version, used in CI to test new gradle versions
# Must be called with one of values from https://services.gradle.org/versions
# e.g. ./set-gradle-version.py release-nightly
import fileinput
import json
import os
import pathlib
import sys
import urllib.request

if len(sys.argv) != 2:
    raise SystemExit('Must invoke with a gradle version like current, nightly etc')
version=sys.argv[1]
properties_file=os.path.join(pathlib.Path(__file__).parent.resolve(), "gradle/wrapper/gradle-wrapper.properties")
print("Will set version to", version, "in ", properties_file)
with urllib.request.urlopen("https://services.gradle.org/versions/{}".format(version)) as url:
    data = json.loads(url.read().decode())
    downloadUrl=data['downloadUrl']
    checksum=urllib.request.urlopen(data['checksumUrl']).read().decode('ascii')
    print("download url:", downloadUrl)
    print("checksum:", checksum)
    for line in fileinput.input(properties_file, inplace=True):
        if line.startswith("distributionUrl"):
            print('distributionUrl={}'.format(downloadUrl.replace(":", "\:")))
        elif line.startswith("distributionSha256Sum"):
            print('distributionSha256Sum={}'.format(checksum))
        else:
            print(line, end='')