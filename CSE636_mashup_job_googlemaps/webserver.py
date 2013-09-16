#Copyright Jon Berg , turtlemeat.com

import string,cgi,time
from os import curdir, sep
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import urlparse
from urlparse import urlparse, parse_qsl
from indeed import IndeedApi
import xml.etree.ElementTree as ET
#import pri
#http://fragments.turtlemeat.com/pythonwebserver.php
class MyHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        try:
            print self.path
            if self.path.endswith("/"):
                print "end with /"
                f = open(curdir + sep + "first.html") #self.path has /test.html
                #f = open(curdir + sep + "downloadurl.htm")
#note that this potentially makes every file on your computer readable by the internet

                self.send_response(200)
                self.send_header('Content-type',	'text/html')
                self.end_headers()
                self.wfile.write(f.read())
                f.close()
                return
            if self.path.startswith("/lookup"):
                print "startswith lookup"
                lista=urlparse.parse_qsl(self.path.startswith)
                ds=dict(lista)
                print ds
                f = open(curdir + sep + self.path) #self.path has /test.html
#note that this potentially makes every file on your computer readable by the internet

                self.send_response(200)
                self.send_header('Content-type',	'text/html')
                self.end_headers()
                self.wfile.write(f.read())
                f.close()
                return
            if self.path.endswith(".css"):
                print ".css"
                lista=urlparse.parse_qsl(self.path.startswith)
                ds=dict(lista)
                print ds
                f = open(curdir + sep + "default.css") #self.path has /test.html
#note that this potentially makes every file on your computer readable by the internet

                self.send_response(200)
                self.send_header('Content-type',	'text/css')
                self.end_headers()
                self.wfile.write(f.read())
                f.close()
                return
            if self.path.endswith(".js"):
                print ".js"
                f = open(curdir + sep + self.path) #self.path has /test.html
#note that this potentially makes every file on your computer readable by the internet

                self.send_response(200)
                self.send_header('Content-type',	'text/javascript')
                self.end_headers()
                self.wfile.write(f.read())
                f.close()
                return
            if self.path.endswith(".xml"):
                print ".xml"
                f = open(curdir + sep + self.path) #self.path has /test.html
#note that this potentially makes every file on your computer readable by the internet

                self.send_response(200)
                self.send_header('Content-type',	'text/xml')
                self.end_headers()
                self.wfile.write(f.read())
                f.close()
                return
            return
                
        except IOError:
            self.send_error(404,'File Not Found: %s' % self.path)
     

    def do_POST(self):
        global rootnode
        try:
            print "do_post"
            ctype, pdict = cgi.parse_header(self.headers.getheader('content-type'))
            print pdict
            if ctype == 'multipart/form-data':
                print "ctype is multipart/form-data"
                query=cgi.parse_multipart(self.rfile, pdict)
                print query
            self.send_response(200)
            
            self.end_headers()
            job = query.get('job')
            print job[0]
            location = query.get('location')
            print location[0]
            self.askindeed(job[0],location[0])
            #print "filecontent", upfilecontent[0]
            #self.wfile.write("<HTML>POST OK.<BR><BR>");
            f = open(curdir + sep + "downloadurl.htm") #self.path has /test.html
            self.send_response(200)
            self.send_header('Content-type',	'text/html')
            self.end_headers()
            self.wfile.write(f.read())
            f.close()
            
        except :
            pass
    def askindeed(self,job,place):
        print "enter askindeed"
        token = '4141481415336653'
        api = IndeedApi(token)
        print "before indeed search"
        xml_results = api.search(job,place)
        print "xml results"
        print xml_results
        root = ET.fromstring(xml_results)

        for result in root.iter('result'):
            print result.find('jobtitle').text
            print result.find('formattedLocation').text
            print result.find('url').text
        return

def main():
    try:
        server = HTTPServer(('', 80), MyHandler)
        print 'started httpserver...'
        server.serve_forever()
    except KeyboardInterrupt:
        print '^C received, shutting down server'
        server.socket.close()

if __name__ == '__main__':
    main()

