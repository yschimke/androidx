# Script to update gradle version
import urllib.request, json, fileinput, sys,pathlib, os
if len(sys.argv) != 2:
    raise SystemExit('Must invoke with a gradle version like current, nightly etc')
version=sys.argv[1]
properties_file=os.path.join(pathlib.Path(__file__).parent.resolve(), "gradle/wrapper/gradle-wrapper.properties")
print("Will set version to", version, "in ", properties_file)
with urllib.request.urlopen("https://services.gradle.org/versions/{}".format(version)) as url:
    data = json.loads(url.read().decode())
    downloadUrl=data['downloadUrl']
    print(downloadUrl)
    for line in fileinput.input(properties_file, inplace=True):
        if line.startswith("distributionUrl"):
            print('distributionUrl={}'.format(downloadUrl.replace(":", "\:")))
        else:
            print(line, end='')
        
            