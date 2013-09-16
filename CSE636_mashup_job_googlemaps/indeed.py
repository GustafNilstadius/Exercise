import urllib2
import json
from urllib import urlencode
import types
import StringIO


#https://github.com/fission6/python-indeed
#http://docs.python.org/2/library/xml.etree.elementtree.html#module-xml.etree.ElementTree
class IndeedApi(object):
    
    def __init__(self, publisher_id, **kwargs):
    
        self.publisher_id = publisher_id
        self.base_url = 'http://api.indeed.com/ads/'

    def search(self, query=None, location='US', country_code='us'):

        action = 'apisearch'
        query_params = {
            'q' : query,
            'l' : location,
            'co': country_code,
            'format' : 'xml',
            'v' : '2',
            'publisher' : self.publisher_id
        }

        query_params = dict([(k, v.encode('utf-8') if type(v) is types.UnicodeType else v) \
                             for (k, v) in query_params.items()])
        query_string = urlencode(query_params)
        service_req = '{0}{1}?{2}'.format(self.base_url, action, query_string)
        
        request = urllib2.urlopen(service_req)
        results = request.read()
        #data = json.loads(results)
        
        #return data
        a = open('result.xml', 'w')
        a.write(results)
        a.close()
        return results
    
    def job_details(self, job_keys):
        
        action = 'apigetjobs'
        query_params = {
            'jobkeys' : ','.join(job_keys),
            'format' : 'xml',
            'v' : '2',
            'publisher' : self.publisher_id
        }
        
        query_string = urlencode(query_params)
        service_req = '{0}{1}?{2}'.format(self.base_url, action, query_string)
        
        request = urllib2.urlopen(service_req)
        results = request.read()
        #data = json.loads(results)
        
        return data
     

#print "json_details"
#json_details = api.job_details([json_results['results'][0]['jobkey']])

#print json_details
