import urllib2
import sys
from xml.dom.ext.reader import Sax2
from xml.dom.NodeFilter import NodeFilter
from xml import xpath
# ------------------ FUNCTIONS ------------------

class PsicquicService:
    def __init__(self, name, restUrl):
        self.name = name
        self.restUrl = restUrl

def readURL(url):
    try:
        fileHandle = urllib2.urlopen(url)
        content = fileHandle.read()
        fileHandle.close()
    except IOError:
        print 'Cannot open URL' % url
        content = ''

    return content


def readActiveServicesFromRegistry():
    registryActiveUrl = 'http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry?action=ACTIVE&format=xml'

    content = readURL(registryActiveUrl)

    reader = Sax2.Reader()
    doc = reader.fromString(content)

    serviceNodes = xpath.Evaluate('service', doc.documentElement)

    services = []

    for serviceNode in serviceNodes:
        name = serviceNode.getElementsByTagName('name')[0].firstChild.nodeValue
        restUrl = serviceNode.getElementsByTagName('restUrl')[0].firstChild.nodeValue

        service = PsicquicService(name, restUrl)
        services.append(service)

    return services


def getXrefByDatabase(line, database):
   fields = line.split('|')

   for field in fields:
       parts = field.split(':')

       db = parts[0]
       value = parts[1].split('(')[0]

       if database == db:
           return value

   else:
    # if no db found, return the first field
        return fields[0]


def queryPsicquic(psicquicRestUrl, query, offset, maxResults):
    psicquicUrl = psicquicRestUrl + 'query/' + query + '?firstResult=' + str(offset) + '&maxResults=' + str(maxResults);

    print '\t\tURL: ' + psicquicUrl

    psicquicResultLines = readURL(psicquicUrl).splitlines()

    for line in psicquicResultLines:
        cols = line.split('\t')

        print '\t' + getXrefByDatabase(cols[0], 'uniprotkb') + ' interacts with ' + getXrefByDatabase(cols[1], 'uniprotkb')

# -----------------------------------------------------

query = 'BBC1'


services = readActiveServicesFromRegistry()

for service in services:
    print 'Service: ' + service.name + ' ================================================================== '

    queryPsicquic(service.restUrl, query, 0, 200)

    print '\n'

