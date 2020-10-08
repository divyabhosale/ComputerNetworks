java httpc help
java httpc help get
java httpc help post
java httpc get http://httpbin.org/get
java httpc post http://httpbin.org/post
java httpc get 'http://httpbin.org/get?course=networking&assignment=1'
java httpc post 'http://httpbin.org/post?course=networking&assignment=1'
java httpc get -h ContentType:json 'http://httpbin.org/get?course=networking&assignment=1'
java httpc post -h ContentType:json 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -h ContentType:json -h abc:def 'http://httpbin.org/post?course=networking&assignment=1'
java httpc get -v -h ContentType:json 'http://httpbin.org/get?course=networking&assignment=1'
java httpc post -d 'some data' 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -f 'inputFile.txt' 'http://httpbin.org/post?course=networking&assignment=1'
java httpc post -d 'some data' -f 'inputFile.txt' 'http://httpbin.org/post?course=networking&assignment=1' --ERROR
java httpc post -d 'some data' -o 'hello.txt' 'http://httpbin.org/post?course=networking&assignment=1'
