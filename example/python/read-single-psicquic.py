import urllib2
import sys

# ------------------ MITAB FUNCTIONS ------------------

def getXrefByDatabase(line, database):
   fields = line.split('|')

   for field in fields:
       parts = field.split(':')

       db = parts[0]
       value = parts[1].split('(')[0]

       if database == db:
           return value

# -----------------------------------------------------
# Note that we are only going to get 10 interactions at most
queryUrl = "http://www.ebi.ac.uk/Tools/webservices/psicquic/intact/webservices/current/search/query/BBC1?firstResult=0&maxResults=10";

try:
    fileHandle = urllib2.urlopen(queryUrl)
    content = fileHandle.read()
    fileHandle.close()
except IOError:
    print 'Cannot open URL' % urlStr
    content = ''

lines = content.splitlines()

for line in lines:
    cols = line.split('\t')

    print getXrefByDatabase(cols[0], 'uniprotkb') + ' interacts with ' + getXrefByDatabase(cols[1], 'uniprotkb')

