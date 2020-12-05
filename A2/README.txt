Update::
SERVER
java httpfs -v -p 3453 -d 'C:\Users\mital\git\ComputerNetworks\ServerDirectory'

Client
GET:
java httpc get -v http://localhost:3453/ or java httpc get -v http://localhost:3453/GET/
httpc get -v http://localhost:3453/GET/testfile3.txt --FOUND
java httpc get -v http://localhost:3453/GET/foo.txt --NOT FOUND

POST:
java httpc post -v http://localhost:3453/foo1.txt -d 'network' or java httpc post -v http://localhost:3453/POST/foo1.txt -d 'network'
java httpc post -v http://localhost:3453/POST/foo1.txt?overwrite=false -d 'network append' -- append 
java httpc post -v http://localhost:3453/POST/foo1.txt?overwrite=true -d 'network overwrite' -- overwrite

SECURITY:
java httpc post -v http://localhost:3453/POST/sub/foo1.txt -d 'network' --Denied
java httpc post -v http://localhost:3453/POST/../foo1.txt -d 'network' --Denied

MULTISUPPORT:
both READ : java httpc get -v http://localhost:3453/GET/foo1.txt -- allowed
both write : java httpc post -v http://localhost:3453/POST/foom.txt -d 'network' -- Message displayed
first write then read - Message displayed
first read then write - Message displayed

Content type/disposition:
java httpc get -v http://localhost:3453/GET/foom.xml 