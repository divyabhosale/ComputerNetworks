Help:
java httpc
java httpc help
java httpc help get
java httpc help post

Get:
java httpc get http://httpbin.org/get
java httpc get 'http://httpbin.org/get?course=networking&assignment=1'
java httpc get -h ContentType:json 'http://httpbin.org/get?course=networking&assignment=1'
java httpc get -v -h ContentType:json 'http://httpbin.org/get?course=networking&assignment=1'

Post:
java httpc post http://httpbin.org/post
java httpc post 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -h ContentType:json 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -h ContentType:json -h abc:def 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -d 'some data' 'http://httpbin.org/post?course=networking&assignment=1' --PLAIN TEXT
java httpc post -h Content-Type:application/json -d '{"Assignment": 1}' http://httpbin.org/post --JSON
java httpc post -d 'some data' -f 'inputFile.txt' 'http://httpbin.org/post?course=networking&assignment=1' --ERROR
java httpc post -d 'some data' -o 'hello.txt' 'http://httpbin.org/post?course=networking&assignment=1' --OPTIONAL 2
java httpc post -f 'inputFile.txt' 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -h 123:123 -d '{"Assignment": 1}' http://httpbin.org/post

Redirect:
java httpc get -v 'http://google.com/'