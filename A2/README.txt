SERVER
java httpfs -v
java httpfs -v -p 3453
java httpfs -v -d 'C:\Users\mital\git\ComputerNetworks\ServerDirectory'


CLIENT
java httpc get http://localhost:8080/
java httpc get http://localhost:8080/GET/
java httpc get http://localhost:8080/GET/foo.txt

java httpc post http://localhost:8080/foo1.txt -d 'network'
java httpc post http://localhost:8080/POST/foo.txt -d 'network'

SECURITY
java httpc post http://localhost:8080/POST/../foo.txt -- NOT ALLOWED
java httpc post http://localhost:8080/POST/assignment/foo.txt -- NOT ALLOWED

OVERWRITE
java httpc post http://localhost:8080/POST/foo1.txt?overwrite=true -d 'overwritten done' 